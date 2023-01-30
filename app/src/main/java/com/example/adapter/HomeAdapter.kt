package com.example.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.*
import com.example.util.Posts
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Response


class HomeAdapter(
    private var posts: ArrayList<Posts>,
    private var context: Context,
    private var page: String,
    private var myList: HashMap<String,String>,
    private var myFollowList: HashMap<String,String>,
    private var myFavList: HashMap<String,String>,
    private var owner: LifecycleOwner,

) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    private lateinit var userPreferences: UserPreferences

    private val SMILE: String = "smile"
    private val LOVE: String = "love"
    private val CRY: String = "cry"
    private val WOW: String = "wow"
    private val ANGRY: String = "angry"
    private val HAHA: String = "haha"
    private var likesCount = 0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name : TextView = view.findViewById(R.id.name_post)
        val time : TextView = view.findViewById(R.id.post_time)
        val tags : TextView = view.findViewById(R.id.tags)
        val title : TextView = view.findViewById(R.id.title)
        val content : TextView = view.findViewById(R.id.post_content)
        val reacts : TextView = view.findViewById(R.id.no_of_reacts)
        val profilePic : ImageView = view.findViewById(R.id.profile_pic)
        val postPic : ImageView = view.findViewById(R.id.post_pic)
        val likePic : ImageView = view.findViewById(R.id.like_pic)
        val reactBtn : LinearLayout = view.findViewById(R.id.react_btn)
        val postContainer : LinearLayout = view.findViewById(R.id.post_layout)
        val headLinear : LinearLayout = view.findViewById(R.id.head_linear)
        val likeLayout : LinearLayout = view.findViewById(R.id.like_layout)
        val likeBtn : Button = view.findViewById(R.id.like_btn)
        val shareBtn : Button = view.findViewById(R.id.share_btn)
        val followBtn : Button = view.findViewById(R.id.follow_post_btn)
        val deleteBtn : Button = view.findViewById(R.id.Delete_btn)
        val fav : ImageButton = view.findViewById(R.id.fav)
        val smile : ImageView = view.findViewById(R.id.smile)
        val love : ImageView = view.findViewById(R.id.love)
        val cry : ImageView = view.findViewById(R.id.cry)
        val wow : ImageView = view.findViewById(R.id.wow)
        val angry : ImageView = view.findViewById(R.id.angry)
        val haha : ImageView = view.findViewById(R.id.haha)
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
        else if(page.contentEquals("OtherProfile") || page.contentEquals("searchProfile")){
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

        if (!post.picture.isNullOrEmpty()){
            holder.postPic.visibility = View.VISIBLE
            Picasso.with(context).load(post.picture).into(holder.postPic)
        } else {
            holder.postPic.visibility = View.GONE
        }

        likesCount = post.likesCount.toInt()
        if (post.userId == Util.userId){
            holder.followBtn.visibility = View.GONE
        }
        if (!myFavList.containsKey(post.id)) {
            holder.fav.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_24)
        } else {
            holder.fav.setBackgroundResource(R.drawable.ic_baseline_bookmark_24)
        }
        if(!myFollowList.containsKey(post.user.id)){
            holder.followBtn.text = "follow"
        } else {
            holder.followBtn.text = "unfollow"
        }
        if(myList.contains(post.id)){
            holder.likePic.visibility = View.VISIBLE
            when(myList[post.id]){
                SMILE -> holder.likePic.setImageResource(R.drawable.ic_smile)
                LOVE -> holder.likePic.setImageResource(R.drawable.ic_love)
                CRY -> holder.likePic.setImageResource(R.drawable.ic_cry)
                WOW -> holder.likePic.setImageResource(R.drawable.ic_wow)
                ANGRY -> holder.likePic.setImageResource(R.drawable.ic_angry)
                HAHA -> holder.likePic.setImageResource(R.drawable.ic_haha)
            }
        }
        else{
            holder.likePic.visibility = View.GONE
        }
        holder.name.text = post.user.name
        holder.tags.text = post.tags
        holder.title.text = post.title
        holder.time.text = Util.getTimeAgo(post.createdAt)
        holder.content.text = post.content
        holder.reacts.text = "$likesCount people reacts"
        if (!post.user.picture.isNullOrEmpty()){
            Picasso.with(context).load(post.user.picture).into(holder.profilePic)
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
            Log.e("likebtn","bascxghavxahgvgh")
            val myContext: Context = ContextThemeWrapper(context, R.style.menuStyle)
            val popup = PopupMenu(myContext, holder.likeBtn)
            popup.menuInflater.inflate(R.menu.react_menu, popup.menu)
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.smile -> {
                        likePost(post,SMILE,holder)
                    }
                    R.id.love -> {
                        likePost(post,LOVE,holder)
                    }
                    R.id.cry -> {
                        likePost(post,CRY,holder)
                    }
                    R.id.wow -> {
                        likePost(post,WOW,holder)
                    }
                    R.id.angry -> {
                        likePost(post,ANGRY,holder)
                    }
                    R.id.haha -> {
                        likePost(post,HAHA,holder)
                    }
                }
                true
            })
            popup.show()
            /*holder.likeLayout.visibility = View.VISIBLE
            holder.likeBtn.postDelayed(
                {
                    holder.likeLayout.visibility = View.GONE
                },10000
            )
            true*/
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
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salvation Lamb")
                var shareMessage = "${post.content} \n\n\n\nLet me recommend you this application\n\n"
                shareMessage = """
                    ${shareMessage + "http://salvationlamb.vehasoft.com/" + post.id}                    
                    """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                context.startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                Log.e("exception",e.toString())
            }
        }
        holder.fav.setOnClickListener {
            favPost(Util.userId,post.id,holder)

        }
        holder.followBtn.setOnClickListener {
            follow(Util.userId,post.userId,holder)
        }
        holder.reacts.setOnClickListener {
            val intent = Intent(context, ViewLikesActivity::class.java)
            intent.putExtra("postId",post.id)
            context.startActivity(intent)
        }
        holder.smile.setOnClickListener {
            likePost(post,SMILE,holder)
        }
        holder.love.setOnClickListener {
            likePost(post,LOVE,holder)
        }
        holder.cry.setOnClickListener {
            likePost(post,CRY,holder)
        }
        holder.wow.setOnClickListener {
            likePost(post,WOW,holder)
        }
        holder.angry.setOnClickListener {
            likePost(post,ANGRY,holder)
        }
        holder.haha.setOnClickListener {
            likePost(post,HAHA,holder)
        }
        holder.headLinear.setOnClickListener {
            if (page != "profile" && page != "OtherProfile"){
                val intent = Intent(context,ViewProfileActivity::class.java)
                intent.putExtra("userId",post.userId)
                context.startActivity(intent)
            }
        }
        holder.deleteBtn.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setMessage("Delete post")
            builder.setTitle("Alert !")
            builder.setCancelable(false)
            builder.setPositiveButton("Delete") { _: DialogInterface?, _: Int -> deletePost(post)
                posts.removeAt(posts.indexOf(post))}
            builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()

        }
    }

    private fun likePost(post: Posts,reaction: String,holder: ViewHolder) {
        val data = JsonObject()
        data.addProperty("userId",Util.userId)
        data.addProperty("postId",post.id)
        data.addProperty("reaction",reaction)
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it", "like",data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val msg: String = Gson().fromJson(response.body()!!.get("message"), String::class.java)
                            val likesCount: String = Gson().fromJson(response.body()!!.get("likesCount"), String::class.java)
                            if(msg == "liked"){
                                myList.put(post.id,reaction)
                                holder.likePic.visibility = View.VISIBLE
                                holder.likeLayout.visibility = View.GONE
                                when(reaction){
                                    SMILE -> holder.likePic.setImageResource(R.drawable.ic_smile)
                                    LOVE -> holder.likePic.setImageResource(R.drawable.ic_love)
                                    CRY -> holder.likePic.setImageResource(R.drawable.ic_cry)
                                    WOW -> holder.likePic.setImageResource(R.drawable.ic_wow)
                                    ANGRY -> holder.likePic.setImageResource(R.drawable.ic_angry)
                                    HAHA -> holder.likePic.setImageResource(R.drawable.ic_haha)
                                }
                                holder.reacts.text = "$likesCount people reacts"
                            } else if(msg == "unliked"){
                                holder.likePic.visibility = View.GONE
                                myList.remove(post.id)
                                holder.likeLayout.visibility = View.GONE
                                holder.reacts.text = "$likesCount people reacts"
                            }
                        } else {
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
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

    private fun deletePost(post: Posts) {
        Log.e("postid  ==== ",post.id)
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.deletePost("Bearer $it", post.id)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            Toast.makeText(context,"Deleted Successfully"+posts.indexOf(post),Toast.LENGTH_LONG).show()
                            notifyItemRemoved(posts.indexOf(post))
                        } else {
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
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

    private fun follow(userId: String, followerId: String,holder: ViewHolder) {
        val followData = JsonObject()
        followData.addProperty("userId",userId)
        followData.addProperty("followerId",followerId)
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postFollow("Bearer $it", followData)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val msg: String = Gson().fromJson(response.body()!!.get("message"), String::class.java)
                            Log.e("msg follow",msg)
                            if (msg == "unfollow"){
                                holder.followBtn.text = "Follow"
                                myFollowList.put(followerId,userId)
                            }else if (msg == "follow"){
                                holder.followBtn.text = "unfollow"
                                myFollowList.remove(followerId)
                            }
                            notifyDataSetChanged()
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
    }
    fun favPost(userId: String, postId: String,holder: ViewHolder) {
        val followData = JsonObject()
        followData.addProperty("userId",userId)
        followData.addProperty("postId",postId)
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postFav("Bearer $it", followData)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            Log.e("Follow",response.body().toString())
                            val resp = response.body()
                            val msg: String = Gson().fromJson(resp!!.get("message"), String::class.java)
                            Log.e("Status", postId)
                            Log.e("map bef", myFavList.toString())
                            if (msg.equals("fav")){
                                myFavList.put(postId,userId)
                                holder.fav.setBackgroundResource(R.drawable.ic_baseline_bookmark_24)
                            }else if (msg.equals("unfav")){
                                myFavList.remove(postId)
                                holder.fav.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_24)
                            }
                            notifyDataSetChanged()
                        } else {
                            Log.e("fail fav",response.errorBody().toString())
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("errorMessage", errorMessage)
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
    fun getPost(postId: String,holder: ViewHolder) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it", postId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val post: Posts = Gson().fromJson(response.body()?.get("result"), Posts::class.java)
                            holder.reacts.text = "${post.likesCount} people reacts"

                        } else {
                            Log.e("fail fav",response.errorBody().toString())
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
    fun addItem(post: ArrayList<Posts>){
        posts.addAll(post)
        notifyDataSetChanged()
    }
}