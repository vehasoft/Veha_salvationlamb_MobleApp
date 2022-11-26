package com.example.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Util {
    public static final String SUCCESS = "success";
    public static String token = "";
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



