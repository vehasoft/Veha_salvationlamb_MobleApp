package com.veha.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.adapter.HomeAdapter
import com.veha.activity.LoginActivity
import com.veha.activity.R
import com.veha.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response


class AdminVideoFragment : Fragment() {

    lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog
    lateinit var list: RecyclerView
    lateinit var nodata: LinearLayout
    lateinit var contexts: Context
    lateinit var adapter: HomeAdapter
    var updated: Boolean = false

    private lateinit var likeslist: ArrayList<PostLikes>
    private lateinit var myFollowMap: HashMap<String, String>
    private lateinit var myFavMap: HashMap<String, String>
    private var page: Int = 1

    private lateinit var myLikesMap: HashMap<String, String>
    private var myLikes: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        updated = false
        myFollowMap = HashMap()
        myFavMap = HashMap()
        myLikesMap = HashMap()
        val view = inflater.inflate(R.layout.fragment_admin_video, container, false)
        contexts = container!!.context
        userPreferences = UserPreferences(contexts)
        dialog = SpotsDialog.Builder().setContext(contexts).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        dialog.dismiss()
        list = view.findViewById(R.id.list)
        nodata = view.findViewById(R.id.no_data)
        getMyDetails(viewLifecycleOwner)
        getallLikes(viewLifecycleOwner)
        page = 1
        adapter = HomeAdapter(ArrayList(), contexts, "home", myLikesMap, myFollowMap, myFavMap, this@AdminVideoFragment)
        val layoutManager = LinearLayoutManager(activity)
        list.layoutManager = layoutManager
        list.adapter = adapter
        return view
    }

    fun getallPosts(context: Context, owner: LifecycleOwner, postlist: ArrayList<Posts> = ArrayList()) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                var count: Int
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getVideoPost("Bearer $it", page, 10)
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
                                    if (postlist.size <= 0 && page == 1) {
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
                                    dialog.hide()
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("AdminVideoFragment.getAllPosts", "fail")
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
            Log.e("AdminVideoFragment.getAllPosts", e.toString())
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
                                    dialog.hide()
                                }
                                getallFav(owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("AdminVideoFragment.getAllLikes", "fail")
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
            Log.e("AdminVideoFragment.getAllLikes", e.toString())
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
                                    dialog.hide()
                                }
                                getallPosts(contexts, owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("AdminVideoFragment.getAllFollowers", "fail")
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
            Log.e("AdminVideoFragment.getAllFollowers", e.toString())
        }
    }

    fun getallFav(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
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
                                    dialog.hide()
                                }
                                getallFollowers(owner)
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("AdminVideoFragment.getAllFav", "fail")
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
            Log.e("AdminVideoFragment.getAllFav", e.toString())
        }
    }

    private fun getMyDetails(owner: LifecycleOwner) {
        try {
            if (Commons().isNetworkAvailable(context)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
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
                                }
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.hide()
                                }
                                Log.e("AdminVideoFragment.getMyDetails", "fail")
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
            Log.e("AdminVideoFragment.getMyDetails", e.toString())
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

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (!isVisibleToUser) {
            fragmentManager?.beginTransaction()?.detach(this@AdminVideoFragment)?.attach(this@AdminVideoFragment)
                ?.commit()
        }
    }
}