package com.example.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.HomeAdapter
import com.example.fbproject.R
import com.example.util.Post
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View =  inflater.inflate(R.layout.fragment_profile, container, false)

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
        list.adapter = HomeAdapter(posts, container!!.context)

        var profilePic : ImageView = view.findViewById(R.id.profile_pic_main)
        Picasso.with(container!!.context).load("https://www.gstatic.com/webp/gallery/1.jpg").into(profilePic)


        return  view
    }

}