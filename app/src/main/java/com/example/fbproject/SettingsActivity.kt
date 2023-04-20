package com.example.fbproject

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.util.Commons
import com.example.util.UserPreferences
import com.example.util.Util
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var userPreferences: UserPreferences
    private lateinit var fontSize: TextView
    private lateinit var changePass: TextView
    private lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        userPreferences = UserPreferences(this)
        fontSize = findViewById(R.id.change_font_size)
        changePass = findViewById(R.id.change_password)

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

        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        menu.setOnClickListener {
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
        }
    }
}