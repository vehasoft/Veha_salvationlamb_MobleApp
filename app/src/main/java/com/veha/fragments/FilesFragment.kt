package com.veha.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.adapter.FileAdapter
import com.veha.activity.LoginActivity
import com.veha.activity.R
import com.veha.util.Commons
import com.veha.util.FilesAndFolders
import com.veha.util.UserPreferences
import com.veha.util.Util
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class FilesFragment : Fragment() {


    lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog
    lateinit var recyclerView: RecyclerView
    lateinit var noFilesText: LinearLayout

    //lateinit var header_main: ConstraintLayout
    lateinit var listIcon: ImageView

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
        val view = inflater.inflate(R.layout.fragment_files, container, false)
        userPreferences = UserPreferences(contexts)
        dialog = SpotsDialog.Builder().setContext(contexts).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)

        listIcon = view.findViewById(R.id.view_icon)
        if (Util.listview) {
            listIcon.setImageResource(R.drawable.ic_baseline_list_24)
        } else {
            listIcon.setImageResource(R.drawable.ic_baseline_grid_on_24)
        }
        listIcon.setOnClickListener {
            Util.listview = !Util.listview
            requireFragmentManager().beginTransaction().detach(this@FilesFragment).attach(this@FilesFragment).commit()
        }

        recyclerView = view.findViewById(R.id.recycler_view)
        noFilesText = view.findViewById(R.id.no_data)/*
        header_main = view.findViewById(R.id.header_main)
        header_main.visibility = View.GONE*/

        getFilesAndFolder("", contexts)
        return view
    }

    private fun getFilesAndFolder(folderID: String, context: Context) {
        try {
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
                                Log.e("FilesFragment.getFilesAndFolder", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(context, "Somthing Went Wrong \nLogin again to continue", Toast.LENGTH_LONG)
                            .show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FilesFragment.getFilesAndFolder", e.toString())
        }
    }
}