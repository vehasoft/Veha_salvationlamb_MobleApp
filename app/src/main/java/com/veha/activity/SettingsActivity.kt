package com.veha.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.veha.activity.R
import com.veha.util.UserPreferences
import com.veha.util.Util
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var userPreferences: UserPreferences
    private lateinit var fontSize: TextView
    private lateinit var changePass: TextView
    private lateinit var theme: TextView
    private lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        userPreferences = UserPreferences(this)
        fontSize = findViewById(R.id.change_font_size)
        changePass = findViewById(R.id.change_password)
        theme = findViewById(R.id.settings_theme)

        changePass.setOnClickListener {
            val intent = Intent(this,ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        fontSize.setOnClickListener {
            val items = arrayOf<CharSequence>(
                "small",
                "medium",
                "large"
            )
            val builder1 = AlertDialog.Builder(this@SettingsActivity)
            builder1.setTitle("Change Font")
            builder1.setItems(items) { dialog, item ->
                //com.sun.org.apache.bcel.internal.classfile.Utility.checkPermission(this@EditProfileActivity)
                lifecycleScope.launch {
                    if (items[item] == "small") {
                        userPreferences.saveTextSize(10.0F)
                        Util.fontSize = 10.0F
                    } else if (items[item] == "medium") {
                        userPreferences.saveTextSize(15.0F)
                        Util.fontSize = 15.0F
                    } else if (items[item] == "large") {
                        userPreferences.saveTextSize(20.0F)
                        Util.fontSize = 20.0F
                    }
                    val intent = Intent(this@SettingsActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            builder1.show()
        }
        theme.setOnClickListener {
            val items = arrayOf<CharSequence>(
                Util.DAY,
                Util.NIGHT,
                Util.DEFAULT,
            )
            Log.e("mode",AppCompatDelegate.getDefaultNightMode().toString())//.getLocalNightMode())
            val builder1 = AlertDialog.Builder(this@SettingsActivity)
            builder1.setTitle("Change Theme")
            builder1.setItems(items) { dialog, item ->
                lifecycleScope.launch {
                    if (items[item] == Util.DAY) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        Util.isNight = Util.DAY
                        userPreferences.saveIsNightModeEnabled(Util.DAY)
                        /*val intent = Intent(this@SettingsActivity, MainActivity::class.java)
                        finish()
                        startActivity(intent)*/
                    } else if (items[item] == Util.NIGHT) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        Util.isNight = Util.NIGHT
                        userPreferences.saveIsNightModeEnabled(Util.NIGHT)
                        /*val intent = Intent(this@SettingsActivity, MainActivity::class.java)
                        finish()
                        startActivity(intent)*/
                    }
                    else if (items[item] == Util.DEFAULT) {
                        userPreferences.saveIsNightModeEnabled(Util.DEFAULT)
                        Util.isNight = Util.DEFAULT
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        /*val nightModeFlags: Int = this@SettingsActivity.resources.configuration.uiMode and
                                Configuration.UI_MODE_NIGHT_MASK
                        Log.e("night",Configuration.UI_MODE_NIGHT_MASK.toString())
                        Log.e("night",nightModeFlags.toString())
                        Log.e("night",this@SettingsActivity.resources.configuration.uiMode.toString())
                        when (nightModeFlags) {
                            Configuration.UI_MODE_NIGHT_YES -> {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                Util.isNight = true
                            }

                            Configuration.UI_MODE_NIGHT_NO -> {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                Util.isNight = false
                            }

                            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                Util.isNight = false
                            }
                        }*/

                    }
                    Log.e("isnight",Util.isNight.toString())
                }
            }
            builder1.show()
        }

        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        /*menu.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@SettingsActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            if (Util.isWarrior) {
                popup.menu.findItem(R.id.warrior).isVisible = false
            }
            popup.menu.findItem(R.id.settings).isVisible = false
            val night: MenuItem = popup.menu.findItem(R.id.nightmode)
            if (Util.isNight) {
                night.title = "Day Mode"
            } else {
                night.title = "Night Mode"
            }
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.warrior -> {
                        Commons().makeWarrior(this, this)
                    }
                    R.id.edit_profile ->{
                        val intent = Intent(this@SettingsActivity, EditProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.logout -> {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@SettingsActivity)
                        builder.setMessage("Do you want to Logout ?")
                        builder.setTitle("Alert !")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                            finish()
                            lifecycleScope.launch {
                                userPreferences.deleteAuthToken()
                                userPreferences.deleteUserId()
                            }
                            val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }
                        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }

                    R.id.fav -> {
                        val intent = Intent(this@SettingsActivity, FavoritesActivity::class.java)
                        startActivity(intent)
                    }

                    R.id.nightmode -> {
                        if (Util.isNight) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            Util.isNight = false
                            night.title = "Day Mode"
                            lifecycleScope.launch { userPreferences.saveIsNightModeEnabled(false) }
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            Util.isNight = true
                            night.title = "Night Mode"
                            lifecycleScope.launch { userPreferences.saveIsNightModeEnabled(true) }
                        }
                    }
                }
                true
            })
            popup.show()
        }*/
    }
}