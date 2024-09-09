package com.veha.adapter

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.veha.activity.AddPostActivity
import com.veha.activity.BibleActivity
import com.veha.activity.ExpandableView
import com.veha.activity.LoginActivity
import com.veha.activity.MainActivity
import com.veha.activity.R
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.Util
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response

class BibleAdapter(val context: Context, val bibleContent: JSONArray, val type: String, val owner: LifecycleOwner, var details: String) :
    RecyclerView.Adapter<BibleAdapter.ViewHolder>() {
    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        userPreferences = UserPreferences(context)
        dialog = SpotsDialog.Builder().setContext(context).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
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
        holder.bibleDef.text = details
        var namee: String = ""
        namee = if (details.contains("Old")){
            "Old Edition"
        } else {
            "new Edition"
        }
        holder.bibleName.text = namee
        if (type == "list") {
            if (name == "C") {
                holder.bibleTitle.text = bible.get("n").toString()
                val json: JSONArray = bible.get(name) as JSONArray
                holder.count.text = "Total Chapters : " + json.length().toString()
                jsonType = "list"
            } else if (name == "V") {
                val json: JSONArray = bible.get(name) as JSONArray
                holder.count.text = "Total Verses : " + json.length().toString()
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
            if (name == "V") {
                intent.putExtra("details", details + "/" + "Chapter " + (position + 1))
            }
            if (name == "C") {
                intent.putExtra("details",details + "/" + bible.get("n").toString())
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        holder.postBtn.setOnClickListener {
            holder.postBtn.isEnabled = false
            val data = JsonObject()
            val tags = details.split("/");
            data.addProperty("title", "Bible Post - " + tags[0] + " Edition")
            data.addProperty("content", holder.bibleContent.text.toString())
            data.addProperty("tags", tags[1] + "," + tags[2])
            data.addProperty("image", "")
            data.addProperty("url", "")
            data.addProperty("type", "image")
            data.addProperty("userId", Util.userId)
            postData(data)
/*
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)*/
        }
        holder.copyBtn.setOnClickListener {
            val clipBoardManager: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData = ClipData.newPlainText("bible",holder.bibleContent.text.toString())
            clipBoardManager.setPrimaryClip(clipData)
        }
        holder.shareBtn.setOnClickListener {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salvation Lamb")
                var shareMessage = "${holder.bibleContent.text.toString()} \n\n\n\nLet me recommend you this application\n\n"
                shareMessage = """
                    ${shareMessage + "https://salvationlamb.com/redirect"}                    
                    """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                context.startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                Log.e("exception", e.toString())
            }
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
        var count: TextView
        var bibleDef: TextView
        var bibleName: TextView

        init {
            bibleContent = itemView.findViewById(R.id.bible_content)
            postBtn = itemView.findViewById(R.id.post_btn)
            shareBtn = itemView.findViewById(R.id.share_btn)
            copyBtn = itemView.findViewById(R.id.copy_btn)
            titleLayout = itemView.findViewById(R.id.title_layout)
            bodyLayout = itemView.findViewById(R.id.body_layout)
            bibleTitle = itemView.findViewById(R.id.bible_title)
            count = itemView.findViewById(R.id.count)
            bibleName = itemView.findViewById(R.id.bible_name)
            bibleDef = itemView.findViewById(R.id.bible_definition)
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
    private fun postData(data: JsonObject) {
        try {
            dialog.show()
            if (Commons().isNetworkAvailable(context)) {
                val retrofit = Util.getRetrofit()
                val userPreferences = UserPreferences(context);
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call1: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it", "post", data)
                        call1!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    data.remove("userId")
                                    data.remove("title")
                                    data.remove("content")
                                    val intent = Intent(context, MainActivity::class.java)
                                    context.startActivity(intent)
                                } else {
                                    Log.e("failAddPost - Status", response.code().toString())
                                    Log.e("failAddPost", response.errorBody().toString())
                                    Log.e("failAddPost", response.toString())
                                }
                                call1.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("Bible.postData", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AddPostActivity.postData", e.toString())
        } finally {
            dialog.dismiss()
        }
    }
}