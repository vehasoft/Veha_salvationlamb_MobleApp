package com.veha.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.adapter.HomeAdapter
import com.veha.activity.*
import com.veha.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import com.veha.activity.R
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var myLikes: String = ""
    private lateinit var contexts: Context
    private lateinit var userId: String
    private lateinit var who: String
    private var picture: String = ""
    private var myLikesMap: HashMap<String, String> = HashMap()
    private lateinit var likeslist: ArrayList<PostLikes>
    private lateinit var followersList: ArrayList<AllFollowerList>
    private lateinit var followingList: ArrayList<AllFollowerList>

    private lateinit var adapter: HomeAdapter
    var followerCount = 0
    var followingCount = 0
    var postCount = 0
    var updated: Boolean = false

    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog
    private lateinit var list: RecyclerView
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

    private var page: Int = 0

    companion object {
        fun getInstance(userId: String, who: String): ProfileFragment {
            val profileFrag = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("userId", userId)
            bundle.putString("who", who)
            profileFrag.arguments = bundle
            return profileFrag
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            requireFragmentManager().beginTransaction().detach(this).attach(this).commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        updated = false
        contexts = container!!.context
        userId = arguments?.get("userId").toString()
        who = arguments?.get("who").toString()

        userPreferences = UserPreferences(contexts)
        dialog = SpotsDialog.Builder().setContext(contexts).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        dialog.dismiss()
        getallLikes(viewLifecycleOwner)
        val view: View = inflater.inflate(R.layout.fragment_profile, container, false)

        profilePic = view.findViewById(R.id.profile_pic_main)
        profileName = view.findViewById(R.id.profile_name)
        profileFollowers = view.findViewById(R.id.followers)
        profileFollowing = view.findViewById(R.id.following)
        profilePosts = view.findViewById(R.id.posts)
        editProfile = view.findViewById(R.id.edit_profile)
        followerLinear = view.findViewById(R.id.followers_linear)
        followingLinear = view.findViewById(R.id.following_linear)
        list = view.findViewById(R.id.my_post_list)
        nodata = view.findViewById(R.id.no_data)

        if (who != "me") {
            editProfile.visibility = View.GONE

        } else {
            editProfile.visibility = View.VISIBLE
        }
        profilePic.setOnClickListener {
            if (!picture.isNullOrEmpty()) {
                val intent = Intent(context, ImageDetailActivity::class.java)
                intent.putExtra("profilePic", picture)
                startActivity(intent)
            }
        }

        editProfile.setOnClickListener {
            val intent = Intent(context, AboutActivity::class.java)
            startActivity(intent)
        }
        followerLinear.setOnClickListener {
            val intent = Intent(context, FollowerActivity::class.java)
            intent.putExtra("page", "follower")
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
        followingLinear.setOnClickListener {
            val intent = Intent(context, FollowerActivity::class.java)
            intent.putExtra("page", "following")
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        list.layoutManager = LinearLayoutManager(activity)
        val whoPage = if (who == "me") "profile" else "OtherProfile"
        adapter = HomeAdapter(ArrayList(), contexts, whoPage, HashMap(), HashMap(), myLikesMap, this@ProfileFragment)
        list.adapter = adapter
        return view
    }

    private fun getallPosts(context: Context, owner: LifecycleOwner, postlist: ArrayList<Posts> = ArrayList()) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                var count: Int
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getMyPosts("Bearer $it", userId, page, 10)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                                    count = Integer.parseInt(resp?.get("count").toString())
                                    profilePosts.text = count.toString()
                                    count /= 10
                                    for (post in loginresp) {
                                        val pos = Gson().fromJson(post, Posts::class.java)
                                        postlist.add(pos)
                                    }
                                    if (postlist.size <= 0) {
                                        list.visibility = View.GONE
                                        nodata.visibility = View.VISIBLE
                                    } else {
                                        list.visibility = View.VISIBLE
                                        nodata.visibility = View.GONE
                                        if (!updated) {
                                            adapter.addItem(postlist)
                                            updated = true
                                        }
                                        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                            override fun onScrollStateChanged(recyclerView: RecyclerView, dx: Int) {
                                                if (!recyclerView.canScrollVertically(1)) {
                                                    if (count > page) {
                                                        page++
                                                        getallPosts(context, owner)
                                                        updated = false
                                                    }
                                                }
                                            }
                                        })
                                    }
                                }
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                getallFollowers(owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("ProfileFragment.getAllPosts", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(contexts, "Somthing Went Wrong \nLogin again to continue", Toast.LENGTH_LONG)
                            .show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(contexts, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment.getAllPosts", e.toString())
        }
    }

    fun getallLikes(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getUserLikes("Bearer $it", userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                                    likeslist = ArrayList()
                                    for (likes in loginresp) {
                                        val pos = Gson().fromJson(likes, PostLikes::class.java)
                                        likeslist.add(pos)
                                        myLikes += pos.postId + ","
                                        myLikesMap.put(pos.postId, pos.reaction)
                                    }
                                }
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                getallPosts(contexts, owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("ProfileFragment.getAllLikes", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(contexts, "Somthing Went Wrong \nLogin again to continue", Toast.LENGTH_LONG)
                            .show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(contexts, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment.getAllLikes", e.toString())
        }
    }

    private fun getallFollowers(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getFollowers("Bearer $it", userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                                    followersList = ArrayList()
                                    for (likes in loginresp) {
                                        val pos = Gson().fromJson(likes, AllFollowerList::class.java)
                                        followersList.add(pos)
                                    }
                                    followerCount = followersList.size
                                    profileFollowers.text = followerCount.toString()
                                }
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                getallFollowing(owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("ProfileFragment.getAllFollowers", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(contexts, "Somthing Went Wrong \nLogin again to continue", Toast.LENGTH_LONG)
                            .show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(contexts, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment.getAllFollowers", e.toString())
        }
    }

    private fun getallFollowing(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getFollowing("Bearer $it", userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                                    followingList = ArrayList()
                                    for (likes in loginresp) {
                                        val pos = Gson().fromJson(likes, AllFollowerList::class.java)
                                        followingList.add(pos)
                                    }
                                    followingCount = followingList.size
                                    profileFollowing.text = followingCount.toString()
                                } else {
                                    Log.e("following", "fails - " + response.code())
                                }
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                getmyDetails(contexts, owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("ProfileFragment.getAllFollowing", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(contexts, "Somthing Went Wrong \nLogin again to continue", Toast.LENGTH_LONG)
                            .show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(contexts, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment.getAllFollowing", e.toString())
        }
    }

    private fun getmyDetails(context: Context, owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                                    if (!loginresp.picture.isNullOrEmpty()) {
                                        picture = loginresp.picture
                                        Picasso.with(context).load(loginresp.picture).into(profilePic)
                                    } else {
                                        picture = ""
                                    }
                                    val role = if (loginresp.role == "admin") {
                                        " - " + "Admin"
                                    } else if (loginresp.isWarrior.toBoolean()) {
                                        " - " + Util.WARRIOR
                                    } else {
                                        ""
                                    }
                                    profileName.text = loginresp.name + role
                                }
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("ProfileFragment.getMyDetails", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(contexts, "Somthing Went Wrong \nLogin again to continue", Toast.LENGTH_LONG)
                            .show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(contexts, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment.getMyDetails", e.toString())
        }
    }
    override fun onPause() {
        super.onPause()
        dialog.dismiss()
    }
    override fun onResume() {
        super.onResume()
        dialog.dismiss()
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}