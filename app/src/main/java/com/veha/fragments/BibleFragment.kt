package com.veha.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veha.activity.BibleActivity
import com.veha.activity.R
import com.veha.adapter.BibleAdapter
import com.veha.util.UserPreferences
import com.veha.util.Util
import org.chromium.base.Log

class BibleFragment : Fragment() {

    lateinit var userPreferences: UserPreferences
    lateinit var oldd: ConstraintLayout
    lateinit var neww: ConstraintLayout
    lateinit var contexts: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        contexts = container!!.context
        val view =  inflater.inflate(R.layout.fragment_bible, container, false)
        userPreferences = UserPreferences(contexts)
        oldd = view.findViewById(R.id.oldd)
        neww = view.findViewById(R.id.neww)
        oldd.setOnClickListener {
            val intent = Intent(contexts, BibleActivity::class.java)
            intent.putExtra("type", "old")
            intent.putExtra("details", "Old")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            contexts.startActivity(intent)
        }
        neww.setOnClickListener {
            val intent = Intent(contexts, BibleActivity::class.java)
            intent.putExtra("type", "new")
            intent.putExtra("details", "New")
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            contexts.startActivity(intent)
        }

        return view
    }
}