package com.example.util
/*
*
*
* Pending
* Delete post
*
*
*
*
*
*
*
*
*
*
 */
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface RetrofitAPI {
    @POST("api/v1/{url}")
    fun postCall(@Path (value = "url") url: String,@Body dataModal: JsonObject?): Call<JsonObject?>?
//use it for login and register
    @POST("api/v1/password/forgot-password")
    fun postForgotPassword(@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/password/verify-otp")
    fun postForgotPasswordOtp(@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/password/confirm-password")
    fun postChangePassword(@Body dataModal: JsonObject?): Call<JsonObject?>?

    @POST("api/v1/{url}")
    fun postCallHead(@Header(value="Authorization") head: String,@Path (value = "url") url: String,@Body dataModal: JsonObject?): Call<JsonObject?>?
//use it for like post changepassword
    @POST("api/v1/follows")
    fun postFollow(@Header(value="Authorization") head: String,@Body dataModal: JsonObject?): Call<JsonObject?>?
    //userid followerid
    @POST("api/v1/favorites")
    fun postFav(@Header(value="Authorization") head: String,@Body dataModal: JsonObject?): Call<JsonObject?>?
//userid and postid





    @GET("api/v1/favorites/{userId}")
    fun getFav(@Header(value="Authorization") head: String,@Path (value = "userId") userId: String): Call<JsonObject?>?

    @GET("api/v1/favorites/{userId}")
    fun getMyFav(@Header(value="Authorization") head: String,@Path (value = "userId") userId: String): Call<JsonObject?>?

    @GET("api/v1/like/post/{postId}")
    fun getPostLike(@Header(value="Authorization") head: String,@Path (value = "postId") postId: String): Call<JsonObject?>?

    @GET("api/v1/users/{userId}")
    fun getUser(@Header(value="Authorization") head: String,@Path (value = "userId") userId: String): Call<JsonObject?>?

    @GET("api/v1/like/user/{userId}")
    fun getUserLikes(@Header(value="Authorization") head: String,@Path (value = "userId") userId: String): Call<JsonObject?>?

    @GET("api/v1/like/{userId}")
    fun getLike(@Header(value="Authorization") head: String,@Path (value = "userId") userId: String): Call<JsonObject?>?

    @GET("api/v1/like/{postId}")
    fun getAllLike(@Header(value="Authorization") head: String,@Path (value = "postId") postId: String): Call<JsonObject?>?

    @GET("api/v1/post/user/{userId}")
    fun getMyPosts(@Header(value="Authorization") head: String,@Path (value = "userId") userId: String, @Query("page") page: Int, @Query("size") size: Int): Call<JsonObject?>?

    @GET("api/v1/post")
    fun getPost(@Header("Authorization") dataModal: String?, @Query("page") page: Int, @Query("size") size: Int): Call<JsonObject?>?

    @GET("api/v1/follows/user/{userId}")
    fun getFollowers(@Header("Authorization") dataModal: String?,@Path (value = "userId") userId: String): Call<JsonObject?>?

    @GET("api/v1/follows/{userId}")
    fun getFollowing(@Header("Authorization") dataModal: String?,@Path (value = "userId") userId: String): Call<JsonObject?>?

    @GET("api/v1/search")
    fun getSearch(@Header(value="Authorization") head: String,@Query("query") query: String?): Call<JsonObject?>?

    @GET("api/v1/post/{postId}")
    fun getPost(@Header("Authorization") dataModal: String?,@Path (value = "postId") postId: String): Call<JsonObject?>?


    @GET("api/v0.1/countries/")
    fun getCountries(): Call<JsonObject>?


    @PUT("api/v1/users/{userId}")
    fun putUser(@Header(value="Authorization") head: String,@Path (value = "userId") userId: String,@Body dataModal: JsonObject?): Call<JsonObject?>?
//same as register




    @DELETE("api/v1/follows/{postId}")
    fun deletePost(@Header(value="Authorization") head: String,@Path (value = "postId") postId: String): Call<JsonObject?>?

    @DELETE("api/v1/follows/{followerId}")
    fun deleteFollow(@Header(value="Authorization") head: String,@Path (value = "followerId") followerId: String): Call<JsonObject?>?

    @DELETE("api/v1/like/{postId}")
    fun unlikePost(@Header(value="Authorization") head: String,@Path (value = "postId") postId: String): Call<JsonObject?>?

}