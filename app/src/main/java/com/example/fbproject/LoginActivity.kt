package com.example.fbproject

import android.app.ProgressDialog
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
    lateinit var dialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences = UserPreferences(this@LoginActivity)
        dialog = ProgressDialog(this)
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
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
        if (!dialog.isShowing) {
            dialog.show()
        }
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
                        userPreferences.saveIsNightModeEnabled(false)
                        Util.userId = loginresp.id
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

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
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Toast.makeText(this@LoginActivity,"No Internet",Toast.LENGTH_LONG).show()
                Log.e("responseee","fail")
                if (dialog.isShowing) {
                            dialog.dismiss()
                        }
            }
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}

