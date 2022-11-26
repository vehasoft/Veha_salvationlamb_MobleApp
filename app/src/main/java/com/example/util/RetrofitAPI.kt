package com.example.util

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface RetrofitAPI {
    @POST("api/v1/{url}")
    fun postCall(@Path (value = "url") url: String,@Body dataModal: JsonObject?): Call<JsonObject?>?

    @GET("api/v1/{url}")
    fun getCall(@Path (value = "url") url: String,@Body dataModal: JsonObject?): Call<JsonObject?>?

    @GET("api/v1/post")
    // on below line we are creating a method to post our data.
    fun getPost(@Header("Authorization") dataModal: String?, @Query("page") page: Int, @Query("size") size: Int): Call<JsonObject?>?

    @PUT("api/v1/{url}")
    fun putCall(@Path (value = "url") url: String,@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/{url}")
    fun postCallHead(@Header(value="Authorization") head: String,@Path (value = "url") url: String,@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/{url}")
    fun getCallHead(@Header(value="Authorization") head: String,@Path (value = "url") url: String,@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/{url}")
    fun putCallHead(@Header(value="Authorization") head: String,@Path (value = "url") url: String,@Body dataModal: JsonObject?): Call<JsonObject?>?


}