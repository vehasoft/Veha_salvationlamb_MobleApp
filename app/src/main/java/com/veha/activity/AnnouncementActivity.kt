package com.veha.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.veha.adapter.AnnouncementAdapter
import com.veha.adapter.NotificationListAdapter
import com.veha.util.Commons
import com.veha.util.NotificationList
import com.veha.util.Posts
import com.veha.util.UserPreferences
import com.veha.util.Util
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class AnnouncementActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    lateinit var list: RecyclerView
    lateinit var nodata: LinearLayout
    lateinit var logo: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcement)

        userPreferences = UserPreferences(this)
        list = findViewById(R.id.announcement_recycler)
        nodata = findViewById(R.id.no_data)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        getAnnouncenents()

    }

    private fun getAnnouncenents(postlist: ArrayList<Posts> = ArrayList()){

        try {
            if (Commons().isNetworkAvailable(this@AnnouncementActivity)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this@AnnouncementActivity) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getAnnouncements("Bearer $it")
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                Log.e("response",response.body().toString())
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("announcement"), JsonArray::class.java)
                                    for (notification in loginresp) {
                                        val pos = Gson().fromJson(notification, Posts::class.java)
                                        postlist.add(pos)
                                    }
                                    if (postlist.size <= 0) {
                                        list.visibility = View.GONE
                                        nodata.visibility = View.VISIBLE
                                    } else {
                                        list.visibility = View.VISIBLE
                                        nodata.visibility = View.GONE

                                        list.layoutManager = LinearLayoutManager(this@AnnouncementActivity)
                                        list.adapter = AnnouncementAdapter(postlist, this@AnnouncementActivity,this@AnnouncementActivity)
                                    }

                                } else if (response.code() == 401) {
                                    Toast.makeText(
                                        this@AnnouncementActivity,
                                        resources.getString(R.string.Deleted_account),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val intent = Intent(this@AnnouncementActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("HomeFragment.getMyDetails", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@AnnouncementActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@AnnouncementActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment.getMyDetails", e.toString())
        }
    }
}