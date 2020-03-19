package com.example.documentation_20190713;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.documentation_20190713.Retrofit.Message;
import com.example.documentation_20190713.Retrofit.RetrofitClient;
import com.example.documentation_20190713.Retrofit.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    TextView tvLogin;
    EditText etName, etEmail, etPassword;
    Button btnRegister;
    Message message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String Name = etName.getText().toString();
                String Email = etEmail.getText().toString();
                String Password = etPassword.getText().toString();
                register(Name, Email, Password);
            }
        });

        //for switching to RegisterActivity
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go to register window
                Intent i = new Intent(RegisterActivity.this,
                        MainActivity.class);
                startActivity(i);
            }
        });
    }

    private void register(final String Name, final String Email, final String Password) {
        User user = new User(Name, Email, Password);

        Call<User> call = RetrofitClient.getInstance().getApi().register(user);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(!response.isSuccessful()) {
                    return;
                }

                final Message message = response.body().getMessages();

                if(message.getError() != null) {
                    if(message.getError() == 0) {
                        Toast.makeText(getApplicationContext(), "User created successfully!", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(RegisterActivity.this,
                                MainActivity.class);
                        startActivity(i);
                        finish();
                    } else {
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
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                //
            }
        });
    }
}
