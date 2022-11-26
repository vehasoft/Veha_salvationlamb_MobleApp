package com.example.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.HomeAdapter
import com.example.fbproject.R
import com.example.models.Posts
import com.example.util.APIUtil
import com.example.util.Post
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import jp.wasabeef.richeditor.RichEditor
import retrofit2.Call
import retrofit2.Response


class HomeFragment : Fragment() {
    //lateinit var userPreferences: UserPreferences
    var posts: ArrayList<Posts> = ArrayList()
    lateinit var list : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_home, container, false)
        richTextEditer(view)
        list = view.findViewById(R.id.list)


        getallPosts(container!!.context)
        return  view
    }
    fun richTextEditer (view: View){
        var mEditor = view.findViewById<RichEditor>(R.id.editor)
        mEditor.setEditorHeight(40)
        mEditor.setEditorFontSize(15)
        mEditor.setEditorFontColor(Color.BLACK);
        mEditor.setPlaceholder("Write something here...")
        mEditor.setPadding(10, 10, 10, 10);
        view.findViewById<ImageButton>(R.id.action_undo).setOnClickListener { mEditor.undo() }
        view.findViewById<ImageButton>(R.id.action_redo).setOnClickListener { mEditor.redo() }
        view.findViewById<ImageButton>(R.id.action_bold).setOnClickListener { mEditor.setBold() }
        view.findViewById<ImageButton>(R.id.action_italic).setOnClickListener { mEditor.setItalic() }
        view.findViewById<ImageButton>(R.id.action_strikethrough).setOnClickListener { mEditor.setStrikeThrough() }
        view.findViewById<ImageButton>(R.id.action_underline).setOnClickListener { mEditor.setUnderline() }
        view.findViewById<ImageButton>(R.id.action_heading1).setOnClickListener { mEditor.setHeading(1) }
        view.findViewById<ImageButton>(R.id.action_heading2).setOnClickListener { mEditor.setHeading(2) }
        view.findViewById<ImageButton>(R.id.action_heading3).setOnClickListener { mEditor.setHeading(3) }
        view.findViewById<ImageButton>(R.id.action_align_left).setOnClickListener { mEditor.setAlignLeft() }
        view.findViewById<ImageButton>(R.id.action_align_center).setOnClickListener { mEditor.setAlignCenter() }
        view.findViewById<ImageButton>(R.id.action_align_right).setOnClickListener { mEditor.setAlignRight() }
        view.findViewById<ImageButton>(R.id.action_insert_bullets).setOnClickListener { mEditor.setBullets() }
    }
    fun getallPosts(context: Context){
        val postlist: ArrayList<Posts> = ArrayList()
        val retrofit = APIUtil.getRetrofit()
        val call: Call<JsonObject?>? = retrofit.getPost("Bearer ${Util.token}",1,10)
        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if (response.code()==200){
                    val resp = response.body()
                    val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)

                    Log.e("Status", resp?.get("status").toString())
                    Log.e("result", resp?.get("results").toString())
                    Log.e("result", loginresp.toString())

                    for (post in loginresp){
                        val pos = Gson().fromJson(post,Posts::class.java)
                        postlist.add(pos)
                    }
                    Log.e("postlist",postlist.toString())
                    list.layoutManager = LinearLayoutManager(activity)
                    list.adapter = HomeAdapter(postlist, context,"home")
                }
            }

            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Log.e("fail ","Posts")
            }
        })
    }
}