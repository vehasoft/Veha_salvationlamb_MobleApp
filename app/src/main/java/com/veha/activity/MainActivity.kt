package com.veha.activity

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.veha.adapter.TabAdapter
import com.veha.activity.R
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.UserRslt
import com.veha.util.Util
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
class MainActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    lateinit var dialog: android.app.AlertDialog
    lateinit var search: ImageView
    lateinit var logo: ImageView
    var i = 0
    var userType: String = ""
    private fun requestPermission() {
        //on below line we are requesting the read external storage permissions.
        ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, CAMERA_SERVICE, NOTIFICATION_SERVICE), 255)
        dialog.dismiss()
    }
    private fun checkPermission() {
        // in this method we are checking if the permissions are granted or not and returning the result.
        val result = ContextCompat.checkSelfPermission(applicationContext, READ_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, CAMERA_SERVICE)
        val result2 = ContextCompat.checkSelfPermission(applicationContext, NOTIFICATION_SERVICE)
        if( !(result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED)){
            requestPermission()
            dialog.dismiss()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        userPreferences = UserPreferences(this@MainActivity)
        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        dialog.dismiss()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dialog.dismiss()
        checkPermission()
        getMyDetails()
        if (Util.isFirst != null && Util.isFirst){
            if (Util.isWarrior) {
                val nagDialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar)
                nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                nagDialog.setCancelable(false)
                nagDialog.setContentView(R.layout.preview_image)
                val btnClose: Button = nagDialog.findViewById(R.id.btnIvClose)
                val img: ImageView = nagDialog.findViewById(R.id.iv_preview_image)
                img.setImageResource(R.drawable.covre_pic)
                btnClose.setOnClickListener {
                    firstTime()
                    nagDialog.dismiss()
                }

                nagDialog.show()
            } else{
                Commons().makeWarrior(this,this)
                firstTime()
            }
        }
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        Log.e("isnight",Util.isNight.toString())
        when (Util.isNight){
            Util.DAY -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            Util.NIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            Util.DEFAULT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }


        }
        val userPreferences = UserPreferences(this)
        userPreferences.authToken.asLiveData().observe(this) {
            if (TextUtils.isEmpty(it) && it.equals("null") && it.isNullOrEmpty()) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
        userPreferences.userId.asLiveData().observe(this) {
            Util.userId = it
        }
        search = findViewById(R.id.search)
        search.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }
        menu.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@MainActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            if (Util.isWarrior){ popup.menu.findItem(R.id.warrior).isVisible = false }
            if (Util.user.isReviewState.toBoolean()) { popup.menu.findItem(R.id.warrior).isVisible = false }
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.warrior -> {
                       Commons().makeWarrior(this,this)
                    }
                    R.id.logout ->{
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                        builder.setMessage("Do you want to Logout ?")
                        builder.setTitle("Alert !")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish()
                            lifecycleScope.launch {
                                userPreferences.deleteAuthToken()
                                userPreferences.deleteUserId()
                            }
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }
                        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }

                    R.id.edit_profile ->{
                        val intent = Intent(this@MainActivity, EditProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.fav ->{
                        val intent = Intent(this@MainActivity, FavoritesActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.settings -> {
                        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                    /*R.id.nightmode ->{
                        if (Util.isNight){
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            Util.isNight = false
                            night.title = "Day Mode"
                            lifecycleScope.launch { userPreferences.saveIsNightModeEnabled(false) }
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            Util.isNight = true
                            night.title = "Night Mode"
                            lifecycleScope.launch { userPreferences.saveIsNightModeEnabled(true) }
                        }
                    }*/
                }
                true
            })
            popup.show()
        }
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val home = tabLayout.newTab()
        val profile = tabLayout.newTab()
        val pdf = tabLayout.newTab()
        val adminVideo = tabLayout.newTab()
        val adminAudio = tabLayout.newTab()
        home.icon = getDrawable(R.drawable.ic_baseline_home_24)
        profile.icon = getDrawable(R.drawable.profile)
        pdf.icon = getDrawable(R.drawable.ic_baseline_folder_24)
        adminVideo.icon = getDrawable(R.drawable.ic_baseline_video_library_24)
        adminAudio.icon = getDrawable(R.drawable.ic_baseline_audio_file_24)
        home.tag = "Home"
        profile.tag = "Profile"
        adminVideo.tag = "video"
        adminAudio.tag = "audio"
        pdf.tag = "Files"/*
        home.text = "Home"
        profile.text = "Profile"
        pdf.text = "Files"*/
        tabLayout.addTab(home,0)
        tabLayout.addTab(pdf,1)
        tabLayout.addTab(adminVideo,2)
        tabLayout.addTab(adminAudio,3)
        tabLayout.addTab(profile,4)
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        val adapter = TabAdapter(this@MainActivity,this@MainActivity.supportFragmentManager,tabLayout.tabCount)
        val viewPager : ViewPager = findViewById(R.id.viewPager)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    viewPager.currentItem = tab.position
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }
    private fun getMyDetails() {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(this) {
                if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", Util.userId)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            if (response.code() == 200) {
                                val resp = response.body()
                                val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                                Log.e("resp", loginresp.toString())
                                Util.user = loginresp
                                Util.isFirst = loginresp.isFreshUser.toBoolean()
                                val isWarrior: Boolean =
                                    loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"
                                Util.isWarrior = isWarrior
                                userType = if (isWarrior) Util.WARRIOR else Util.USER
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                            }
                        }

                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            Log.e("MainActivity.getDetails", "fail")
                        }
                    })
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Somthing Went Wrong \nLogin again to continue",
                        Toast.LENGTH_LONG
                    ).show()
                    lifecycleScope.launch {
                        userPreferences.deleteAuthToken()
                        userPreferences.deleteUserId()
                    }
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
    private fun firstTime() {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(this) {
                if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? = retrofit.putFreshUser("Bearer $it", Util.userId)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            Log.e("firstttime", response.code().toString())
                            Util.isFirst = false
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                        }

                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            Log.e("MainActivity.firstTime", "fail")
                        }
                    })
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Somthing Went Wrong \nLogin again to continue",
                        Toast.LENGTH_LONG
                    ).show()
                    lifecycleScope.launch {
                        userPreferences.deleteAuthToken()
                        userPreferences.deleteUserId()
                    }
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
    override fun onPause() {
        super.onPause()
        dialog.dismiss()
    }
}