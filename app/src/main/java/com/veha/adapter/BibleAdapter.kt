package com.veha.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.veha.activity.AddPostActivity
import com.veha.activity.BibleActivity
import com.veha.activity.ExpandableView
import com.veha.activity.MainActivity
import com.veha.activity.R
import com.veha.util.UserPreferences
import com.veha.util.Util
import org.json.JSONArray
import org.json.JSONObject

class BibleAdapter(val context: Context, val bibleContent: JSONArray, val type: String, val owner: LifecycleOwner) :
    RecyclerView.Adapter<BibleAdapter.ViewHolder>() {
    private lateinit var userPreferences: UserPreferences
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        userPreferences = UserPreferences(context)
        val view = LayoutInflater.from(context).inflate(R.layout.child_bible, parent, false)
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
        var jsonType: String = "list"
        if (type == "list") {
            if (name == "C") {
                holder.bibleTitle.text = bible.get("n").toString()
                jsonType = "list"
            } else if (name == "V") {
                holder.bibleTitle.text = "Chapter "+ (position+1)
                jsonType = "content"
            }
        } else {
            holder.bibleContent.text = bible.get("V").toString()
        }
        holder.titleLayout.setOnClickListener { v: View? ->
            val intent = Intent(context, BibleActivity::class.java)
            intent.putExtra("type", jsonType)
            intent.putExtra("content", bible.get(name).toString())
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        holder.postBtn.setOnClickListener {
            holder.postBtn.isEnabled = false
            val data = JsonObject()
            data.addProperty("title", "bible content")
            data.addProperty("content", holder.bibleContent.text.toString())
            data.addProperty("tags", "")
            data.addProperty("image", "")
            data.addProperty("url", "")
            data.addProperty("type", "text")
            data.addProperty("userId", Util.userId)
            AddPostActivity().postData(data,context,owner)

            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
        holder.copyBtn.setOnClickListener {
            val clipBoardManager: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData = ClipData.newPlainText("bible",holder.bibleContent.text.toString())
            clipBoardManager.setPrimaryClip(clipData)
        }
    }

    override fun getItemCount(): Int {
        return bibleContent.length()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var bibleContent: ExpandableView
        var postBtn: Button
        var shareBtn: Button
        var copyBtn: ImageView
        var titleLayout: LinearLayout
        var bodyLayout: LinearLayout
        var bibleTitle: TextView

        init {
            bibleContent = itemView.findViewById(R.id.bible_content)
            postBtn = itemView.findViewById(R.id.post_btn)
            shareBtn = itemView.findViewById(R.id.share_btn)
            copyBtn = itemView.findViewById(R.id.copy_btn)
            titleLayout = itemView.findViewById(R.id.title_layout)
            bodyLayout = itemView.findViewById(R.id.body_layout)
            bibleTitle = itemView.findViewById(R.id.bible_title)
            if (type == "list"){
                bodyLayout.visibility = View.GONE
                titleLayout.visibility = View.VISIBLE
                copyBtn.visibility = View.GONE
            } else {
                bodyLayout.visibility = View.VISIBLE
                titleLayout.visibility = View.GONE
            }
        }
    }
}