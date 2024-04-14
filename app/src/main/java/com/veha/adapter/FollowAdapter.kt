package com.veha.adapter

import android.app.AlertDialog
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
import com.veha.activity.ViewProfileActivity
import com.veha.activity.R
import com.veha.util.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import dmax.dialog.SpotsDialog
import retrofit2.Call
import retrofit2.Response

class FollowAdapter(
    private val follows: ArrayList<PostUser>,
    private val context: Context,
    private var myFollowList: HashMap<String, String>,
    private var owner: LifecycleOwner
) : RecyclerView.Adapter<FollowAdapter.ViewHolder>() {
    private lateinit var userPreferences: UserPreferences

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name_fol)
        val profilePic: ImageView = view.findViewById(R.id.profile_pic_fol)
        val followBtn: Button = view.findViewById(R.id.follow_btn)
        val followListLinear: LinearLayout = view.findViewById(R.id.follow_list_linear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        var items: View = layoutInflater.inflate(R.layout.child_follow, parent, false)
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
        if (!myFollowList.containsKey(Util.userId)) {
            holder.followBtn.text = "follow"
        } else {
            holder.followBtn.text = "unfollow"
        }
        if (myFollowList.containsValue(Util.userId)) {
            holder.followBtn.text = "follow"
        } else {
            holder.followBtn.text = "unfollow"
        }
        holder.followListLinear.setOnClickListener {
            val intent = Intent(context, ViewProfileActivity::class.java)
            intent.putExtra("userId", follow.id)
            context.startActivity(intent)
        }
        holder.followBtn.setOnClickListener {
            if (holder.followBtn.text.equals("follow")) {
                follow(Util.userId, follow.id)
                holder.followBtn.text = "unfollow"
            } else {
                follow(Util.userId, follow.id)
                holder.followBtn.text = "follow"
            }
        }
    }

    private fun follow(userId: String, followerId: String) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val followData = JsonObject()
                followData.addProperty("userId", userId)
                followData.addProperty("followerId", followerId)
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.postFollow("Bearer $it", followData)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    if (myFollowList.containsKey(followerId)) {
                                        myFollowList.remove(followerId)
                                    } else {
                                        myFollowList.put(followerId, userId)
                                    }
                                } else {
                                    Log.e("failFollow", response.errorBody().toString())
                                    val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("result", errorMessage)
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("FollowAdapter.follow", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FollowAdapter.follow", e.toString())
        }
    }
}