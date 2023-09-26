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
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.veha.adapter.HomeAdapter
import com.veha.activity.AddPostActivity
import com.veha.activity.LoginActivity
import com.veha.activity.R
import com.veha.util.FavPost
import com.veha.util.PostUser
import com.veha.util.Posts
import com.veha.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response


class HomeFragment : Fragment() {
    lateinit var userPreferences: UserPreferences

    //lateinit var dialog: ProgressDialog
    lateinit var dialog: AlertDialog
    lateinit var list: RecyclerView
    lateinit var nodata: LinearLayout
    lateinit var userType: String
    lateinit var type: String
    lateinit var contexts: Context
    lateinit var adapter: HomeAdapter
    lateinit var refresh: SwipeRefreshLayout
    var updated: Boolean = false

    private lateinit var likeslist: ArrayList<PostLikes>
    private lateinit var myFollowMap: HashMap<String, String>
    private lateinit var myFavMap: HashMap<String, String>
    private var page: Int = 0
    private var showCreatePost: Boolean = false
    lateinit var addPost: FloatingActionButton

    private var myLikes: String = ""
    private lateinit var myLikesMap: HashMap<String, String>

    companion object {
        fun getInstance(type: String): HomeFragment {
            val homeFrag = HomeFragment()
            val bundle = Bundle()
            bundle.putString("type", type)
            homeFrag.arguments = bundle
            return homeFrag
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        updated = false
        contexts = container!!.context
        type = arguments?.get("type").toString()
        myFollowMap = HashMap()
        myFavMap = HashMap()
        myLikesMap = HashMap()
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        userPreferences = UserPreferences(contexts)
        dialog = SpotsDialog.Builder().setContext(contexts).build()
        //dialog = ProgressDialog(contexts)
        dialog.setMessage("Please Wait")
        //dialog.setProgressDrawable(resources.getDrawable(R.drawable.ic_sl_logo_01_svg))
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        dialog.dismiss()
        list = view.findViewById(R.id.list)
        nodata = view.findViewById(R.id.no_data)
        refresh = view.findViewById(R.id.refresh)
        addPost = view.findViewById(R.id.add_post)
        if (Util.isWarrior) {
            addPost.visibility = View.VISIBLE
        } else {
            addPost.visibility = View.GONE
        }
        addPost.setOnClickListener {
            val intent = Intent(contexts, AddPostActivity::class.java)
            contexts.startActivity(intent)
        }
        getMyDetails(viewLifecycleOwner)
        getallLikes(viewLifecycleOwner)
        page = 1
        adapter = HomeAdapter(ArrayList(), contexts, "home", myLikesMap, myFollowMap, myFavMap, this@HomeFragment)
        val layoutManager = LinearLayoutManager(activity)
        list.layoutManager = layoutManager
        list.adapter = adapter

        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, dx: Int) {
                if (!recyclerView.canScrollVertically(-1)) {
                    refresh.isEnabled = true
                    updated=false
                    refresh.setOnRefreshListener {
                        refresh.isRefreshing = false
                        requireFragmentManager().beginTransaction().detach(this@HomeFragment).attach(this@HomeFragment)
                            .commit()
                    }
                } else {
                    refresh.isEnabled = false
                }
            }
        })

        return view
    }

    fun getfavPosts(context: Context, owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.getMyFav("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                                    val postlist: ArrayList<Posts> = ArrayList()

                                    for (post in loginresp) {
                                        val pos = Gson().fromJson(post, FavPost::class.java)
                                        postlist.add(pos.posts)
                                    }
                                    if (postlist.size <= 0) {
                                        list.visibility = View.GONE
                                        nodata.visibility = View.VISIBLE
                                    } else {
                                        list.visibility = View.VISIBLE
                                        nodata.visibility = View.GONE
                                        list.adapter = HomeAdapter(
                                            postlist,
                                            context,
                                            "home",
                                            myLikesMap,
                                            myFollowMap,
                                            myFavMap,
                                            this@HomeFragment
                                        )
                                    }
                                } else {
                                    /*val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("result", errorMessage)*/
                                    list.visibility = View.GONE
                                    nodata.visibility = View.VISIBLE
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeFragment.getFavPosts", "fail")
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
            Log.e("HomeFragment.getFavPosts", e.toString())
        }
    }

    fun getallPosts(context: Context, owner: LifecycleOwner, postlist: ArrayList<Posts> = ArrayList()) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                var count: Int
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it", page, 10)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                                    count = Integer.parseInt(resp?.get("count").toString())
                                    count /= 10
                                    for (post in loginresp) {
                                        val pos = Gson().fromJson(post, Posts::class.java)
                                        postlist.add(pos)
                                    }
                                    if (postlist.size <= 0 && page == 0) {
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
                                } else {
                                    /*val resp = response.errorBody()
                                    val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("result", errorMessage)*/
                                    list.visibility = View.GONE
                                    nodata.visibility = View.VISIBLE
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeFragment.getAllPosts", "fail")
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
            Log.e("HomeFragment.getAllPosts", e.toString())
        }
    }

    fun getallLikes(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.getUserLikes("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    likeslist = ArrayList()
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)

                                    for (likes in loginresp) {
                                        val pos = Gson().fromJson(likes, PostLikes::class.java)
                                        likeslist.add(pos)
                                        myLikes += pos.postId + " , "
                                        myLikesMap.put(pos.postId, pos.reaction)
                                    }
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                getallFav(owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeFragment.getAllLikes", "fail")
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
            Log.e("HomeFragment.getAllLikes", e.toString())
        }
    }

    private fun getallFollowers(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.getFollowing("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                                    for (likes in loginresp) {
                                        val pos = Gson().fromJson(likes, PostUser::class.java)
                                        myFollowMap.put(pos.id, Util.userId)
                                    }
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                if (type == "fav") getfavPosts(contexts, owner) else getallPosts(contexts, owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeFragment.getAllFollowers", "fail")
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
            Log.e("HomeFragment.getAllFollowers", e.toString())
        }
    }

    fun getallFav(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.getFav("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                                    for (likes in loginresp) {
                                        val pos = Gson().fromJson(likes, AllFavList::class.java)
                                        myFavMap.put(pos.postId, pos.userId)
                                    }
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                getallFollowers(owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeFragment.getAllFav", "fail")
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
            Log.e("HomeFragment.getAllFav", e.toString())
        }
    }

    private fun getMyDetails(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        if (!dialog.isShowing) {
                            dialog.show()
                        }
                        val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                                    Util.user = loginresp
                                    if (loginresp.blocked.toBoolean()){
                                        Toast.makeText(contexts,resources.getString(R.string.Blocked_account),Toast.LENGTH_LONG).show()
                                        val intent = Intent(contexts, LoginActivity::class.java)
                                        startActivity(intent)
                                    }
                                    val isWarrior: Boolean =
                                        loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"
                                    userType = if (isWarrior) Util.WARRIOR else Util.USER
                                    showCreatePost = (userType == Util.WARRIOR) && (type != "fav")
                                } else if (response.code() == 401) {
                                    Toast.makeText(contexts,resources.getString(R.string.Deleted_account),Toast.LENGTH_LONG).show()
                                    val intent = Intent(contexts, LoginActivity::class.java)
                                    startActivity(intent)
                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("HomeFragment.getMyDetails", "fail")
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
            Log.e("HomeFragment.getMyDetails", e.toString())
        }
    }

    override fun onPause() {
        super.onPause()
            dialog.dismiss()

        if (Util.player != null) {
            Util.player.stop()
            Util.player.reset()
            Util.player.release()
            Util.player = null
        }
    }
    override fun onDestroy() {
        super.onDestroy()
            dialog.dismiss()
        if (Util.player != null) {
            Util.player.stop()
            Util.player.reset()
            Util.player.release()
            Util.player = null
        }
    }
    override fun onResume() {
        super.onResume()
            dialog.dismiss()
        
    }
}