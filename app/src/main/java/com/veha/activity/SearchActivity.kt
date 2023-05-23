package com.veha.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.veha.adapter.SearchAdapter
import com.veha.activity.R
import com.veha.util.*
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class SearchActivity : AppCompatActivity() {

    private lateinit var search: EditText
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    lateinit var userPreferences: UserPreferences
    lateinit var profilelist: ArrayList<PostUser>
    lateinit var postlist: ArrayList<Posts>
    lateinit var dialog: AlertDialog
    var currentTab: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        Log.e("current tab", currentTab.toString())
        userPreferences = UserPreferences(this)
        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        tabLayout = findViewById(R.id.tabLayout)
        search = findViewById(R.id.search)
        val home = tabLayout.newTab()
        val profile = tabLayout.newTab()
        home.tag = "Posts"
        profile.tag = "Profiles"
        home.text = "Posts"
        profile.text = "Profiles"

        tabLayout.addTab(home,0)
        tabLayout.addTab(profile,1)
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        viewPager = findViewById(R.id.viewPager)
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    viewPager.currentItem = tab.position
                    currentTab = tab.position
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    viewPager.currentItem = tab.position
                    currentTab = tab.position
                }
            }
        })
        search.addTextChangedListener {
            if (it != null) {
                for (fragment in supportFragmentManager.fragments) {
                    supportFragmentManager.beginTransaction().remove(fragment).commit()
                }
                getContent(it.toString())
            }
        }
    }
    private fun getContent(text: String) {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val data = JsonObject()
            data.addProperty("query", text)
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(this) {
                if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? = retrofit.getSearch("Bearer $it", text)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            if (response.code() == 200) {
                                val resp = response.body()
                                profilelist = ArrayList()
                                postlist = ArrayList()
                                val loginresp: JsonObject =
                                    Gson().fromJson(resp?.get("results"), JsonObject::class.java)
                                val allProfiles: JsonArray =
                                    Gson().fromJson(loginresp.get("users"), JsonArray::class.java)
                                val allPosts: JsonArray = Gson().fromJson(loginresp.get("posts"), JsonArray::class.java)

                                for (post in allPosts) {
                                    val pos = Gson().fromJson(post, Posts::class.java)
                                    postlist.add(pos)
                                }
                                for (profile in allProfiles) {
                                    val pos = Gson().fromJson(profile, PostUser::class.java)
                                    profilelist.add(pos)
                                }
                                viewPager.adapter = SearchAdapter(
                                    this@SearchActivity,
                                    supportFragmentManager,
                                    tabLayout.tabCount,
                                    profilelist,
                                    postlist
                                )
                                Log.e("current tab", currentTab.toString())
                                viewPager.currentItem = currentTab
                            }
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                        }

                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            Log.e("SearchActivity.getContent", "fail")
                        }
                    })
                } else {
                    Toast.makeText(this, "Somthing Went Wrong \nLogin again to continue", Toast.LENGTH_LONG).show()
                    lifecycleScope.launch {
                        userPreferences.deleteAuthToken()
                        userPreferences.deleteUserId()
                    }
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}