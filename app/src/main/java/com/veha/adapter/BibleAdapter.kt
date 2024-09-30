package com.veha.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.veha.activity.BibleActivity
import com.veha.activity.ExpandableView
import com.veha.activity.R
import com.veha.util.BibleSelector
import com.veha.util.UserPreferences
import dmax.dialog.SpotsDialog


class BibleAdapter(val context: Context, val bibleContent: JsonArray, val owner: LifecycleOwner) :
    RecyclerView.Adapter<BibleAdapter.ViewHolder>() {
    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog

    //val selectedText: ArrayList<String> = ArrayList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        userPreferences = UserPreferences(context)
        dialog = SpotsDialog.Builder().setContext(context).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        val view = LayoutInflater.from(context).inflate(R.layout.child_bible, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bible: JsonObject = bibleContent[position] as JsonObject
            holder.bind(BibleSelector(bible.get("V").toString(),false))
            holder.bibleContent.text = bible.get("V").asString

        holder.bibleContent.setOnLongClickListener {
            Log.e("longpress","pressed")
            holder.selectedCheckBox.isChecked = true
            true
        }
        holder.selectedCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked)
            {
                Log.e("checked",position.toString())
                BibleActivity.selectedText.add(holder.bibleContent.text.toString())
                holder.bind(BibleSelector(holder.bibleContent.text.toString(), true))
            } else {
                Log.e("Not checked",position.toString())
                BibleActivity.selectedText.remove(holder.bibleContent.text.toString())
                holder.bind(BibleSelector(holder.bibleContent.text.toString(), false))
            }
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return bibleContent.size()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var bibleContent: ExpandableView
        var bodyLayout: LinearLayout
        var selectedCheckBox: CheckBox


        fun bind(selector: BibleSelector) {
            bibleContent.text = selector.content
            itemView.setBackgroundColor(if (selector.isSelected) Color.LTGRAY else Color.WHITE)
            Log.e("countt",BibleActivity.selectedText.size.toString())
            if (BibleActivity.selectedText.size <= 0) {
                selectedCheckBox.visibility = View.GONE
            } else {
                selectedCheckBox.visibility = View.VISIBLE
            }

        }
        init {
            bibleContent = itemView.findViewById(R.id.bible_content)
            bodyLayout = itemView.findViewById(R.id.body_layout)
            selectedCheckBox = itemView.findViewById(R.id.selected)
            selectedCheckBox.visibility = View.GONE
        }
    }
}