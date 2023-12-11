package com.veha.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.adapter.NotificationListAdapter
import com.veha.adapter.ViewLikesAdapter
import com.veha.util.NotificationList
import kotlinx.android.synthetic.main.activity_notification_view.*

class NotificationViewActivity : AppCompatActivity() {

    private lateinit var notificationRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_view)

        notificationRecyclerView=findViewById(R.id.notification_recycler)

        val n1 = NotificationList("1","test","liked your post")
        val n2 = NotificationList("1","test1","is following you")
        val n3 = NotificationList("1","test","liked your post")
        val n4 = NotificationList("1","test","is following you")
        val n5 = NotificationList("1","test","liked your post")
        val n6 = NotificationList("1","test","is following you")
        val n7 = NotificationList("1","test","liked your post")
        val n8 = NotificationList("1","admin","deleted your post")
        val n9 = NotificationList("1","test","is following you")

        val notificationList :ArrayList<NotificationList> = ArrayList()
        notificationList.add(n1)
        notificationList.add(n2)
        notificationList.add(n3)
        notificationList.add(n4)
        notificationList.add(n5)
        notificationList.add(n6)
        notificationList.add(n7)
        notificationList.add(n8)
        notificationList.add(n9)
        notificationRecyclerView.layoutManager = LinearLayoutManager(this@NotificationViewActivity)
        notificationRecyclerView.adapter = NotificationListAdapter(notificationList, this)

    }
}