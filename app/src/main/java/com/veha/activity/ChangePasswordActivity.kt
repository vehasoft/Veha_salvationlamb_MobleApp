package com.veha.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.Util
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var passwordTxt: String
    private lateinit var cnfmPasswordTxt: String
    private lateinit var oldPasswordTxt: String

    private lateinit var changePasswordButton: Button
    private lateinit var cancelButton: Button
    private lateinit var oldPasswordOp: TextInputLayout
    private lateinit var newPasswordTextView: TextView
    private lateinit var oldPasswordTextView: TextView
    private lateinit var cnfmPasswordTextView: TextView

    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        userPreferences = UserPreferences(this)

        changePasswordButton = findViewById(R.id.change_pwd_btn)
        oldPasswordOp = findViewById(R.id.old_pwd_op)
        newPasswordTextView = findViewById(R.id.new_pwd)
        cnfmPasswordTextView = findViewById(R.id.cnfm_pwd)
        oldPasswordTextView = findViewById(R.id.old_pwd)
        cancelButton = findViewById(R.id.cancel_btn)

        val email = intent.getStringExtra("email")
        val otp = intent.getStringExtra("otp")
        if (TextUtils.isEmpty(email?.trim())) oldPasswordOp.visibility =
            View.VISIBLE else oldPasswordOp.visibility =
            View.GONE

        changePasswordButton.setOnClickListener {
            passwordTxt = newPasswordTextView.text.toString()
            cnfmPasswordTxt = cnfmPasswordTextView.text.toString()
            oldPasswordTxt = oldPasswordTextView.text.toString()
            if (!Util.isValidPassword(passwordTxt)) {
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8",
                    Toast.LENGTH_LONG
                ).show()
                newPasswordTextView.error =
                    "Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8"
            } else if (TextUtils.isEmpty(passwordTxt.trim())) {
                Toast.makeText(this@ChangePasswordActivity, "Enter Password", Toast.LENGTH_LONG)
                    .show()
                newPasswordTextView.error = "Enter Password"
            } else if (!passwordTxt.equals(cnfmPasswordTxt, false)) {
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "New Password and confirm password are not same",
                    Toast.LENGTH_LONG
                ).show()
                newPasswordTextView.error = "New Password and confirm password are not same"
            } else if (oldPasswordTxt.equals(cnfmPasswordTxt, false)) {
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "new password is same as old password",
                    Toast.LENGTH_LONG
                ).show()
                newPasswordTextView.error = "new password is same as old password"
            } else {
                changePasswordButton.isEnabled = false
                if (TextUtils.isEmpty(email?.trim())) {
                    if (Util.userId == null) {
                        userPreferences.userId.asLiveData().observe(this) {
                            Util.userId = it
                        }
                    }
                    val data = JsonObject()
                    data.addProperty("userId", Util.userId)
                    data.addProperty("oldPassword", oldPasswordTxt)
                    data.addProperty("newPassword", passwordTxt)
                    changePassword(data)
                } else {
                    val data = JsonObject()
                    data.addProperty("email", email)
                    data.addProperty("otp", otp)
                    data.addProperty("password", passwordTxt)
                    forgotPassword(data)
                }
            }
        }
        cancelButton.setOnClickListener {
            finish()
        }

    }

    private fun changePassword(data: JsonObject) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? =
                            retrofit.postChangePassword("Bearer $it", data)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    data.remove("newPassword")
                                    data.remove("oldPassword")
                                    data.remove("userId")
                                    Toast.makeText(
                                        this@ChangePasswordActivity,
                                        "Password changed successfully",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    finish()
                                } else {
                                    Log.e("code", response.code().toString())
                                    Log.e("err", response.errorBody().toString())
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("ChangePasswordActivity.changePassword", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@ChangePasswordActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChangePasswordActivity.changePassword", e.toString())
        } finally {
            changePasswordButton.isEnabled = true
        }
    }

    private fun forgotPassword(data: JsonObject) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = retrofit.postChangeForgotPassword(data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(
                        call: Call<JsonObject?>,
                        response: Response<JsonObject?>
                    ) {
                        if (response.code() == 200) {
                            data.remove("email")
                            data.remove("otp")
                            data.remove("password")
                            Log.e("ok1", response.code().toString())
                            finish()
                        } else {
                            Log.e("code", response.code().toString())
                            Log.e("err", response.errorBody().toString())
                        }
                        call.cancel()
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("ChangePasswordActivity.forgotPassword", "fail")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("ChangePasswordActivity.forgotPassword", e.toString())
        } finally {
            changePasswordButton.isEnabled = true
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
}