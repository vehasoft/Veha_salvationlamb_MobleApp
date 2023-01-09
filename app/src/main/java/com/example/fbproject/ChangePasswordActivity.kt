package com.example.fbproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.models.UserRslt
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

    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        userPreferences = UserPreferences(this)

        val email = intent.getStringExtra("email")
        val otp = intent.getStringExtra("otp")
        if (TextUtils.isEmpty(email?.trim())) old_pwd.visibility = View.VISIBLE else old_pwd.visibility = View.GONE

        passwordTxt = new_pwd.text.toString()
        cnfmPasswordTxt = cnfm_pwd.text.toString()

        change_pwd_btn.setOnClickListener {
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
                    data.addProperty("oldPassword",passwordTxt)
                    data.addProperty("newPassword",cnfmPasswordTxt)
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


    }
    private fun changePassword(data: JsonObject) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postCallHead("/users/change-password","Bearer $it", data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Log.e("userrrrr", resp.toString())
                            val loginresp: com.example.util.UserRslt =
                                Gson().fromJson(resp?.get("result"), com.example.util.UserRslt::class.java)
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
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
        val retrofit = Util.getRetrofit()
        val call: Call<JsonObject?>? = retrofit.postCall("password/confirm-password",data)
        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.code() == 200) {
                    val resp = response.body()

                    Log.e("Status", resp?.get("status").toString())
                    Log.e("result", resp?.get("result").toString())

                    finish()
                } else {
                    val resp = response.errorBody()
                    val registerResp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                    val status = registerResp.get("status").toString()
                    val errorMessage = registerResp.get("errorMessage").toString()
                    Log.e("Status", status)
                    Log.e("result", errorMessage)
                    Toast.makeText(this@ChangePasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Log.e("responseee", "fail")
            }
        })
    }
}