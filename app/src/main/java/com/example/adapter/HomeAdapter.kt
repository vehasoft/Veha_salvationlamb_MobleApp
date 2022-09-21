package com.example.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.R
import com.example.util.Post
import com.squareup.picasso.Picasso
import java.net.URL


class HomeAdapter() : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    private lateinit var posts: ArrayList<Post>
    private  lateinit var context: Context

    constructor(posts: ArrayList<Post>,context: Context) : this() {
        this.posts = posts
        this.context = context
    }
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name : TextView = view.findViewById(R.id.name_post)
        val time : TextView = view.findViewById(R.id.post_time)
        val tags : TextView = view.findViewById(R.id.tags)
        val content : TextView = view.findViewById(R.id.post_content)
        val reacts : TextView = view.findViewById(R.id.no_of_reacts)
        val profilePic : ImageView = view.findViewById(R.id.profile_pic)
        val reactBtn : Button = view.findViewById(R.id.react_btn)
        val shareBtn : Button = view.findViewById(R.id.share_btn)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var layoutInflater : LayoutInflater = LayoutInflater.from(context)
        var items : View = layoutInflater.inflate(R.layout.child_post,parent,false)
        var viewHolder = ViewHolder(items)
        return viewHolder

    }

    override fun getItemCount(): Int {
        return  posts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post : Post = posts[position]
        holder.name.text = post.name
        holder.tags.text = post.tags
        holder.time.text = post.time
        holder.content.text = post.content
        holder.reacts.text = post.noOfReacts + " people reacts"
        Picasso.with(context).load(post.image).into(holder.profilePic)
        holder.reactBtn.setOnClickListener { Toast.makeText(context,"REACTED", Toast.LENGTH_LONG).show() }
        holder.shareBtn.setOnClickListener { Toast.makeText(context,"SHARED", Toast.LENGTH_LONG).show() }
    }
}