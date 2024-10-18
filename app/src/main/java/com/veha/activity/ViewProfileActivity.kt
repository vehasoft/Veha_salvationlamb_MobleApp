package com.veha.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.veha.fragments.ProfileFragment
import com.veha.util.Commons
import com.veha.util.Permission
import com.veha.util.PermissionType
import com.veha.util.UserPreferences
import com.veha.util.Util
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch

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
            finish()
        }
    }
}