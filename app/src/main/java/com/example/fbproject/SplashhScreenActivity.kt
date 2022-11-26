package com.example.fbproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.example.util.UserPreferences
import com.example.util.Util.token

class SplashhScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashh_screen)

        val userPreferences = UserPreferences(this)
        userPreferences.authToken.asLiveData().observe(this) {
            token = it
            Log.e("token################", token)

        }
        if (TextUtils.isEmpty(token)){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else if(token!=null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}