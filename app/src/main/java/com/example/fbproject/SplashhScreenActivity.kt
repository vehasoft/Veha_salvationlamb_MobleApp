package com.example.fbproject

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.util.Commons
import com.example.util.UserPreferences
import com.example.util.UserRslt
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import java.security.AccessController.getContext

class SplashhScreenActivity : AppCompatActivity() {
    private lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashh_screen)
        userPreferences = UserPreferences(this)
        val nightModeFlags: Int = this.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                lifecycleScope.launch {
                    userPreferences.saveIsNightModeEnabled(true)
                    Util.isNight = true
                }
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                lifecycleScope.launch {
                    userPreferences.saveIsNightModeEnabled(false)
                    Util.isNight = false
                }
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                lifecycleScope.launch {
                    userPreferences.saveIsNightModeEnabled(false)
                    Util.isNight = false
                }
            }
        }

        val content = findViewById<View>(android.R.id.content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            content.viewTreeObserver.addOnDrawListener { false }
        }
        userPreferences.authToken.asLiveData().observe(this) { it ->
            if (TextUtils.isEmpty(it) || it.equals("null") || it.isNullOrEmpty()) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                userPreferences.userId.asLiveData().observe(this) {
                    Util.userId = it
                }
                userPreferences.textSize.asLiveData().observe(this){
                    Util.fontSize = it
                }
                userPreferences.isNightModeEnabled.asLiveData().observe(this) {
                    if (it == null || !it) {
                        Util.isNight = false
                    } else {
                        Util.isNight = it
                    }

                    Log.e("isnight", it.toString())
                }
                getMyDetails(it)
            }
        }
        Thread.sleep(2000)
    }
    private fun getMyDetails(token: String) {
        if (Commons().isNetworkAvailable(this)) {
            val retrofit = Util.getRetrofit()
            val call: Call<JsonObject?>? = retrofit.getUser("Bearer $token", Util.userId)
            call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.code() == 200) {
                        val resp = response.body()
                        val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                        Util.user = loginresp
                        val isWarrior: Boolean = loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"
                        Util.isWarrior = isWarrior
                        Util.isFirst = loginresp.isFreshUser.toBoolean()
                        lifecycleScope.launch {
                            userPreferences.saveIsFirstTime(loginresp.isFreshUser.toBoolean())
                            Util.isFirst = loginresp.isFreshUser.toBoolean()
                        }
                        Thread.sleep(2000)
                        val intent = Intent(this@SplashhScreenActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                         Log.e("responseee", "fail")
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("Splashscreen", "fail")
                }
            })
        }
    }
}