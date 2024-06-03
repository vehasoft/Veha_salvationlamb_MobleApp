package com.veha.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.veha.activity.R
import com.veha.activity.ViewPostActivity
import com.veha.util.NotificationList
import com.veha.util.Posts
import com.veha.util.UserPreferences

class AnnouncementAdapter(var announcements: ArrayList<Posts>, var context: Context,owner: LifecycleOwner) :
    RecyclerView.Adapter<NotificationListAdapter.ViewHolder>() {
    lateinit var userPreferences: UserPreferences
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //val name : TextView = view.findViewById(R.id.name_fol)
        //val profilePic : ImageView = view.findViewById(R.id.profile_pic_fol)
        val notificationContent: TextView = view.findViewById(R.id.notification_content)
        val notificationtime: TextView = view.findViewById(R.id.time_ago)
        val notificationLayout: LinearLayout = view.findViewById(R.id.notification_list_linear)
        val profilePic: ImageView = view.findViewById(R.id.profile_pic_fol)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationListAdapter.ViewHolder {
        userPreferences = UserPreferences(context)
        var layoutInflater: LayoutInflater = LayoutInflater.from(context)
        var items: View = layoutInflater.inflate(R.layout.child_notification_list, parent, false)
        return NotificationListAdapter.ViewHolder(items)
    }

    override fun getItemCount(): Int {
        return announcements.size
    }

    override fun onBindViewHolder(holder: NotificationListAdapter.ViewHolder, position: Int) {
        val announcement = announcements[position]
        if (!announcement.user.picture.isNullOrEmpty()) {
            Picasso.with(context).load(announcement.user.picture).into(holder.profilePic)
        } else {
            holder.profilePic.setImageResource(R.drawable.ic_profile)
        }
        holder.notificationtime.visibility = View.GONE

        val html = "<b>" + announcement.user.name + "</b>" + "  " + announcement.title
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(html)
        }).also { holder.notificationContent.text = it }
        holder.notificationLayout.setOnClickListener {
            val intent = Intent(context, ViewPostActivity::class.java)
            //intent.putExtra("post", announcement)
            context.startActivity(intent)
        }
    }
    fun addItem(post: ArrayList<Posts>) {
        announcements.addAll(post)
        notifyItemRangeInserted(announcements.size, post.size)
    }
}