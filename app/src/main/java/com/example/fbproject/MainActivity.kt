package com.example.fbproject

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.example.adapter.TabAdapter
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    lateinit var userPreferences: UserPreferences

    //lateinit var nightMode: ImageButton
    //lateinit var dayMode: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        userPreferences = UserPreferences(this@MainActivity)
        super.onCreate(savedInstanceState)
        val userPreferences = UserPreferences(this)
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (TextUtils.isEmpty(it) || it.equals("null") || it.isNullOrEmpty()) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
        userPreferences.userId.asLiveData().observe(this) {
            Log.e("UserId################", it)
            Util.userId = it
        }

        setContentView(R.layout.activity_main)

        menu.setOnClickListener {
            openOptionsMenu()
        }

        menu.setOnClickListener {
            //val popup = PopupMenu(this@MainActivity, menu)
            val myContext: Context = ContextThemeWrapper(this@MainActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu);
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.warrior -> {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                        builder.setMessage("Make me warrior")
                        builder.setTitle("Alert !")
                        builder.setCancelable(false)
                        builder.setPositiveButton("I agree") { _: DialogInterface?, _: Int -> finish()}
                        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                        Toast.makeText(this@MainActivity, "You Clicked : " + item.title, Toast.LENGTH_SHORT).show()
                    }
                    R.id.logout ->{
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                        builder.setMessage("Do you want to Logout ?")
                        builder.setTitle("Alert !")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish()
                            lifecycleScope.launch {
                                userPreferences.deleteAuthToken()
                                userPreferences.deleteUserId()
                            }
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }
                        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }

                    R.id.edit_profile ->{
                        val intent = Intent(this@MainActivity, EditProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.fav ->{
                        val intent = Intent(this@MainActivity, FavoritesActivity::class.java)
                        startActivity(intent)
                    }
                }
                true
            })
            popup.show()
        }


        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        //nightMode = findViewById(R.id.night_mode)
        //dayMode = findViewById(R.id.day_mode)
        val home = tabLayout.newTab()
        val profile = tabLayout.newTab()
        home.tag = "Home"
        profile.tag = "Profile"
        home.text = "Home"
        profile.text = "Profile"

        tabLayout.addTab(home,0)
        tabLayout.addTab(profile,1)

        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = TabAdapter(this,supportFragmentManager,tabLayout.tabCount)
        val viewPager : ViewPager = findViewById(R.id.viewPager)
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
        /*nightMode.setOnClickListener{
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
        }*/
    }
    override fun onBackPressed() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setMessage("Do you want to exit ?")
        builder.setTitle("Alert !")
        builder.setCancelable(false)
        builder.setPositiveButton("Exit") { _: DialogInterface?, _: Int -> finish()}
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

}