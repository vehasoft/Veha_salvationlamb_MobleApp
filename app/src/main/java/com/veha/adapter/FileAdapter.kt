package com.veha.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import com.veha.activity.FileListActivity
import com.veha.activity.PdfActivity2
import com.veha.activity.R
import com.veha.util.Commons
import com.veha.util.FilesAndFolders
import com.veha.util.UserPreferences
import com.veha.util.Util
import retrofit2.Call
import retrofit2.Response

class FileAdapter(
    val context: Context,
    val filesAndFolders: ArrayList<FilesAndFolders>,
    val owner: LifecycleOwner
) :
    RecyclerView.Adapter<FileAdapter.ViewHolder>() {
    private lateinit var userPreferences: UserPreferences
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        userPreferences = UserPreferences(context)
        val view = LayoutInflater.from(context).inflate(R.layout.child_folders, parent, false)
        val views: ViewHolder = ViewHolder(view)
        if (!Util.listview) {
            views.fileLinear.orientation = LinearLayout.VERTICAL
            views.fileLinear.gravity = Gravity.CENTER
            views.textView.setTextColor(context.getColor(R.color.black))
        } else {
            views.fileLinear.orientation = LinearLayout.HORIZONTAL
            views.textView.setTextColor(context.getColor(R.color.black))
        }
        return views
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filesAndFolder = filesAndFolders[position]
        holder.textView.text = filesAndFolder.name
        if (filesAndFolder.type == "folder") {
            holder.imageView.setImageResource(R.drawable.folder_icon)
        } else {
            holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24)
        }
        if (java.lang.Boolean.parseBoolean(filesAndFolder.isProtected)) {
            holder.lockSymbol.visibility = View.VISIBLE
        } else {
            holder.lockSymbol.visibility = View.GONE
        }
        holder.itemView.setOnClickListener { v: View? ->
            var result = true
            if (java.lang.Boolean.parseBoolean(filesAndFolder.isProtected)) {
                result = false
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Password")
                val view = View.inflate(context, R.layout.password_layout, null)
                val passwordView = view.findViewById<TextInputEditText>(R.id.password)
                builder.setView(view)
                builder.setMessage("Enter Password")
                builder.setPositiveButton("Ok") { dialog: DialogInterface?, which: Int ->
                    val password = passwordView.text.toString()
                    if (password.isNotEmpty()) {
                        val passwordJson = JsonObject()
                        passwordJson.addProperty("password", password)
                        checkPassword(passwordJson, filesAndFolder.id, filesAndFolder)
                    } else {
                        Toast.makeText(context, "No password provided", Toast.LENGTH_LONG).show()
                    }
                }
                builder.setNegativeButton("cancel") { dialog: DialogInterface, which: Int -> dialog.cancel() }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            } else {
                if (filesAndFolder.type == "folder") {
                    val intent = Intent(context, FileListActivity::class.java)
                    intent.putExtra("folderId", filesAndFolder.id)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, PdfActivity2::class.java)
                    intent.putExtra("url", filesAndFolder.url)
                    intent.putExtra("fileName", filesAndFolder.name)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return filesAndFolders.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView
        var imageView: ImageView
        var lockSymbol: ImageView
        var fileLinear: LinearLayout

        init {
            textView = itemView.findViewById(R.id.file_name_text_view)
            imageView = itemView.findViewById(R.id.icon_view)
            lockSymbol = itemView.findViewById(R.id.lock_symbol)
            fileLinear = itemView.findViewById(R.id.file_linear)

            textView.setTextColor(context.getColor(R.color.black))
        }
    }

    fun checkPassword(password: JsonObject, fileId: String, filesAndFolder: FilesAndFolders) {
        if (Commons().isNetworkAvailable(context)) {
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(owner) {
                if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? =
                        retrofit.postCheckPassword("Bearer $it", fileId, password)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                        override fun onResponse(
                            call: Call<JsonObject?>,
                            response: Response<JsonObject?>
                        ) {
                            if (response.code() == 200) {
                                if (filesAndFolder.type == "folder") {
                                    val intent = Intent(context, FileListActivity::class.java)
                                    intent.putExtra("folderId", filesAndFolder.id)
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } else {
                                    val intent = Intent(context, PdfActivity2::class.java)
                                    intent.putExtra("url", filesAndFolder.url)
                                    intent.putExtra("fileName", filesAndFolder.name)
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            } else {
                                Toast.makeText(context, "Invalid password", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }

                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            Log.e("FileAdapter.checkpassword", "fail")
                        }
                    })
                }
            }
        }
    }
}