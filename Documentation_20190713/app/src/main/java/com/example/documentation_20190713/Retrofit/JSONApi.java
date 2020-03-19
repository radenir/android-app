package com.example.documentation_20190713.Retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface JSONApi {

    //register
    @POST("api/register")
    Call<User> register(@Body User user);

    //login
    @POST("api/login")
    Call<User> login(@Body User user);

    //logout
    @GET("api/logout")
    Call<User> logout(@Header("Authorization") String auth);

    //get user information after login
    @GET("api/user")
    Call<User> getUser(@Header("Authorization") String auth);

    //save temperature entry to database for this user
    @POST("api/temperatures")
    Call<Temperature> saveTemperature(@Header("Authorization") String auth, @Body Temperature temperature);

    //save pulse entry to database for this user
    @POST("api/pulses")
    Call<Pulse> savePulse(@Header("Authorization") String auth, @Body Pulse pulse);

    //save position entry to database for this user
    @POST("api/positions")
    Call<Position> savePosition(@Header("Authorization") String auth, @Body Position position);
}
