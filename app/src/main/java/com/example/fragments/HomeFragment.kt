package com.example.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.HomeAdapter
import com.example.fbproject.R
import com.example.util.Post
import jp.wasabeef.richeditor.RichEditor


class HomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_home, container, false)
        richTextEditer(view)


        var post1 = Post("Temp1","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/1.jpg","M. Hari prasath1","30 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")
        var post2 = Post("Temp2","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/2.jpg","M. Hari prasath2","31 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")
        var post3 = Post("Temp3","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/3.jpg","M. Hari prasath3","32 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")
        var post4 = Post("Temp4","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/4.jpg","M. Hari prasath4","33 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")
        var post5 = Post("Temp5","#veha,#salvationlamb","https://www.gstatic.com/webp/gallery/5.jpg","M. Hari prasath5","34 mins ago","12","Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s")

        var posts : ArrayList<Post> = ArrayList()
        posts.add(post1)
        posts.add(post2)
        posts.add(post3)
        posts.add(post4)
        posts.add(post5)


        var list : RecyclerView = view.findViewById(R.id.list)
        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = HomeAdapter(posts, container!!.context,"home")
        return  view
    }
    fun richTextEditer (view: View){
        var mEditor = view.findViewById<RichEditor>(R.id.editor)
        mEditor.setEditorHeight(40)
        mEditor.setEditorFontSize(15)
        mEditor.setEditorFontColor(Color.BLACK);
        mEditor.setPlaceholder("Write something here...")
        mEditor.setPadding(10, 10, 10, 10);
        view.findViewById<ImageButton>(R.id.action_undo).setOnClickListener { mEditor.undo() }
        view.findViewById<ImageButton>(R.id.action_redo).setOnClickListener { mEditor.redo() }
        view.findViewById<ImageButton>(R.id.action_bold).setOnClickListener { mEditor.setBold() }
        view.findViewById<ImageButton>(R.id.action_italic).setOnClickListener { mEditor.setItalic() }
        view.findViewById<ImageButton>(R.id.action_strikethrough).setOnClickListener { mEditor.setStrikeThrough() }
        view.findViewById<ImageButton>(R.id.action_underline).setOnClickListener { mEditor.setUnderline() }
        view.findViewById<ImageButton>(R.id.action_heading1).setOnClickListener { mEditor.setHeading(1) }
        view.findViewById<ImageButton>(R.id.action_heading2).setOnClickListener { mEditor.setHeading(2) }
        view.findViewById<ImageButton>(R.id.action_heading3).setOnClickListener { mEditor.setHeading(3) }
        view.findViewById<ImageButton>(R.id.action_align_left).setOnClickListener { mEditor.setAlignLeft() }
        view.findViewById<ImageButton>(R.id.action_align_center).setOnClickListener { mEditor.setAlignCenter() }
        view.findViewById<ImageButton>(R.id.action_align_right).setOnClickListener { mEditor.setAlignRight() }
        view.findViewById<ImageButton>(R.id.action_insert_bullets).setOnClickListener { mEditor.setBullets() }
    }
}