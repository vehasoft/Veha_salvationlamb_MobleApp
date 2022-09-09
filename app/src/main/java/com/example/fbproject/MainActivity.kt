package com.example.fbproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.example.adapter.TabAdapter
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        var home = tabLayout.newTab()
        var profile = tabLayout.newTab()
        home.tag = "Home"
        profile.tag = "Profile"
        home.text = "Home"
        profile.text = "Profile"

        tabLayout.addTab(home,0)
        tabLayout.addTab(profile,1)

        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        var adapter : TabAdapter = TabAdapter(this,supportFragmentManager,tabLayout.tabCount)
        var viewPager : ViewPager = findViewById(R.id.viewPager)
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
}