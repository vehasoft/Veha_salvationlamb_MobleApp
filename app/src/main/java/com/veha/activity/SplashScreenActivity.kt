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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.veha.adapter.NotificationListAdapter
import com.veha.util.Commons
import com.veha.util.NotificationType
import com.veha.util.Permission
import com.veha.util.PermissionType
import com.veha.util.UserPreferences
import com.veha.util.UserRslt
import com.veha.util.Util
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

                Util.getBible()
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
                            if (loginresp.role == "admin"){
                                Util.isWarrior = true
                            }
                            Util.isFirst = loginresp.isFreshUser.toBoolean()
                            lifecycleScope.launch {
                                userPreferences.saveIsFirstTime(loginresp.isFreshUser.toBoolean())
                                Util.isFirst = loginresp.isFreshUser.toBoolean()
                            }
                            Thread.sleep(2000)
                            if (loginresp.isVerified.toBoolean()) {
                                if (intent.extras != null) {
                                    val bundle = intent.extras
                                    if (bundle != null) {
                                        for (key in bundle.keySet()) {
                                            Log.e(
                                                "intenttt",
                                                key + " : " + if (bundle[key] != null) bundle[key] else "NULL"
                                            )
                                        }
                                    }
                                    val id = intent.extras!!.getString("id")
                                    if (intent.extras!!.getString("type").equals(NotificationType.POST.value)){
                                        if (Util.hasPermission(PermissionType.POST.value, Permission.READ.value)) {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                ViewPostActivity::class.java
                                            )
                                            intent.putExtra("postId", id)
                                            intent.putExtra("type", NotificationType.POST.value)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            val intent = Intent(this@SplashScreenActivity, NoPermissionActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else if (intent.extras!!.getString("type").equals(NotificationType.USER.value)){
                                        Log.e("extraaaa",intent.extras!!.getString("type").toString())
                                        Log.e("extraaaa",intent.extras!!.getString("id").toString())
                                        if (Util.hasPermission(PermissionType.USER.value, Permission.READ.value)) {
                                            val intent = Intent(this@SplashScreenActivity, ViewProfileActivity::class.java)
                                            intent.putExtra("userId", id)
                                            intent.putExtra("type", NotificationType.USER.value)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            val intent = Intent(this@SplashScreenActivity, NoPermissionActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                    }else if (intent.extras!!.getString("type").equals(NotificationType.WARRIOR.value)){
                                        if (Util.hasPermission(PermissionType.USER.value, Permission.EDIT.value)) {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                ApproveRequestActivity::class.java
                                            )
                                            intent.putExtra("userId", id)
                                            intent.putExtra("type", NotificationType.USER.value)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            val intent = Intent(this@SplashScreenActivity, NoPermissionActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                    }else if (intent.extras!!.getString("type").equals(NotificationType.ANNOUNCEMENT.value)){
                                        if (Util.hasPermission(PermissionType.ANNOUNCEMENT.value, Permission.READ.value)) {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                ViewPostActivity::class.java
                                            )
                                            intent.putExtra("postId", id)
                                            intent.putExtra(
                                                "type",
                                                NotificationType.ANNOUNCEMENT.value
                                            )
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            val intent = Intent(this@SplashScreenActivity, NoPermissionActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                    }else if (intent.extras!!.getString("type").equals(NotificationType.FILE.value)){
                                        val url = intent.extras!!.getString("fileUrl")
                                        val intent = Intent(this@SplashScreenActivity, PdfActivity2::class.java)
                                        intent.putExtra("fileName", id)
                                        intent.putExtra("url", url)
                                        startActivity(intent)
                                        finish()
                                    } else if (intent.extras!!.getString("type").equals(NotificationType.EVENT.value)){
                                        val intent = Intent(this@SplashScreenActivity, WebViewActivity::class.java)
                                        intent.putExtra("url", id)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        val intent =
                                            Intent(this@SplashScreenActivity, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    if (intent.extras!!.getString("notificationId") != null){
                                        readNotification(intent.extras!!.getString("notificationId").toString())
                                    }
                                } else {
                                    val intent =
                                        Intent(this@SplashScreenActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                getMyPermission(token)
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
    private fun getMyPermission(token: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = retrofit.getPermissions("Bearer $token", Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Log.e("reslt",resp.toString())
                            Util.permissionMap = Gson().fromJson(resp?.get("results"), Map::class.java) as MutableMap<String, String>?
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
    fun readNotification(id: String){
        try {
            val data = JsonObject()
            data.addProperty("isVisited", true)
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this@SplashScreenActivity) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.putReadNotification("Bearer $it", id, data)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {

                                } else {
                                    Log.e("code",response.code().toString())
                                    Log.e("err",response.errorBody().toString())
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("NotificationListAdapter.readNotification", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationListAdapter.readNotification", e.toString())
        }
    }
}