package com.example.fbproject

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.ViewLikesAdapter
import com.example.util.Util
import com.example.util.PostLikes
import com.example.util.UserPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class ViewLikesActivity : AppCompatActivity() {

    lateinit var userPreferences: UserPreferences
    lateinit var list : RecyclerView
    lateinit var nodata : LinearLayout

    lateinit var likeslist: ArrayList<PostLikes>

    private var postId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_likes)
        list = findViewById(R.id.likesListRecycler)
        nodata = findViewById(R.id.no_data)
        userPreferences = UserPreferences(this@ViewLikesActivity)
        val indent = intent
        postId = indent.getStringExtra("postId").toString()
        getALlLikes(this@ViewLikesActivity)

        menu.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@ViewLikesActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            if (Util.isWarrior){ popup.menu.findItem(R.id.warrior).isVisible = false }
            val night: MenuItem = popup.menu.findItem(R.id.nightmode)
            if (Util.isNight){ night.title = "Day Mode" } else{ night.title = "Night Mode" }
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.warrior -> {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@ViewLikesActivity)
                        builder.setMessage("You will become warrior after the admin approval")
                        builder.setTitle("Alert !")
                        val view = View.inflate(myContext,R.layout.child_warrior,null)
                        builder.setView(view)
                        val religion: Spinner = view.findViewById(R.id.religion)
                        val church: EditText = view.findViewById(R.id.church)
                        val list = Util.getReligion()
                        var rel = ""
                        val adapter = ArrayAdapter(this@ViewLikesActivity, R.layout.spinner_text, list)
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
                                Toast.makeText(this@ViewLikesActivity,"Please select Religion",Toast.LENGTH_LONG).show()
                            } else if(church.text.isNullOrEmpty()){
                                Toast.makeText(this@ViewLikesActivity,"Please Enter ChurchName",Toast.LENGTH_LONG).show()
                            } else {
                                val data = JsonObject()
                                data.addProperty("religion",rel)
                                data.addProperty("church",church.text.toString())
                                Toast.makeText(this@ViewLikesActivity, "Waiting for admin Approval", Toast.LENGTH_SHORT).show()
                            }
                        }
                        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }
                    R.id.logout ->{
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@ViewLikesActivity)
                        builder.setMessage("Do you want to Logout ?")
                        builder.setTitle("Alert !")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish()
                            lifecycleScope.launch {
                                userPreferences.deleteAuthToken()
                                userPreferences.deleteUserId()
                            }
                            val intent = Intent(this@ViewLikesActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }
                        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }

                    R.id.edit_profile ->{
                        val intent = Intent(this@ViewLikesActivity, EditProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.fav ->{
                        val intent = Intent(this@ViewLikesActivity, FavoritesActivity::class.java)
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
    fun getALlLikes(context: Context){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getPostLike("Bearer $it",postId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            likeslist = ArrayList()
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            for (post in loginresp){
                                val pos = Gson().fromJson(post, PostLikes::class.java)
                                likeslist.add(pos)
                            }
                            if (likeslist.size <= 0){
                                list.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else {
                                list.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                list.layoutManager = LinearLayoutManager(context)
                                list.adapter = ViewLikesAdapter(likeslist, context)
                            }
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(this@ViewLikesActivity,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(this@ViewLikesActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }

    }
}