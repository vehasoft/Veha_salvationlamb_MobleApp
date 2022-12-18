package com.example.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.R
import com.example.util.AllFollowerList
import com.example.util.Followers
import com.squareup.picasso.Picasso

class FollowAdapter() : RecyclerView.Adapter<FollowAdapter.ViewHolder>() {
    private lateinit var follows: ArrayList<AllFollowerList>
    private  lateinit var context: Context
    private  lateinit var page: String

    constructor(follows:  ArrayList<AllFollowerList>, context: Context, page: String) : this() {
        this.follows = follows
        this.context = context
        this.page = page
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name_fol)
        val profilePic: ImageView = view.findViewById(R.id.profile_pic_fol)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var layoutInflater : LayoutInflater = LayoutInflater.from(parent.context)
        var items : View = layoutInflater.inflate(R.layout.child_follow,parent,false)
        var viewHolder = ViewHolder(items)

        return viewHolder
    }

    override fun getItemCount(): Int {
        return follows.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val follow : AllFollowerList = follows[position]
        holder.name.text = follow.user.name
        if (follow.user.picture.isNullOrEmpty()){
            Picasso.with(context).load("https://www.gravatar.com/avatar/205e460b479e2e5b48aec07710c08d50").into(holder.profilePic)
        }else {
            Picasso.with(context).load(follow.user.picture).into(holder.profilePic)
        }
    }
}