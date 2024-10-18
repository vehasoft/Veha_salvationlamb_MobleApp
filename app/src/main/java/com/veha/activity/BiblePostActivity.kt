package com.veha.activity

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.Util
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class BiblePostActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    lateinit var logo: ImageView
    lateinit var title: TextView
    lateinit var tags: TextView
    lateinit var postBtn: Button
    lateinit var content: TextView
    lateinit var col1: LinearLayout
    lateinit var col2: LinearLayout
    lateinit var col3: LinearLayout
    lateinit var col4: LinearLayout
    lateinit var col5: LinearLayout
    lateinit var bibleLayout: ConstraintLayout
    var editionTxt = ""
    var colorCode = "#25B567"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bible_post)
        userPreferences = UserPreferences(this@BiblePostActivity)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        title = findViewById(R.id.bible_title)
        tags = findViewById(R.id.bible_tags)
        content = findViewById(R.id.bible_content)
        col1 = findViewById(R.id.col1)
        col2 = findViewById(R.id.col2)
        col3 = findViewById(R.id.col3)
        col4 = findViewById(R.id.col4)
        col5 = findViewById(R.id.col5)
        postBtn = findViewById(R.id.bible_post_btn)
        bibleLayout = findViewById(R.id.bible_cons_layout)

        editionTxt = intent.extras!!.getString("edition").toString()
        val tagsTxt = intent.extras!!.getString("tags")
        val contentTxt = intent.extras!!.getString("content")

        title.text = "Bible post - " + if (editionTxt == "old") "Old edition" else "New Edition"
        tags.text = tagsTxt
        content.text = contentTxt

        col1.setOnClickListener {
            bibleLayout.setBackgroundColor(Color.parseColor("#25B567"))
            colorCode = "#25B567"
        }
        col2.setOnClickListener {
            bibleLayout.setBackgroundColor(Color.parseColor("#E24B4B"))
            colorCode = "#E24B4B"
        }
        col3.setOnClickListener {
            bibleLayout.setBackgroundColor(Color.parseColor("#6E26A6"))
            colorCode = "#6E26A6"
        }
        col4.setOnClickListener {
            bibleLayout.setBackgroundColor(Color.parseColor("#2C95E1"))
            colorCode = "#2C95E1"
        }
        col5.setOnClickListener {
            bibleLayout.setBackgroundColor(Color.parseColor("#C121C1"))
            colorCode = "#C121C1"
        }
        postBtn.setOnClickListener {
            postBtn.isEnabled = false
            val data = JsonObject()
            data.addProperty("title", title.text.toString())
            data.addProperty("content", contentTxt)
            data.addProperty("tags", tagsTxt)
            data.addProperty("image", "")
            data.addProperty("url", "")
            data.addProperty("type", "bible")
            data.addProperty("userId", Util.userId)
            data.addProperty("colorCode", colorCode)
            postData(data)
        }
    }

    private fun postData(data: JsonObject) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call1: Call<JsonObject?>? =
                            retrofit.postCallHead("Bearer $it", "post", data)
                        call1!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    postBtn.isEnabled = true
                                    data.remove("tags")
                                    data.remove("content")
                                    data.remove("title")
                                    data.remove("userId")
                                    val intent =
                                        Intent(this@BiblePostActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    postBtn.isEnabled = true
                                    val resp = response.errorBody()
                                    val loginresp: JsonObject =
                                        Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("result", errorMessage)
                                }
                                call1.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("AddPostActivity.postData", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@BiblePostActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@BiblePostActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AddPostActivity.postData", e.toString())
        }
    }


    override fun onBackPressed() {
        val intent = Intent(this, BibleActivity::class.java)
        intent.putExtra("type", editionTxt)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        super.onBackPressed()
    }
}