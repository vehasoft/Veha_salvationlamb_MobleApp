package com.veha.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.veha.fragments.*
import com.veha.util.Util


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
                HomeFragment.getInstance("user")
            }
            1 -> {
                FilesFragment()
            }
            2 -> {
                FilesFragment()
            }
            3 -> {
                AdminVideoFragment()
            }
            4 -> {
                AdminAudioFragment()
            }
            5 -> {
                ProfileFragment.getInstance(Util.userId,"me")
            }
            else -> b as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}