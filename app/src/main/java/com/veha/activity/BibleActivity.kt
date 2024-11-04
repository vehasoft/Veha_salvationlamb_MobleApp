package com.veha.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.veha.util.UserPreferences
import com.veha.util.Util
import org.chromium.base.Log

class BibleActivity : AppCompatActivity() {
    lateinit var logo: ImageView
    lateinit var recyclerView: RecyclerView
    lateinit var bibleLinear: LinearLayout
    lateinit var copy: ImageView
    lateinit var share: ImageView
    lateinit var next: ImageView
    lateinit var previous: ImageView
    lateinit var post: Button
    lateinit var adapterr: MyAdapter
    private lateinit var buttonContainer: ConstraintLayout
    lateinit var bibleDropdown: Spinner
    lateinit var contentDropdown: Spinner
    lateinit var chapterDropdown: Spinner
    lateinit var userPreferences: UserPreferences
    lateinit var shimmerFrameLayout: ShimmerFrameLayout
    val bibleMap: HashMap<String, JsonArray> = HashMap()
    var keyList: ArrayList<String> = ArrayList()
    val chapterMap: HashMap<String, JsonArray> = HashMap()
    var chapterList: ArrayList<String> = ArrayList()
    var bibleList: ArrayList<String> = ArrayList()

    private var isMultiSelect = false

