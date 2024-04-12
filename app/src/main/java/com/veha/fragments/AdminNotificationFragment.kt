package com.veha.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.activity.R
import com.veha.adapter.NotificationListAdapter
import com.veha.util.NotificationList
import com.veha.util.PostUser
import com.veha.util.NotificationType

class AdminNotificationFragment : Fragment() {
    lateinit var list: RecyclerView
    lateinit var nodata: LinearLayout
    lateinit var contexts: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_admin_notification, container, false)
        contexts = container!!.context
        list = view.findViewById(R.id.notification_admin_recycler)
        nodata = view.findViewById(R.id.no_data)


        val n1 = NotificationList("1","18e83160-db79-11ee-991e-9f64ae4c00f8","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n2 = NotificationList("1","086b6270-b451-11ee-a013-91221e4f7ee9","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.USER.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n3 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n4 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n5 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n6 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n7 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n8 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n9 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n10 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
        val n11 = NotificationList("1","37b83040-64ff-11ee-b633-99dbe0476c63","086b6270-b451-11ee-a013-91221e4f7ee9","Followed Your Profile","Rajkumar Lakshmanan Followed your Profile",NotificationType.POST.name,"086b6270-b451-11ee-a013-91221e4f7ee9","false","2024-02-13T02:53:06.000Z","2024-02-13T02:53:06.000Z",
            PostUser("086b6270-b451-11ee-a013-91221e4f7ee9","Rajkumar Lakshmanan","","true","l.raajkumar@gmail.com",)
        )
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
        list.layoutManager = LinearLayoutManager(contexts)
        list.adapter = NotificationListAdapter(notificationList, contexts)

        return view
    }
}