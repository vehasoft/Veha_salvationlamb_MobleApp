package com.veha.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.veha.fragments.ProfileFragment
import com.veha.util.UserPreferences

class ViewProfileActivity : AppCompatActivity() {
    private lateinit var userPreferences: UserPreferences
    private lateinit var logo: ImageView
    private lateinit var close: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile)
        userPreferences = UserPreferences(this)
        val userId = intent.getStringExtra("userId")
        val viewProfile = ProfileFragment.getInstance(userId!!, "other")
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.view_profile, viewProfile)
        ft.commit()
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        close = findViewById(R.id.close)
        close.setOnClickListener {
            if (isTaskRoot){
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            finish()
        }
    }

    override fun onBackPressed() {
        if (isTaskRoot){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        super.onBackPressed()
    }
}