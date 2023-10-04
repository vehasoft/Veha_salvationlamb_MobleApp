package com.veha.fragments

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.adapter.HomeAdapter
import com.veha.activity.R
import com.veha.util.Posts
import com.veha.util.UserPreferences

class SearchPostFragment : Fragment() {
    lateinit var userPreferences: UserPreferences
    private lateinit var contexts: Context
    lateinit var list : RecyclerView
    lateinit var nodata: LinearLayout
    lateinit var postList: ArrayList<Posts>
    companion object {
        fun getFragment(postList: ArrayList<Posts>): SearchPostFragment {
            val profileFrag = SearchPostFragment()
            val bundle = Bundle()
            bundle.putParcelableArrayList("postList", postList as (ArrayList<Parcelable>))
            profileFrag.arguments = bundle
            return profileFrag
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
        postList = arguments?.get("postList") as ArrayList<Posts>
        userPreferences = UserPreferences(contexts)
        val view =  inflater.inflate(R.layout.fragment_search_post, container, false)
        list = view.findViewById(R.id.list)
        nodata = view.findViewById(R.id.no_data)

        if (postList.size <= 0){
            list.visibility = View.GONE
            nodata.visibility = View.VISIBLE
        } else {
            list.visibility = View.VISIBLE
            nodata.visibility = View.GONE
            list.layoutManager = LinearLayoutManager(contexts)
            list.adapter = HomeAdapter(postList, contexts,"searchProfile",HashMap(),HashMap(),HashMap(),this@SearchPostFragment)
        }

        return view
    }

}