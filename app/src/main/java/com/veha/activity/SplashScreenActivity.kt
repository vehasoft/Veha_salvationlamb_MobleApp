package com.veha.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.UserRslt
import com.veha.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashh_screen)
        userPreferences = UserPreferences(this)

        val content = findViewById<View>(android.R.id.content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            content.viewTreeObserver.addOnDrawListener { false }
        }
        userPreferences.isNightModeEnabled.asLiveData().observe(this) {
            when (it) {
                Util.NIGHT -> {
                    Util.isNight = Util.NIGHT
                }

                Util.DAY -> {
                    Util.isNight = Util.DAY
                }

                Util.DEFAULT -> {
                    Util.isNight = Util.DEFAULT
                }

                else -> {
                    lifecycleScope.launch {
                        userPreferences.saveIsNightModeEnabled(Util.DEFAULT)
                        Util.isNight = Util.DEFAULT
                    }
                }
            }
        }
        userPreferences.authToken.asLiveData().observe(this) { it ->
            if (TextUtils.isEmpty(it) || it.equals("null") || it.isNullOrEmpty()) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                userPreferences.userId.asLiveData().observe(this) {
                    if (TextUtils.isEmpty(it) || it.equals("null") || it.isNullOrEmpty()) {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    Util.userId = it
                }
                userPreferences.textSize.asLiveData().observe(this) {
                    Util.fontSize = it
                }

                Thread.sleep(2000)
                getMyDetails(it)
            }
        }
    }

    private fun getMyDetails(token: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = retrofit.getUser("Bearer $token", Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                            Util.user = loginresp
                            if (loginresp.blocked.toBoolean()){
                                Toast.makeText(this@SplashScreenActivity,resources.getString(R.string.Blocked_account),Toast.LENGTH_LONG).show()
                                val intent = Intent(this@SplashScreenActivity, LoginActivity::class.java)
                                startActivity(intent)
                            }
                            val isWarrior: Boolean =
                                loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"
                            Util.isWarrior = isWarrior
                            Util.isFirst = loginresp.isFreshUser.toBoolean()
                            lifecycleScope.launch {
                                userPreferences.saveIsFirstTime(loginresp.isFreshUser.toBoolean())
                                Util.isFirst = loginresp.isFreshUser.toBoolean()
                            }
                            Thread.sleep(2000)
                            if (loginresp.isVerified.toBoolean()) {
                                val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                val intent = Intent(this@SplashScreenActivity, ForgotPasswordActivity::class.java)
                                intent.putExtra("page", "verify")
                                intent.putExtra("email", loginresp.email)
                                startActivity(intent)
                            }
                        }  else if (response.code() == 401) {
                            Toast.makeText(this@SplashScreenActivity,resources.getString(R.string.Deleted_account),Toast.LENGTH_LONG).show()
                            val intent = Intent(this@SplashScreenActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }else {
                            Log.e("responseee", "fail")
                            Toast.makeText(
                                this@SplashScreenActivity,
                                "Somthing Went Wrong \nLogin again to continue",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(this@SplashScreenActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("Splashscreen", "fail")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("Splashscreen", e.toString())
        }
    }
}