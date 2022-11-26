package com.example.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.R
import com.example.models.PostUser
import com.example.models.Posts
import com.example.util.Post
import com.example.util.Util
import com.squareup.picasso.Picasso


class HomeAdapter() : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    private lateinit var posts: ArrayList<Posts>
    private  lateinit var context: Context
    private  lateinit var page: String
    private  var liked: Boolean = false
    private  var following: Boolean = false

    constructor(posts: ArrayList<Posts>, context: Context, page: String) : this() {
        this.posts = posts
        this.context = context
        this.page = page
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
        val followBtn : Button = view.findViewById(R.id.follow_btn)
        val deleteBtn : Button = view.findViewById(R.id.Delete_btn)
        val fav : ImageButton = view.findViewById(R.id.fav)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var layoutInflater : LayoutInflater = LayoutInflater.from(context)
        var items : View = layoutInflater.inflate(R.layout.child_post,parent,false)
        var viewHolder = ViewHolder(items)
        if(page.contentEquals("home")){
            viewHolder.followBtn.visibility = View.VISIBLE
            viewHolder.deleteBtn.visibility = View.GONE
            viewHolder.fav.visibility = View.VISIBLE
        }
        else if(page.contentEquals("profile")){
            viewHolder.followBtn.visibility = View.GONE
            viewHolder.deleteBtn.visibility = View.VISIBLE
            viewHolder.fav.visibility = View.GONE
        }
        return viewHolder

    }

    override fun getItemCount(): Int {
        return  posts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post : Posts = posts[position]


       /* if (!post.liked) {
            holder.fav.setBackgroundResource(R.drawable.heart_white)
            post.liked = true

        } else {
            holder.fav.setBackgroundResource(R.drawable.heart_red)
            post.liked = false
        }

        if (post.following) {
            holder.followBtn.text = "unfollow"
            post.following = false
        } else {
            holder.followBtn.text = "follow"
            post.following = true
        }*/
        if (post.user==null){
            post.user = PostUser("12345","TEMP","https://www.gravatar.com/avatar/205e460b479e2e5b48aec07710c08d50","false","temp@temp.com")
        }

        holder.name.text = post.user.name
        holder.tags.text = post.tags
        holder.time.text = Util.getTimeAgo(post.createdAt)
        holder.content.text = post.content
        holder.reacts.text = post.likesCount + " people reacts"
        if (post.user.picture.isNullOrEmpty()){

        }else {
            Picasso.with(context).load(post.user.picture).into(holder.profilePic)
        }
        holder.reactBtn.setOnClickListener { Toast.makeText(context,"REACTED", Toast.LENGTH_LONG).show() }
        holder.shareBtn.setOnClickListener { Toast.makeText(context,"SHARED", Toast.LENGTH_LONG).show() }
        holder.fav.setOnClickListener {
            /*if (!post.liked) {
                holder.fav.setBackgroundResource(R.drawable.heart_white)
                post.liked = true

            } else {
                holder.fav.setBackgroundResource(R.drawable.heart_red)
                post.liked = false
            }*/
        }
        holder.followBtn.setOnClickListener {
            /*if (post.following) {
                holder.followBtn.text = "unfollow"
                post.following = false
            } else {
                holder.followBtn.text = "follow"
                post.following = true
            }*/
        }
    }
}