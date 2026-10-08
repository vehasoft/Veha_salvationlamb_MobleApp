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
import com.veha.util.Commons
import com.veha.util.NotificationType
import com.veha.util.Permission
import com.veha.util.PermissionType
import com.veha.util.UserPreferences
import com.veha.util.UserRslt
import com.veha.util.Util
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var userPreferences: UserPreferences

    /** Guards [bailToLogin] — four DataStore observers can each reach a failure path. */
    private var bailedOut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashh_screen)
        userPreferences = UserPreferences(this)

        getBible()
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
                userPreferences.bibleBookmark.asLiveData().observe(this) {
                    Util.bookmarkedBible = it
                }

                getMyDetails(it)
            }
        }
    }
    fun getBible() {
        val file = File(filesDir,"bible.json")
        if (!file.exists()) {
            val retrofitAPI1 = Util.getRetrofit("https://files.salvationlamb.com/")
            val call = retrofitAPI1.getContent()
            call!!.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    try {
                        try {
                            FileOutputStream(file).use {
                                it.write(response.body().toString().toByteArray())
                            }
                            Util.bible = JSONObject(file.readText())
                        } catch (e: Exception) {
                            Log.e("FILE_ERROR", "Error saving JSON to file", e)
                        }
                        //Util.bible = JSONObject(response.body().toString())

//                    Util.oldBible = Util.bible.get("old") as JSONObject
//                    Util.newBible = Util.bible.get("new") as JSONObject
                    } catch (e: java.lang.Exception) {
                        Log.e("bible.error", e.toString())
                        throw RuntimeException(e)
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.e("bible.fail", t.toString())
                }
            })
        } else {
            Util.bible = JSONObject(file.readText())
        }
    }
    private fun getMyDetails(token: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = retrofit.getUser("Bearer $token", Util.userId)
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
                                    this@SplashScreenActivity,
                                    resources.getString(R.string.Blocked_account),
                                    Toast.LENGTH_LONG
                                ).show()
                                val intent =
                                    Intent(this@SplashScreenActivity, LoginActivity::class.java)
                                startActivity(intent)
                            }
                            val isWarrior: Boolean =
                                loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"
                            Util.isWarrior = isWarrior
                            if (loginresp.role == "admin") {
                                Util.isWarrior = true
                            }
                            Util.isFirst = loginresp.isFreshUser.toBoolean()
                            lifecycleScope.launch {
                                userPreferences.saveIsFirstTime(loginresp.isFreshUser.toBoolean())
                                Util.isFirst = loginresp.isFreshUser.toBoolean()
                            }
                            if (loginresp.isVerified.toBoolean()) {
                                // T-025 / G13: the permission map must be loaded BEFORE we route,
                                // otherwise every Util.hasPermission() gate on the destination
                                // screen is evaluated against an empty map.
                                getMyPermission(token) {
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
                                    if (intent.extras!!.getString("type")
                                            .equals(NotificationType.POST.value)
                                    ) {
                                        if (Util.hasPermission(
                                                PermissionType.POST.value,
                                                Permission.READ.value
                                            )
                                        ) {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                ViewPostActivity::class.java
                                            )
                                            intent.putExtra("postId", id)
                                            intent.putExtra("type", NotificationType.POST.value)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                NoPermissionActivity::class.java
                                            )
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else if (intent.extras!!.getString("type")
                                            .equals(NotificationType.USER.value)
                                    ) {
                                        Log.e(
                                            "extraaaa",
                                            intent.extras!!.getString("type").toString()
                                        )
                                        Log.e(
                                            "extraaaa",
                                            intent.extras!!.getString("id").toString()
                                        )
                                        if (Util.hasPermission(
                                                PermissionType.USER.value,
                                                Permission.READ.value
                                            )
                                        ) {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                ViewProfileActivity::class.java
                                            )
                                            intent.putExtra("userId", id)
                                            intent.putExtra("type", NotificationType.USER.value)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                NoPermissionActivity::class.java
                                            )
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else if (intent.extras!!.getString("type")
                                            .equals(NotificationType.WARRIOR.value)
                                    ) {
                                        if (Util.hasPermission(
                                                PermissionType.USER.value,
                                                Permission.EDIT.value
                                            )
                                        ) {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                ApproveRequestActivity::class.java
                                            )
                                            intent.putExtra("userId", id)
                                            intent.putExtra("type", NotificationType.USER.value)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                NoPermissionActivity::class.java
                                            )
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else if (intent.extras!!.getString("type")
                                            .equals(NotificationType.ANNOUNCEMENT.value)
                                    ) {
                                        if (Util.hasPermission(
                                                PermissionType.ANNOUNCEMENT.value,
                                                Permission.READ.value
                                            )
                                        ) {
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
                                            val intent = Intent(
                                                this@SplashScreenActivity,
                                                NoPermissionActivity::class.java
                                            )
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else if (intent.extras!!.getString("type")
                                            .equals(NotificationType.FILE.value)
                                    ) {
                                        val url = intent.extras!!.getString("fileUrl")
                                        val intent = Intent(
                                            this@SplashScreenActivity,
                                            PdfActivity2::class.java
                                        )
                                        intent.putExtra("fileName", id)
                                        intent.putExtra("url", url)
                                        startActivity(intent)
                                        finish()
                                    } else if (intent.extras!!.getString("type")
                                            .equals(NotificationType.EVENT.value)
                                    ) {
                                        val intent = Intent(
                                            this@SplashScreenActivity,
                                            WebViewActivity::class.java
                                        )
                                        intent.putExtra("url", id)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        val intent =
                                            Intent(
                                                this@SplashScreenActivity,
                                                MainActivity::class.java
                                            )
                                        startActivity(intent)
                                        finish()
                                    }
                                    if (intent.extras!!.getString("notificationId") != null) {
                                        readNotification(
                                            intent.extras!!.getString("notificationId").toString()
                                        )
                                    }
                                } else {
                                    val intent =
                                        Intent(this@SplashScreenActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                } // end getMyPermission callback (T-025)
                            } else {
                                val intent = Intent(
                                    this@SplashScreenActivity,
                                    ForgotPasswordActivity::class.java
                                )
                                intent.putExtra("page", "verify")
                                intent.putExtra("email", loginresp.email)
                                startActivity(intent)
                            }
                        } else if (response.code() == 401) {
                            // T-024: was a toast + startActivity with no finish() and no session
                            // clear, so the dead token survived and the 401 repeated next launch.
                            bailToLogin(R.string.Deleted_account)
                        } else {
                            Log.e(
                                "SplashScreenActivity.getMyDetails",
                                "HTTP ${response.code()}"
                            )
                            bailToLogin(R.string.splash_server_unreachable)
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("SplashScreenActivity.getMyDetails", "fail", t)
                        bailToLogin(R.string.splash_server_unreachable)
                    }
                })
            } else {
                bailToLogin(R.string.splash_offline)
            }
        } catch (e: Exception) {
            Log.e("SplashScreenActivity.getMyDetails", e.toString())
            bailToLogin(R.string.splash_server_unreachable)
        }
    }

    /**
     * Single recovery path for a failed session bootstrap (T-024, closes P0 #1 and #2).
     *
     * The splash is the app's only launcher entry point, so a silent failure here left the user
     * staring at the logo forever. Every dead end now clears the proven-unusable session and
     * routes to Login, so the next cold start begins cleanly instead of repeating the failure.
     *
     * Guarded by [bailedOut] because four DataStore observers can re-enter this flow.
     */
    private fun bailToLogin(messageRes: Int) {
        if (bailedOut) return
        bailedOut = true
        Toast.makeText(this, resources.getString(messageRes), Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            try {
                userPreferences.deleteAuthToken()
                userPreferences.deleteUserId()
            } catch (e: Exception) {
                Log.e("SplashScreenActivity.bailToLogin", e.toString())
            }
            Util.userId = null
            Util.user = null
            Util.clearPermissions()
            startActivity(Intent(this@SplashScreenActivity, LoginActivity::class.java))
            finish()
        }
    }

    /**
     * Loads the permission map, then runs [onComplete] — **always**, success or failure.
     *
     * T-025 / `G13`: previously this was fire-and-forget and ran *after* routing, so the
     * destination screen evaluated its `Util.hasPermission()` gates against an empty map.
     * Combined with the fail-open default in `Util.hasPermission`, a failed fetch silently
     * granted every permission. Now routing waits for the map, and a failure is a hard stop
     * (`bailToLogin`) rather than a silent full-access grant.
     */
    public fun getMyPermission(token: String, onComplete: (() -> Unit)? = null) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = retrofit.getPermissions("Bearer $token", Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(
                        call: Call<JsonObject?>,
                        response: Response<JsonObject?>
                    ) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Util.setPermissionMap(
                                Gson().fromJson(
                                    resp?.get("results"),
                                    Map::class.java
                                ) as MutableMap<String, String>?
                            )
                            onComplete?.invoke()
                        } else {
                            Log.e(
                                "SplashScreenActivity.getMyPermission",
                                "HTTP ${response.code()}"
                            )
                            // Do NOT continue with an unknown permission set (G13).
                            if (onComplete != null) {
                                bailToLogin(R.string.splash_permissions_failed)
                            }
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("SplashScreenActivity.getMyPermission", "fail", t)
                        if (onComplete != null) {
                            bailToLogin(R.string.splash_permissions_failed)
                        }
                    }
                })
            } else {
                if (onComplete != null) {
                    bailToLogin(R.string.splash_offline)
                }
            }
        } catch (e: Exception) {
            Log.e("SplashScreenActivity.getMyPermission", e.toString())
            if (onComplete != null) {
                bailToLogin(R.string.splash_permissions_failed)
            }
        }
    }

    fun readNotification(id: String) {
        try {
            val data = JsonObject()
            data.addProperty("isVisited", true)
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this@SplashScreenActivity) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? =
                            retrofit.putReadNotification("Bearer $it", id, data)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() != 200) {
                                    Log.e("code", response.code().toString())
                                    Log.e("err", response.errorBody().toString())
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