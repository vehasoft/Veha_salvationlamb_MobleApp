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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
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

class FileAdapter(val context: Context, val filesAndFolders: ArrayList<FilesAndFolders>, val owner: LifecycleOwner) :
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
        holder.itemView.setOnClickListener { v: View? ->
            Log.e("type", filesAndFolder.type)
            if (java.lang.Boolean.parseBoolean(filesAndFolder.isProtected)) {
                val builder = AlertDialog.Builder(
                    context
                )
                val view = EditText(context)
                builder.setTitle("Password")
                builder.setView(view)
                builder.setMessage("Enter Password")
                builder.setPositiveButton("Ok") { dialog: DialogInterface?, which: Int ->
                    val password = view.text.toString()
                    if (password.isNotEmpty()){
                        val passwordJson = JsonObject()
                        passwordJson.addProperty("password",password)
                        val result = checkPassword(passwordJson,filesAndFolder.id)
                        if (result){
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
                            view.error = "Invalid Password"
                        }
                    } else {
                        view.error = "Enter Password"
                    }
                }
                builder.setNegativeButton("") { dialog: DialogInterface, which: Int -> dialog.cancel() }
            }

        }
    }

    override fun getItemCount(): Int {
        return filesAndFolders.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView
        var imageView: ImageView
        var fileLinear: LinearLayout

        init {
            textView = itemView.findViewById(R.id.file_name_text_view)
            imageView = itemView.findViewById(R.id.icon_view)
            fileLinear = itemView.findViewById(R.id.file_linear)
        }
    }

    fun checkPassword(password: JsonObject,fileId: String): Boolean {
        var result: Boolean = false
        if (Commons().isNetworkAvailable(context)) {
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(owner) {
                if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? = retrofit.postCheckPassword("Bearer $it",fileId,password)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                        override fun onResponse(
                            call: Call<JsonObject?>,
                            response: Response<JsonObject?>
                        ) {
                            if (response.code() == 200) {
                                result = true
                            }
                        }
                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            Log.e("FileAdapter.checkpassword", "fail")
                        }
                    })
                }
            }
        }
        return result
    }
}