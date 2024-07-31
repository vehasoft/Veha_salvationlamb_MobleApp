package com.veha.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.veha.fragments.AdminAudioFragment
import com.veha.fragments.AdminNotificationFragment
import com.veha.fragments.UserNotificationFragment
import com.veha.fragments.WarriorNotificationFragment

class NotificationTabAdapter (c: Context, fm: FragmentManager?, totalTabs: Int) :
    FragmentPagerAdapter(fm!!) {
    var context: Context
    var totalTabs: Int

    init {
        context = c
        this.totalTabs = totalTabs
    }

    override fun getItem(position: Int): Fragment {
        var b: Any? = null
        return when (position) {
            0 -> {
                UserNotificationFragment()
            }

            1 -> {
                AdminNotificationFragment()
            }

            2 -> {
                WarriorNotificationFragment()
            }
            else -> b as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}