package com.veha.activity

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.veha.util.Commons
import com.veha.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import retrofit2.Call
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {
    lateinit var page: String
    lateinit var emailID: String

    private lateinit var forgotPasswordHead: TextView
    private lateinit var forgotPasswordContent: TextView
    private lateinit var email: TextInputEditText
    private lateinit var otp: TextInputEditText
    private lateinit var otpOp: TextInputLayout
    private lateinit var emailOp: TextInputLayout
    private lateinit var cancelButton: Button
    private lateinit var forgotButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        forgotPasswordHead = findViewById(R.id.forgot_pwd_head)
        forgotPasswordContent = findViewById(R.id.forgot_pwd_content)
        email = findViewById(R.id.email)
        otp = findViewById(R.id.otp)
        otpOp = findViewById(R.id.otp_op)
        emailOp = findViewById(R.id.email_op)
        cancelButton = findViewById(R.id.cancel_btn)
        forgotButton = findViewById(R.id.forgot_btn)

        page = intent.getStringExtra("page").toString()
        if (page.contentEquals("verify")) {
            emailID = intent.getStringExtra("email").toString()
            forgotPasswordHead.text = "OTP Verification"
            forgotPasswordContent.text = "Welcome to SalvationLamb. Please enter otp to verify your registered email id"
            email.text = Editable.Factory.getInstance().newEditable(emailID)
            otpOp.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            emailOp.visibility = View.GONE
            checkValid(emailID)
            forgotButton.setOnClickListener {
                if (TextUtils.isEmpty(otp.text!!.trim())) {
                    otp.error = "Enter OTP"
                    Toast.makeText(this@ForgotPasswordActivity, "Enter OTP", Toast.LENGTH_LONG).show()
                } else {
                    forgotButton.isEnabled = false
                    checkOtp(emailID, otp.text.toString())
                }
            }
            cancelButton.setOnClickListener {
                val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
                finish()
                startActivity(intent)
            }

        } else {
            forgotPasswordHead.text = "Forgot Password"
            forgotPasswordContent.text = "Welcome to SalvationLamb. Please enter your email-id."
            forgotButton.setOnClickListener {
                if (TextUtils.isEmpty(email.text!!.trim())) {
                    email.error = "Enter Email"
                    Toast.makeText(this@ForgotPasswordActivity, "Enter Email", Toast.LENGTH_LONG).show()
                } else {
                    forgotButton.isEnabled = false
                    checkValid(email.text.toString())
                }
            }
            cancelButton.setOnClickListener {
                finish()
            }
        }

    }

    private fun checkValid(emailtxt: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
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
                            otpOp.visibility = View.VISIBLE
                            forgotButton.text = "confirm"
                            if (forgotButton.text != "verify") {
                                forgotButton.setOnClickListener {
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
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("ForgotPasswordActivity.checkValid", "fail")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("ForgotPasswordActivity.checkValid", e.toString())
        }
        finally {
            forgotButton.isEnabled = true
        }
    }

    private fun checkOtp(emailtxt: String, otpTxt: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
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
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("ForgotPasswordActivity.checkOTP", "fail")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("ForgotPasswordActivity.checkOTP", e.toString())
        }
        finally {
            forgotButton.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
        finish()
        startActivity(intent)
    }
}