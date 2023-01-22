package com.example.fragments

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
import com.example.adapter.HomeAdapter
import com.example.fbproject.AddPostActivity
import com.example.fbproject.LoginActivity
import com.example.fbproject.R
import com.example.util.FavPost
import com.example.util.PostUser
import com.example.util.Posts
import com.example.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response


class HomeFragment: Fragment() {
    lateinit var userPreferences: UserPreferences
    lateinit var list : RecyclerView
    lateinit var nodata: LinearLayout
    lateinit var userType: String
    lateinit var type: String
    lateinit var contexts: Context
    lateinit var adapter: HomeAdapter
    lateinit var addPost: FloatingActionButton

    private lateinit var likeslist: ArrayList<PostLikes>
    private lateinit var myFollowMap: HashMap<String,String>
    private lateinit var myFavMap: HashMap<String,String>
    private var page: Int = 0
    private var showCreatePost: Boolean = false

    private var myLikes: String = ""
    private lateinit var myLikesMap: HashMap<String,String>

    companion object {
        fun getInstance(type: String): HomeFragment{
            val homeFrag = HomeFragment()
            val bundle = Bundle()
            bundle.putString("type",type)
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
        contexts = container!!.context
        type = arguments?.get("type").toString()
        myFollowMap = HashMap()
        myFavMap = HashMap()
        myLikesMap = HashMap()
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        userPreferences = UserPreferences(contexts)
        list = view.findViewById(R.id.list)
        nodata = view.findViewById(R.id.no_data)
        addPost = view.findViewById(R.id.add_post)
        getMyDetails(viewLifecycleOwner)

        addPost.setOnClickListener {
            val intent = Intent(contexts, AddPostActivity::class.java)
            startActivity(intent)
        }
        getallLikes(viewLifecycleOwner)
        adapter = HomeAdapter(ArrayList(), contexts,"home",myLikesMap,myFollowMap,myFavMap,this@HomeFragment)
        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = adapter
        return  view
    }

    fun getfavPosts(context: Context,owner: LifecycleOwner){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getMyFav("Bearer $it",Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            val postlist: ArrayList<Posts> = ArrayList()

                            for (post in loginresp){
                                val pos = Gson().fromJson(post,FavPost::class.java)
                                postlist.add(pos.posts)
                            }
                            if (postlist.size <= 0){
                                list.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                list.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                list.adapter = HomeAdapter(postlist, context,"home",myLikesMap,myFollowMap,myFavMap,this@HomeFragment)
                            }
                        }
                        else{
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("result", errorMessage)
                            list.visibility = View.GONE
                            nodata.visibility = View.VISIBLE
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }

    }
    fun getallPosts(context: Context,owner: LifecycleOwner,postlist: ArrayList<Posts> = ArrayList()){
        var count: Int
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            //Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it",page,10)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            count = Integer.parseInt(resp?.get("count").toString())
                            count /= 10
                            for (post in loginresp){
                                val pos = Gson().fromJson(post,Posts::class.java)
                                postlist.add(pos)

                            }
                            list.findViewHolderForLayoutPosition(2)
                            if (postlist.size <= 0){
                                list.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                list.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                adapter.addItem(postlist)

                                list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                    override fun onScrollStateChanged(recyclerView: RecyclerView, dx: Int) {
                                        if (!recyclerView.canScrollVertically(1)) {
                                            list.isNestedScrollingEnabled = true
                                            if(count > page){
                                                page++
                                                getallPosts(context,owner)
                                            }
                                        }
                                    }
                                })
                            }
                        }
                        else{
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("result", errorMessage)
                            list.visibility = View.GONE
                            nodata.visibility = View.VISIBLE
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }

    }
    fun getallLikes(owner: LifecycleOwner){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getUserLikes("Bearer $it",Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            likeslist = ArrayList()
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)

                            for (likes in loginresp){
                                val pos = Gson().fromJson(likes,PostLikes::class.java)
                                likeslist.add(pos)
                                myLikes += pos.postId + " , "
                                myLikesMap.put(pos.postId,pos.reaction)
                            }
                        }
                        getallFav(owner)
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun getallFollowers(owner: LifecycleOwner){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFollowing("Bearer $it",Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)

                            for (likes in loginresp){
                                val pos = Gson().fromJson(likes, PostUser::class.java)
                                myFollowMap.put(pos.id,Util.userId)
                            }
                        }
                        if (type == "fav") getfavPosts(contexts,owner) else getallPosts(contexts,owner)
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun getallFav(owner: LifecycleOwner){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFav("Bearer $it",Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            for (likes in loginresp){
                                val pos = Gson().fromJson(likes, AllFavList::class.java)
                                myFavMap.put(pos.postId,pos.userId)
                            }
                        }
                        getallFollowers(owner)
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun getMyDetails(owner: LifecycleOwner) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(owner) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                            Util.user = loginresp
                            val isWarrior: Boolean = loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"
                            userType = if(isWarrior) Util.WARRIOR else Util.USER
                            showCreatePost = (userType == Util.WARRIOR) && (type != "fav")
                            if(showCreatePost) {
                                addPost.visibility = View.VISIBLE
                            } else{
                                addPost.visibility = View.GONE
                            }
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
}