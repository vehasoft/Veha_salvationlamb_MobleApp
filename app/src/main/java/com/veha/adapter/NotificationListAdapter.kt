package com.veha.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import com.veha.activity.ApproveRequestActivity
import com.veha.activity.MainActivity
import com.veha.activity.NoPermissionActivity
import com.veha.activity.PdfActivity2
import com.veha.activity.R
import com.veha.activity.ViewPostActivity
import com.veha.activity.ViewProfileActivity
import com.veha.activity.WebViewActivity
import com.veha.util.Commons
import com.veha.util.NotificationList
import com.veha.util.NotificationType
import com.veha.util.Permission
import com.veha.util.PermissionType
import com.veha.util.Posts
import com.veha.util.UserPreferences
import com.veha.util.Util
import retrofit2.Call
import retrofit2.Response

class NotificationListAdapter() : RecyclerView.Adapter<NotificationListAdapter.ViewHolder>() {
    private lateinit var notifications: ArrayList<NotificationList>
    private lateinit var context: Context
    private lateinit var userPreferences: UserPreferences
    private lateinit var owner: LifecycleOwner

    constructor(
        notifications: ArrayList<NotificationList>,
        context: Context,
        owner: LifecycleOwner
    ) : this() {
        this.notifications = notifications
        this.context = context
        this.owner = owner
    }

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
    ): ViewHolder {
        userPreferences = UserPreferences(context)
        var layoutInflater: LayoutInflater = LayoutInflater.from(context)
        var items: View = layoutInflater.inflate(R.layout.child_notification_list, parent, false)
        return ViewHolder(items)
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification: NotificationList = notifications[position]
        if (!notification.user.picture.isNullOrEmpty()) {
            Picasso.get().load(notification.user.picture).into(holder.profilePic)
        } else {
            holder.profilePic.setImageResource(R.drawable.ic_profile)
        }
        if (!java.lang.Boolean.parseBoolean(notification.isVisited)) {
            holder.notificationLayout.setBackgroundColor(context.resources.getColor(R.color.secondary_blue))
        } else {
            holder.notificationLayout.setBackgroundColor(context.resources.getColor(R.color.white))
        }

        val html = "<b>" + notification.user.name + "</b>" + "  " + notification.message.replace(
            notification.user.name,
            ""
        )

        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(html)
        }).also { holder.notificationContent.text = it }
        holder.notificationtime.text = Util.getTimeAgo(notification.createdAt)
        holder.notificationLayout.setOnClickListener {
            readNotification(holder, notification.id)
            if (NotificationType.POST.value == notification.type) {
                if (Util.hasPermission(PermissionType.POST.value, Permission.READ.value)) {
                    val intent = Intent(context, ViewPostActivity::class.java)
                    intent.putExtra("type", NotificationType.POST.value)
                    intent.putExtra("postId", notification.data)
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, NoPermissionActivity::class.java)
                    context.startActivity(intent)
                }
            } else if (NotificationType.USER.value == notification.type) {
                if (Util.hasPermission(PermissionType.USER.value, Permission.READ.value)) {
                    val intent = Intent(context, ViewProfileActivity::class.java)
                    intent.putExtra("userId", notification.data)
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, NoPermissionActivity::class.java)
                    context.startActivity(intent)
                }
            } else if (NotificationType.WARRIOR.value == notification.type) {
                if (Util.hasPermission(PermissionType.USER.value, Permission.EDIT.value)) {
                    val intent = Intent(context, ApproveRequestActivity::class.java)
                    intent.putExtra("userId", notification.data)
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, NoPermissionActivity::class.java)
                    context.startActivity(intent)
                }
            } else if (NotificationType.ANNOUNCEMENT.value == notification.type) {
                if (Util.hasPermission(PermissionType.ANNOUNCEMENT.value, Permission.READ.value)) {
                    val intent = Intent(context, ViewPostActivity::class.java)
                    intent.putExtra("type", NotificationType.ANNOUNCEMENT.value)
                    intent.putExtra("postId", notification.data)
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, NoPermissionActivity::class.java)
                    context.startActivity(intent)
                }
            } else if (NotificationType.FILE.value == notification.type) {
                val intent = Intent(context, PdfActivity2::class.java)
                intent.putExtra("fileName", notification.data)
                intent.putExtra("url", notification.fileUrl)
                context.startActivity(intent)
            } else if (NotificationType.EVENT.value == notification.type) {
                val intent = Intent(context, WebViewActivity::class.java)
                intent.putExtra("pageUrl", notification.data)
                context.startActivity(intent)
            }
        }
    }

    fun readNotification(holder: ViewHolder, id: String) {
        try {
            val data = JsonObject()
            data.addProperty("isVisited", true)
            if (Commons().isNetworkAvailable(context)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? =
                            retrofit.putReadNotification("Bearer $it", id, data)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    data.remove("isVisited")
                                    holder.notificationLayout.setBackgroundColor(
                                        context.resources.getColor(
                                            R.color.white
                                        )
                                    )
                                } else {
                                    Log.e("code", response.code().toString())
                                    Log.e("err", response.errorBody().toString())
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("NotificationListAdapter.readNotification", "fail")
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationListAdapter.readNotification", e.toString())
        }
    }

    fun addItem(post: ArrayList<NotificationList>) {
        notifications.addAll(post)
        notifyItemRangeInserted(notifications.size, post.size)
    }
}