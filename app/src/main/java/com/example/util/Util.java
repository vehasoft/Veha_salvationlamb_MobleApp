package com.example.util;

import android.util.Log;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Util {
    public static final String SUCCESS = "success";
    public static Float fontSize = 10.0F;
    public static String url = "https://server.salvationlamb.com";
    //public static String url = "https://salvationlamb-env.eba-smicznsb.ap-south-1.elasticbeanstalk.com";
    public static String userId;
    public static Boolean isFirst = true;
    public static boolean listview = true;
    public static final String WARRIOR = "Warrior";
    public static final String USER = "User";
    public static Boolean isNight;
    private static ArrayList<String> religion;
    public static Boolean isWarrior;
    public static UserRslt user;
    private static RetrofitAPI retrofitAPI;

    public static ArrayList getReligion(){
        religion = new ArrayList<>();
        religion.add("Select");
        religion.add("Adventist");
        religion.add("Anglican / Episcopal");
        religion.add("Apostolic");
        religion.add("Assembly of God (A.G)");
        religion.add("Assyrian");
        religion.add("Baptist");
        religion.add("Born Again");
        religion.add("Brethren");
        religion.add("Calvinist");
        religion.add("Christian");
        religion.add("Church of God");
        religion.add("Church of South India (C.S.I)");
        religion.add("Congregational");
        religion.add("Evangelical");
        religion.add("Jacobite");
        religion.add("Jehovah`s Witnesses");
        religion.add("Jewish");
        religion.add("Latin Catholic");
        religion.add("Latter day saints");
        religion.add("Lutheran");
        religion.add("Malankara");
        religion.add("Marthoma");
        religion.add("Melkite");
        religion.add("Mennonite");
        religion.add("Methodist");
        religion.add("Moravian");
        religion.add("Orthodox");
        religion.add("Others");
        religion.add("Pentecostal");
        religion.add("Presbyterian");
        religion.add("Protestant");
        religion.add("Roman Catholic");
        religion.add("Seventh-day Adventist");
        religion.add("Syrian Catholic");
        religion.add("Syro Malabar");

        return religion;
    }

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
    public static String formatDate(String date,String pattern) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date past = format.parse(date);
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        return formatter.format(past);
    }
    public static String getTimeAgo(String date){
        String timeAgo = "";
        try
        {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            //SimpleDateFormat format1 = ;
            Date past = format.parse(new Commons().getDate(date));
            Log.e("timeAgo", new SimpleDateFormat("dd MMM yyyy HH:mm a").format(past));
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
            else if(days>=1){
                if (days == 1) {
                    timeAgo = "today";
                }
                else if (days == 2) {
                    timeAgo = "yesterday";
                }
                else {
                    timeAgo = new SimpleDateFormat("dd MMM yyyy, HH:mm a").format(past);
                }
            }
            Log.e("timeAgo",timeAgo);
        }
        catch (Exception j){
            j.printStackTrace();
            Log.e("timeAgo",j.toString());
        }
        return timeAgo;
    }
    public static boolean isValidEmail(String email){
        return Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE).matcher(email).find();
    }
    public static boolean isValidPassword(String password){
        return Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$").matcher(password).find();
    }
    public static String getVideo(String url){
        url ="https://salvationlamb.com/video/" + url;
        return url;
    }

}



