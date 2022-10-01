package com.example.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.EditProfileActivity
import com.example.adapter.HomeAdapter
import com.example.fbproject.R
import com.example.util.Post
import com.example.util.Profile
import com.squareup.picasso.Picasso

class ProfileFragment(val  profile: Profile) : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View =  inflater.inflate(R.layout.fragment_profile, container, false)
        val profilePic: ImageView = view.findViewById(R.id.profile_pic_main)
        val name: TextView = view.findViewById(R.id.profile_name)
        val about: TextView = view.findViewById(R.id.profile_about)
        val followers: TextView = view.findViewById(R.id.followers)
        val following: TextView = view.findViewById(R.id.following)
        val postCount: TextView = view.findViewById(R.id.posts)

        Picasso.with(container!!.context).load(profile.image).into(profilePic)
        name.text = profile.name
        about.text = profile.about
        followers.text = profile.followers
        following.text = profile.following
        postCount.text = profile.noOfPosts


        var post1 = Post("Temp1","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/1.jpg","M. Hari prasath1","30 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")
        var post2 = Post("Temp2","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/2.jpg","M. Hari prasath2","31 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")
        var post3 = Post("Temp3","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/3.jpg","M. Hari prasath3","32 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")
        var post4 = Post("Temp4","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/4.jpg","M. Hari prasath4","33 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")
        var post5 = Post("Temp5","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/5.jpg","M. Hari prasath5","34 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")

        var posts : ArrayList<Post> = ArrayList()
        posts.add(post1)
        posts.add(post2)
        posts.add(post3)
        posts.add(post4)
        posts.add(post5)


        var list : RecyclerView = view.findViewById(R.id.my_post_list)
        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = HomeAdapter(posts, container!!.context,"profile")

        var editProfile : Button = view.findViewById(R.id.edit_profile)
        editProfile.setOnClickListener(){
            val intent = Intent(context, EditProfileActivity::class.java)
            startActivity(intent)
        }


        return  view
    }


}