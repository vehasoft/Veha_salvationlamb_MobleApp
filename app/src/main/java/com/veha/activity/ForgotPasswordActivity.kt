package com.veha.activity

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.veha.activity.R
import com.veha.util.Commons
import com.veha.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_forgot_password.*
import retrofit2.Call
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {
    lateinit var dialog: AlertDialog
    lateinit var page: String
    lateinit var emailID: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)

        page = intent.getStringExtra("page").toString()
        if (page.contentEquals("verify")) {
            emailID = intent.getStringExtra("email").toString()
            forgot_pwd_head.text = "OTP Verification"
            forgot_pwd_content.text = "Welcome to SalvationLamb. Please enter otp to verify your registered email id"
            email.text = Editable.Factory.getInstance().newEditable(emailID)
            otp_op.visibility = View.VISIBLE
            cancel_btn.visibility = View.VISIBLE
            email_op.visibility = View.GONE
            checkValid(emailID)
            forgot_btn.setOnClickListener {
                if (TextUtils.isEmpty(otp.text!!.trim())) {
                    otp.error = "Enter OTP"
                    Toast.makeText(this@ForgotPasswordActivity, "Enter OTP", Toast.LENGTH_LONG).show()
                } else {
                    Log.e("email", emailID)
                    checkOtp(emailID, otp.text.toString())
                }
            }
            cancel_btn.setOnClickListener {
                val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
                finish()
                startActivity(intent)
            }

        } else {
            forgot_pwd_head.text = "Forgot Password"
            forgot_pwd_content.text = "Welcome to SalvationLamb. Please enter your email-id."
            forgot_btn.setOnClickListener {
                if (TextUtils.isEmpty(email.text!!.trim())) {
                    email.error = "Enter Email"
                    Toast.makeText(this@ForgotPasswordActivity, "Enter Email", Toast.LENGTH_LONG).show()
                } else {
                    checkValid(email.text.toString())
                }
            }
            cancel_btn.setOnClickListener {
                finish()
            }
        }

    }

    private fun checkValid(emailtxt: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val data = JsonObject()
                data.addProperty("email", emailtxt)
                if (page.contentEquals("verify")) {
                    data.addProperty("isVerifyMail", true)
                }
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = retrofit.postForgotPassword(data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Toast.makeText(
                                this@ForgotPasswordActivity,
                                resp?.get("message").toString(),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            otp_op.visibility = View.VISIBLE
                            forgot_btn.text = "confirm"
                            if (forgot_btn.text != "verify") {
                                forgot_btn.setOnClickListener {
                                    if (TextUtils.isEmpty(otp.text!!.trim())) {
                                        otp.error = "Enter OTP"
                                        Toast.makeText(this@ForgotPasswordActivity, "Enter OTP", Toast.LENGTH_LONG)
                                            .show()
                                    } else {
                                        checkOtp(email.text.toString(), otp.text.toString())
                                    }
                                }
                            }
                        } else {
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            email.error = errorMessage
                            Toast.makeText(this@ForgotPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                        Log.e("ForgotPasswordActivity.checkValid", "fail")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("ForgotPasswordActivity.checkValid", e.toString())
        }
    }

    private fun checkOtp(emailtxt: String, otpTxt: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val data = JsonObject()
                data.addProperty("email", emailtxt)
                data.addProperty("otp", otpTxt)
                Log.e("data", data.toString())
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = if (page.contentEquals("verify")) {
                    retrofit.postVerifyUser(data)
                } else {
                    retrofit.postForgotPasswordOtp(data)
                }
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Toast.makeText(
                                this@ForgotPasswordActivity,
                                resp?.get("message").toString(),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            if (page.contentEquals("verify")) {
                                if (Util.userId.isNullOrEmpty()) {
                                    val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    val intent = Intent(this@ForgotPasswordActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            } else {
                                val intent = Intent(this@ForgotPasswordActivity, ChangePasswordActivity::class.java)
                                intent.putExtra("email", emailtxt)
                                intent.putExtra("otp", otpTxt)
                                startActivity(intent)
                                finish()
                            }

                        } else {
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val errorMessage = loginresp.get("errorMessage").toString()
                            otp.error = errorMessage
                            Toast.makeText(this@ForgotPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                        Log.e("ForgotPasswordActivity.checkOTP", "fail")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("ForgotPasswordActivity.checkOTP", e.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
        finish()
        startActivity(intent)
    }
}