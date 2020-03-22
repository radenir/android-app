package com.example.documentation_20190713;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.documentation_20190713.Retrofit.RetrofitClient;
import com.example.documentation_20190713.Retrofit.User;
import com.example.documentation_20190713.Security.SecurePreferences;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    //elements in the layout
    private EditText etEmail, etPassword;
    private TextView tvRegister;
    private Button btnLogin;

    //for storing/retrieving token
    private SecurePreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //get the layout file
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize preferences for token storage, the data is encrypted based on the secureKey.
        preferences = new SecurePreferences(getApplicationContext(), "user-info",
                "YourSecurityKey", true);

        //initialize elements in layout
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);

        //check the old, stored token, if it works, go to HomeActivity, otherwise stay here
        String old_token = preferences.getString("token");
        if(old_token != null)
        {
            isLoggedIn(old_token);
        }
        //THIS CODE SKIPS LOGIN
/*        Intent i = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(i);*/
        //END OF SKIP LOGIN

        //if old_token is null just keep executing the code here
        //no token stored, stay in MainActivity

        //for switching to RegisterActivity
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go to register window
                Intent i = new Intent(MainActivity.this,
                        RegisterActivity.class);
                startActivity(i);
            }
        });

        //for going to HomeActivity when the data typed in by the user are correct
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String Email = etEmail.getText().toString();
                String Password = etPassword.getText().toString();
                login(Email, Password);
            }
        });
    }

    //function implementing connection with the server and retrieving the token from there.
    private void login(final String Email, final String Password) {
        User request = new User(Email, Password);

        //send a request to the server
        Call<User> call = RetrofitClient.getInstance().getApi().login(request);
        call.enqueue(new Callback<User>() {
            //if response comes back from the server we check if is was successful one
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                //give the error code if request is unsuccessful
                if(!response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Error: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                //otherwise check if the token sent by the server is not empty.
                if(response.body().getToken() != null)
                {
                    //If token is not empty we store it in preferences and switch the intent to HomeActivity
                    String token = response.body().getToken();
                    preferences.put("token", token);
                    Intent i = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(i);
                }
                //if token is 0
                else
                {
                    //check if message has been sent from the server
                    if (response.body().getMessages().getOther() != null) {
                        Toast.makeText(getApplicationContext(), response.body().getMessages().getOther(), Toast.LENGTH_LONG).show();
                    }
                    //if no message and token is 0, tell the user that something went wrong
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Something went wrong.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            //if there is no response from the server, user is informed about it
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    //this function checks if the token stored in preferences still works, this function is executed onCreate of this activity.
    private void isLoggedIn(final String token) {
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
                    //try to connect to the server, if connection works, go to HomeActivity
                    Intent i = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(i);
                }
                else
                {
                    preferences.put("token", null);
                }
            }
            //if no response, the user gets an error message
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
