package com.veha.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.adapter.BibleAdapter
import com.veha.util.UserPreferences
import org.chromium.base.Log
import org.json.JSONArray

class BibleActivity : AppCompatActivity() {
    lateinit var logo: ImageView
    lateinit var recyclerView: RecyclerView
    lateinit var content: TextView
    lateinit var txtLinear: LinearLayout
    lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bible)
        userPreferences = UserPreferences(this@BibleActivity)
        recyclerView = findViewById(R.id.recycler_view)
        logo = findViewById(R.id.prod_logo)
        content = findViewById(R.id.content)
        txtLinear = findViewById(R.id.txt_linear)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val type: String = intent.extras!!.getString("type").toString()
        val obj: JSONArray
        Log.e("type",type)
        if (type == "list"){
            obj = JSONArray(intent.extras!!.get("content").toString())
            recyclerView.visibility = View.VISIBLE
            txtLinear.visibility = View.GONE
            recyclerView.layoutManager = LinearLayoutManager(this)
            //Log.e("biblejsonarray",obj.toString())
            recyclerView.adapter = BibleAdapter(this,obj)
        } else{
            recyclerView.visibility = View.GONE
            txtLinear.visibility = View.VISIBLE
            content.text = intent.extras!!.get("content").toString()
        }
    }
}