package com.example.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.R
import com.example.util.Followers
import com.squareup.picasso.Picasso

class FollowAdapter() : RecyclerView.Adapter<FollowAdapter.ViewHolder>() {
    private lateinit var follows: ArrayList<Followers>
    private  lateinit var context: Context
    private  lateinit var page: String

    constructor(follows: ArrayList<Followers>, context: Context, page: String) : this() {
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
        val follow : Followers = follows[position]
        holder.name.text = follow.name
        Picasso.with(context).load(follow.image).into(holder.profilePic)
    }
}