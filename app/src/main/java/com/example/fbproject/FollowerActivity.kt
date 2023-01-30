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
import com.example.adapter.FollowAdapter
import com.example.util.PostUser
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class FollowerActivity : AppCompatActivity() {


    private lateinit var userPreferences: UserPreferences
    private lateinit var followList: ArrayList<PostUser>

    lateinit var lists: RecyclerView
    lateinit var nodata: LinearLayout
    private lateinit var userId: String

    private var myFollowerMap: HashMap<String,String> = HashMap()
    private var followingMap: HashMap<String,String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follower)
        userPreferences = UserPreferences(this)
        userId = intent.extras!!.get("userId").toString()
        if(intent.extras!!.get("page") == "follower")
            getallFollowers(this)
        if(intent.extras!!.get("page") == "following")
            getallFollowing(this)

        lists = findViewById(R.id.my_follow_list)
        nodata = findViewById(R.id.no_data)

        menu.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@FollowerActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            if (Util.isWarrior){ popup.menu.findItem(R.id.warrior).isVisible = false }
            val night: MenuItem = popup.menu.findItem(R.id.nightmode)
            if (Util.isNight){ night.title = "Day Mode" } else{ night.title = "Night Mode" }
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.warrior -> {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@FollowerActivity)
                        builder.setMessage("You will become warrior after the admin approval")
                        builder.setTitle("Alert !")
                        val view = View.inflate(myContext,R.layout.child_warrior,null)
                        builder.setView(view)
                        val religion: Spinner = view.findViewById(R.id.religion)
                        val church: EditText = view.findViewById(R.id.church)
                        val list = Util.getReligion()
                        var rel = ""
                        val adapter = ArrayAdapter(this@FollowerActivity, R.layout.spinner_text, list)
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
                                Toast.makeText(this@FollowerActivity,"Please select Religion",Toast.LENGTH_LONG).show()
                            } else if(church.text.isNullOrEmpty()){
                                Toast.makeText(this@FollowerActivity,"Please Enter ChurchName",Toast.LENGTH_LONG).show()
                            } else {
                                val data = JsonObject()
                                data.addProperty("religion",rel)
                                data.addProperty("church",church.text.toString())
                                Toast.makeText(this@FollowerActivity, "Waiting for admin Approval", Toast.LENGTH_SHORT).show()
                            }
                        }
                        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }
                    R.id.logout ->{
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@FollowerActivity)
                        builder.setMessage("Do you want to Logout ?")
                        builder.setTitle("Alert !")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish()
                            lifecycleScope.launch {
                                userPreferences.deleteAuthToken()
                                userPreferences.deleteUserId()
                            }
                            val intent = Intent(this@FollowerActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }
                        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }

                    R.id.edit_profile ->{
                        val intent = Intent(this@FollowerActivity, EditProfileActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.fav ->{
                        val intent = Intent(this@FollowerActivity, FavoritesActivity::class.java)
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
    private fun getallFollowers(context: Context) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFollowers("Bearer $it", userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            followList = ArrayList()
                            for (likes in loginresp) {
                                val pos = Gson().fromJson(likes, PostUser::class.java)
                                followList.add(pos)
                                myFollowerMap.put(Util.userId,pos.id)
                            }
                            if (followList.size <= 0){
                                lists.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                lists.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                lists.layoutManager = LinearLayoutManager(context)
                                lists.adapter = FollowAdapter(followList, context,myFollowerMap,this@FollowerActivity)
                            }

                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            }
        }
    }
    private fun getallFollowing(context: Context) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFollowing("Bearer $it", userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            followList = ArrayList()
                            for (followings in loginresp) {
                                val pos = Gson().fromJson(followings, PostUser::class.java)
                                followList.add(pos)
                                followingMap.put(pos.id,Util.userId)
                            }
                            if (followList.size <= 0){
                                lists.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                lists.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                lists.layoutManager = LinearLayoutManager(context)
                                lists.adapter = FollowAdapter(followList, context,followingMap,this@FollowerActivity)
                            }
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            }
        }
    }
}