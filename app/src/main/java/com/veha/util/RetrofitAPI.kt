package com.veha.util

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface RetrofitAPI {
    @POST("api/v1/{url}")
    fun postCall(@Path(value = "url") url: String, @Body dataModal: JsonObject?): Call<JsonObject?>?

    //use it for login and register
    @POST("api/v1/password/forgot-password")
    fun postForgotPassword(@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/password/confirm-password")
    fun postChangeForgotPassword(@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/password/verify-otp")
    fun postForgotPasswordOtp(@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/users/verify-otp")
    fun postVerifyUser(@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/{url}")
    fun postCallHead(
        @Header(value = "Authorization") head: String,
        @Path(value = "url") url: String,
        @Body dataModal: JsonObject?
    ): Call<JsonObject?>?

    @POST("api/v1/users/warrior")
    fun postWarrior(@Header(value = "Authorization") head: String, @Body dataModal: JsonObject?): Call<JsonObject?>?

    //use it for like post changepassword
    @POST("api/v1/follows")
    fun postFollow(@Header(value = "Authorization") head: String, @Body dataModal: JsonObject?): Call<JsonObject?>?

    //userid followerid
    @POST("api/v1/favorites")
    fun postFav(@Header(value = "Authorization") head: String, @Body dataModal: JsonObject?): Call<JsonObject?>?
//userid and postid

    @POST("api/v1/Users/image/{userId}")
    fun postProfilePic(
        @Header(value = "Authorization") head: String,
        @Path(value = "userId") userId: String,
        @Body dataModal: JsonObject?
    ): Call<JsonObject?>?

    @POST("api/v1/users/change-password")
    fun postChangePassword(
        @Header(value = "Authorization") head: String,
        @Body dataModal: JsonObject?
    ): Call<JsonObject?>?


    @GET("api/v1/favorites/{userId}")
    fun getFav(
        @Header(value = "Authorization") head: String,
        @Path(value = "userId") userId: String
    ): Call<JsonObject?>?

    @GET("api/v1/favorites/{userId}")
    fun getMyFav(
        @Header(value = "Authorization") head: String,
        @Path(value = "userId") userId: String
    ): Call<JsonObject?>?

    @GET("api/v1/like/post/{postId}")
    fun getPostLike(
        @Header(value = "Authorization") head: String,
        @Path(value = "postId") postId: String
    ): Call<JsonObject?>?

    @GET("api/v1/users/{userId}")
    fun getUser(
        @Header(value = "Authorization") head: String,
        @Path(value = "userId") userId: String
    ): Call<JsonObject?>?

    @GET("api/v1/like/user/{userId}")
    fun getUserLikes(
        @Header(value = "Authorization") head: String,
        @Path(value = "userId") userId: String
    ): Call<JsonObject?>?

    @GET("api/v1/post/user/{userId}")
    fun getMyPosts(
        @Header(value = "Authorization") head: String,
        @Path(value = "userId") userId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<JsonObject?>?

    @GET("api/v1/post")
    fun getPost(
        @Header("Authorization") dataModal: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<JsonObject?>?

    @GET("api/v1/post/admin/video")
    fun getVideoPost(
        @Header("Authorization") dataModal: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<JsonObject?>?

    @GET("api/v1/post/admin/audio")
    fun getAudioPost(
        @Header("Authorization") dataModal: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Call<JsonObject?>?

    @GET("api/v1/follows/user/{userId}")
    fun getFollowers(
        @Header("Authorization") dataModal: String?,
        @Path(value = "userId") userId: String
    ): Call<JsonObject?>?

    @GET("api/v1/follows/{userId}")
    fun getFollowing(
        @Header("Authorization") dataModal: String?,
        @Path(value = "userId") userId: String
    ): Call<JsonObject?>?

    @GET("api/v1/search")
    fun getSearch(@Header(value = "Authorization") head: String, @Query("query") query: String?): Call<JsonObject?>?

    @GET("api/v1/post/{postId}")
    fun getPost(@Header("Authorization") dataModal: String?, @Path(value = "postId") postId: String): Call<JsonObject?>?

    @GET("api/v1/files/{folderId}")
    fun getFilesAndFolders(
        @Header("Authorization") dataModal: String?,
        @Path(value = "folderId") folderId: String
    ): Call<JsonObject?>?


    @GET("api/v1/Country")
    fun getCountries(): Call<JsonObject>?

    @GET("api/v1/state/{countryID}")
    fun getState(@Path(value = "countryID") countryID: String): Call<JsonObject>?

    @GET("api/v1/city/{stateId}")
    fun getCity(@Path(value = "stateId") stateId: String): Call<JsonObject>?


    @PUT("api/v1/users/{userId}")
    fun putUser(
        @Header(value = "Authorization") head: String,
        @Path(value = "userId") userId: String,
        @Body dataModal: JsonObject?
    ): Call<JsonObject?>?
//same as register

    @PUT("api/v1/users/freshUser/{userId}")
    fun putFreshUser(
        @Header(value = "Authorization") head: String,
        @Path(value = "userId") userId: String
    ): Call<JsonObject?>?


    @DELETE("api/v1/post/{postId}")
    fun deletePost(
        @Header(value = "Authorization") head: String,
        @Path(value = "postId") postId: String
    ): Call<JsonObject?>?
}
