package com.veha.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.adapter.FileAdapter
import com.veha.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class FileListActivity : AppCompatActivity() {
    var path: String = ""
    var name: String = ""
    lateinit var recyclerView: RecyclerView
    lateinit var noFilesText: LinearLayout
    lateinit var logo: ImageView
    lateinit var listIcon: ImageView
    private lateinit var fileHead: TextView
    var filesAndFolders: ArrayList<FilesAndFolders> = ArrayList()

    lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)
        userPreferences = UserPreferences(this@FileListActivity)
        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        dialog.dismiss()
        recyclerView = findViewById(R.id.recycler_view)
        noFilesText = findViewById(R.id.no_data)
        logo = findViewById(R.id.prod_logo)
        fileHead = findViewById(R.id.file_head)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        listIcon = findViewById(R.id.view_icon)
        if (Util.listview) {
            listIcon.setImageResource(R.drawable.ic_baseline_list_24)
        } else {
            listIcon.setImageResource(R.drawable.ic_baseline_grid_on_24)
        }
        listIcon.setOnClickListener {
            Util.listview = !Util.listview
            startActivity(intent)
            finish()
        }
        path = intent.getStringExtra("folderId").toString()
        name = intent.getStringExtra("folderName").toString()
        fileHead.text = name
        getFilesAndFolder(path, this)

    }

    private fun getFilesAndFolder(folderID: String, context: Context) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.getFilesAndFolders("Bearer $it", folderID)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    filesAndFolders = ArrayList()
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("files"), JsonArray::class.java)
                                    for (files in loginresp) {
                                        val pos = Gson().fromJson(files, FilesAndFolders::class.java)
                                        filesAndFolders.add(pos)
                                    }
                                    if (filesAndFolders.size <= 0) {
                                        noFilesText.visibility = View.VISIBLE
                                        recyclerView.visibility = View.GONE
                                    } else {
                                        noFilesText.visibility = View.GONE
                                        recyclerView.visibility = View.VISIBLE

                                        if (Util.listview) {
                                            recyclerView.layoutManager = LinearLayoutManager(context)
                                        } else {
                                            recyclerView.layoutManager = GridLayoutManager(context, 3)
                                        }
                                        recyclerView.adapter = FileAdapter(applicationContext, filesAndFolders)
                                    }
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("FileListActivity.getFilesAndFolder", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@FileListActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@FileListActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileListActivity.getFilesAndFolder", e.toString())
        }
    }

    override fun onPause() {
        super.onPause()
        dialog.dismiss()
    }

    override fun onResume() {
        super.onResume()
        dialog.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}