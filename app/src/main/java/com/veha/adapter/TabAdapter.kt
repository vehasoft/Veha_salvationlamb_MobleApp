package com.veha.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.veha.fragments.*
import com.veha.util.Permission
import com.veha.util.PermissionType
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
                if (Util.hasPermission(PermissionType.POST.value, Permission.READ.value)) {
                    HomeFragment.getInstance("user")
                } else {
                    NoPermissionFragment()
                }
            }
            1 -> {
                if (Util.hasPermission(PermissionType.FILE.value, Permission.READ.value)) {
                    FilesFragment()
                } else {
                    NoPermissionFragment()
                }
            }
           /* 2 -> {
                FilesFragment()
            }*/
            2 -> {
                if (Util.hasPermission(PermissionType.POST.value, Permission.READ.value) && Util.hasPermission(PermissionType.VIDEO.value, Permission.READ.value)) {
                    AdminVideoFragment()
                } else {
                    NoPermissionFragment()
                }
            }
            3 -> {
                if (Util.hasPermission(PermissionType.POST.value, Permission.READ.value) && Util.hasPermission(PermissionType.AUDIO.value, Permission.READ.value)) {
                    AdminAudioFragment()
                } else {
                    NoPermissionFragment()
                }
            }
            4 -> {
                if (Util.hasPermission(PermissionType.PROFILE.value, Permission.READ.value)) {
                    ProfileFragment.getInstance(Util.userId,"me")
                } else {
                    NoPermissionFragment()
                }
            }
            else -> b as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}