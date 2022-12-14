package com.example.util;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Util {
    public static final String SUCCESS = "success";
    public static String url = "http://salvation-env.eba-nhpvydpr.us-east-1.elasticbeanstalk.com/";
    public static String userId;
    public static final String WARRIOR = "Warrior";
    public static final String USER = "User";
    public static UserRslt user;

    private static RetrofitAPI retrofitAPI;

    public static RetrofitAPI getRetrofit(){
        if(retrofitAPI != null){
            return retrofitAPI;
        }
        OkHttpClient.Builder okhttpClientBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient = okhttpClientBuilder.build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        retrofitAPI = retrofit.create(RetrofitAPI.class);
        return retrofitAPI;
    }

    public static ArrayList<String> getCountries(){
        ArrayList countries = new ArrayList<String>();
        OkHttpClient.Builder okhttpClientBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient = okhttpClientBuilder.build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://countriesnow.space/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        RetrofitAPI retrofi = retrofit.create(RetrofitAPI.class);
        Call<JsonObject> call = retrofi.getCountries();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonArray array = (JsonArray) response.body().get("data");
                for (int i = 0;i<array.size();i++){
                    JsonObject country = (JsonObject) array.get(i);
                    countries.add(country.get("country"));
                }
                Log.e("countriesssss",countries.toString());

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });

        Log.e("countriesssss",countries.toString());
        return countries;
    }

    public static String getTimeAgo(String date){
        String timeAgo = "";
        try
        {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date past = format.parse(date);
            Date now = new Date();
            long seconds= TimeUnit.MILLISECONDS.toSeconds(now.getTime() - past.getTime());
            long minutes=TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime());
            long hours=TimeUnit.MILLISECONDS.toHours(now.getTime() - past.getTime());
            long days=TimeUnit.MILLISECONDS.toDays(now.getTime() - past.getTime());
            if(seconds<60)
                timeAgo = (seconds+" seconds ago");
            else if(minutes<60)
                timeAgo = (minutes+" minutes ago");
            else if(hours<24)
                timeAgo = (hours+" hours ago");
            else
                timeAgo = (days+" days ago");
        }
        catch (Exception j){
            j.printStackTrace();
        }
        return timeAgo;
    }
    public static boolean isValidEmail(String email){
        return Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE).matcher(email).find();
    }
    public static boolean isValidPassword(String password){
        return Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$", Pattern.CASE_INSENSITIVE).matcher(password).find();
    }

}



