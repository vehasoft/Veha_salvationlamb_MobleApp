package com.veha.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.veha.adapter.NotificationTabAdapter
import com.veha.util.Util

class NotificationViewActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager
    lateinit var logo: ImageView
    lateinit var close: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_view)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        close = findViewById(R.id.close)
        close.setOnClickListener {
            finish()
        }
        val tabLayout = findViewById<TabLayout>(R.id.notification_tab_layout)
        val user = tabLayout.newTab()
        user.tag = "User"
        user.text = "User"
        val admin = tabLayout.newTab()
        admin.tag = "Admin"
        admin.text = "Admin"
        val warrior = tabLayout.newTab()
        warrior.tag = "Warrior"
        warrior.text = "Warrior"
        tabLayout.addTab(user, 0)
        tabLayout.addTab(admin, 1)
        if (Util.user == null){
            MainActivity().getMyDetails()
        }
        if (Util.user.role == "admin") {
            tabLayout.addTab(warrior, 2)
        }
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        val adapter = NotificationTabAdapter(
            this@NotificationViewActivity,
            this@NotificationViewActivity.supportFragmentManager,
            tabLayout.tabCount
        )

        viewPager = findViewById(R.id.notification_viewpager)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    viewPager.currentItem = tab.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}