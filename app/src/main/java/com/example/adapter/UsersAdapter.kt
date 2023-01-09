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
import com.example.models.PostUser
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


    private lateinit var userPreferences: UserPreferences



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
        val follow : PostUser = follows[position]
        holder.name.text = follow.name
        if (!follow.picture.isNullOrEmpty()){
            Picasso.with(context).load(follow.picture).into(holder.profilePic)
        }
        holder.listLinear.setOnClickListener {
            val intent = Intent(context,ViewProfileActivity::class.java)
            intent.putExtra("userId",follow.id)
            context.startActivity(intent)
        }
        /*if(!myFollowList.containsKey(Util.userId)){
            holder.followBtn.text = "follow"
        } else {
            holder.followBtn.text = "unfollow"
        }
        if(myFollowList.containsValue(Util.userId)){
            holder.followBtn.text = "follow"
        } else {
            holder.followBtn.text = "unfollow"
        }*/
        /*holder.followListLinear.setOnClickListener {
            val intent = Intent(context, ViewProfileActivity::class.java)
            intent.putExtra("userId",follow.followerId)
            context.startActivity(intent)
        }
        holder.followBtn.setOnClickListener {
            if (holder.followBtn.text.equals("follow")){
                follow(follow.userId,follow.followerId)
                holder.followBtn.text = "unfollow"
            } else {
                follow(follow.userId,follow.followerId)
                holder.followBtn.text = "follow"
            }
        }*/
    }/*
    private fun follow(userId: String, followerId: String) {
        val followData = JsonObject()
        followData.addProperty("userId",userId)
        followData.addProperty("followerId",followerId)
        // Log.e("data",data.toString())
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            // Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postFollow("Bearer $it", followData)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            Log.e("Follow",response.body().toString())
                            //Toast.makeText(context,"Followed",Toast.LENGTH_LONG).show()
                            if (myFollowList.containsKey(followerId)){
                                myFollowList.remove(followerId)
                            }
                            else{
                                myFollowList.put(followerId,userId)
                            }
                            Log.e("myfollowlist ===",myFollowList.toString())
                        } else {
                            Log.e("failFollow",response.errorBody().toString())
                            //Toast.makeText(context,"Followed Failed",Toast.LENGTH_LONG).show()
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("result", errorMessage)
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Toast.makeText(context, "No Internet", Toast.LENGTH_LONG).show()
                        Log.e("responseee", "fail")
                    }
                })
            }
        }
    }*/
}