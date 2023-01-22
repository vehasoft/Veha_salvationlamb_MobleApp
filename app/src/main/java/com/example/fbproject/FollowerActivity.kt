package com.example.fbproject

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.FollowAdapter
import com.example.util.PostUser
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response

class FollowerActivity : AppCompatActivity() {


    private lateinit var userPreferences: UserPreferences
    private lateinit var followList: ArrayList<PostUser>

    lateinit var lists: RecyclerView
    lateinit var nodata: LinearLayout
    private lateinit var userId: String

    private var myFollowerMap: HashMap<String,String> = HashMap()
    private var followingMap: HashMap<String,String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follower)
        userPreferences = UserPreferences(this)
        userId = intent.extras!!.get("userId").toString()
        if(intent.extras!!.get("page") == "follower")
            getallFollowers(this)
        if(intent.extras!!.get("page") == "following")
            getallFollowing(this)

        lists = findViewById(R.id.my_follow_list)
        nodata = findViewById(R.id.no_data)
    }
    private fun getallFollowers(context: Context) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFollowers("Bearer $it", userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            followList = ArrayList()
                            for (likes in loginresp) {
                                val pos = Gson().fromJson(likes, PostUser::class.java)
                                followList.add(pos)
                                myFollowerMap.put(Util.userId,pos.id)
                            }
                            if (followList.size <= 0){
                                lists.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                lists.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                lists.layoutManager = LinearLayoutManager(context)
                                lists.adapter = FollowAdapter(followList, context,myFollowerMap,this@FollowerActivity)
                            }

                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            }
        }
    }
    private fun getallFollowing(context: Context) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFollowing("Bearer $it", userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            followList = ArrayList()
                            for (followings in loginresp) {
                                val pos = Gson().fromJson(followings, PostUser::class.java)
                                followList.add(pos)
                                followingMap.put(pos.id,Util.userId)
                            }
                            if (followList.size <= 0){
                                lists.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                lists.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                lists.layoutManager = LinearLayoutManager(context)
                                lists.adapter = FollowAdapter(followList, context,followingMap,this@FollowerActivity)
                            }
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            }
        }
    }
}