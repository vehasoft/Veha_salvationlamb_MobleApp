package com.example.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.EditProfileActivity
import com.example.adapter.HomeAdapter
import com.example.fbproject.FollowerActivity
import com.example.fbproject.R
import com.example.models.PostUser
import com.example.models.Posts
import com.example.models.User
import com.example.util.APIUtil
import com.example.util.Post
import com.example.util.Profile
import com.example.util.UserPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*
import retrofit2.Call
import retrofit2.Response

class ProfileFragment(val  profile: Profile) : Fragment() {

    lateinit var userPreferences: UserPreferences
    lateinit var list : RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        userPreferences = UserPreferences(container!!.context)
        val view : View =  inflater.inflate(R.layout.fragment_profile, container, false)

        Picasso.with(container.context).load(profile.image).into(profile_pic_main)
        profile_name.text = profile.name
        profile_about.text = profile.about
        followers.text = profile.followers
        following.text = profile.following
        posts.text = profile.noOfPosts

/*
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


        list = view.findViewById(R.id.my_post_list)
        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = HomeAdapter(posts, container.context,"profile")*/

        edit_profile.setOnClickListener(){
            val intent = Intent(context, EditProfileActivity::class.java)
            startActivity(intent)
        }
        followers_linear.setOnClickListener(){
            val intent = Intent(context, FollowerActivity::class.java)
            startActivity(intent)
        }
        following_linear.setOnClickListener(){
            val intent = Intent(context, FollowerActivity::class.java)
            startActivity(intent)
        }

        getallPosts(container.context)
        return  view
    }

    private fun getallPosts(context: Context){
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
                            list.adapter = HomeAdapter(postlist, context,"profile")
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