package com.example.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.R
import com.example.fbproject.ViewProfileActivity
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
        val likeListLinear : LinearLayout = view.findViewById(R.id.like_list_linear)
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
            "smile" -> holder.emoji.setImageResource(R.drawable.ic_smile)
            "love" -> holder.emoji.setImageResource(R.drawable.ic_love)
            "cry" -> holder.emoji.setImageResource(R.drawable.ic_cry)
            "wow" -> holder.emoji.setImageResource(R.drawable.ic_wow)
            "angry" -> holder.emoji.setImageResource(R.drawable.ic_angry)
            "haha" -> holder.emoji.setImageResource(R.drawable.ic_haha)
        }

        holder.name.text = post.user.name
        if (!post.user.picture.isNullOrEmpty()){
            Picasso.with(context).load(post.user.picture).into(holder.profilePic)
        }

        holder.likeListLinear.setOnClickListener {
            val intent = Intent(context, ViewProfileActivity::class.java)
            intent.putExtra("userId",post.userId)
            context.startActivity(intent)
        }
    }
}