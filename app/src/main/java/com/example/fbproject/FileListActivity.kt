package com.example.fbproject

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.FileAdapter
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import java.io.File

class FileListActivity : AppCompatActivity() {
    var path: String = ""
    lateinit var recyclerView: RecyclerView
    lateinit var noFilesText: TextView
    lateinit var logo: ImageView
    var filesAndFolders: ArrayList<FilesAndFolders> = ArrayList()

    lateinit var userPreferences: UserPreferences
    lateinit var dialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)
        userPreferences = UserPreferences(this@FileListActivity)
        dialog = ProgressDialog(this)
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        recyclerView = findViewById(R.id.recycler_view)
        noFilesText = findViewById(R.id.nofiles_textview)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        path = intent.getStringExtra("folderId").toString()
        getFilesAndFolder(path,this)

    }

    private fun getFilesAndFolder(folderID: String , context: Context) {

        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(this) {
                if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? = retrofit.getFilesAndFolders("Bearer $it", folderID)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            if (response.code() == 200) {
                                filesAndFolders = ArrayList()
                                val resp = response.body()
                                val loginresp: JsonArray = Gson().fromJson(resp?.get("files"), JsonArray::class.java)
                                for (files in loginresp) {
                                    Log.e("files",files.toString())
                                    val pos = Gson().fromJson(files, FilesAndFolders::class.java)
                                    filesAndFolders.add(pos)
                                }
                                if (filesAndFolders.size <= 0){
                                    noFilesText.visibility = View.VISIBLE
                                    recyclerView.visibility = View.GONE
                                } else {
                                    noFilesText.visibility = View.GONE
                                    recyclerView.visibility = View.VISIBLE

                                    recyclerView.layoutManager = LinearLayoutManager(context)
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
                            Log.e("MainActivity.getDetails", "fail")
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
    }
}