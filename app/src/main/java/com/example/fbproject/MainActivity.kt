package com.example.fbproject

import android.R.attr.button
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.example.adapter.TabAdapter
import com.example.util.UserPreferences
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    lateinit var userPreferences: UserPreferences

    lateinit var nightMode: ImageButton
    lateinit var dayMode: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        userPreferences = UserPreferences(this@MainActivity)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        menu.setOnClickListener {
            openOptionsMenu()
        }




        menu.setOnClickListener { // Initializing the popup menu and giving the reference as current context
            /*val popupMenu = PopupMenu(this@MainActivity, menu)

            // Inflating popup menu from popup_menu.xml file
            popupMenu.getMenuInflater().inflate(R.menu.main_menu, popupMenu.getMenu())
            popupMenu.setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener() {
                override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                    // Toast message on menu item clicked
                    Toast.makeText(this@MainActivity, "You Clicked " + menuItem.title, Toast.LENGTH_SHORT).show()
                    return true
                }
            })
            // Showing the popup menu
            popupMenu.show()*/

            val popup = PopupMenu(this@MainActivity, menu)
            popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.warrior ->
                        Toast.makeText(this@MainActivity, "You Clicked : " + item.title, Toast.LENGTH_SHORT).show()
                    R.id.logout ->{
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                        }
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }

                    R.id.edit_profile ->{
                        val intent = Intent(this@MainActivity, EditProfileActivity::class.java)
                        startActivity(intent)
                    }
                }
                true
            })
            popup.show()




        }






        var tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        nightMode = findViewById(R.id.night_mode)
        dayMode = findViewById(R.id.day_mode)
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
        nightMode.setOnClickListener{
            //dayMode.isEnabled = true
            dayMode.visibility = View.VISIBLE
            nightMode.visibility = View.GONE
            //nightMode.isEnabled = false
            //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        }
        dayMode.setOnClickListener{
           // nightMode.isEnabled = true
            nightMode.visibility = View.VISIBLE
            dayMode.visibility = View.GONE
          //  dayMode.isEnabled = false
            //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        userPreferences = UserPreferences(this@MainActivity)
        // Handle item selection
        return when (item.itemId) {
            R.id.warrior -> {

                true
            }
            R.id.logout -> {
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                }
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.edit_profile -> {
                val intent = Intent(this@MainActivity, EditProfileActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }*/
}