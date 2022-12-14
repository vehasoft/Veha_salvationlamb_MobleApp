package com.example.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragments.SearchPostFragment
import com.example.fragments.SearchProfileFragment
import com.example.models.PostUser
import com.example.models.Posts


internal class SearchAdapter(val context: Context,
                             fm: FragmentManager?,
                             private var totalTabs: Int,
                             val profilelist: ArrayList<PostUser>,
                             val postlist: ArrayList<Posts>)
    : FragmentPagerAdapter(fm!!) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                SearchProfileFragment(context,profilelist)
            }
            1 -> {
                SearchPostFragment(context,postlist)
            }
            else -> null as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}