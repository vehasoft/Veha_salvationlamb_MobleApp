package com.veha.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.veha.activity.BibleActivity
import com.veha.activity.R
import com.veha.util.UserPreferences
import org.json.JSONArray
import org.json.JSONObject

class BibleAdapter(val context: Context, val bibleContent: JSONArray) :
    RecyclerView.Adapter<BibleAdapter.ViewHolder>() {
    private lateinit var userPreferences: UserPreferences
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        userPreferences = UserPreferences(context)
        val view = LayoutInflater.from(context).inflate(R.layout.child_folders, parent, false)
        val views: ViewHolder = ViewHolder(view)
        /*if (!Util.listview) {
            views.fileLinear.orientation = LinearLayout.VERTICAL
            views.fileLinear.gravity = Gravity.CENTER
            views.textView.setTextColor(context.getColor(R.color.black))
        } else {
            views.fileLinear.orientation = LinearLayout.HORIZONTAL
            views.textView.setTextColor(context.getColor(R.color.black))
        }*/
        return views
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bible: JSONObject = bibleContent[position] as JSONObject
        val name: String = bible.names()?.get(0).toString()
        holder.textView.text = name
        var type: String
        if (name != "V"){
            type = "list"
        } else {
            type = "content"
        }
        holder.itemView.setOnClickListener { v: View? ->
            val intent = Intent(context, BibleActivity::class.java)
            intent.putExtra("type", type)
            intent.putExtra("content", bible.get(name).toString())
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }/*
        holder.textView.text = bible.get(name).toString()

        holder.itemView.setOnClickListener { v: View? ->
            Log.e("type", filesAndFolder.type)
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

        }*/
    }

    override fun getItemCount(): Int {
        return bibleContent.length()
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
            imageView.visibility = View.GONE
            lockSymbol.visibility = View.GONE
        }
    }
}