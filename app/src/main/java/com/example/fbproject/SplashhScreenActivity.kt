package com.example.fbproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.example.util.UserPreferences
import com.example.util.Util

class SplashhScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashh_screen)

        //Thread.sleep(5000)

        val userPreferences = UserPreferences(this)
        userPreferences.authToken.asLiveData().observe(this) { it ->
            Log.e("token################", it)
            if (TextUtils.isEmpty(it) || it.equals("null") || it.isNullOrEmpty()){
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                userPreferences.userId.asLiveData().observe(this) {
                    Util.userId = it
                }
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

        }



    }
}