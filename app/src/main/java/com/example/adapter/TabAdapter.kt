package com.example.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragments.HomeFragment
import com.example.fragments.ProfileFragment
import com.example.util.Util


internal class TabAdapter(c: Context, fm: FragmentManager?, totalTabs: Int, userType: String) : FragmentPagerAdapter(fm!!) {
    var context: Context
    var totalTabs: Int

    init {
        context = c
        this.totalTabs = totalTabs
    }

    override fun getItem(position: Int): Fragment {
        var b : Any ? =null
        return when (position) {
            0 -> {
                HomeFragment(context,"user")
            }
            1 -> {
                ProfileFragment(Util.userId,context,"me")
            }
            else -> b as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}