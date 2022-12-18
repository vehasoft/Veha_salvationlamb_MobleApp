package com.example.fbproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.fragments.HomeFragment
import com.example.fragments.ProfileFragment

class FavoritesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        //val userId = intent.getStringExtra("userId")
        val viewProfile = HomeFragment(this,"fav")
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.view_fav,viewProfile)
        ft.commit()
    }
}