package com.example.documentation_20190713.Home;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.documentation_20190713.Bluetooth.SerialListener;
import com.example.documentation_20190713.Bluetooth.SerialService;
import com.example.documentation_20190713.Bluetooth.SerialSocket;
import com.example.documentation_20190713.R;
import com.example.documentation_20190713.Retrofit.Position;
import com.example.documentation_20190713.Retrofit.Pulse;
import com.example.documentation_20190713.Retrofit.RetrofitClient;
import com.example.documentation_20190713.Retrofit.Temperature;
import com.example.documentation_20190713.Security.SecurePreferences;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeasurementsFragment extends Fragment implements ServiceConnection, SerialListener {

    //for token access
    private String token;
    //connection status enum
    private enum Connected { False, Pending, True}
    //bluetooth device address
    private String deviceAddress = null;

    //stores device address for bluetooth
    SecurePreferences preferences;
    //for handling toolbar menu
    Menu menu = null;

    //variables for Bluetooth connection implementation
    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;

    //send data to webserver
    boolean sendToWebserver = true;
    //at first we are not connected
    private Connected connected = Connected.False;

    //received data from bluetooth (raw message)
    byte[] data;

    //chart initialization
    //pulse
    LineChart chartPulse;
    LineData dataPulse;
    TextView tvPulse;
    float pulse = 0.0f;
    //acc
    LineChart chartAcc;
    LineData dataAcc;
    TextView tvAccelerometer;
    float accX = 0.0f;
    float accY = 0.0f;
    float accZ = 0.0f;
    //temperature
    LineChart chartTmp;
    LineData dataTmp;
    TextView tvTmp;
    float tmp = 0.0f;
    //maximal number of data seen in charts
    final int MAX_CHART_ENTRIES = 500;
    boolean stop_chart = false;

    //text recognition
    private TextToSpeech textToSpeech;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private SpeechRecognizer speechRecognizer;


    //constructor
    public MeasurementsFragment() {
        dataPulse = new LineData();
        dataAcc = new LineData();
        dataTmp = new LineData();
    }

    //Life cycle of the fragment
    //called when fragment is created (called for the first time)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize preferences for token storage, the data is encrypted based on the secureKey.
        preferences = new SecurePreferences(getContext(), "user-info",
                "YourSecurityKey", true);
        this.token = preferences.getString("token");

        //initialize options menu
        setHasOptionsMenu(true);
        setRetainInstance(true);
        //give your device an address, as for now it is constant
        //in the future we have to retrieve the address from
        //user
        //deviceAddress = "B4:E6:2D:F6:C8:53";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_measurements, container, false);

        /*Charts*/
        //pulse
        chartPulse = view.findViewById(R.id.chart_pulse);
        dataPulse.setValueTextColor(Color.BLACK);
        chartPulse.setData(dataPulse);
        chartSettings(chartPulse);
        tvPulse = view.findViewById(R.id.tv_pulse);
        //acc
        chartAcc = view.findViewById(R.id.chart_accelerometer);
        dataAcc.setValueTextColor(Color.BLACK);
        chartAcc.setData(dataAcc);
        chartSettings(chartAcc);
        tvAccelerometer = view.findViewById(R.id.tv_accelerometer);
        //tmp
        chartTmp = view.findViewById(R.id.chart_temperature);
        dataTmp.setValueTextColor(Color.BLACK);
        chartTmp.setData(dataTmp);
        chartSettings(chartTmp);
        tvTmp = view.findViewById(R.id.tv_temperature);

        //text to speech initialization
        initializeTextToSpeech();
        initializeSpeechRecongizer();

        return view;
    }

    //called when fragment is destroyed
    @Override
    public void onDestroy() {
        //disconnect Bluetooth if the user is connected or pending
        if(connected != Connected.False) {
            disconnect();
        }
        //destroy service if application is destroyed
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }
    //called when fragment starts to work (after onCreate)
    @Override
    public void onStart() {
        super.onStart();
        //after initializing menu and setting the Bluetooth address of the device
        //we will connect to, we will initialize the service here if it is not
        //already running
        if(service != null) {
            service.attach(this);
        } else {
            // prevents service destroy on unbind from recreated
            // activity caused by orientation change
            getActivity().startService(new Intent(getActivity(), SerialService.class));
        }
    }

    //for toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_microphone) {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.RECORD_AUDIO)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO},MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                // Permission has already been granted
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
                speechRecognizer.startListening(intent);
            }
            return true;
        } else if(id == R.id.menu_bluetooth) {
            if(socket != null) {
                disconnect();
                getActivity().stopService(new Intent(getActivity(), SerialService.class));
            } else {
                connect();
                getActivity().startService(new Intent(getActivity(), SerialService.class));
            }
            return true;
        } else if(id == R.id.menu_internet) {
            if(sendToWebserver == true) {
                sendToWebserver = false;
                menu.findItem(R.id.menu_internet).setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_portable_wifi_off_white_24dp));
            } else {
                sendToWebserver = true;
                menu.findItem(R.id.menu_internet).setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_wifi_tethering_white_24dp));
            }
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }



    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.menu = menu;
    }

    //called before onDestroy
    @Override
    public void onStop() {
        //detach service
        if(service != null && !getActivity().isChangingConfigurations()) {
            service.detach();
        }
        super.onStop();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        getActivity().bindService(new Intent(getContext(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {

        }
        super.onDetach();
    }

    //app again in the foreground of the phone
    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
        stop_chart = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        stop_chart = true;
    }

    //Service Connection necessitates to import these functions
    //execute after launching the service
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }
    //execute when service shuts down
    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    //when connected to the device
    @Override
    public void onSerialConnect() {
        Toast.makeText(getContext(), "Successfully connected to the device", Toast.LENGTH_LONG).show();
        connected = Connected.True;
    }

    //when some connection error comes up
    @Override
    public void onSerialConnectError(Exception e) {
        Toast.makeText(getContext(), "Failed to connect to the device", Toast.LENGTH_LONG).show();
        disconnect();
    }

    //handle data coming from the device
    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    //do in case of lost connection
    @Override
    public void onSerialIoError(Exception e) {
        Toast.makeText(getContext(), "Connection lost: " + e.getMessage(), Toast.LENGTH_LONG).show();
        disconnect();
    }

    //connect to service and bluetooth
    private void connect() {
        deviceAddress = preferences.getString("device_address");
        if(deviceAddress != null) {
            try {
                //get bluetooth device by address
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                //get the name of the device
                String deviceName = device.getName() != null ? device.getName() : device.getAddress();
                Toast.makeText(getContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                connected = Connected.Pending;
                //initialize serial socket
                socket = new SerialSocket();
                //this name will be displayed in service notification
                service.connect(this, "Connected to " + deviceName);
                //inform the user that the app succeeded in connecting to device
                Toast.makeText(getContext(), "Connected to " + deviceName + " (" + deviceAddress + ")", Toast.LENGTH_SHORT).show();
                //change the icon in toolbar
                menu.findItem(R.id.menu_bluetooth).setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_bluetooth_white_24dp));
                socket.connect(getContext(), service, device);
            } catch (Exception e) {
                onSerialConnectError(e);
            }
        } else {
            Toast.makeText(getContext(), "deviceAddress is null", Toast.LENGTH_LONG).show();
        }
    }
    //disconnect from service and bluetooth
    private void disconnect() {
        Toast.makeText(getContext(), "Disconnected", Toast.LENGTH_SHORT).show();
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
        menu.findItem(R.id.menu_bluetooth).setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_bluetooth_disabled_white_24dp));
    }

    //receive data from device
    private void receive(byte[] _data) {
        data = _data;
        final String str = new String(_data);

        //based on first string decide what to do with data
        switch(str.charAt(0))
        {
            //if x display the value in the
            case 'x':
                accX = Float.parseFloat(str.substring(1));
                addEntry(accX, chartAcc, 0, R.color.colorChartAccX);
                break;
            case 'y':
                accY = Float.parseFloat(str.substring(1));
                //addEntry(Float.parseFloat(str.substring(1)), chartAcc, 1, R.color.colorChartAccY);
                break;
            case 'z':
                accZ = Float.parseFloat(str.substring(1));
                if(sendToWebserver) {
                    savePosition(this.token, accX, accY, accZ);
                }
                //addEntry(Float.parseFloat(str.substring(1)), chartAcc, 2, R.color.colorChartAccZ);
                break;
            case 't':
                tmp = Float.parseFloat(str.substring(1));
                if(sendToWebserver) {
                    saveTemperature(this.token, tmp);
                }
                addEntry(tmp, chartTmp,0, R.color.colorChartTmp);
                break;
            case 'p':
                pulse = Float.parseFloat(str.substring(1));
                if(sendToWebserver) {
                    savePulse(this.token, pulse);
                }
                addEntry(pulse, chartPulse,0, R.color.colorChartPulse);
                if(pulse == 63) {
/*                    speak("Your pulse is " + Float.toString(pulse) + "bpm " +
                            "and your body temperature is " + Float.toString(tmp) + "degrees Celsius");*/

/*                    speak("Dein Puls ist " + Float.toString(pulse) + "bpm, " +
                            "deine Körpertemperatur ist " + Float.toString(tmp) + "Grad Celsius");*/
                }
                break;
            case 'n':
                //error on ESP32
                break;
            default:
                break;
        }
        //saveTemperature(this.token, Integer.parseInt(str));
        //if(stop_chart == false) {
        //addEntry(Float.parseFloat(str.substring(2)), lineChart);
        //}
        //receiveText.setText(new String(_data));

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvPulse.setText("Pulse: " + Float.toString(pulse));
                tvAccelerometer.setText("AccX: " + Float.toString(accX) + " AccY: " + Float.toString(accY) +
                        " AccZ: " + Float.toString(accZ));
                tvTmp.setText("Temperature: " + Float.toString(tmp));
            }
        });
    }

    //chart methods
    private void chartSettings(LineChart ch) {
        //chart
        // enable description text
        ch.getDescription().setEnabled(false);
        // enable touch gestures
        ch.setTouchEnabled(false);
        // enable scaling and dragging
        ch.setDragEnabled(false);
        ch.setScaleEnabled(false);
        ch.setDrawGridBackground(false);
        // if disabled, scaling can be done on x- and y-axis separately
        ch.setPinchZoom(true);
        // set an alternative background color
        //chart.setBackgroundColor(Color.WHITE);

        // add empty data
        //ch.setData(lineData);

        //set all margins to 0
        //ch.setViewPortOffsets(0f, 0f, 0f, 0f);

        // get the legend (only possible after setting data)
        Legend l = ch.getLegend();
        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        /*        l.setTypeface(tfLight);*/
        l.setTextColor(Color.BLACK);
        l.setEnabled(false);

        XAxis xl = ch.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        /*        xl.setTypeface(tfLight);*/
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(false);

        YAxis leftAxis = ch.getAxisLeft();
        /*        leftAxis.setTypeface(tfLight);*/
        leftAxis.setTextColor(Color.BLACK);

        //set maximum of axis (if dynamic comment out)
        //leftAxis.setAxisMaximum(15f);
        //leftAxis.setAxisMinimum(-15f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setEnabled(true);

        //axis only on the left-hand side, rightAxis disabled
        YAxis rightAxis = ch.getAxisRight();
        rightAxis.setDrawAxisLine(false);
        rightAxis.setEnabled(false);
    }

    private void addEntry(float YAxis, LineChart ch, int dataSetIndex, @ColorRes int colorId) {

        LineData data = ch.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(dataSetIndex);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet("Data" + dataSetIndex, colorId);
                data.addDataSet(set);
            }

            if (set.getEntryCount() >= MAX_CHART_ENTRIES) {
                set.removeFirst();
                for (int i=0; i<set.getEntryCount(); i++) {
                    Entry entryToChange = set.getEntryForIndex(i);
                    entryToChange.setX(entryToChange.getX() - 1);
                }
            }

            data.addEntry(new Entry(set.getEntryCount(), YAxis), dataSetIndex);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            ch.notifyDataSetChanged();

            // limit the number of visible entries
            ch.setVisibleXRangeMaximum(MAX_CHART_ENTRIES);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            ch.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet(String name, @ColorRes int colorId) {
        LineDataSet set = new LineDataSet(null, name);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(getResources().getColor(colorId));
        //set.setCircleColor(getResources().getColor(R.color.color4));
        set.setLineWidth(3.5f);
        //set.setCircleRadius(2f);
        set.setFillAlpha(0);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.BLACK);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        return set;
    }

    //text to speech implementation
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(textToSpeech.getEngines().size() == 0) {
                    //error
                } else {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    //speak("I'm ready to serve, Adrian!");
/*                    speak("Ich bin bereit zum Einsatz, Adrian!");*/
                }
            }
        });
    }

    private void speak(String message) {
        if(Build.VERSION.SDK_INT >= 21){
            textToSpeech.speak(message,TextToSpeech.QUEUE_FLUSH,null,null);
        } else {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH,null);
        }
    }

    private void initializeSpeechRecongizer() {
        if(SpeechRecognizer.isRecognitionAvailable(getContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float rmsdB) {

                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int error) {

                }

                @Override
                public void onResults(Bundle results) {
                    List<String> result_arr = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    processResult(result_arr.get(0));
                }

                @Override
                public void onPartialResults(Bundle partialResults) {

                }

                @Override
                public void onEvent(int eventType, Bundle params) {

                }
            });
        }
    }

    private void processResult(String result_message) {
        result_message = result_message.toLowerCase();

//        Handle at least four sample cases

//        First: What is your Name?
//        Second: What is the time?
//        Third: Is the earth flat or a sphere?
//        Fourth: Open a browser and open url
        if(result_message.indexOf("what") != -1){
            if(result_message.indexOf("your name") != -1){
                speak("My Name is Serchro. Nice to meet you!");
            }

            else if (result_message.indexOf("time") != -1){
                String time_now = DateUtils.formatDateTime(getContext(), new Date().getTime(),DateUtils.FORMAT_SHOW_TIME);
                speak("The time is now: " + time_now);
            }

            else if (result_message.indexOf("pulse") != -1) {
                speak("Your pulse is " + Double.toString(pulse));
            }

            else if (result_message.indexOf("body temperature") != -1) {
                speak("Your body temperature is " + Float.toString(tmp));
            }

            else {
                speak("I'm sorry, I couldn't understand you, can you repeat your question?");
            }

        } else if (result_message.indexOf("pulse") != -1){
            speak("Opening a browser right away master.");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/AnNJPf-4T70"));
            startActivity(intent);
        } else {
            speak("I'm sorry, I couldn't understand you.");
        }
    }

    //communication with webserver
    private void saveTemperature(final String token, final Float value) {
        final Temperature request = new Temperature(value);

        String auth = "Bearer " + token;
        Call<Temperature> call = RetrofitClient.getInstance().getApi().saveTemperature(auth, request);

        call.enqueue(new Callback<Temperature>() {
            @Override
            public void onResponse(Call<Temperature> call, Response<Temperature> response) {
                if(!response.isSuccessful()) {
                    Toast.makeText(getContext(), "Code: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                //tvName.setText("Value: "+ response.body().getValue() + "\n");
            }

            @Override
            public void onFailure(Call<Temperature> call, Throwable t) {
                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void savePulse(final String token, final Float value) {
        final Pulse request = new Pulse(value);

        String auth = "Bearer " + token;
        Call<Pulse> call = RetrofitClient.getInstance().getApi().savePulse(auth, request);

        call.enqueue(new Callback<Pulse>() {
            @Override
            public void onResponse(Call<Pulse> call, Response<Pulse> response) {
                if(!response.isSuccessful()) {
                    Toast.makeText(getContext(), "Code: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                //tvName.setText("Value: "+ response.body().getValue() + "\n");
            }

            @Override
            public void onFailure(Call<Pulse> call, Throwable t) {
                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void savePosition(final String token, final Float value_x, final Float value_y, final Float value_z) {
        final Position request = new Position(value_x, value_y, value_z);

        String auth = "Bearer " + token;
        Call<Position> call = RetrofitClient.getInstance().getApi().savePosition(auth, request);

        call.enqueue(new Callback<Position>() {
            @Override
            public void onResponse(Call<Position> call, Response<Position> response) {
                if(!response.isSuccessful()) {
                    Toast.makeText(getContext(), "Code: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                //tvName.setText("Value: "+ response.body().getValue() + "\n");
            }

            @Override
            public void onFailure(Call<Position> call, Throwable t) {
                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
