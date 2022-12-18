package com.example.util

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object APIUtil {
    var url = "http://salvation-env.eba-nhpvydpr.us-east-1.elasticbeanstalk.com/"
    var header = ""
    private var retrofitAPI: RetrofitAPI? = null
    val retrofit: RetrofitAPI?
        get() {
            if (retrofitAPI != null) {
                return retrofitAPI
            }
            val okhttpClientBuilder = OkHttpClient.Builder()
            val okHttpClient = okhttpClientBuilder.build()
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
            retrofitAPI = retrofit.create(RetrofitAPI::class.java)
            return retrofitAPI
        }
}