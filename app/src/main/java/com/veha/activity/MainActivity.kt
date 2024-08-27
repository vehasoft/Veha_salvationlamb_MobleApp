package com.veha.activity

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.veha.adapter.TabAdapter
import com.veha.util.Commons
import com.veha.util.Permission
import com.veha.util.PermissionType
import com.veha.util.UserPreferences
import com.veha.util.UserRslt
import com.veha.util.Util
import kotlinx.coroutines.launch
import org.json.JSONObject
import pl.droidsonroids.gif.GifImageView
import retrofit2.Call
import retrofit2.Response
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    lateinit var search: ImageView
    lateinit var logo: ImageView
    lateinit var viewPager: ViewPager
    var userType: String = ""

    private lateinit var notification: ImageView
    private lateinit var announcement: ImageView
    private lateinit var menu: ImageView
    private lateinit var bannerClose: Button
    private lateinit var banner: ConstraintLayout
    private lateinit var makeWarrior: TextView
    private lateinit var notificationCount: TextView
    private lateinit var makeWarriorGif: GifImageView

    var storagePermissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        READ_EXTERNAL_STORAGE, CAMERA_SERVICE, POST_NOTIFICATIONS, NOTIFICATION_SERVICE
    )

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    var storagePermissions33 = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.CAMERA,
        POST_NOTIFICATIONS,
    )

    private fun permissions(): Array<String> {
        val p: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storagePermissions33
        } else {
            storagePermissions
        }
        return p
    }

    private fun requestPermission() {
        //on below line we are requesting the read external storage permissions.
        permissions().let {
            ActivityCompat.requestPermissions(
                this,
                it,
                255
            )
        }
    }

    private fun checkPermission() {
        // in this method we are checking if the permissions are granted or not and returning the result.
        val result = ContextCompat.checkSelfPermission(applicationContext, READ_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, CAMERA_SERVICE)
        val result2 = ContextCompat.checkSelfPermission(applicationContext, NOTIFICATION_SERVICE)
        if (!(result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED)) {
            requestPermission()
        }
    }

    private val rotationObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            requestedOrientation =
                if (Settings.System.getInt(
                        contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,
                        0
                    ) == 1
                ) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation =
            if (Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                ) == 1
            ) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        contentResolver.registerContentObserver(
            Settings.System.getUriFor
                (Settings.System.ACCELEROMETER_ROTATION),
            true, rotationObserver
        )
        when (Util.isNight) {
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userPreferences = UserPreferences(this@MainActivity)

        notification = findViewById(R.id.notification)
        announcement = findViewById(R.id.announcement)
        menu = findViewById(R.id.menu)
        bannerClose = findViewById(R.id.banner_close)
        banner = findViewById(R.id.banner)
        makeWarrior = findViewById(R.id.makewarrior)
        makeWarriorGif = findViewById(R.id.makewarrior_gif)
        notificationCount = findViewById(R.id.notification_count)

        checkPermission()
        getMyDetails()
        getNotificationCount()
        if (Util.isFirst != null && Util.isFirst) {
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
            } /*else{
                Commons().makeWarrior(this,this)
                firstTime()
            }*/
        }
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        notification.setOnClickListener {
            val intent = Intent(this, NotificationViewActivity::class.java)
            startActivity(intent)
        }
        announcement.setOnClickListener {
            if (Util.hasPermission(PermissionType.ANNOUNCEMENT.value, Permission.READ.value)) {
                val intent = Intent(this, AnnouncementActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this@MainActivity, NoPermissionActivity::class.java)
                startActivity(intent)
            }
        }
        bannerClose.setOnClickListener {
            banner.visibility = View.GONE
        }
        banner.setOnClickListener {
            if (!Util.isWarrior && !Util.user.isReviewState.toBoolean()) {
                Commons().makeWarrior(this@MainActivity, this)
                banner.visibility = View.GONE
            }
        }
        val userPreferences = UserPreferences(this)
        userPreferences.authToken.asLiveData().observe(this) {
            if (TextUtils.isEmpty(it) && it.equals("null") && it.isNullOrEmpty()) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
        userPreferences.fcmToken.asLiveData().observe(this) {
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
            if (Util.isWarrior) {
                popup.menu.findItem(R.id.warrior).isVisible = false
            }
            if (Util.user.isReviewState.toBoolean()) {
                popup.menu.findItem(R.id.warrior).isVisible = false
            }
            popup.menu.findItem(R.id.feedback).isVisible = true
            popup.menu.findItem(R.id.invite).isVisible = true
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.warrior -> {
                        Commons().makeWarrior(this, this)
                    }
                    R.id.feedback -> {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                        builder.setTitle("FEEDBACK FORM")
                        val view = View.inflate(this@MainActivity, R.layout.feedback_form, null)
                        builder.setView(view)
                        val feedbackType: Spinner = view.findViewById(R.id.feedback_type)
                        val feedback: EditText = view.findViewById(R.id.feedback)
                        val list = ArrayList<String>()
                        list.add("Select")
                        list.add("Suggestions")
                        list.add("Customer Support")
                        list.add("Performance")
                        var feedbackTypeTxt = ""
                        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, list)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        feedbackType.adapter = adapter
                        feedbackType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                                if (list[pos] != "Select") {
                                    feedbackTypeTxt = list[pos].toString()
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                        builder.setCancelable(false)
                        builder.setPositiveButton("Send") { dialog: DialogInterface?, _: Int ->
                            //apicall
                            val data = JsonObject()
                            data.addProperty("userName",Util.user.name)
                            data.addProperty("email",Util.user.email)
                            data.addProperty("type",feedbackTypeTxt)
                            data.addProperty("feedback",feedback.text.toString())
                            postFeedback(data)
                            dialog?.cancel()

                        }
                        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }
                        builder.create().show()
                    }
                    R.id.invite -> {
                        try {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salvation Lamb")
                        var shareMessage = " Hey friends! \n" +
                                "\n" +
                                "Exciting news! I've just joined Salvation Lamb, a vibrant new social media platform where we can connect, share, and discover together! \n" +
                                "\n" +
                                "Join me and let's stay connected like never before. Here's why you'll love it:\n" +
                                "It's all about making connections and having fun! Click the link below to download Salvation Lamb and join me on this journey. Let's create something awesome together!\n" +
                                "\n" +
                                "[App Store/Google Play Store Link]\n" +
                                "\n" +
                                "Can't wait to see you there! "
                        shareMessage = """
                    $shareMessage                    
                    """.trimIndent()
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                        startActivity(Intent.createChooser(shareIntent, "choose one"))
                    } catch (e: Exception) {
                        Log.e("exception", e.toString())
                    }
                    }

                    R.id.logout -> {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                        builder.setMessage("Do you want to Logout?")
                        builder.setTitle("Logout")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                            finish()
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

                    R.id.edit_profile -> {
                        if (Util.hasPermission(PermissionType.PROFILE.value, Permission.EDIT.value)) {
                            val intent = Intent(this@MainActivity, EditProfileActivity::class.java)
                            startActivity(intent)
                        } else {
                            val intent = Intent(this@MainActivity, NoPermissionActivity::class.java)
                            startActivity(intent)
                        }
                    }

                    R.id.fav -> {
                        val intent = Intent(this@MainActivity, FavoritesActivity::class.java)
                        startActivity(intent)
                    }

                    R.id.settings -> {
                        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }
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
        val bibleBook = tabLayout.newTab()
        home.icon = getDrawable(R.drawable.ic_baseline_home_24)
        profile.icon = getDrawable(R.drawable.profile)
        pdf.icon = getDrawable(R.drawable.ic_baseline_folder_24)
        adminVideo.icon = getDrawable(R.drawable.ic_baseline_video_library_24)
        adminAudio.icon = getDrawable(R.drawable.ic_baseline_audio_file_24)
        bibleBook.icon = getDrawable(R.drawable.bible_book)
        home.tag = "Home"
        profile.tag = "Profile"
        adminVideo.tag = "video"
        adminAudio.tag = "audio"
        pdf.tag = "Files"
        bibleBook.tag = "Bible"
        tabLayout.addTab(home, 0)
        tabLayout.addTab(pdf, 1)
        tabLayout.addTab(bibleBook, 2)
        tabLayout.addTab(adminVideo, 3)
        tabLayout.addTab(adminAudio, 4)
        tabLayout.addTab(profile, 5)
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        val adapter = TabAdapter(
            this@MainActivity,
            this@MainActivity.supportFragmentManager,
            tabLayout.tabCount
        )
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (Util.player != null) {
                    Util.player.stop()
                }
                if (tab != null) {
                    viewPager.currentItem = tab.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
        if (intent.extras != null){
            viewPager.currentItem = intent.extras!!.getInt("gotopage")
        }
    }

    public fun getMyDetails() {
        if (Util.userId == null) {
            userPreferences.userId.asLiveData().observe(this){
                Util.userId = it
            }
        }
        userPreferences.fcmToken.asLiveData().observe(this){
            if (it.isNullOrEmpty()){
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: UserRslt =
                                        Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                                    Util.user = loginresp
                                    if (loginresp.blocked.toBoolean()) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            resources.getString(R.string.Blocked_account),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        val intent =
                                            Intent(this@MainActivity, LoginActivity::class.java)
                                        startActivity(intent)
                                    }
                                    Util.isFirst = loginresp.isFreshUser.toBoolean()
                                    val isWarrior: Boolean = loginresp.isWarrior.toBoolean()
                                    Util.isWarrior = isWarrior
                                    userType = if (isWarrior) Util.WARRIOR else Util.USER
                                    if (loginresp.isWarrior.toBoolean()) {
                                        banner.visibility = View.GONE
                                    } else if (loginresp.isReviewState.toBoolean()) {
                                        banner.visibility = View.VISIBLE
                                        makeWarrior.text =
                                            "Your warrior request is \nwaiting for admin approval"
                                        makeWarriorGif.visibility = View.GONE
                                    } else {
                                        banner.visibility = View.VISIBLE
                                        makeWarrior.text = "Make Me Warrior"
                                        makeWarriorGif.visibility = View.VISIBLE
                                    }
                                } else if (response.code() == 401) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        resources.getString(R.string.Deleted_account),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val intent =
                                        Intent(this@MainActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    Log.e("code",response.code().toString())
                                    Log.e("err",response.errorBody().toString())
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("MainActivity.getDetails", "fail$t")
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
        } catch (e: Exception) {
            Log.e("MainActivity.getMyDetails", e.toString())
        }
    }

    private fun firstTime() {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? =
                            retrofit.putFreshUser("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    Log.e("firstttime", response.code().toString())
                                    Util.isFirst = false
                                } else {
                                    Log.e("code", response.code().toString())
                                    Log.e("err", response.errorBody().toString())
                                }
                            }
                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
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
        } catch (e: Exception) {
            Log.e("MainActivity.firstTime", e.toString())
        }
    }
    private fun getNotificationCount() {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? =
                            retrofit.getNotificationCount("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val count = Integer.parseInt(resp?.get("count").toString())
                                    if (count > 0 ){
                                        notificationCount.visibility = View.VISIBLE
                                        notificationCount.text = count.toString()
                                    }
                                } else {
                                    Log.e("code",response.code().toString())
                                    Log.e("err",response.errorBody().toString())
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
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
        } catch (e: Exception) {
            Log.e("MainActivity.firstTime", e.toString())
        }
    }
    private fun postFeedback(data: JsonObject) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? =
                            retrofit.postCallHead("Bearer $it", "feedback",data)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    Toast.makeText(this@MainActivity,"Feedback submitted successfully",Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this@MainActivity,"Feedback submission failed",Toast.LENGTH_LONG).show()
                                    Log.e("code",response.code().toString())
                                    Log.e("err",response.errorBody().toString())
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
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
        } catch (e: Exception) {
            Log.e("MainActivity.firstTime", e.toString())
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (viewPager.currentItem == 0) {
            exitProcess(-1)
        } else {
            viewPager.currentItem = 0
        }
    }
}