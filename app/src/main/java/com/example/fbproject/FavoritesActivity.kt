package com.example.fbproject

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.fragments.HomeFragment
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        userPreferences = UserPreferences(this)

        val viewProfile = HomeFragment.getInstance("fav")
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.view_fav,viewProfile)
        ft.commit()

        menu.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@FavoritesActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            if (Util.isWarrior){ popup.menu.findItem(R.id.warrior).isVisible = false }
            val night: MenuItem = popup.menu.findItem(R.id.nightmode)
            if (Util.isNight){ night.title = "Day Mode" } else{ night.title = "Night Mode" }
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.warrior -> {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@FavoritesActivity)
                        builder.setMessage("You will become warrior after the admin approval")
                        builder.setTitle("Alert !")
                        val view = View.inflate(myContext,R.layout.child_warrior,null)
                        builder.setView(view)
                        val religion: Spinner = view.findViewById(R.id.religion)
                        val church: EditText = view.findViewById(R.id.church)
                        val list = Util.getReligion()
                        var rel = ""
                        val adapter = ArrayAdapter(this@FavoritesActivity, R.layout.spinner_text, list)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        religion.adapter = adapter
                        religion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                                if (list[pos] != "Select"){
                                    rel = list[pos].toString()
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                        builder.setCancelable(false)
                        builder.setPositiveButton("I agree") { _: DialogInterface?, _: Int ->
                            if (rel.isNullOrEmpty() || rel == "Select"){
                                Toast.makeText(this@FavoritesActivity,"Please select Religion",Toast.LENGTH_LONG).show()
                            } else if(church.text.isNullOrEmpty()){
                                Toast.makeText(this@FavoritesActivity,"Please Enter ChurchName",Toast.LENGTH_LONG).show()
                            } else {
                                val data = JsonObject()
                                data.addProperty("religion",rel)
                                data.addProperty("church",church.text.toString())
                                Toast.makeText(this@FavoritesActivity, "Waiting for admin Approval", Toast.LENGTH_SHORT).show()
                            }
                        }
                        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }
                    R.id.logout ->{
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@FavoritesActivity)
                        builder.setMessage("Do you want to Logout ?")
                        builder.setTitle("Alert !")
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
                    R.id.nightmode ->{
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
                    }
                }
                true
            })
            popup.show()
        }
    }
}