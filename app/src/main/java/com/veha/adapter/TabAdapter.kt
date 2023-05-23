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
            4 -> {
                ProfileFragment.getInstance(Util.userId,"me")
            }
            1 -> {
                FilesFragment()
            }
            2 -> {
                AdminVideoFragment()
            }
            3 -> {
                AdminAudioFragment()
            }
            else -> b as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}