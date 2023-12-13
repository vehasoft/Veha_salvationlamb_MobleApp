package com.veha.adapter

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.veha.activity.R
import com.veha.util.NotificationList

class NotificationListAdapter() : RecyclerView.Adapter<NotificationListAdapter.ViewHolder>() {
    private lateinit var notifications: ArrayList<NotificationList>
    private  lateinit var context: Context

    constructor(notifications: ArrayList<NotificationList>, context: Context) : this() {
        this.notifications = notifications
        this.context = context
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //val name : TextView = view.findViewById(R.id.name_fol)
        //val profilePic : ImageView = view.findViewById(R.id.profile_pic_fol)
        val notificationContent : TextView = view.findViewById(R.id.notification_content)
        val notificationListLinear : LinearLayout = view.findViewById(R.id.notification_list_linear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationListAdapter.ViewHolder {
        var layoutInflater: LayoutInflater = LayoutInflater.from(context)
        var items: View = layoutInflater.inflate(R.layout.child_notification_list, parent, false)
        return ViewHolder(items)
    }

    override fun getItemCount(): Int {
        return  notifications.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification : NotificationList = notifications[position]

        val html = "<b>" + notification.name + "</b>" +  " " + "<p>" + notification.content + "<br>" + notification.createdAt + "</p>"

        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(html)
        }).also { holder.notificationContent.text = it }
        /*holder.name.text = notification.user.name
        if (!notification.user.picture.isNullOrEmpty()){
            Picasso.with(context).load(notification.user.picture).into(holder.profilePic)
        }

        holder.likeListLinear.setOnClickListener {
            val intent = Intent(context, ViewProfileActivity::class.java)
            intent.putExtra("userId",notification.userId)
            context.startActivity(intent)
        }*/
    }
}