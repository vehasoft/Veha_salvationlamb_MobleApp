package com.veha.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView

class NoPermissionActivity : AppCompatActivity() {
    lateinit var logo: ImageView
    lateinit var goBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_permission)

        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        goBack = findViewById(R.id.go_back)
        goBack.setOnClickListener {
            finish()
        }
    }
}