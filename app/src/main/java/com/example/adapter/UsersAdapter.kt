package com.example.adapter

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.R
import com.example.fbproject.ViewProfileActivity
import com.example.util.PostUser
import com.example.util.AllFollowerList
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Response

class UsersAdapter(private  val follows:  ArrayList<PostUser>,
                    private val context: Context,
                    private  var owner: LifecycleOwner
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {




    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name_fol)
        val profilePic: ImageView = view.findViewById(R.id.profile_pic_fol)
        val listLinear: LinearLayout = view.findViewById(R.id.follow_list_linear)
        //val followBtn: Button = view.findViewById(R.id.follow_btn)
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
        val follow: PostUser = follows[position]
        holder.name.text = follow.name
        if (!follow.picture.isNullOrEmpty()) {
            Picasso.with(context).load(follow.picture).into(holder.profilePic)
        }
        holder.listLinear.setOnClickListener {
            val intent = Intent(context, ViewProfileActivity::class.java)
            intent.putExtra("userId", follow.id)
            context.startActivity(intent)
        }
    }
}