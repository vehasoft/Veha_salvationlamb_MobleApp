package com.example.fbproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.models.Loginresp
import com.example.util.APIUtil
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences = UserPreferences(this@LoginActivity)
        /*userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null")) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }*/
        setContentView(R.layout.activity_login)
        signup_btn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
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
        val retrofit = APIUtil.getRetrofit()
        val call: Call<JsonObject?>? = retrofit.postCall("login",data)
        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.code()==200){
                    val resp = response.body()
                    val loginresp: Loginresp = Gson().fromJson(resp?.get("result"),Loginresp::class.java)
                    Util.user = loginresp
                    lifecycleScope.launch {
                        userPreferences.saveAuthToken(loginresp.token)
                    }
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

