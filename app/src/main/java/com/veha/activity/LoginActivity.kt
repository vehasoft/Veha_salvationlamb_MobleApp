package com.veha.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.veha.util.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog

    private lateinit var signupButton: Button
    private lateinit var loginButton: Button
    private lateinit var forgotPassword: TextView
    private lateinit var termsAndConditions: TextView
    private lateinit var privacyPolicy: TextView
    private lateinit var email: TextView
    private lateinit var password: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences = UserPreferences(this@LoginActivity)
        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        setContentView(R.layout.activity_login)

        signupButton = findViewById(R.id.signup_btn)
        loginButton = findViewById(R.id.login_btn)
        forgotPassword = findViewById(R.id.forgot_pwd)
        termsAndConditions = findViewById(R.id.terms)
        privacyPolicy = findViewById(R.id.privacy)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)

        signupButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        forgotPassword.setOnClickListener {
            val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
        termsAndConditions.setOnClickListener {
            val intent = Intent(this@LoginActivity, WebViewActivity::class.java)
            intent.putExtra("WebPageName", "terms")
            startActivity(intent)
        }
        privacyPolicy.setOnClickListener {
            val intent = Intent(this@LoginActivity, WebViewActivity::class.java)
            intent.putExtra("WebPageName", "privacy")
            startActivity(intent)
        }
        loginButton.setOnClickListener {
            val emailstr = email.text.toString()
            val passwordstr = password.text.toString()
            val data = JsonObject()
            data.addProperty("email", emailstr)
            data.addProperty("password", passwordstr)
            data.addProperty("isMobile", true)
            if (!Util.isValidEmail(emailstr))
                Toast.makeText(this, "Invalid Email", Toast.LENGTH_LONG).show()
            else if (!Util.isValidPassword(passwordstr))
                Toast.makeText(this, "Invalid Password", Toast.LENGTH_LONG).show()
            else
                login(data)
        }

    }

    private fun login(data: JsonObject) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = retrofit.postCall("login", data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: Loginresp = Gson().fromJson(resp?.get("result"), Loginresp::class.java)
                            Util.isFirst = loginresp.isFreshUser.toBoolean()
                            Util.isWarrior = loginresp.isWarrior.toBoolean()
                            Util.userId = loginresp.id
                            lifecycleScope.launch {
                                userPreferences.saveAuthToken(loginresp.token)
                                userPreferences.saveUserId(loginresp.id)
                                userPreferences.saveIsNightModeEnabled(Util.DEFAULT)
                                userPreferences.saveIsFirstTime(loginresp.isFreshUser.toBoolean())
                                Util.isFirst = loginresp.isFreshUser.toBoolean()
                                Util.isWarrior = loginresp.isWarrior.toBoolean()
                                Util.userId = loginresp.id
                                getMyDetails(loginresp.token)
                                /*val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()*/
                            }

                        } else if (response.code() == 401) {
                            Toast.makeText(this@LoginActivity,resources.getString(R.string.Deleted_account),Toast.LENGTH_LONG).show()
                            val intent = Intent(this@LoginActivity, LoginActivity::class.java)
                            startActivity(intent)
                        } else {
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("result", errorMessage)
                            if (errorMessage.contains("Invalid password", true)) {
                                Toast.makeText(this@LoginActivity, "INVALID PASSWORD", Toast.LENGTH_LONG).show()
                            } else if (errorMessage.contains("Invalid Email-Id", true)) {
                                Toast.makeText(this@LoginActivity, "INVALID USER", Toast.LENGTH_LONG).show()
                            }
                        }
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("LoginActivity.login()", "fail")
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("LoginActivity.login", e.toString())
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
                                Toast.makeText(this@LoginActivity,resources.getString(R.string.Blocked_account),Toast.LENGTH_LONG).show()
                                val intent = Intent(this@LoginActivity, LoginActivity::class.java)
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
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
                                intent.putExtra("page", "verify")
                                intent.putExtra("email", loginresp.email)
                                startActivity(intent)
                            }
                        }  else if (response.code() == 401) {
                            Toast.makeText(this@LoginActivity,resources.getString(R.string.Deleted_account),Toast.LENGTH_LONG).show()
                            val intent = Intent(this@LoginActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }else {
                            Log.e("responseee", "fail")
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("LoginActivity.getMyDetails", "fail")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("LoginActivity.getMyDetails", e.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
            dialog.dismiss()
        
    }
}

