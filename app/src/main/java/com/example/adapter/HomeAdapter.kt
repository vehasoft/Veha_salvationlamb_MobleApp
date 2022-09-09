package com.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fbproject.R


class HomeAdapter() : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var layoutInflater : LayoutInflater = LayoutInflater.from(parent.context)
        var items : View = layoutInflater.inflate(R.layout.child_post,parent,false)
        var viewHolder = ViewHolder(items)
        return viewHolder

    }

    override fun getItemCount(): Int {
        return  10
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //data binding via datamodel class and on click etc...
        /*val myListData: MyListData = listdata.get(position)
        holder.textView.setText(listdata.get(position).getDescription())
        holder.imageView.setImageResource(listdata.get(position).getImgId())
        holder.relativeLayout.setOnClickListener(View.OnClickListener { view ->
            Toast.makeText(
                view.context,
                "click on item: " + myListData.getDescription(),
                Toast.LENGTH_LONG
            ).show()
        })*/
    }
}