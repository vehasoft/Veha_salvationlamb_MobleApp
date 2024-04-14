package com.veha.activity

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.*
import androidx.lifecycle.asLiveData
import com.veha.util.Commons
import com.veha.util.Posts
import com.veha.util.UserPreferences
import com.veha.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.squareup.picasso.Picasso
import dmax.dialog.SpotsDialog
import retrofit2.Call
import retrofit2.Response

class ViewPostActivity : AppCompatActivity() {
    private lateinit var userPreferences: UserPreferences
    lateinit var name: TextView
    lateinit var time: TextView
    lateinit var tags: TextView
    lateinit var title: TextView
    lateinit var content: ExpandableView
    lateinit var reacts: TextView
    lateinit var profilePic: ImageView
    lateinit var postPic: ImageView
    lateinit var pauseBtn: ImageView
    lateinit var playBtn: ImageView
    lateinit var postVideo: YouTubePlayerView
    lateinit var audioLayout: LinearLayout
    lateinit var seekbar: SeekBar
    lateinit var likeBtn: Button
    lateinit var shareBtn: Button
    lateinit var followBtn: Button
    lateinit var logo: ImageView
    lateinit var dialog: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_post)
        userPreferences = UserPreferences(this)
        logo = findViewById(R.id.prod_logo)
        name = findViewById(R.id.name_post)
        time = findViewById(R.id.post_time)
        tags = findViewById(R.id.tags)
        content = findViewById(R.id.post_content)
        reacts = findViewById(R.id.no_of_reacts)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        var postId: String = intent.extras!!.get("postId").toString()
        getPost(postId)
    }

    fun getPost(postId: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it", postId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    val post: Posts = Gson().fromJson(
                                        response.body()?.get("result"),
                                        Posts::class.java
                                    )
                                    Log.e("postttttttt",post.toString())
                                    name.text = post.user.name
                                    title.text = post.title
                                    tags.text = post.tags
                                    time.text = Util.getTimeAgo(post.createdAt)
                                    content.text = post.content
                                    reacts.text = "${post.likesCount} people reacts"
                                    if (!post.user.picture.isNullOrEmpty()) {
                                        Picasso.with(this@ViewPostActivity).load(post.user.picture).into(profilePic)
                                    } else {
                                        profilePic.setImageResource(R.drawable.ic_profile)
                                    }
                                } else {
                                    Log.e("fail post", response.errorBody().toString())
                                    /*val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()*/
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("ViewPost", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ViewPost", e.toString())
        }
    }
}