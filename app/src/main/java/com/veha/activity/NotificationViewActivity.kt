package com.veha.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.adapter.NotificationListAdapter
import com.veha.util.NotificationList

class NotificationViewActivity : AppCompatActivity() {

    private lateinit var notificationRecyclerView: RecyclerView
    lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_view)

        notificationRecyclerView=findViewById(R.id.notification_recycler)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val n1 = NotificationList("1","test","liked your post","1 min ago")
        val n2 = NotificationList("1","test1","is following you","1 min ago")
        val n3 = NotificationList("1","test","liked your post","1 min ago")
        val n4 = NotificationList("1","test","is following you","1 min ago")
        val n5 = NotificationList("1","test","liked your post","1 min ago")
        val n6 = NotificationList("1","test","is following you","1 min ago")
        val n7 = NotificationList("1","test","liked your post","1 min ago")
        val n8 = NotificationList("1","admin","deleted your post","1 min ago")
        val n9 = NotificationList("1","test","is following you","1 min ago")
        val n10 = NotificationList("1","test","is following you","1 min ago")
        val n11 = NotificationList("1","test","is following you","1 min ago")

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
        notificationList.add(n10)
        notificationList.add(n11)
        notificationRecyclerView.layoutManager = LinearLayoutManager(this@NotificationViewActivity)
        notificationRecyclerView.adapter = NotificationListAdapter(notificationList, this)

    }
}