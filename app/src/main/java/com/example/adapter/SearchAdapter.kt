package com.example.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragments.SearchPostFragment
import com.example.fragments.SearchProfileFragment
import com.example.util.PostUser
import com.example.util.Posts


internal class SearchAdapter(val context: Context,
                             fm: FragmentManager?,
                             private var totalTabs: Int,
                             val profilelist: ArrayList<PostUser>,
                             val postlist: ArrayList<Posts>)
    : FragmentPagerAdapter(fm!!) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            1 -> {
                SearchProfileFragment(context,profilelist)
            }
            0 -> {
                SearchPostFragment(context,postlist)
            }
            else -> null as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}