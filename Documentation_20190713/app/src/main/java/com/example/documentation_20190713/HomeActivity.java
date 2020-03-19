package com.example.documentation_20190713;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.documentation_20190713.Home.MeasurementsFragment;
import com.example.documentation_20190713.Home.SettingsFragment;
import com.example.documentation_20190713.Retrofit.RetrofitClient;
import com.example.documentation_20190713.Retrofit.User;
import com.example.documentation_20190713.Security.SecurePreferences;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    //user's data
    User user = null;
    //variable storing token
    String token;
    //connection to place where the token is stored
    SecurePreferences preferences;
    //for view pager
    TabLayout tabLayout;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //get layout (activity_home)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //initialize preferences and retrieve the token
        preferences = new SecurePreferences(getApplicationContext(), "user-info",
                "YourSecurityKey", true);
        this.token = preferences.getString("token");

        //initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getUser(token);

        //ViewPager
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.container);
        tabLayout.setupWithViewPager(viewPager);
        setUpViewPager(viewPager);

        //fragment initialization (bluetooth/chart)
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null) {

/*            Bundle bundle = new Bundle();
            bundle.putString("token", this.token);
            MeasurementsFragment measurementsFragment = new MeasurementsFragment();
            measurementsFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_measurements, measurementsFragment).commit();*/
            //getSupportFragmentManager().beginTransaction().add(R.id.fragment, new TerminalFragment(), "terminal").commit();

/*            DashboardFragment dashboardFragment = new DashboardFragment();
            dashboardFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_dashboard, dashboardFragment).commit();*/
        }
        else {
            onBackStackChanged();
        }
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.menu_logout) {
            logout(token);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    //retrofit
    //get user's data
    //this function checks if the token stored in preferences still works, this function is executed onCreate of this activity.
    private void getUser(final String token) {
        //we will try to retrieve the information about the user based on the token
        String auth = "Bearer " + token;
        Call<User> call = RetrofitClient.getInstance().getApi().getUser(auth);
        call.enqueue(new Callback<User>() {
            //if response comes back from the server we check if is was successful one
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                //send code if the request was unsuccessful
                if(!response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }

                //check if no error was sent from the server if the request was successful
                if(response.body().getMessages().getError() == 0)
                {
                    user = response.body();
                    getSupportActionBar().setTitle("Serchro - " + user.getName());
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG).show();
                    //close all fragments
                    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                        if (fragment != null) {
                            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                        }
                    }
                    Intent i = new Intent(HomeActivity.this, MainActivity.class);
                    startActivity(i);
                }
            }
            //if no response, the user gets an error message
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    //logout
    private void logout(final String token) {
        String auth = "Bearer " + token;
        Call<User> call = RetrofitClient.getInstance().getApi().logout(auth);

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Error: " + response.code(), Toast.LENGTH_LONG).show();
                }
                //close all fragments
                for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                    if (fragment != null) {
                        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                    }
                }
                //go to MainActivity and set stored token to null
                preferences.put("token", null);
                if(response.body().getMessages().getOther() != null) {
                    Toast.makeText(getApplicationContext(), response.body().getMessages().getOther(), Toast.LENGTH_LONG).show();
                }
                Intent i = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(i);
            }

            @Override
            public void onFailure(Call<User> call, Throwable t)
            {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    //view pager
    public void setUpViewPager(ViewPager viewpage){
        MyViewPageAdapter Adapter = new MyViewPageAdapter(getSupportFragmentManager());

        Adapter.AddFragmentPage(new MeasurementsFragment(), "Measurements");
        Adapter.AddFragmentPage(new SettingsFragment(), "Settings");
        //Adapter.AddFragmentPage(new Page3(), "Page 3");
        //We Need Fragment class now

        viewpage.setAdapter(Adapter);

    }

    public class MyViewPageAdapter extends FragmentPagerAdapter {
        private List<Fragment> MyFragment = new ArrayList<>();
        private List<String> MyPageTitle = new ArrayList<>();

        public MyViewPageAdapter(FragmentManager manager){
            super(manager);
        }

        public void AddFragmentPage(Fragment Frag, String Title){
            Bundle bundle = new Bundle();
            bundle.putString("token", token);
            Frag.setArguments(bundle);
            MyFragment.add(Frag);
            MyPageTitle.add(Title);
        }

        @Override
        public Fragment getItem(int position) {
            return MyFragment.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return MyPageTitle.get(position);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