    companion object {
        var selectedText: ArrayList<String> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bible)
        userPreferences = UserPreferences(this@BibleActivity)
        recyclerView = findViewById(R.id.recycler_view)
        logo = findViewById(R.id.prod_logo)
        copy = findViewById(R.id.copy_txt)
        share = findViewById(R.id.share_txt)
        next = findViewById(R.id.next_btn)
        previous = findViewById(R.id.prev_btn)
        post = findViewById(R.id.post_txt)
        bibleDropdown = findViewById(R.id.bible)
        contentDropdown = findViewById(R.id.heading)
        chapterDropdown = findViewById(R.id.chapter)
        bibleLinear = findViewById(R.id.bible_linear)
        buttonContainer = findViewById(R.id.button_container)
        shimmerFrameLayout = findViewById(R.id.bible_shimmer_layout)
        shimmerFrameLayout.startShimmer()
        bibleCheck()
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = LinearLayoutManager(this)
        val type: String = intent.extras!!.getString("type").toString()
        setBibleEdition(type)
        next.setOnClickListener {
            if (chapterDropdown.selectedItemPosition < chapterList.size - 1) {
                selectedText = ArrayList()
                chapterDropdown.setSelection(chapterDropdown.selectedItemPosition + 1)// = chapterList[chapterDropdown.selectedItemPosition+1]
            }
        }
        previous.setOnClickListener {
            if (chapterDropdown.selectedItemPosition > 0) {
                selectedText = ArrayList()
                chapterDropdown.setSelection(chapterDropdown.selectedItemPosition - 1)// = chapterList[chapterDropdown.selectedItemPosition+1]
            }
        }
        var text = ""
        copy.setOnClickListener {
            text = ""
            for (selectedTexts in selectedText) {
                text = text + selectedTexts + "\n"
            }
            val clipBoardManager: ClipboardManager =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData = ClipData.newPlainText("bible", text)
            clipBoardManager.setPrimaryClip(clipData)
        }
        share.setOnClickListener {
            text = ""
            for (selectedTexts in selectedText) {
                text = text + selectedTexts + "\n"
            }
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salvation Lamb")
                var shareMessage = "$text \n\n\n\nLet me recommend you this application\n\n"
                shareMessage = """
                    ${shareMessage + "https://salvationlamb.com/redirect"}                    
                    """.trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                android.util.Log.e("exception", e.toString())
            }
            selectedText = ArrayList()
        }
        post.setOnClickListener {
            if (selectedText.size <= 0) {
                Toast.makeText(this, "Please select atleast one", Toast.LENGTH_LONG).show()
            } else {
                text = ""
                for (selectedTexts in selectedText) {
                    text = text + selectedTexts + "\n" + "\n"
                }

                val intent = Intent(this, BiblePostActivity::class.java)
                intent.putExtra("edition", type)
                intent.putExtra("content", text)
                intent.putExtra(
                    "tags",
                    contentDropdown.selectedItem.toString() + ", " + chapterDropdown.selectedItem.toString()
                )
                startActivity(intent)
            }
            selectedText = ArrayList()
        }
    }

    fun setBibleEdition(edition: String) {
        bibleDropdown.adapter = null
        bibleList = ArrayList()
        bibleList.add(this@BibleActivity.getString(R.string.oldBible))
        bibleList.add(this@BibleActivity.getString(R.string.newBible))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bibleList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bibleDropdown.adapter = adapter
        if (edition.contentEquals("old")) {
            bibleDropdown.setSelection(0)
        } else {
            bibleDropdown.setSelection(1)
        }
        bibleDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                if (pos == 0) {
                    setHeading(
                        Gson().fromJson(
                            Util.bible.get("Old").toString(),
                            JsonArray::class.java
                        )
                    )
                } else {
                    setHeading(
                        Gson().fromJson(
                            Util.bible.get("New").toString(),
                            JsonArray::class.java
                        )
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun setHeading(list: JsonArray) {
        contentDropdown.adapter = null
        keyList = ArrayList()
        for (bibleContent in list) {
            val key = Gson().fromJson(bibleContent, JsonObject::class.java)
            keyList.add(key.get("n").asString)
            bibleMap.put(key.get("n").asString, key.get("C").asJsonArray)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, keyList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        contentDropdown.adapter = adapter
        contentDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                Log.e(keyList[pos], bibleMap[keyList[pos]].toString())
                setChapter(bibleMap[keyList[pos]]!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun setChapter(list: JsonArray) {
        chapterDropdown.adapter = null
        chapterList = ArrayList()
        var chapter = 0
        for (bibleContent in list) {
            chapter++
            val key = Gson().fromJson(bibleContent, JsonObject::class.java)
            chapterList.add("அதிகாரம் $chapter")
            chapterMap.put("அதிகாரம் $chapter", key.get("V").asJsonArray)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, chapterList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chapterDropdown.adapter = adapter
        chapterDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                // Log.e(chapterList[pos],chapterMap.get(chapterList[pos]).toString())
                isMultiSelect = false
                buttonContainer.visibility = View.GONE
                selectedText = ArrayList()
                adapterr = MyAdapter(chapterMap[chapterList[pos]]!!)
                recyclerView.adapter = adapterr
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun bibleCheck() {
        if (Util.bible != null) {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = View.GONE
            bibleLinear.visibility = View.VISIBLE
        } else {
            Util.getBible()
            bibleCheck()
        }
    }

    inner class MyAdapter(private val bibleArray: JsonArray) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view =
                LayoutInflater.from(this@BibleActivity).inflate(R.layout.child_bible, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val bible: JsonObject = bibleArray[position] as JsonObject
            val text = "" + (position + 1) + ". " + bible.get("V").asString
            holder.content.text = text
            holder.itemView.setOnLongClickListener {
                if (!isMultiSelect) {
                    isMultiSelect = true
                    buttonContainer.visibility = View.VISIBLE
                    adapterr.notifyDataSetChanged()
                }
                toggleSelection(holder, text)
                true
            }

            holder.itemView.setOnClickListener {
                if (isMultiSelect) {
                    toggleSelection(holder, text)
                }
            }

            holder.selectedCheckBox.visibility = if (isMultiSelect) View.VISIBLE else View.GONE
            holder.selectedCheckBox.isChecked = selectedText.contains(text)
        }

        override fun getItemCount() = bibleArray.size()

        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val content: TextView = itemView.findViewById(R.id.bible_content)
            val selectedCheckBox: CheckBox = itemView.findViewById(R.id.selected)
        }
    }

    private fun toggleSelection(holder: MyAdapter.MyViewHolder, text: String) {
        Log.e("selected text", selectedText.toString())
        Log.e("selected text", selectedText.size.toString())
        if (selectedText.contains(text)) {
            selectedText.remove(text)
            holder.selectedCheckBox.isChecked = false
        } else {
            selectedText.add(text)
            holder.selectedCheckBox.isChecked = true
        }

        if (selectedText.isEmpty()) {
            isMultiSelect = false
            buttonContainer.visibility = View.GONE
            adapterr.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() {
        if (isMultiSelect) {
            isMultiSelect = false
            buttonContainer.visibility = View.GONE
            adapterr.notifyDataSetChanged()
            return
        }
        super.onBackPressed()
    }
}