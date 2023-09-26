package com.veha.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.squareup.picasso.Picasso
import com.veha.activity.*
import com.veha.util.Commons
import com.veha.util.Posts
import com.veha.util.UserPreferences
import com.veha.util.Util
import dmax.dialog.SpotsDialog
import retrofit2.Call
import retrofit2.Response


class HomeAdapter(
    private var posts: ArrayList<Posts>,
    private var context: Context,
    private var page: String,
    private var myList: HashMap<String, String>,
    private var myFollowList: HashMap<String, String>,
    private var myFavList: HashMap<String, String>,
    private var owner: LifecycleOwner,
) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: android.app.AlertDialog
    private var likesCount = 0
    private var currentHolder: ViewHolder? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name_post)
        val time: TextView = view.findViewById(R.id.post_time)
        val fullTime: TextView = view.findViewById(R.id.post_full_time)
        val tags: TextView = view.findViewById(R.id.tags)
        val saveTxt: TextView = view.findViewById(R.id.save_txt)
        val title: TextView = view.findViewById(R.id.title)
        val content: ExpandableView = view.findViewById(R.id.post_content)
        val reacts: TextView = view.findViewById(R.id.no_of_reacts)
        val profilePic: ImageView = view.findViewById(R.id.profile_pic)
        val postPic: ImageView = view.findViewById(R.id.post_pic)
        val postVideo: YouTubePlayerView = view.findViewById(R.id.post_video)
        val audioLayout: LinearLayout = view.findViewById(R.id.audio_layout)
        val pauseBtn: ImageView = view.findViewById(R.id.pause_btn)
        val playBtn: ImageView = view.findViewById(R.id.play_btn)
        val seekbar: SeekBar = view.findViewById(R.id.seekBar)
        val headLinear: LinearLayout = view.findViewById(R.id.head_linear)
        val likeBtn: Button = view.findViewById(R.id.like_btn)
        val shareBtn: Button = view.findViewById(R.id.share_btn)
        val followBtn: Button = view.findViewById(R.id.follow_post_btn)
        val deleteBtn: Button = view.findViewById(R.id.Delete_btn)
        val fav: ImageButton = view.findViewById(R.id.fav)
        val overallLayout: ConstraintLayout = view.findViewById(R.id.child_post_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        userPreferences = UserPreferences(context)
        dialog = SpotsDialog.Builder().setContext(context).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        var layoutInflater: LayoutInflater = LayoutInflater.from(context)
        var items: View = layoutInflater.inflate(R.layout.child_post, parent, false)
        var viewHolder = ViewHolder(items)
        viewHolder.title.textSize = 20F
        viewHolder.content.textSize = Util.fontSize.toFloat()
        viewHolder.tags.textSize = Util.fontSize.toFloat()
        viewHolder.tags.setTextColor(context.getColor(R.color.primary_blue))



        if (page.contentEquals("home")) {
            viewHolder.followBtn.visibility = View.VISIBLE
            viewHolder.deleteBtn.visibility = View.GONE
            viewHolder.fav.visibility = View.VISIBLE
            viewHolder.saveTxt.visibility = View.VISIBLE
        } else if (page.contentEquals("profile")) {
            viewHolder.followBtn.visibility = View.GONE
            viewHolder.deleteBtn.visibility = View.VISIBLE
            viewHolder.fav.visibility = View.GONE
            viewHolder.saveTxt.visibility = View.GONE
        } else if (page.contentEquals("OtherProfile") || page.contentEquals("searchProfile")) {
            viewHolder.followBtn.visibility = View.GONE
            viewHolder.deleteBtn.visibility = View.GONE
            viewHolder.fav.visibility = View.GONE
            viewHolder.saveTxt.visibility = View.GONE
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun getTags(tags: String): String {
        var opTags = ""
        if (!TextUtils.isEmpty(tags)) {
            val tagArray = tags.split(",")
            tagArray.forEach {
                opTags += "#$it"
            }
        }
        return opTags
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post: Posts = posts[position]
        holder.content.setText(post.content)
        holder.content.setOnClickListener { holder.content.expand() }
        if (!post.type.isNullOrEmpty()) {
            when (post.type) {
                "image" -> {
                    holder.audioLayout.visibility = View.GONE
                    holder.postVideo.visibility = View.GONE
                    if (!post.picture.isNullOrEmpty()) {
                        holder.postPic.visibility = View.VISIBLE
                        Picasso.with(context).load(post.picture).fit().centerInside().into(holder.postPic)
                    } else {
                        holder.postPic.visibility = View.GONE
                    }
                    holder.postPic.setOnClickListener {
                        if (!post.picture.isNullOrEmpty()) {
                            val intent = Intent(context, ImageDetailActivity::class.java)
                            intent.putExtra("profilePic", post.picture)
                            context.startActivity(intent)
                        }
                    }
                }

                "audio" -> {
                    holder.postVideo.visibility = View.GONE
                    holder.postPic.visibility = View.GONE
                    val myHandler = Handler()
                    if (!post.url.isNullOrEmpty()) {
                        holder.audioLayout.visibility = View.VISIBLE
                        //mediaPlayer.setDataSource("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3")
                        //mediaPlayer.setDataSource("https://salvationlamb-images.s3.ap-south-1.amazonaws.com/post/168735710115316871075951701686907491197+%281%29.mp3")
                        //mediaPlayer.setDataSource("https://salvationlamb-images.s3.ap-south-1.amazonaws.com/post/16871075951701686907491197.mp3")
                        //mediaPlayer.setDataSource("https://salvationlamb-images.s3.ap-south-1.amazonaws.com/post/ff-16b-1c-44100hz.aac")
                        //mediaPlayer.setDataSource("https://salvationlamb-images.s3.ap-south-1.amazonaws.com/post/1687282774781ff-16b-1c-44100hz.aac")

                        val updateSongTime: Runnable = object : Runnable {
                            override fun run() {
                                if (Util.player.isPlaying) {
                                    currentHolder!!.seekbar.progress = Util.player.currentPosition
                                    myHandler.postDelayed(this, 100)
                                } else {
                                    myHandler.removeCallbacks(this)
                                    holder.pauseBtn.visibility = View.GONE
                                    holder.playBtn.visibility = View.VISIBLE

                                }
                            }
                        }
                        holder.playBtn.setOnClickListener {

                            if (currentHolder != null && Util.player != null && Util.player.isPlaying) {
                                myHandler.removeCallbacks(updateSongTime)
                                Util.player.pause()
                                currentHolder!!.pauseBtn.visibility = View.GONE
                                currentHolder!!.seekbar.progress = 0
                                currentHolder!!.playBtn.visibility = View.VISIBLE
                                Util.player.stop()
                                Util.player.release()
                                Util.player = null
                            }
                            Util.player = MediaPlayer()
                            try {
                                Util.player.setDataSource(post.url)
                                Util.player.prepare()
                                holder.seekbar.max = Util.player.duration
                                holder.seekbar.isClickable = true
                                Util.player.start()
                                holder.seekbar.progress = Util.player.currentPosition
                                myHandler.postDelayed(updateSongTime, 100)
                                holder.playBtn.visibility = View.GONE
                                holder.pauseBtn.visibility = View.VISIBLE
                                holder.seekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                        if (fromUser) {
                                            Util.player.seekTo(progress)
                                        }
                                    }
                                })
                            } catch (e: Exception) {
                                posts.removeAt(position)
                                Handler().post { this@HomeAdapter.notifyItemRemoved(position) }
                            }
                            currentHolder = holder
                        }
                        holder.pauseBtn.setOnClickListener {
                            if (Util.player.isPlaying) {
                                myHandler.removeCallbacks(updateSongTime)
                                Util.player.pause()
                            }
                            holder.pauseBtn.visibility = View.GONE
                            holder.playBtn.visibility = View.VISIBLE
                        }
                    } else {
                        holder.audioLayout.visibility = View.GONE
                    }

                }

                "video" -> {
                    holder.audioLayout.visibility = View.GONE
                    holder.postPic.visibility = View.GONE
                    if (!post.url.isNullOrEmpty()) {/*holder.postVideo.settings.javaScriptEnabled = true
                        holder.postVideo.settings.domStorageEnabled = true
                        holder.postVideo.webChromeClient = WebChromeClient()
                        holder.postVideo.webViewClient = WebViewClient()
                        holder.postVideo.visibility = View.VISIBLE
                        holder.postVideo.loadUrl(Util.getVideo(post.url))*/
                        holder.postVideo.visibility = View.VISIBLE
                        try {
                            (context as MainActivity).lifecycle.addObserver(holder.postVideo)
                        }catch (e: Exception){
                            (context as SearchActivity).lifecycle.addObserver(holder.postVideo)
                        }
                        val youTubePlayerListener = object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                youTubePlayer.cueVideo(post.url, 0f)
                            }
                        }
                        val iFramePlayerOptions = IFramePlayerOptions.Builder().controls(1).autoplay(0).build()

                        holder.postVideo.enableAutomaticInitialization = false
                        try {

                            holder.postVideo.initialize(youTubePlayerListener, iFramePlayerOptions)
                        } catch (e: Exception) {
                            Log.e("Exception", e.toString());
                        }
                    } else {
                        holder.postVideo.visibility = View.GONE
                    }
                }
            }
        }

        likesCount = post.likesCount.toInt()
        if (post.userId == Util.userId || page.contentEquals("OtherProfile") || page.contentEquals("searchProfile")) holder.followBtn.visibility =
            View.GONE else holder.followBtn.visibility = View.VISIBLE
        if (!myFavList.containsKey(post.id)) {
            holder.fav.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_24)
            holder.saveTxt.text = "Save"
        } else {
            holder.fav.setBackgroundResource(R.drawable.ic_baseline_bookmark_24)
            holder.saveTxt.text = "Saved"
        }
        if (!myFollowList.containsKey(post.user.id)) {
            holder.followBtn.text = "Follow"
        } else {
            holder.followBtn.text = "Unfollow"
        }
        if (myList.contains(post.id)) {
            holder.likeBtn.text = myList[post.id]
        } else {
            holder.likeBtn.text = "React"
        }
        holder.name.text = post.user.name
        holder.tags.text = getTags(post.tags)
        holder.title.text = post.title
        holder.time.text = Util.getTimeAgo(post.createdAt)
        holder.fullTime.text = post.createdAt
        holder.reacts.text = "$likesCount people reacts"
        if (!post.user.picture.isNullOrEmpty()) {
            Picasso.with(context).load(post.user.picture).into(holder.profilePic)
        } else {
            holder.profilePic.setImageResource(R.drawable.ic_profile)
        }
        holder.likeBtn.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(context, R.style.menuStyle)
            val popup = PopupMenu(myContext, holder.likeBtn)
            popup.menuInflater.inflate(R.menu.react_menu, popup.menu)
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.react1 -> {
                        likePost(post, myContext.getString(R.string.react1), holder)
                    }

                    R.id.react2 -> {
                        likePost(post, myContext.getString(R.string.react2), holder)
                    }

                    R.id.react3 -> {
                        likePost(post, myContext.getString(R.string.react3), holder)
                    }

                    R.id.react4 -> {
                        likePost(post, myContext.getString(R.string.react4), holder)
                    }

                    R.id.react5 -> {
                        likePost(post, myContext.getString(R.string.react5), holder)
                    }

                    R.id.react6 -> {
                        likePost(post, myContext.getString(R.string.react6), holder)
                    }

                    R.id.react7 -> {
                        likePost(post, myContext.getString(R.string.react7), holder)
                    }

                    R.id.react8 -> {
                        likePost(post, myContext.getString(R.string.react8), holder)
                    }

                    R.id.react9 -> {
                        likePost(post, myContext.getString(R.string.react9), holder)
                    }

                    R.id.react10 -> {
                        likePost(post, myContext.getString(R.string.react10), holder)
                    }

                    R.id.react11 -> {
                        likePost(post, myContext.getString(R.string.react11), holder)
                    }

                    R.id.react12 -> {
                        likePost(post, myContext.getString(R.string.react12), holder)
                    }

                    R.id.react13 -> {
                        likePost(post, myContext.getString(R.string.react13), holder)
                    }

                    R.id.react14 -> {
                        likePost(post, myContext.getString(R.string.react14), holder)
                    }

                    /*R.id.react15 -> {
                        likePost(post, myContext.getString(R.string.react15), holder)
                    }*/

                }
                true
            })
            popup.show()
        }
        holder.shareBtn.setOnClickListener {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salvation Lamb")
                var shareMessage = "${post.content} \n\n\n\nLet me recommend you this application\n\n"
                shareMessage = """
                    ${shareMessage + "https://salvationlamb.com/"}                    
                    """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                context.startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                Log.e("exception", e.toString())
            }
        }
        holder.fav.setOnClickListener {
            favPost(Util.userId, post.id, holder)

        }
        holder.followBtn.setOnClickListener {
            follow(Util.userId, post.userId, holder)
        }
        holder.reacts.setOnClickListener {
            val intent = Intent(context, ViewLikesActivity::class.java)
            intent.putExtra("postId", post.id)
            context.startActivity(intent)
        }
        holder.headLinear.setOnClickListener {
            if (page != "profile" && page != "OtherProfile") {
                val intent = Intent(context, ViewProfileActivity::class.java)
                intent.putExtra("userId", post.userId)
                context.startActivity(intent)
            }
        }
        holder.deleteBtn.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setMessage("Delete post")
            builder.setTitle("Alert !")
            builder.setCancelable(false)
            builder.setPositiveButton("Delete") { _: DialogInterface?, _: Int ->
                deletePost(post)
                posts.removeAt(position)
                notifyItemRemoved(position)
            }
            builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()

        }
    }

    private fun likePost(post: Posts, reaction: String, holder: ViewHolder) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val data = JsonObject()
                data.addProperty("userId", Util.userId)
                data.addProperty("postId", post.id)
                data.addProperty("reaction", reaction)
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it", "like", data)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val msg: String =
                                        Gson().fromJson(response.body()!!.get("message"), String::class.java)
                                    val likesCount: String =
                                        Gson().fromJson(response.body()!!.get("likesCount"), String::class.java)
                                    if (msg == "liked") {
                                        myList.put(post.id, reaction)
                                        holder.likeBtn.text = reaction
                                        holder.reacts.text = "$likesCount people reacts"
                                    } else if (msg == "unliked") {
                                        myList.remove(post.id)
                                        holder.likeBtn.text = "React"
                                        holder.reacts.text = "$likesCount people reacts"
                                    }
                                } else {
                                    val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("result", errorMessage)
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeAdapter.likePost", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeAdapter.likePost", e.toString())
        }
    }

    private fun deletePost(post: Posts) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                Log.e("deleted post : postid  ==== ", post.id)
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.deletePost("Bearer $it", post.id)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    Toast.makeText(
                                        context, "Deleted Successfully" + posts.indexOf(post), Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("result", errorMessage)
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeAdapter.deletePost", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeAdapter.deletePost", e.toString())
        }
    }

    private fun follow(userId: String, followerId: String, holder: ViewHolder) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val followData = JsonObject()
                followData.addProperty("userId", userId)
                followData.addProperty("followerId", followerId)
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.postFollow("Bearer $it", followData)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val msg: String =
                                        Gson().fromJson(response.body()!!.get("message"), String::class.java)
                                    Log.e("msg follow", msg)
                                    if (msg == "unfollow") {
                                        holder.followBtn.text = "Follow"
                                        myFollowList.put(followerId, userId)
                                    } else if (msg == "follow") {
                                        holder.followBtn.text = "Unfollow"
                                        myFollowList.remove(followerId)
                                    }
                                    notifyDataSetChanged()
                                } else {
                                    Log.e("failFollow", response.errorBody().toString())
                                    //Toast.makeText(context,"Followed Failed",Toast.LENGTH_LONG).show()
                                    val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("result", errorMessage)
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeAdapter.follow", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeAdapter.follow", e.toString())
        }
    }

    fun favPost(userId: String, postId: String, holder: ViewHolder) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val followData = JsonObject()
                followData.addProperty("userId", userId)
                followData.addProperty("postId", postId)
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.postFav("Bearer $it", followData)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    Log.e("Follow", response.body().toString())
                                    val resp = response.body()
                                    val msg: String = Gson().fromJson(resp!!.get("message"), String::class.java)
                                    Log.e("Status", postId)
                                    Log.e("map bef", myFavList.toString())
                                    if (msg.equals("fav")) {
                                        myFavList.put(postId, userId)
                                        holder.fav.setBackgroundResource(R.drawable.ic_baseline_bookmark_24)
                                        holder.saveTxt.text = "Saved"
                                    } else if (msg.equals("unfav")) {
                                        myFavList.remove(postId)
                                        holder.fav.setBackgroundResource(R.drawable.ic_baseline_bookmark_border_24)
                                        holder.saveTxt.text = "Save"
                                    }
                                    notifyDataSetChanged()
                                } else {
                                    Log.e("fail fav", response.errorBody().toString())
                                    val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("errorMessage", errorMessage)
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeAdapter.favPost", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeAdapter.favPost", e.toString())
        }
    }

    fun getPost(postId: String) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it", postId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val post: Posts = Gson().fromJson(response.body()?.get("result"), Posts::class.java)

                                } else {
                                    Log.e("fail fav", response.errorBody().toString())
                                    val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeAdapter.getPost", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeAdapter.getPost", e.toString())
        }
    }

    fun addItem(post: ArrayList<Posts>) {
        posts.addAll(post)
        notifyItemRangeInserted(posts.size, post.size)
    }

}