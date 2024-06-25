package com.veha.fragments

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
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.veha.activity.LoginActivity
import com.veha.activity.R
import com.veha.adapter.NotificationListAdapter
import com.veha.util.Commons
import com.veha.util.NotificationList
import com.veha.util.PostUser
import com.veha.util.NotificationType
import com.veha.util.UserPreferences
import com.veha.util.Util
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class UserNotificationFragment : Fragment() {
    lateinit var userPreferences: UserPreferences
    lateinit var list: RecyclerView
    lateinit var nodata: LinearLayout
    lateinit var contexts: Context
    private var page: Int = 0
    lateinit var adapter: NotificationListAdapter
    var updated: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        updated = false
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_user_notification, container, false)
        contexts = container!!.context
        userPreferences = UserPreferences(contexts)
        list = view.findViewById(R.id.notification_user_recycler)
        nodata = view.findViewById(R.id.no_data)
        page = 1
        adapter = NotificationListAdapter(ArrayList(),contexts,this@UserNotificationFragment)
        val layoutManager = LinearLayoutManager(activity)
        list.layoutManager = layoutManager
        list.adapter = adapter
        getNotifications(viewLifecycleOwner)
        return view
    }
    private fun getNotifications(owner: LifecycleOwner, postlist: ArrayList<NotificationList> = ArrayList()){

        try {
            if (Commons().isNetworkAvailable(context)) {
                var count: Int
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getNotifications("Bearer $it", Util.userId,page,10,"user")
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginresp: JsonArray =
                                        Gson().fromJson(resp?.get("notification"), JsonArray::class.java)
                                    count = Integer.parseInt(resp?.get("count").toString())
                                    count /= 10
                                    for (notification in loginresp) {
                                        val pos = Gson().fromJson(notification, NotificationList::class.java)
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
                                            page++
                                            updated = true
                                        }
                                        list.addOnScrollListener(object :
                                            RecyclerView.OnScrollListener() {
                                            override fun onScrollStateChanged(
                                                recyclerView: RecyclerView,
                                                dx: Int
                                            ) {
                                                if (!recyclerView.canScrollVertically(1)) {
                                                    if ((count+2) > page) {
                                                        getNotifications(owner)
                                                        updated = false
                                                    }
                                                }
                                            }
                                        })
                                    }

                                } else if (response.code() == 401) {
                                    Toast.makeText(
                                        contexts,
                                        resources.getString(R.string.Deleted_account),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val intent = Intent(contexts, LoginActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    Log.e("code",response.code().toString())
                                    Log.e("err",response.errorBody().toString())
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("HomeFragment.getMyDetails", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            contexts,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        )
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
}