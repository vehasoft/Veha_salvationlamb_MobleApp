package com.veha.activity

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.asLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.squareup.picasso.Picasso
import com.veha.util.Commons
import com.veha.util.NotificationType
import com.veha.util.Posts
import com.veha.util.UserPreferences
import com.veha.util.Util
import retrofit2.Call
import retrofit2.Response


class ViewPostActivity : AppCompatActivity() {
    private lateinit var userPreferences: UserPreferences
    lateinit var name: TextView
    lateinit var time: TextView
    lateinit var fullTime: TextView
    lateinit var tags: TextView
    lateinit var title: TextView
    lateinit var content: ExpandableView
    lateinit var reacts: TextView
    lateinit var profilePic: ImageView
    lateinit var postPic: ImageView
    lateinit var postVideo: YouTubePlayerView
    lateinit var audioLayout: LinearLayout
    lateinit var pauseBtn: ImageView
    lateinit var playBtn: ImageView
    lateinit var seekbar: SeekBar
    lateinit var headLinear: LinearLayout
    lateinit var likeBtn: Button
    lateinit var shareBtn: Button
    lateinit var fav: ImageButton
    lateinit var overallLayout: ScrollView
    lateinit var logo: ImageView
    lateinit var postId: String
    lateinit var type: String
    lateinit var contentUrl: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_post)
        userPreferences = UserPreferences(this)
        name = findViewById(R.id.name_post)
        time = findViewById(R.id.post_time)
        fullTime = findViewById(R.id.post_full_time)
        tags = findViewById(R.id.tags)
        title = findViewById(R.id.title)
        content = findViewById(R.id.post_content)
        reacts = findViewById(R.id.no_of_reacts)
        profilePic = findViewById(R.id.profile_pic)
        postPic = findViewById(R.id.post_pic)
        postVideo = findViewById(R.id.post_video)
        audioLayout = findViewById(R.id.audio_layout)
        pauseBtn = findViewById(R.id.pause_btn)
        playBtn = findViewById(R.id.play_btn)
        seekbar = findViewById(R.id.seekBar)
        headLinear = findViewById(R.id.head_linear)
        likeBtn = findViewById(R.id.like_btn)
        shareBtn = findViewById(R.id.share_btn)
        overallLayout = findViewById(R.id.child_post_layout)
        contentUrl = findViewById(R.id.content_url)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        type = intent.extras!!.get("type").toString()
        if (type == NotificationType.POST.value) {
            postId = intent.extras!!.get("postId").toString()
            Log.e("post",postId.toString())
            getPost(postId)
        }else if (type == NotificationType.ANNOUNCEMENT.value) {
            postId = intent.extras!!.get("postId").toString()
            getAnnouncement(postId)
        }
        /*if (!intent.extras!!.get("postId").toString().isNullOrEmpty()) {
            postId = intent.extras!!.get("postId").toString()
            getPost(postId)
        } else if (!intent.extras!!.get("post").toString().isNullOrEmpty()){
            val postString: String = intent.extras!!.get("post").toString()
            Log.e("postString",postString)
            val post = Gson().fromJson(postString, Posts::class.java)
            setPostContent(post)
        }*/
        likeBtn.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@ViewPostActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, likeBtn)
            popup.menuInflater.inflate(R.menu.react_menu, popup.menu)
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.react1 -> {
                        likePost(myContext.getString(R.string.react1))
                    }

                    R.id.react2 -> {
                        likePost(myContext.getString(R.string.react2))
                    }

                    R.id.react3 -> {
                        likePost(myContext.getString(R.string.react3))
                    }

                    R.id.react4 -> {
                        likePost(myContext.getString(R.string.react4))
                    }

                    R.id.react5 -> {
                        likePost(myContext.getString(R.string.react5))
                    }

                    R.id.react6 -> {
                        likePost(myContext.getString(R.string.react6))
                    }

                    R.id.react7 -> {
                        likePost(myContext.getString(R.string.react7))
                    }

                    R.id.react8 -> {
                        likePost(myContext.getString(R.string.react8))
                    }

                    R.id.react9 -> {
                        likePost(myContext.getString(R.string.react9))
                    }

                    R.id.react10 -> {
                        likePost(myContext.getString(R.string.react10))
                    }

                    R.id.react11 -> {
                        likePost(myContext.getString(R.string.react11))
                    }

                    R.id.react12 -> {
                        likePost(myContext.getString(R.string.react12))
                    }

                    R.id.react13 -> {
                        likePost(myContext.getString(R.string.react13))
                    }

                    R.id.react14 -> {
                        likePost(myContext.getString(R.string.react14))
                    }

                    /*R.id.react15 -> {
                        likePost(myContext.getString(R.string.react15))
                    }*/

                }
                true
            })
            popup.show()
        }
        shareBtn.setOnClickListener {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salvation Lamb")
                var shareMessage = "${content.text} \n\n\n\nLet me recommend you this application\n\n"
                shareMessage = """
                    ${shareMessage + "https://salvationlamb.com/"}                    
                    """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                Log.e("exception", e.toString())
            }
        }
    }

    fun setPostContent(post: Posts) {
        if (type == NotificationType.ANNOUNCEMENT.value){
            if (!post.message.isNullOrEmpty()){content.setText(post.message)}
        } else if (type == NotificationType.POST.value){
            if (!post.content.isNullOrEmpty()){content.setText(post.content)}
        }
        content.setOnClickListener { content.expand() }
        if (post.user != null){
            name.text = post.user.name
            if (!post.user.picture.isNullOrEmpty()) {
                Picasso.with(this@ViewPostActivity).load(post.user.picture).into(profilePic)
            } else {
                profilePic.setImageResource(R.drawable.ic_profile)
            }
        }
        if (post.contentURL.isNullOrEmpty()){
            contentUrl.visibility = View.GONE
        } else {
            contentUrl.visibility = View.VISIBLE
            val urlList = post.contentURL.split(",")
            var url = ""
            for (urls in urlList) {
                url += "<a href=\"" + urls + "\">" + urls + "</a><br>"
            }
            contentUrl.movementMethod = LinkMovementMethod.getInstance()
            contentUrl.text = Html.fromHtml(url)
            contentUrl.isClickable = true
        }
        if (!post.tags.isNullOrEmpty()){tags.text = getTags(post.tags)}
        if (!post.title.isNullOrEmpty()){title.text = post.title}
        time.text = Util.getTimeAgo(post.createdAt)
        fullTime.text = post.createdAt
        if (!post.likesCount.isNullOrEmpty()){reacts.text = post.likesCount + "people reacts"}
        if (!post.type.isNullOrEmpty()) {
            when (post.type) {
                "image" -> {
                    audioLayout.visibility = View.GONE
                    postVideo.visibility = View.GONE
                    if (type == NotificationType.POST.value && post.picture != null) {
                        postPic.visibility = View.VISIBLE
                        Picasso.with(this@ViewPostActivity).load(post.picture).fit().centerInside().into(postPic)
                    } else if(type == NotificationType.ANNOUNCEMENT.value && post.url != null){
                        postPic.visibility = View.VISIBLE
                        Picasso.with(this@ViewPostActivity).load(post.url).fit().centerInside().into(postPic)
                    }
                    else {
                        postPic.visibility = View.GONE
                    }
                    postPic.setOnClickListener {
                        if (!post.picture.isNullOrEmpty()) {
                            val intent = Intent(this@ViewPostActivity, ImageDetailActivity::class.java)
                            intent.putExtra("profilePic", post.picture)
                            startActivity(intent)
                        }
                    }
                }

                "audio" -> {
                    postVideo.visibility = View.GONE
                    postPic.visibility = View.GONE
                    val myHandler = Handler()
                    if (!post.url.isNullOrEmpty()) {
                        audioLayout.visibility = View.VISIBLE
                        val updateSongTime: Runnable = object : Runnable {
                            override fun run() {
                                if (Util.player.isPlaying) {
                                    seekbar.progress = Util.player.currentPosition
                                    myHandler.postDelayed(this, 100)
                                } else {
                                    myHandler.removeCallbacks(this)
                                    pauseBtn.visibility = View.GONE
                                    playBtn.visibility = View.VISIBLE

                                }
                            }
                        }
                        playBtn.setOnClickListener {

                            /*if (currentHolder != null && Util.player != null && Util.player.isPlaying) {
                                myHandler.removeCallbacks(updateSongTime)
                                Util.player.pause()
                                currentHolder!!.pauseBtn.visibility = View.GONE
                                currentHolder!!.seekbar.progress = 0
                                currentHolder!!.playBtn.visibility = View.VISIBLE
                                Util.player.stop()
                                Util.player.release()
                                Util.player = null
                            }*/
                            Util.player = MediaPlayer()
                            try {
                                Util.player.setDataSource(post.url)
                                Util.player.prepare()
                                seekbar.max = Util.player.duration
                                seekbar.isClickable = true
                                Util.player.start()
                                seekbar.progress = Util.player.currentPosition
                                myHandler.postDelayed(updateSongTime, 100)
                                playBtn.visibility = View.GONE
                                pauseBtn.visibility = View.VISIBLE
                                seekbar.setOnSeekBarChangeListener(object :
                                    SeekBar.OnSeekBarChangeListener {
                                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                        if (fromUser) {
                                            Util.player.seekTo(progress)
                                        }
                                    }
                                })
                            } catch (e: Exception) {
                                //posts.removeAt(position)
                                //Handler().post { this@HomeAdapter.notifyItemRemoved(position) }
                            }
                            //currentHolder = holder
                        }
                        pauseBtn.setOnClickListener {
                            if (Util.player.isPlaying) {
                                myHandler.removeCallbacks(updateSongTime)
                                Util.player.pause()
                            }
                            pauseBtn.visibility = View.GONE
                            playBtn.visibility = View.VISIBLE
                        }
                    } else {
                        audioLayout.visibility = View.GONE
                    }

                }

                "video" -> {
                    audioLayout.visibility = View.GONE
                    postPic.visibility = View.GONE
                    if (!post.url.isNullOrEmpty()) {
                        postVideo.visibility = View.VISIBLE
                        try {
                            this@ViewPostActivity.lifecycle.addObserver(postVideo)
                        }catch (e: Exception){
                            this@ViewPostActivity.lifecycle.addObserver(postVideo)
                        }
                        val youTubePlayerListener = object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                youTubePlayer.cueVideo(post.url, 0f)
                            }
                        }
                        val iFramePlayerOptions = IFramePlayerOptions.Builder().controls(1).autoplay(0).build()

                        postVideo.enableAutomaticInitialization = false
                        try {

                            postVideo.initialize(youTubePlayerListener, iFramePlayerOptions)
                        } catch (e: Exception) {
                            Log.e("Exception", e.toString());
                        }
                    } else {
                        postVideo.visibility = View.GONE
                    }
                }



            }
        }
    }
    fun getPost(postId: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it", postId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    val post: Posts = Gson().fromJson(
                                        response.body()?.get("result"),
                                        Posts::class.java
                                    )
                                    Log.e("postttttttt",post.toString())
                                    setPostContent(post)
                                } else {
                                    Log.e("code",response.code().toString())
                                    Log.e("err",response.errorBody().toString())
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("ViewPost", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ViewPost", e.toString())
        }
    }
    fun getAnnouncement(postId: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getAnnouncements("Bearer $it", postId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    Log.e("announcement",response.body().toString())
                                    val post: Posts = Gson().fromJson(
                                        response.body()?.get("announcement"),
                                        Posts::class.java
                                    )
                                    Log.e("postttttttt",post.toString())
                                    setPostContent(post)
                                } else {
                                    Log.e("code",response.code().toString())
                                    Log.e("err",response.errorBody().toString())
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("ViewPost", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ViewPost", e.toString())
        }
    }

    private fun likePost(reaction: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val data = JsonObject()
                data.addProperty("userId", Util.userId)
                data.addProperty("postId", postId)
                data.addProperty("reaction", reaction)
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        likeBtn.isEnabled = false
                        val call: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it", "like", data)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val msg: String =
                                        Gson().fromJson(response.body()!!.get("message"), String::class.java)
                                    val likesCount: String =
                                        Gson().fromJson(response.body()!!.get("likesCount"), String::class.java)
                                    if (msg == "liked") {
                                        likeBtn.text = reaction
                                        reacts.text = "$likesCount people reacts"
                                    } else if (msg == "unliked") {
                                        likeBtn.text = "React"
                                        reacts.text = "$likesCount people reacts"
                                    }
                                } else {
                                    Log.e("code",response.code().toString())
                                    Log.e("err",response.errorBody().toString())
                                }
                                call.cancel()
                                likeBtn.isEnabled = true
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                likeBtn.isEnabled = true
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
    override fun onBackPressed() {
        if (Util.player != null) {
            Util.player.stop()
            Util.player.reset()
            Util.player.release()
            Util.player = null
        }
        if (isTaskRoot) {
            val intent =
                Intent(this@ViewPostActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        super.onBackPressed()
    }

    override fun onDestroy() { if (
        Util.player != null) {
        Util.player.stop()
        Util.player.reset()
        Util.player.release()
        Util.player = null
    }
        super.onDestroy()
    }
}