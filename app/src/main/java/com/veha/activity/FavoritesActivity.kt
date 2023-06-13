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
import com.veha.activity.R
import com.veha.fragments.HomeFragment
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.Util
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var userPreferences: UserPreferences
    private lateinit var logo: ImageView
    private lateinit var dialog: android.app.AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        userPreferences = UserPreferences(this)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val viewProfile = HomeFragment.getInstance("fav")
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.view_fav,viewProfile)
        ft.commit()

        menu.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@FavoritesActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            if (Util.isWarrior){ popup.menu.findItem(R.id.warrior).isVisible = false }
            popup.menu.findItem(R.id.logout).isVisible = false
            popup.menu.findItem(R.id.fav).isVisible = false
            if (Util.user.isReviewState.toBoolean()) { popup.menu.findItem(R.id.warrior).isVisible = false }
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.warrior -> {
                       Commons().makeWarrior(this,this)
                    }
                    R.id.logout ->{
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@FavoritesActivity)
                        builder.setMessage("Do you want to Logout?")
                        builder.setTitle("Logout")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish()
                            lifecycleScope.launch {
                                userPreferences.deleteAuthToken()
                                userPreferences.deleteUserId()
                            }
                            val intent = Intent(this@FavoritesActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }
                        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }

                    R.id.edit_profile ->{
                        val intent = Intent(this@FavoritesActivity, EditProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.fav ->{
                        val intent = Intent(this@FavoritesActivity, FavoritesActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.settings -> {
                        val intent = Intent(this@FavoritesActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                    /*R.id.nightmode ->{
                        if (Util.isNight){
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
                    }*/
                }
                true
            })
            popup.show()
        }
    }
    override fun onPause() {
        super.onPause()
        dialog.dismiss()
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}