package com.example.util;

import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIUtil {
    public static String url = "http://salvation-env.eba-nhpvydpr.us-east-1.elasticbeanstalk.com/";
    public static String header = "";
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

    public static Response<JsonObject> postCall(String url, JsonObject data, Boolean headerCall){
        final Response[] output = {null};
        Call<JsonObject> call = headerCall ? getRetrofit().postCallHead(header,url,data) : getRetrofit().postCall(url,data);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NotNull Call<JsonObject> call, @NotNull Response<JsonObject> response) {
                output[0] = response;
            }

            @Override
            public void onFailure(@NotNull Call<JsonObject> call, @NotNull Throwable t) {

            }
        });
        return output[0];
    }
    public static Response<JsonObject> getCall(String url, JsonObject data, Boolean headerCall){
        final Response[] output = {null};
        Call<JsonObject> call = headerCall ? getRetrofit().getCallHead(header,url,data) : getRetrofit().getCall(url,data);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NotNull Call<JsonObject> call, @NotNull Response<JsonObject> response) {
                output[0] = response;
            }

            @Override
            public void onFailure(@NotNull Call<JsonObject> call, @NotNull Throwable t) {

            }
        });
        return output[0];
    }
    public static Response<JsonObject> putCall(String url, JsonObject data, Boolean headerCall){
        final Response[] output = {null};
        Call<JsonObject> call = headerCall ? getRetrofit().putCallHead(header,url,data) : getRetrofit().putCall(url,data);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NotNull Call<JsonObject> call, @NotNull Response<JsonObject> response) {
                output[0] = response;
            }

            @Override
            public void onFailure(@NotNull Call<JsonObject> call, @NotNull Throwable t) {

            }
        });
        return output[0];
    }
}
