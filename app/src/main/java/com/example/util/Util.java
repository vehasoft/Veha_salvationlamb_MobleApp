package com.example.util;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Util {
    public static final String SUCCESS = "success";
    public static String url = "http://salvation-env.eba-nhpvydpr.us-east-1.elasticbeanstalk.com/";
    public static String userId;
    public static final String WARRIOR = "Warrior";
    public static final String USER = "User";
    public static Boolean isNight;
    public static Boolean isWarrior;
    public static UserRslt user;
    public static Map<String,String> countryMap = new HashMap();
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
    public static String formatDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date past = format.parse(date);
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        return formatter.format(past);
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



