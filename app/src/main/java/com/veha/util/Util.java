package com.veha.util;

import android.media.MediaPlayer;
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
    public static final String DAY = "Day";
    public static final String NIGHT = "Night";
    public static final String DEFAULT = "Default";
    public static Float fontSize = 10.0F;
    public static String url = "https://server.salvationlamb.com";
    //public static String url = "https://salvationlamb-env.eba-smicznsb.ap-south-1.elasticbeanstalk.com";
    public static String userId;
    public static Boolean isFirst = true;
    public static boolean listview = true;
    public static final String WARRIOR = "Warrior";
    public static final String USER = "User";
    public static String isNight = DAY;
    private static ArrayList<String> religion;
    public static Boolean isWarrior = false;
    public static UserRslt user;
    private static RetrofitAPI retrofitAPI;
    public static MediaPlayer player = null;

    public static ArrayList getReligion() {
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

    public static RetrofitAPI getRetrofit() {
        if (retrofitAPI != null) {
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

    public static String formatDate(String date, String toPattern, String fromPattern) throws ParseException {
        if (date == null || date.isEmpty()) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(fromPattern);
        Date past = format.parse(date);
        SimpleDateFormat formatter = new SimpleDateFormat(toPattern);
        return formatter.format(past);
    }

    public static String getTimeAgo(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        String timeAgo = "";
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date past = format.parse(new Commons().getDate(date));
            Date now = new Date();
            long seconds = TimeUnit.MILLISECONDS.toSeconds(now.getTime() - past.getTime());
            long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime());
            long hours = TimeUnit.MILLISECONDS.toHours(now.getTime() - past.getTime());
            long days = TimeUnit.MILLISECONDS.toDays(now.getTime() - past.getTime());
            if (seconds < 60)
                timeAgo = (seconds + " seconds ago");
            else if (minutes < 60)
                timeAgo = (minutes + " minutes ago");
            else if (hours < 24)
                timeAgo = (hours + " hours ago");
            else if (days >= 1) {
                if (days == 1) {
                    timeAgo = "yesterday";
                } else {
                    timeAgo = new SimpleDateFormat("dd MMM yyyy, HH:mm a").format(past);
                }
            }
        } catch (Exception j) {
            j.printStackTrace();
            Log.e("timeAgo", j.toString());
        }
        return timeAgo;
    }

    public static boolean isValidEmail(String email) {
        return Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE).matcher(email).find();
    }

    public static boolean isValidName(String name) {
        return Pattern.compile("^[a-zA-Z\\s]+\\.?$", Pattern.CASE_INSENSITIVE).matcher(name).find();
    }

    public static boolean isValidPassword(String password) {
        return Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$").matcher(password).find();
    }

    public static boolean isValidMobile(String mobile) {
        return Pattern.compile("^(\\+?\\d{1,4}[\\s-])?(?!0+\\s+,?$)\\d{10}\\s*,?$").matcher(mobile).find();
    }

    public static String getVideo(String url) {
        url = "https://salvationlamb.com/video/" + url;
        return url;
    }

}



