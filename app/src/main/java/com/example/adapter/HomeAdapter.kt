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
import com.example.fbproject.*
import com.example.models.Posts
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
        layoutInflater.inflate(R.layout.add_post,parent,false)
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
        Log.e("postttt",post.toString())

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
            holder.fav.setBackgroundResource(R.drawable.heart_white)
        } else {
            holder.fav.setBackgroundResource(R.drawable.heart_red)
        }
        Log.e("followlistttttt",myFollowList.toString())
        Log.e("followlistttttt",post.user.id.toString())
        Log.e("followlistttttt",myFollowList.containsKey(post.user.id).toString())
        if(!myFollowList.containsKey(post.user.id)){
            holder.followBtn.text = "follow"
        } else {
            holder.followBtn.text = "unfollow"
        }

       // Log.e("REACTIONNNNList", myList[post.id].toString())
        if(myList.contains(post.id)){
            holder.likePic.visibility = View.VISIBLE
           // Log.e("REACTIONNNN", myList[post.id].toString())
            when(myList[post.id]){
                SMILE -> holder.likePic.setImageResource(R.drawable.ic_smile)
                LOVE -> holder.likePic.setImageResource(R.drawable.ic_love)
                CRY -> holder.likePic.setImageResource(R.drawable.ic_cry)
                WOW -> holder.likePic.setImageResource(R.drawable.ic_wow)
                ANGRY -> holder.likePic.setImageResource(R.drawable.ic_angry)
                HAHA -> holder.likePic.setImageResource(R.drawable.ic_haha)
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
        holder.reacts.text = "$likesCount people reacts"
        if (!post.user.picture.isNullOrEmpty()){
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
            //Log.e("likebtntxtttt",holder.likeBtn.text.contentEquals("liked",true).toString())
            if(holder.likeBtn.text.contentEquals("liked",true)){
                holder.likeBtn.text = "Like"
                holder.likePic.visibility = View.GONE
                myList.remove(post.id)
                likesCount--
                holder.reacts.text = "$likesCount people reacts"
            }else {
                holder.likeLayout.visibility = View.VISIBLE
                holder.likeBtn.postDelayed(
                    {
                        holder.likeLayout.visibility = View.GONE
                    },10000
                )
                true
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

            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salvation Lamb")
                var shareMessage = "\nLet me recommend you this application\n\n"
                shareMessage = """
                    ${shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID}                    
                    """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                context.startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                //e.toString();
            }


           /* val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "URL TO BE SENT")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)*/
            //Toast.makeText(context,"SHARED", Toast.LENGTH_LONG).show()
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
        holder.smile.setOnClickListener {
            if(holder.likeBtn.text.contentEquals("liked",true)){
                holder.likeBtn.text = "Like"
                holder.likePic.visibility = View.GONE
                myList.remove(post.id)
                likesCount--
                holder.reacts.text = "$likesCount people reacts"
                holder.likeLayout.visibility = View.GONE
            } else{
                holder.likePic.visibility = View.VISIBLE
                holder.likePic.setImageResource(R.drawable.ic_smile)
                holder.likeBtn.text = "liked"
                holder.likeLayout.visibility = View.GONE
                likesCount++
                holder.reacts.text = "$likesCount people reacts"
            }
            likePost(post,SMILE)
        }
        holder.love.setOnClickListener {
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_love)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
            likePost(post,LOVE)
            likesCount++
            holder.reacts.text = "$likesCount people reacts"
        }
        holder.cry.setOnClickListener {
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_cry)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
            likePost(post,CRY)
            likesCount++
            holder.reacts.text = "$likesCount people reacts"
        }
        holder.wow.setOnClickListener {
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_wow)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
            likePost(post,WOW)
            likesCount++
            holder.reacts.text = "$likesCount people reacts"
        }
        holder.angry.setOnClickListener {
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_angry)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
            likePost(post,ANGRY)
            likesCount++
            holder.reacts.text = "$likesCount people reacts"
        }
        holder.haha.setOnClickListener {
            holder.likePic.visibility = View.VISIBLE
            holder.likePic.setImageResource(R.drawable.ic_haha)
            holder.likeBtn.text = "liked"
            holder.likeLayout.visibility = View.GONE
            likePost(post,HAHA)
            likesCount++
            holder.reacts.text = "$likesCount people reacts"
        }
        holder.headLinear.setOnClickListener {
            if (page != "profile" && page != "OtherProfile"){
                val intent = Intent(context,ViewProfileActivity::class.java)
                intent.putExtra("userId",post.userId)
                context.startActivity(intent)
            }
        }
        holder.deleteBtn.setOnClickListener {
            deletePost(post.id)
        }
        /*holder.postContainer.setOnClickListener {
            val intent = Intent(context,ViewPostActivity::class.java)
            intent.putExtra("postId",post.id)
            context.startActivity(intent)
        }*/
    }

    private fun likePost(post: Posts,reaction: String) {
        val data = JsonObject()
        data.addProperty("userId",post.userId)
        data.addProperty("postId",post.id)
        data.addProperty("reaction",reaction)
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it", "like",data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            myList.put(post.id,reaction)
                            //Log.e("likepostttttt",response.code().toString())
                            //Log.e("likepostttttt",response.body().toString())
                            Toast.makeText(context,"Liked Successfully",Toast.LENGTH_LONG).show()
                        } else {
                            //Log.e("likepostttttt",response.code().toString())
                            //Log.e("likepostttttt",response.errorBody().toString())
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("result", errorMessage)
                            Toast.makeText(context,"like failed",Toast.LENGTH_LONG).show()
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

    private fun deletePost(id: String) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.deletePost("Bearer $it", id)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            Toast.makeText(context,"Deleted Successfully",Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context,"Deletion Failed",Toast.LENGTH_LONG).show()
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
                            //Log.e("myfollowlist ===",myFollowList.toString())
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
                            //Toast.makeText(context,"Fav added",Toast.LENGTH_LONG).show()
                            val resp = response.body()
                            Log.e("Status", postId)
                            Log.e("map bef", myFavList.toString())
                            if (!myFavList.containsKey(postId)){
                                myFavList.put(postId,userId)
                            }else {
                                myFavList.remove(postId)
                            }
                            //Log.e("map aft", myFavList.toString())
                        } else {
                            Log.e("fail fav",response.errorBody().toString())
                            //Toast.makeText(context,"Followed Failed",Toast.LENGTH_LONG).show()
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