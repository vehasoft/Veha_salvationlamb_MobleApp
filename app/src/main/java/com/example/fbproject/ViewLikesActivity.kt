package com.example.fbproject

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.HomeAdapter
import com.example.adapter.ViewLikesAdapter
import com.example.models.Posts
import com.example.util.Util
import com.example.util.PostLikes
import com.example.util.UserPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response

class ViewLikesActivity : AppCompatActivity() {

    lateinit var userPreferences: UserPreferences
    lateinit var list : RecyclerView
    lateinit var nodata : LinearLayout

    val likeslist: ArrayList<PostLikes> = ArrayList()

    private var postId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_likes)
        list = findViewById(R.id.likesListRecycler)
        nodata = findViewById(R.id.no_data)
        userPreferences = UserPreferences(this@ViewLikesActivity)
        val indent = intent
        postId = indent.getStringExtra("postId").toString()
        getALlLikes(this@ViewLikesActivity)
    }
    fun getALlLikes(context: Context){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getPostLike("Bearer $it",postId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)

                            for (post in loginresp){
                                val pos = Gson().fromJson(post, PostLikes::class.java)
                                likeslist.add(pos)
                            }
                            if (likeslist.size <= 0){
                                list.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else {
                                list.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                list.layoutManager = LinearLayoutManager(context)
                                list.adapter = ViewLikesAdapter(likeslist, context)
                            }
                            Log.e("postlist",likeslist.toString())
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            }
        }

    }
}