package com.veha.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.veha.activity.R
import com.veha.activity.ViewProfileActivity
import com.veha.util.PostLikes
import com.squareup.picasso.Picasso
import com.veha.activity.NoPermissionActivity
import com.veha.util.Permission
import com.veha.util.PermissionType
import com.veha.util.Util

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
        val react : TextView = view.findViewById(R.id.react_txt)
        val likeListLinear : LinearLayout = view.findViewById(R.id.like_list_linear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewLikesAdapter.ViewHolder {
        var layoutInflater: LayoutInflater = LayoutInflater.from(context)
        var items: View = layoutInflater.inflate(R.layout.child_likes_list, parent, false)
        return ViewHolder(items)
    }

    override fun getItemCount(): Int {
        return  posts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post : PostLikes = posts[position]

        holder.react.text = post.reaction
        holder.name.text = post.user.name
        if (!post.user.picture.isNullOrEmpty()){
            Picasso.with(context).load(post.user.picture).into(holder.profilePic)
        }

        holder.likeListLinear.setOnClickListener {
            if (Util.hasPermission(PermissionType.USER.value, Permission.READ.value)) {
                val intent = Intent(context, ViewProfileActivity::class.java)
                intent.putExtra("userId", post.userId)
                context.startActivity(intent)
            } else {
                val intent = Intent(context, NoPermissionActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}