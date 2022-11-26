package com.example.fbproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.FollowAdapter
import com.example.util.Followers

class FollowerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follower)
        var lists : RecyclerView = findViewById<RecyclerView>(R.id.my_follow_list)

        var follow1 = Followers("hari ","https://www.gstatic.com/webp/gallery/1.jpg")
        var follow2 = Followers("hari ","https://www.gstatic.com/webp/gallery/2.jpg")
        var follow3 = Followers("hari ","https://www.gstatic.com/webp/gallery/3.jpg")
        var follow4 = Followers("hari ","https://www.gstatic.com/webp/gallery/4.jpg")
        var follow5 = Followers("hari ","https://www.gstatic.com/webp/gallery/5.jpg")
        var follow6 = Followers("hari ","https://www.gstatic.com/webp/gallery/6.jpg")
        var follow7 = Followers("hari ","https://www.gstatic.com/webp/gallery/7.jpg")
        var follow8 = Followers("hari ","https://www.gstatic.com/webp/gallery/8.jpg")

        var followList : ArrayList<Followers> = ArrayList()
        followList.add(follow1)
        followList.add(follow2)
        followList.add(follow3)
        followList.add(follow4)
        followList.add(follow5)
        followList.add(follow6)
        followList.add(follow7)
        followList.add(follow8)

        lists.layoutManager = LinearLayoutManager(this)
        lists.adapter = FollowAdapter(followList, this,"followers")
    }
}