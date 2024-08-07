package com.veha.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.veha.adapter.BibleAdapter
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.Util
import kotlinx.coroutines.launch
import org.chromium.base.Log
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Response

class BibleActivity : AppCompatActivity() {
    lateinit var logo: ImageView
    lateinit var recyclerView: RecyclerView
    lateinit var content: TextView
    lateinit var txtLinear: LinearLayout
    lateinit var copy: Button
    lateinit var post: Button
    lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bible)
        userPreferences = UserPreferences(this@BibleActivity)
        recyclerView = findViewById(R.id.recycler_view)
        logo = findViewById(R.id.prod_logo)
        content = findViewById(R.id.content)
        copy = findViewById(R.id.copy_txt)
        post = findViewById(R.id.post_txt)
        txtLinear = findViewById(R.id.txt_linear)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        var type: String = intent.extras!!.getString("type").toString()
        var details: String = intent.extras!!.getString("details").toString()
        var obj: JSONArray
        if (type == "old"){
            obj = JSONArray(Util.bible.get("Old").toString())
            type = "list"
        } else if (type == "new"){
            obj = JSONArray(Util.bible.get("New").toString())
            type = "list"
        } else {
            obj = JSONArray(intent.extras!!.get("content").toString())
        }
            recyclerView.visibility = View.VISIBLE
            txtLinear.visibility = View.GONE
            recyclerView.layoutManager = LinearLayoutManager(this)
            //Log.e("biblejsonarray",obj.toString())
            recyclerView.adapter = BibleAdapter(this,obj,type,this,details)
        copy.setOnClickListener {
            val clipBoardManager: ClipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData = ClipData.newPlainText("bible",content.text.toString())
            clipBoardManager.setPrimaryClip(clipData)
        }
        post.setOnClickListener {
            post.isEnabled = false
            val data = JsonObject()
            data.addProperty("title", "bible content")
            data.addProperty("content", content.text.toString())
            data.addProperty("tags", "")
            data.addProperty("image", "")
            data.addProperty("url", "")
            data.addProperty("type", "text")
            data.addProperty("userId", Util.userId)
            postData(data)

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }private fun postData(data: JsonObject) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call1: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it", "post", data)
                        call1!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    post.isEnabled = true
                                    val intent = Intent(this@BibleActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    post.isEnabled = true
                                    /*val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("result", errorMessage)*/
                                }
                                call1.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                android.util.Log.e("AddPostActivity.postData", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@BibleActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@BibleActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddPostActivity.postData", e.toString())
        }
    }

}