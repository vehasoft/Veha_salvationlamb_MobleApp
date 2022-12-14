package com.example.fbproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_forgot_password.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        forgot_btn.setOnClickListener {
            if (TextUtils.isEmpty(email.text!!.trim())){
                email.error = "Enter Email"
                Toast.makeText(this@ForgotPasswordActivity,"Enter Email", Toast.LENGTH_LONG).show()
            }
            else{
                checkValid(email.text.toString())
            }
        }



    }
    private fun checkValid(emailtxt: String) {
        val data = JsonObject()
        data.addProperty("email",emailtxt)
        Log.e("data",data.toString())
        val retrofit = Util.getRetrofit()
        val call: Call<JsonObject?>? = retrofit.postForgotPassword(data)
        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.code()==200){
                    val resp = response.body()
                    Toast.makeText(this@ForgotPasswordActivity,resp?.get("message").toString(), Toast.LENGTH_LONG).show()
                    otp_op.visibility = View.VISIBLE
                    forgot_btn.text = "confirm"
                    if (forgot_btn.text != "verify"){
                        forgot_btn.setOnClickListener {
                            if (TextUtils.isEmpty(otp.text!!.trim())){
                                otp.error = "Enter OTP"
                                Toast.makeText(this@ForgotPasswordActivity,"Enter Email", Toast.LENGTH_LONG).show()
                            }
                            else{
                                checkOtp(email.text.toString(),otp.text.toString())
                            }
                        }
                    }
                }
                else{
                    val resp = response.errorBody()
                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                    val status = loginresp.get("status").toString()
                    val errorMessage = loginresp.get("errorMessage").toString()
                    email.error = errorMessage
                    Toast.makeText(this@ForgotPasswordActivity,errorMessage, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Toast.makeText(this@ForgotPasswordActivity,"No Internet", Toast.LENGTH_LONG).show()
                Log.e("responseee","fail")
            }
        })
    }
    private fun checkOtp(emailtxt: String,otpTxt: String) {
        val data = JsonObject()
        data.addProperty("email",emailtxt)
        data.addProperty("otp",otpTxt)
        Log.e("data",data.toString())
        val retrofit = Util.getRetrofit()
        val call: Call<JsonObject?>? = retrofit.postForgotPasswordOtp(data)
        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.code()==200){
                    val resp = response.body()
                    Toast.makeText(this@ForgotPasswordActivity,resp?.get("message").toString(), Toast.LENGTH_LONG).show()
                    val intent = Intent(this@ForgotPasswordActivity, ChangePasswordActivity::class.java)
                    intent.putExtra("email",emailtxt)
                    intent.putExtra("otp",otpTxt)
                    startActivity(intent)
                    finish()
                }
                else{
                    val resp = response.errorBody()
                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                    val errorMessage = loginresp.get("errorMessage").toString()
                    otp.error = errorMessage
                    Toast.makeText(this@ForgotPasswordActivity,errorMessage, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Toast.makeText(this@ForgotPasswordActivity,"No Internet", Toast.LENGTH_LONG).show()
                Log.e("responseee","fail")
            }
        })
    }
}