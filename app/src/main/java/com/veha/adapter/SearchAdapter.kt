package com.veha.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.veha.fragments.NoPermissionFragment
import com.veha.fragments.SearchPostFragment
import com.veha.fragments.SearchProfileFragment
import com.veha.util.Permission
import com.veha.util.PermissionType
import com.veha.util.PostUser
import com.veha.util.Posts
import com.veha.util.Util


internal class SearchAdapter(
    val context: Context,
    fm: FragmentManager?,
    private var totalTabs: Int,
    val profilelist: ArrayList<PostUser>,
    val postlist: ArrayList<Posts>
) : FragmentPagerAdapter(fm!!) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            1 -> {
                if (Util.hasPermission(PermissionType.USER.value, Permission.READ.value)) {
                    SearchProfileFragment.getFragment(profilelist)
                } else {
                    NoPermissionFragment()
                }
            }

            0 -> {
                if (Util.hasPermission(PermissionType.POST.value, Permission.READ.value)) {
                    SearchPostFragment.getFragment(postlist)
                } else {
                    NoPermissionFragment()
                }
            }

            else -> null as Fragment
        }
    }

    override fun getCount(): Int {
        return totalTabs
    }
}