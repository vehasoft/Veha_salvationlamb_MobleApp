package com.veha.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.veha.fragments.SearchPostFragment
import com.veha.fragments.SearchProfileFragment
import com.veha.util.PostUser
import com.veha.util.Posts


internal class SearchAdapter(val context: Context,
                             fm: FragmentManager?,
                             private var totalTabs: Int,
                             val profilelist: ArrayList<PostUser>,
                             val postlist: ArrayList<Posts>)
    : FragmentPagerAdapter(fm!!) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            1 -> {
                SearchProfileFragment.getFragment(profilelist)
            }
            0 -> {
                SearchPostFragment.getFragment(postlist)
            }
            else -> null as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}