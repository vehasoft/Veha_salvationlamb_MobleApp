package com.example.fbproject

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.*
import androidx.lifecycle.asLiveData
import com.example.util.Posts
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response

class ViewPostActivity : AppCompatActivity() {
    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: ProgressDialog
    val name : TextView = findViewById(R.id.name_post)
    val time : TextView = findViewById(R.id.post_time)
    val tags : TextView = findViewById(R.id.tags)
    val content : TextView = findViewById(R.id.post_content)
    val reacts : TextView = findViewById(R.id.no_of_reacts)
    val profilePic : ImageView = findViewById(R.id.profile_pic)
    val postPic : ImageView = findViewById(R.id.post_pic)
    val likePic : ImageView = findViewById(R.id.like_pic)
    val reactBtn : LinearLayout = findViewById(R.id.react_btn)
    val postContainer : LinearLayout = findViewById(R.id.post_layout)
    val headLinear : LinearLayout = findViewById(R.id.head_linear)
    val likeLayout : LinearLayout = findViewById(R.id.like_layout)
    val likeBtn : Button = findViewById(R.id.like_btn)
    val shareBtn : Button = findViewById(R.id.share_btn)
    val fav : ImageButton = findViewById(R.id.fav)
    val logo : ImageView = findViewById(R.id.prod_logo)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_post)
        userPreferences = UserPreferences(this)
        dialog = ProgressDialog(this)
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        var postId: String = intent.extras!!.get("postId").toString()
        getPost(postId)
    }
    fun getPost(postId: String) {
        if (!dialog.isShowing) {
            dialog.show()
        }
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it", postId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val post: Posts = Gson().fromJson(response.body()?.get("result"), Posts::class.java)
                            tags.text = post.tags
                            time.text = Util.getTimeAgo(post.createdAt)
                            content.text = post.content
                            reacts.text = "${post.likesCount} people reacts"
                        } else {
                            Log.e("fail fav",response.errorBody().toString())
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                        }
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Toast.makeText(this@ViewPostActivity, "No Internet", Toast.LENGTH_LONG).show()
                        Log.e("responseee", "fail")
                    }
                })
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}