package com.example.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.R
import com.example.util.PostLikes
import com.squareup.picasso.Picasso

class ViewLikesAdapter() : RecyclerView.Adapter<ViewLikesAdapter.ViewHolder>() {
    private lateinit var posts: ArrayList<PostLikes>
    private  lateinit var context: Context

    constructor(posts: ArrayList<PostLikes>, context: Context) : this() {
        this.posts = posts
        this.context = context
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name : TextView = view.findViewById(R.id.name_fol)
        val profilePic : ImageView = view.findViewById(R.id.profile_pic_fol)
        val emoji : ImageView = view.findViewById(R.id.like_symbol)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewLikesAdapter.ViewHolder {
        var layoutInflater : LayoutInflater = LayoutInflater.from(context)
        var items : View = layoutInflater.inflate(R.layout.child_likes_list,parent,false)
        var viewHolder = ViewLikesAdapter.ViewHolder(items)
        return  viewHolder
    }

    override fun getItemCount(): Int {
        return  posts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post : PostLikes = posts[position]

        when(post.reaction){
            "happy" -> holder.emoji.setImageResource(R.drawable.moon)
            "sad" -> holder.emoji.setImageResource(R.drawable.moon)
            "like" -> holder.emoji.setImageResource(R.drawable.moon)
            "love" -> holder.emoji.setImageResource(R.drawable.moon)
            "cry" -> holder.emoji.setImageResource(R.drawable.moon)
            "angry" -> holder.emoji.setImageResource(R.drawable.moon)
        }

        holder.name.text = post.user.name
        if (post.user.picture.isNullOrEmpty()){
            Picasso.with(context).load("https://www.gravatar.com/avatar/205e460b479e2e5b48aec07710c08d50").into(holder.profilePic)
        }else {
            Picasso.with(context).load(post.user.picture).into(holder.profilePic)
        }


    }
}