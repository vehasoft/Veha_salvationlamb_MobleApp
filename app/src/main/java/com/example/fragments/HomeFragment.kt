package com.example.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
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
import com.example.models.PostUser
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
    lateinit var userPreferences: UserPreferences
    var posts: ArrayList<Posts> = ArrayList()
    lateinit var list : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        userPreferences = UserPreferences(container!!.context)
        richTextEditer(view)
        list = view.findViewById(R.id.list)

        var post1 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath1","https://www.gstatic.com/webp/gallery/1.jpg","",""))
        var post2 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath2","https://www.gstatic.com/webp/gallery/2.jpg","",""))
        var post3 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath3","https://www.gstatic.com/webp/gallery/3.jpg","",""))
        var post4 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath4","https://www.gstatic.com/webp/gallery/4.jpg","",""))
        var post5 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath5","https://www.gstatic.com/webp/gallery/5.jpg","",""))

        var posts : ArrayList<Posts> = ArrayList()
        posts.add(post1)
        posts.add(post2)
        posts.add(post3)
        posts.add(post4)
        posts.add(post5)

        //list.layoutManager = LinearLayoutManager(activity)
        //list.adapter = HomeAdapter(posts, container!!.context,"home")


        getallPosts(container.context)
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
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it",1,10)
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

    }
}