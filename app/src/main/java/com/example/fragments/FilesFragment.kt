package com.example.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.FileAdapter
import com.example.fbproject.FileListActivity
import com.example.fbproject.LoginActivity
import com.example.fbproject.R
import com.example.util.Commons
import com.example.util.FilesAndFolders
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class FilesFragment : Fragment() {


    lateinit var userPreferences: UserPreferences
    lateinit var dialog: ProgressDialog
    lateinit var recyclerView: RecyclerView
    lateinit var noFilesText: TextView
    lateinit var header_main: ConstraintLayout

    var filesAndFolders: ArrayList<FilesAndFolders> = ArrayList()


    lateinit var contexts: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        contexts = container!!.context
        val view = inflater.inflate(R.layout.activity_file_list, container, false)
        userPreferences = UserPreferences(contexts)
        dialog = ProgressDialog(contexts)
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)


        recyclerView = view.findViewById(R.id.recycler_view)
        noFilesText = view.findViewById(R.id.nofiles_textview)
        header_main = view.findViewById(R.id.header_main)
        header_main.visibility = View.GONE

        getFilesAndFolder("",contexts)
        return view
    }

    private fun getFilesAndFolder(folderID: String , context: Context) {

        if (Commons().isNetworkAvailable(context)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(viewLifecycleOwner) {
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
                                    recyclerView.adapter = FileAdapter(context, filesAndFolders)
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
                    Toast.makeText(context,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                    lifecycleScope.launch {
                        userPreferences.deleteAuthToken()
                        userPreferences.deleteUserId()
                    }
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                }
            }
        }
    }
}