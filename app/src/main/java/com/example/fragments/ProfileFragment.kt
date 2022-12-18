package com.example.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.EditProfileActivity
import com.example.adapter.HomeAdapter
import com.example.fbproject.FollowerActivity
import com.example.fbproject.R
import com.example.models.Posts
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Response

class ProfileFragment(private val  userId: String,private val contexts: Context,private val who: String) : Fragment() {

    private var myLikes: String = ""

    private var myLikesMap: HashMap<String,String> = HashMap()
    private lateinit var likeslist: ArrayList<PostLikes>
    private lateinit var followersList: ArrayList<AllFollowerList>
    private lateinit var followingList: ArrayList<AllFollowerList>
    var followerCount = 0
    var followingCount = 0
    var postCount = 0

    private lateinit var userPreferences: UserPreferences
    private lateinit var list : RecyclerView
    private lateinit var profilePic: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileAbout: TextView
    private lateinit var profileFollowers: TextView
    private lateinit var profileFollowing: TextView
    private lateinit var profilePosts: TextView
    private lateinit var editProfile: Button
    private lateinit var followerLinear: LinearLayout
    private lateinit var followingLinear: LinearLayout
    private lateinit var nodata: LinearLayout

    val postlist: ArrayList<Posts> = ArrayList()
    private var page: Int = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        userPreferences = UserPreferences(contexts)
        getallLikes()
        val view : View =  inflater.inflate(R.layout.fragment_profile, container, false)

        profilePic = view.findViewById(R.id.profile_pic_main)
        profileName = view.findViewById(R.id.profile_name)
        profileAbout = view.findViewById(R.id.profile_about)
        profileFollowers = view.findViewById(R.id.followers)
        profileFollowing = view.findViewById(R.id.following)
        profilePosts = view.findViewById(R.id.posts)
        editProfile = view.findViewById(R.id.edit_profile)
        followerLinear = view.findViewById(R.id.followers_linear)
        followingLinear = view.findViewById(R.id.following_linear)
        list = view.findViewById(R.id.my_post_list)
        nodata = view.findViewById(R.id.no_data)


/*
        var post1 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath1","https://www.gstatic.com/webp/gallery/1.jpg","",""))
        var post2 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath2","https://www.gstatic.com/webp/gallery/2.jpg","",""))
        var post3 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath3","https://www.gstatic.com/webp/gallery/3.jpg","",""))
        var post4 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath4","https://www.gstatic.com/webp/gallery/4.jpg","",""))
        var post5 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath5","https://www.gstatic.com/webp/gallery/5.jpg","",""))

        var posts : ArrayList<Posts> = ArrayList()
        posts.add(post1)
        posts.add(post2)
        posts.add(post3)
        posts.add(post4)
        posts.add(post5)


        list = view.findViewById(R.id.my_post_list)
        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = HomeAdapter(posts, contexts,"profile")*/


        if (who != "me"){
            editProfile.visibility = View.GONE

        }
        else {
            editProfile.visibility = View.VISIBLE
        }

        editProfile.setOnClickListener {
            val intent = Intent(context, EditProfileActivity::class.java)
            startActivity(intent)
        }
        followerLinear.setOnClickListener {
            val intent = Intent(context, FollowerActivity::class.java)
            intent.putExtra("page","follower")
            intent.putExtra("userId",userId)
            startActivity(intent)
        }
        followingLinear.setOnClickListener {
            val intent = Intent(context, FollowerActivity::class.java)
            intent.putExtra("page","following")
            intent.putExtra("userId",userId)
            startActivity(intent)
        }

        list.layoutManager = LinearLayoutManager(activity)
        return  view
    }

    private fun getallPosts(context: Context){
        var count: Int
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getMyPosts("Bearer $it",userId,page,10)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            count = Integer.parseInt(resp?.get("count").toString())
                            Log.e("counttttttttt",count.toString())
                            profilePosts.text = count.toString()
                            count /= 10
                            for (post in loginresp){
                                val pos = Gson().fromJson(post,Posts::class.java)
                                postlist.add(pos)
                            }
                            if (postlist.size <= 0){
                                list.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                list.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                val whoPage = if(who == "me") "profile" else "OtherProfile"
                                list.adapter = HomeAdapter(postlist, context,whoPage,HashMap(),HashMap(),myLikesMap,this@ProfileFragment)
                                list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                    override fun onScrollStateChanged(recyclerView: RecyclerView, dx: Int) {
                                        if (!recyclerView.canScrollVertically(1)) {
                                            if(count > page){
                                                page++
                                                getallPosts(context)
                                            }
                                            //Toast.makeText(context, "Last", Toast.LENGTH_LONG).show();

                                        }
                                    }
                                })
                            }
                        }

                        getallFollowers()
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            }
        }

    }
    fun getallLikes(){
        var count: Int
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getUserLikes("Bearer $it",userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            likeslist = ArrayList()
                            for (likes in loginresp){
                                val pos = Gson().fromJson(likes, PostLikes::class.java)
                                likeslist.add(pos)
                                myLikes += pos.postId + ","
                                myLikesMap.put(pos.postId,pos.reaction)
                            }
                        }

                        getallPosts(contexts)
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            }
        }

    }
    private fun getallFollowers() {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFollowers("Bearer $it", userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            followersList = ArrayList()
                            for (likes in loginresp) {
                                val pos = Gson().fromJson(likes, AllFollowerList::class.java)
                                followersList.add(pos)
                                Log.e("MYFOLLOWMAP", pos.followerId + "-----" + pos.id)
                            }
                            followerCount = followersList.size
                            profileFollowers.text = followerCount.toString()
                        }

                        getallFollowing()
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            }
        }
    }
    private fun getallFollowing() {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFollowing("Bearer $it", userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            followingList = ArrayList()
                            for (likes in loginresp) {
                                val pos = Gson().fromJson(likes, AllFollowerList::class.java)
                                followingList.add(pos)
                            }
                            followingCount = followingList.size
                            profileFollowing.text = followingCount.toString()
                        }

                        getmyDetails(contexts)
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            }
        }
    }
    private fun getmyDetails(context: Context) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Log.e("userrrrr",resp.toString())
                            val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                            if (loginresp.picture.isNullOrEmpty()){
                                Picasso.with(context).load("https://www.gravatar.com/avatar/205e460b479e2e5b48aec07710c08d50").into(profilePic)
                            }else {
                                Picasso.with(context).load(loginresp.picture).into(profilePic)
                            }
                            profileName.text = loginresp.name
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            }
        }
    }



}