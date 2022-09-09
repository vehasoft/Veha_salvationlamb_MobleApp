package com.example.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.fragments.HomeFragment
import com.example.fragments.ProfileFragment


internal class TabAdapter(c: Context, fm: FragmentManager?, totalTabs: Int) : FragmentPagerAdapter(fm!!) {
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
                HomeFragment()
            }
            1 -> {
                ProfileFragment()
            }
            else -> b as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}