package com.example.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.FollowAdapter
import com.example.adapter.UsersAdapter
import com.example.fbproject.R
import com.example.models.PostUser

class SearchProfileFragment(private val contexts: Context,private val profileList: ArrayList<PostUser>) : Fragment() {

    lateinit var lists: RecyclerView
    lateinit var nodata: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_search_profile, container, false)

        lists = view.findViewById(R.id.search_profile_list)
        nodata = view.findViewById(R.id.no_data)


        if (profileList.size <= 0){
            lists.visibility = View.GONE
            nodata.visibility = View.VISIBLE
        } else{
            lists.visibility = View.VISIBLE
            nodata.visibility = View.GONE
            lists.layoutManager = LinearLayoutManager(context)
            lists.adapter = UsersAdapter(profileList, contexts,this)
        }



        return view
    }

}