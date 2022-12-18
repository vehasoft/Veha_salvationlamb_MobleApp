package com.example.adapter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.MainActivity
import com.example.fbproject.R
import com.example.fbproject.ViewLikesActivity
import com.example.fbproject.ViewProfileActivity
import com.example.models.Posts
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Response


class HomeAdapter() : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    private lateinit var posts: ArrayList<Posts>
    private  lateinit var context: Context
    private  lateinit var page: String
    private  lateinit var myList: HashMap<String,String>
    private  lateinit var owner: LifecycleOwner
    private lateinit var myFollowList: HashMap<String,String>
    private lateinit var myFavList: HashMap<String,String>
    private lateinit var userPreferences: UserPreferences

    constructor(posts: ArrayList<Posts>, context: Context, page: String, myFollowList: HashMap<String,String>, myfavList: HashMap<String,String>, myList: HashMap<String,String>, owner: LifecycleOwner) : this() {
        this.posts = posts
        this.context = context
        this.page = page
        this.myList = myList
        this.owner = owner
        this.myFollowList = myFollowList
        this.myFavList = myfavList
    }
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name : TextView = view.findViewById(R.id.name_post)
        val time : TextView = view.findViewById(R.id.post_time)
        val tags : TextView = view.findViewById(R.id.tags)
        val content : TextView = view.findViewById(R.id.post_content)
        val reacts : TextView = view.findViewById(R.id.no_of_reacts)
        val profilePic : ImageView = view.findViewById(R.id.profile_pic)
        val likePic : ImageView = view.findViewById(R.id.like_pic)
        val reactBtn : LinearLayout = view.findViewById(R.id.react_btn)
        val headLinear : LinearLayout = view.findViewById(R.id.head_linear)
        val likeLayout : LinearLayout = view.findViewById(R.id.like_layout)
        val likeBtn : Button = view.findViewById(R.id.like_btn)
        val shareBtn : Button = view.findViewById(R.id.share_btn)
        val followBtn : Button = view.findViewById(R.id.follow_btn)
        val deleteBtn : Button = view.findViewById(R.id.Delete_btn)
        val fav : ImageButton = view.findViewById(R.id.fav)
        val happy : ImageView = view.findViewById(R.id.smile)
        val sad : ImageView = view.findViewById(R.id.sad)
        val wow : ImageView = view.findViewById(R.id.love)
        val like : ImageView = view.findViewById(R.id.like)
        val cry : ImageView = view.findViewById(R.id.cry)
        val angry : ImageView = view.findViewById(R.id.angry)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        userPreferences = UserPreferences(context)
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
        else if(page.contentEquals("OtherProfile")){
            viewHolder.followBtn.visibility = View.GONE
            viewHolder.deleteBtn.visibility = View.GONE
            viewHolder.fav.visibility = View.GONE
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return  posts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post : Posts = posts[position]
        if (post.userId.equals(Util.userId)){
            holder.followBtn.visibility = View.GONE
        }
       // Log.e("myLikelist1",myFavList.toString())
        //myFavList.put("dd097530-7e88-11ed-b57e-33170fe2a77e","31e83750-77ed-11ed-904f-d19237cc6d7a")

        Log.e("myLikelist11",myFavList.contains(post.id).toString())
        if (!myFavList.containsKey(post.id)) {
            holder.fav.setBackgroundResource(R.drawable.heart_white)
        } else {
            holder.fav.setBackgroundResource(R.drawable.heart_red)
        }
        if(!myFollowList.containsKey(Util.userId)){
            holder.followBtn.text = "follow"
        } else {
            holder.followBtn.text = "unfollow"
        }

       // Log.e("REACTIONNNNList", myList[post.id].toString())
        if(myList.contains(post.id)){
            holder.likePic.visibility = View.VISIBLE
           // Log.e("REACTIONNNN", myList[post.id].toString())
            when(myList[post.id]){
                "smile" -> holder.likePic.setImageResource(R.drawable.ic_haha)
                "sad" -> holder.likePic.setImageResource(R.drawable.ic__33)
                "wow" -> holder.likePic.setImageResource(R.drawable.ic_wow)
                "like" -> holder.likePic.setImageResource(R.drawable.ic_love)
                "cry" -> holder.likePic.setImageResource(R.drawable.ic_cry)
                "angry" -> holder.likePic.setImageResource(R.drawable.ic_angry)
            }
            holder.likeBtn.text = "liked"
        }
        else{
            holder.likePic.visibility = View.GONE
        }
        holder.name.text = post.user.name
        holder.tags.text = post.tags
        holder.time.text = Util.getTimeAgo(post.createdAt)
        //holder.time.text = "30 mins ago"
        holder.content.text = post.content
        holder.reacts.text = post.likesCount + " people reacts"
        if (post.user.picture.isNullOrEmpty()){
            Picasso.with(context).load("https://www.gravatar.com/avatar/205e460b479e2e5b48aec07710c08d50").into(holder.profilePic)
        }else {
            Picasso.with(context).load(post.user.picture).into(holder.profilePic)
        }
        holder.reactBtn.setOnClickListener {
            //Toast.makeText(context,"REACTED", Toast.LENGTH_LONG).show()
        }
        holder.reactBtn.setOnLongClickListener {
            holder.likeLayout.visibility = View.VISIBLE
            holder.reactBtn.postDelayed(
                {
                    holder.likeLayout.visibility = View.GONE
                },10000
            )
            true
        }
        holder.likeBtn.setOnClickListener {
            if(holder.likeBtn.text.contentEquals("liked",true)){
                holder.likeBtn.text = "Like"
                holder.likePic.visibility = View.GONE
                myList.remove(post.id)
            }
            //Toast.makeText(context,"REACTED", Toast.LENGTH_LONG).show()
        }
        holder.likeBtn.setOnLongClickListener {
            holder.likeLayout.visibility = View.VISIBLE
            holder.likeBtn.postDelayed(
                {
                    holder.likeLayout.visibility = View.GONE
                },10000
            )
            true
        }
        holder.shareBtn.setOnClickListener {
            ShareCompat.IntentBuilder.from(MainActivity())
                .setType("text/plain")
                .setChooserTitle("Share URL")
                .setText("http://www.url.com")
                .startChooser();
            Toast.makeText(context,"SHARED", Toast.LENGTH_LONG).show()
        }
        holder.fav.setOnClickListener {
           // Log.e("myLikelist",myFavList.toString())
            if (myFavList.containsKey(post.id)) {
                favPost(Util.userId,post.id)
                holder.fav.setBackgroundResource(R.drawable.heart_white)

            } else {
                favPost(Util.userId,post.id)
                holder.fav.setBackgroundResource(R.drawable.heart_red)
            }
        }
       // Log.e("myfollowlist",myFollowList.toString())
        if (myFollowList.contains(post.user.id)) {
            holder.followBtn.text = "unfollow"
        } else {
            holder.followBtn.text = "follow"
        }
        holder.followBtn.setOnClickListener {
            if (holder.followBtn.text.equals("follow")){
                follow(Util.userId,post.userId)
                holder.followBtn.text = "unfollow"
            } else {
                follow(Util.userId,post.userId)
                holder.followBtn.text = "follow"
            }
        }
        holder.reacts.setOnClickListener {
            val intent = Intent(context, ViewLikesActivity::class.java)
            intent.putExtra("postId",post.id)
            context.startActivity(intent)
        }
        holder.happy.setOnClickListener {
            myList.put(post.id,"smile")
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_haha)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
        }
        holder.sad.setOnClickListener {
            myList.put(post.id,"sad")
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic__33)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
        }
        holder.angry.setOnClickListener {
            myList.put(post.id,"angry")
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_angry)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
        }
        holder.wow.setOnClickListener {
            myList.put(post.id,"wow")
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_wow)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
        }
        holder.cry.setOnClickListener {
            myList.put(post.id,"cry")
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_cry)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
        }
        holder.like.setOnClickListener {
            myList.put(post.id,"like")
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_love)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
        }
        holder.headLinear.setOnClickListener {
            if (page == "home" && post.userId != Util.userId){
                val intent = Intent(context,ViewProfileActivity::class.java)
                intent.putExtra("userId",post.userId)
                context.startActivity(intent)
            }
        }
    }
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
                            Toast.makeText(context,"Followed",Toast.LENGTH_LONG).show()
                            if (myFollowList.containsKey(followerId)){
                                myFollowList.remove(followerId)
                            }
                            else{
                                myFollowList.put(followerId,userId)
                            }
                            Log.e("myfollowlist ===",myFollowList.toString())
                        } else {
                            Log.e("failFollow",response.errorBody().toString())
                            Toast.makeText(context,"Followed Failed",Toast.LENGTH_LONG).show()
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
    }

    fun favPost(userId: String, postId: String) {
        val followData = JsonObject()
        followData.addProperty("userId",userId)
        followData.addProperty("postId",postId)
        Log.e("data",followData.toString())
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postFav("Bearer $it", followData)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            Log.e("Follow",response.body().toString())
                            Toast.makeText(context,"Fav added",Toast.LENGTH_LONG).show()
                            val resp = response.body()
                            Log.e("Status", postId)
                            Log.e("map bef", myFavList.toString())
                            if (!myFavList.containsKey(postId)){
                                myFavList.put(postId,userId)
                            }else {
                                myFavList.remove(postId)
                            }
                            Log.e("map aft", myFavList.toString())
                        } else {
                            Log.e("fail fav",response.errorBody().toString())
                            Toast.makeText(context,"Followed Failed",Toast.LENGTH_LONG).show()
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Toast.makeText(context, "No Internet", Toast.LENGTH_LONG).show()
                        Log.e("responseee", "fail")
                    }
                })
            }
        }
    }
}