package com.example.fbproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import com.example.fragments.ProfileFragment
import com.example.util.Util

class ViewProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile)
        val userId = intent.getStringExtra("userId")
        val viewProfile = ProfileFragment(userId!!,this,"other")
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.view_profile,viewProfile)
        ft.commit()
    }
}