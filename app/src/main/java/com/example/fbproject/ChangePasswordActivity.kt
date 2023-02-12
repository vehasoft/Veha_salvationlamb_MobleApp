package com.example.fbproject

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.util.UserRslt
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_change_password.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var passwordTxt: String
    private lateinit var cnfmPasswordTxt: String
    private lateinit var oldPasswordTxt: String
    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        userPreferences = UserPreferences(this)

        dialog = ProgressDialog(this)
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        val email = intent.getStringExtra("email")
        val otp = intent.getStringExtra("otp")
        if (TextUtils.isEmpty(email?.trim())) old_pwd_op.visibility = View.VISIBLE else old_pwd_op.visibility = View.GONE


        change_pwd_btn.setOnClickListener {
            passwordTxt = new_pwd.text.toString()
            cnfmPasswordTxt = cnfm_pwd.text.toString()
            oldPasswordTxt = old_pwd.text.toString()
            Log.e("password",passwordTxt)
            if(!Util.isValidPassword(passwordTxt)){
                new_pwd.error = "Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8"
            } else if(TextUtils.isEmpty(passwordTxt.trim())){
                new_pwd.error =  "Enter Password"
            } else if(passwordTxt != cnfmPasswordTxt){
                cnfm_pwd.error = "Password is not same"
            }
            else {
                if (TextUtils.isEmpty(email?.trim())){
                    val data = JsonObject()
                    data.addProperty("userId",Util.userId)
                    data.addProperty("oldPassword",oldPasswordTxt)
                    data.addProperty("newPassword",passwordTxt)
                    changePassword(data)
                } else {
                    val data = JsonObject()
                    data.addProperty("email",email)
                    data.addProperty("otp",otp)
                    data.addProperty("password",passwordTxt)
                    forgotPassword(data)
                }
            }
        }
        cancel_btn.setOnClickListener {
            finish()
        }


    }
    private fun changePassword(data: JsonObject) {
        if (!dialog.isShowing) {
            dialog.show()
        }
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postChangePassword("Bearer $it", data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            Log.e("ok",response.code().toString())
                            finish()
                        }else{
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Toast.makeText(this@ChangePasswordActivity,errorMessage,Toast.LENGTH_LONG).show()
                            Log.e("result", errorMessage)
                            Log.e("ok",response.body().toString())

                        }
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                        Toast.makeText(this@ChangePasswordActivity, "No Internet", Toast.LENGTH_LONG).show()
                        Log.e("responseee", "fail")
                    }
                })
            } else {
                Toast.makeText(this@ChangePasswordActivity,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(this@ChangePasswordActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun forgotPassword(data: JsonObject) {
        if (!dialog.isShowing) {
            dialog.show()
        }
        val retrofit = Util.getRetrofit()
        val call: Call<JsonObject?>? = retrofit.postChangeForgotPassword(data)
        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.code() == 200) {

                    Log.e("ok1",response.code().toString())
                    finish()
                } else {
                    val resp = response.errorBody()
                    Log.e("responseeeee",response.toString())
                    Log.e("responseeeee",resp.toString())
                    val registerResp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                    val status = registerResp.get("status").toString()
                    val errorMessage = registerResp.get("errorMessage").toString()
                    Log.e("Status", status)
                    Log.e("result", errorMessage)
                    Toast.makeText(this@ChangePasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
                if (dialog.isShowing) {
                            dialog.dismiss()
                        }
            }

            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                Toast.makeText(this@ChangePasswordActivity, "No Internet", Toast.LENGTH_LONG).show()
                Log.e("responseee", "fail")
            }
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}