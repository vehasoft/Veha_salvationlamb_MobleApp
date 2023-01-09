package com.example.fbproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.util.Loginresp
import com.example.util.Util
import com.example.util.UserPreferences
import com.example.util.UserRslt
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.email
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences = UserPreferences(this@LoginActivity)
        setContentView(R.layout.activity_login)
        signup_btn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        forgot_pwd.setOnClickListener {
            val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
        login_btn.setOnClickListener {
            val emailstr = email.text.toString()
            val passwordstr = password.text.toString()
            val data = JsonObject()
            data.addProperty("email",emailstr)
            data.addProperty("password",passwordstr)
            data.addProperty("isMobile",true)
            if(!Util.isValidEmail(emailstr))
                Toast.makeText(this,"Invalid Email",Toast.LENGTH_LONG).show()
            else if (!Util.isValidPassword(passwordstr))
                Toast.makeText(this,"Invalid Password",Toast.LENGTH_LONG).show()
            else
                login(data)
        }

    }
    private fun login(data: JsonObject) {
        Log.e("data",data.toString())
        val retrofit = Util.getRetrofit()
        val call: Call<JsonObject?>? = retrofit.postCall("login",data)
        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.code()==200){
                    val resp = response.body()
                    val loginresp: Loginresp = Gson().fromJson(resp?.get("result"),Loginresp::class.java)
                    lifecycleScope.launch {
                        userPreferences.saveAuthToken(loginresp.token)
                        userPreferences.saveUserId(loginresp.id)
                    }
                    Util.userId = loginresp.id
                    Log.e("USERRSLT",loginresp.toString())
                    Util.user = UserRslt(
                        loginresp.id,
                        loginresp.loginSource,
                        loginresp.role,
                        loginresp.name,
                        loginresp.email,
                        loginresp.gender,
                        loginresp.password,
                        loginresp.mobile,
                        loginresp.dateOfBirth,
                        loginresp.updatedAt,
                        loginresp.createdAt,
                        loginresp.picture,
                        loginresp.coverPicture,
                        loginresp.address,
                        loginresp.isWarrior,
                        loginresp.isReviewState,
                        loginresp.state,
                        loginresp.pinCode,
                        loginresp.country,
                        loginresp.religion,
                        loginresp.language,
                        loginresp.blocked
                    )
                    Log.e("token#######################",loginresp.token)
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    Log.e("Status", resp?.get("status").toString())
                    Log.e("result", resp?.get("result").toString())
                    Log.e("result", loginresp.toString())
                }
                else{
                    val resp = response.errorBody()
                    val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
                    val status = loginresp.get("status").toString()
                    val errorMessage = loginresp.get("errorMessage").toString()
                    Log.e("Status", status)
                    Log.e("result", errorMessage)
                    if (errorMessage.contains("Invalid password",true)){
                        Toast.makeText(this@LoginActivity,"INVALID PASSWORD",Toast.LENGTH_LONG).show()
                    }
                    else if (errorMessage.contains("Invalid Email-Id",true)){
                        Toast.makeText(this@LoginActivity,"INVALID USER",Toast.LENGTH_LONG).show()
                    }
                }
            }
            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Toast.makeText(this@LoginActivity,"No Internet",Toast.LENGTH_LONG).show()
                Log.e("responseee","fail")
            }
        })
    }
}

