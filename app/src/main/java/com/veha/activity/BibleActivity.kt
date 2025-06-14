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
import android.widget.AutoCompleteTextView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.veha.util.UserPreferences
import com.veha.util.Util
import com.veha.util.Util.bookmarkedBible
import kotlinx.coroutines.launch
import org.chromium.base.Log

class BibleActivity : AppCompatActivity() {
    lateinit var logo: ImageView
    lateinit var recyclerView: RecyclerView
    lateinit var bibleLinear: LinearLayout
    lateinit var copy: ImageView
    lateinit var share: ImageView
    lateinit var next: ImageView
    lateinit var previous: ImageView
    lateinit var homeBtn: ImageView
    lateinit var bookmarkBtn: ImageView
    lateinit var post: Button
    lateinit var adapterr: MyAdapter
    private lateinit var buttonContainer: ConstraintLayout
    lateinit var bibleDropdown: Spinner
    lateinit var contentDropdown: AutoCompleteTextView

    //    lateinit var chapterDropdown: Spinner
    lateinit var fakeSpinner: AutoCompleteTextView
    lateinit var userPreferences: UserPreferences
    lateinit var shimmerFrameLayout: ShimmerFrameLayout
    val bibleMap: HashMap<String, JsonArray> = HashMap()
    var onLoad = false
    var keyList: ArrayList<String> = ArrayList()
    val chapterMap: HashMap<String, JsonArray> = HashMap()
    var chapterList: ArrayList<String> = ArrayList()
    var bibleList: ArrayList<String> = ArrayList()
    var type: String = "old"
    var bookmarkedChapter: String = "DUMMY"
    var bookmarkedEdition: String = "DUMMY"
    var bookmarkedContent: String = "DUMMY"

    private var isMultiSelect = false

    companion object {
        var selectedText: ArrayList<String> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bible)
        userPreferences = UserPreferences(this@BibleActivity)
        onLoad = true
        recyclerView = findViewById(R.id.recycler_view)
        logo = findViewById(R.id.prod_logo)
        copy = findViewById(R.id.copy_txt)
        share = findViewById(R.id.share_txt)
        next = findViewById(R.id.next_btn)
        previous = findViewById(R.id.prev_btn)
        homeBtn = findViewById(R.id.bible_to_home)
        bookmarkBtn = findViewById(R.id.bible_bookmark)
        post = findViewById(R.id.post_txt)
        bibleDropdown = findViewById(R.id.bible)
        contentDropdown = findViewById(R.id.heading)
//        chapterDropdown = findViewById(R.id.chapter)
        bibleLinear = findViewById(R.id.bible_linear)
        buttonContainer = findViewById(R.id.button_container)
        shimmerFrameLayout = findViewById(R.id.bible_shimmer_layout)
        fakeSpinner = findViewById(R.id.fakeSpinner)
        // Make fakeSpinner act as a grid dropdown
        fakeSpinner.setAdapter(null)
        fakeSpinner.inputType = 0 // Disable keyboard
        fakeSpinner.keyListener = null
        fakeSpinner.setDropDownBackgroundResource(android.R.color.transparent) // Hide default dropdown
        fakeSpinner.setOnClickListener {
            showGridDropdown(chapterList)
        }
        fakeSpinner.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                showGridDropdown(chapterList)
            }
        }
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        homeBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = LinearLayoutManager(this)
        if (!intent.extras!!.getString("type").isNullOrEmpty()) {
            type = intent.extras!!.getString("type").toString()
        }
        if (!bookmarkedBible.isNullOrEmpty() && bookmarkedBible.split(",").size == 3) {
            bookmarkedEdition = bookmarkedBible.split(",")[0]
            bookmarkedContent = bookmarkedBible.split(",")[1]
            bookmarkedChapter = bookmarkedBible.split(",")[2]
        }
        next.setOnClickListener {
//            if (chapterDropdown.selectedItemPosition < chapterList.size - 1) {
//                selectedText = ArrayList()
//                chapterDropdown.setSelection(chapterDropdown.selectedItemPosition + 1)// = chapterList[chapterDropdown.selectedItemPosition+1]
//            }
            if (fakeSpinner.text.toString().toInt() < chapterList.size) {
                selectedText = ArrayList()
                val count = fakeSpinner.text.toString().toInt()
                fakeSpinner.setText((count+1).toString(), false)
                setVersesList(count)
            }
        }
        previous.setOnClickListener {
//            if (chapterDropdown.selectedItemPosition > 0) {
//                selectedText = ArrayList()
//                chapterDropdown.setSelection(chapterDropdown.selectedItemPosition - 1)// = chapterList[chapterDropdown.selectedItemPosition+1]
//            }

            if (fakeSpinner.text.toString().toInt() > 1) {
                selectedText = ArrayList()
                val count = fakeSpinner.text.toString().toInt() - 2
                fakeSpinner.setText((count+1).toString(),false)
                setVersesList(count)
            }
        }
        var text = ""
        copy.setOnClickListener {
            text = ""
            selectedText.sortBy { it.substringBefore('.').toInt() }
            for (selectedTexts in selectedText) {
                text = text + selectedTexts + "\n"
            }
            val clipBoardManager: ClipboardManager =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData = ClipData.newPlainText("bible", text.trim())
            clipBoardManager.setPrimaryClip(clipData)
        }
        share.setOnClickListener {
            text =
                contentDropdown.text.toString() + ", " + fakeSpinner.text.toString()
                    .toString() + "\n\n"
            selectedText.sortBy { it.substringBefore('.').toInt() }
            for (selectedTexts in selectedText) {
                text = text + selectedTexts + "\n"
            }
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Salvation Lamb")
                var shareMessage =
                    "${text.trim()} \n\n\n\nLet me recommend you this application\n\n"
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
                selectedText.sortBy { it.substringBefore('.').toInt() }
                for (selectedTexts in selectedText) {
                    text = text + selectedTexts + "\n" + "\n"
                }
                val intent = Intent(this, BiblePostActivity::class.java)
                intent.putExtra("edition", type)
                intent.putExtra("content", text.trim())
                intent.putExtra(
                    "tags",
                    contentDropdown.text.toString() + ", " + fakeSpinner.text.toString()
                        .toString()
                )
                startActivity(intent)
            }
            selectedText = ArrayList()
        }

        setBibleEdition()
        bookmarkBtn.setOnClickListener {
            if (bookmarkedBible.equals(bibleDropdown.selectedItem.toString() + "," + contentDropdown.text.toString() + "," + fakeSpinner.text.toString())) {
                bookmarkedBible = "Dummy"
                bookmarkBtn.setImageDrawable(this@BibleActivity.getDrawable(R.drawable.ic_baseline_bookmark_border_24))
                lifecycleScope.launch {
                    userPreferences.deleteBibleBookmark()
                }
            } else {
                bookmarkedBible =
                    bibleDropdown.selectedItem.toString() + "," + contentDropdown.text.toString() + "," + fakeSpinner.text.toString()
                bookmarkBtn.setImageDrawable(this@BibleActivity.getDrawable(R.drawable.ic_baseline_bookmark_24))
                lifecycleScope.launch {
                    userPreferences.saveBibleBookmark(
                        bibleDropdown.selectedItem.toString() + "," + contentDropdown.text.toString() + "," + fakeSpinner.text.toString()
                    )
                }
            }
        }
    }

    fun setBibleEdition() {
        bibleDropdown.adapter = null
        bibleList = ArrayList()
        bibleList.add(this@BibleActivity.getString(R.string.oldBible))
        bibleList.add(this@BibleActivity.getString(R.string.newBible))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bibleList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bibleDropdown.adapter = adapter
        if (bookmarkedEdition != "DUMMY") {
            bibleDropdown.setSelection(bibleList.indexOf(bookmarkedEdition))
        } else {
            if (type.contentEquals("old")) {
                bibleDropdown.setSelection(0)
            } else {
                bibleDropdown.setSelection(1)
            }
        }
        bibleDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                if (parent?.selectedItem != null && view != null) {
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
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun setHeading(list: JsonArray) {
        keyList = ArrayList()
        for (bibleContent in list) {
            val key = Gson().fromJson(bibleContent, JsonObject::class.java)
            keyList.add(key.get("n").asString)
            bibleMap.put(key.get("n").asString, key.get("C").asJsonArray)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, keyList)
        contentDropdown.setAdapter(adapter)

        if (bookmarkedContent != "DUMMY") {
            contentDropdown.setText(bookmarkedContent, false)
            loadChapterList(bibleMap[keyList[keyList.indexOf(bookmarkedContent)]]!!)
        } else {
            contentDropdown.setText(keyList[0], false)
            loadChapterList(bibleMap[keyList[0]]!!)
        }
        contentDropdown.setOnItemClickListener { parent, view, pos, id ->
            loadChapterList(bibleMap[keyList[pos]]!!)
        }
    }

    fun loadChapterList(items: JsonArray) {
        chapterList = ArrayList()
        var chapter = 0
        for (bibleContent in items) {
            chapter++
            val key = Gson().fromJson(bibleContent, JsonObject::class.java)
            chapterList.add(chapter.toString())
            chapterMap[chapter.toString()] = key.get("V").asJsonArray
        }
        // Adapter is not set for fakeSpinner, as we use a grid dialog now
        if (bookmarkedChapter != "DUMMY" && onLoad) {
            fakeSpinner.setText(bookmarkedChapter, false)
            onLoad = false
        } else {
            fakeSpinner.setText("1", false)
        }
        setVersesList((fakeSpinner.text.toString().toInt()) - 1)
    }

    private fun showGridDropdown(items: ArrayList<String>) {

        // Inflate custom layout for grid dropdown
        val dialogView = LayoutInflater.from(this).inflate(R.layout.grid_view, null)
        val gridView = dialogView.findViewById<GridView>(R.id.gridView)

        // Adapter for the grid
        gridView.adapter = object : BaseAdapter() {
            override fun getCount(): Int = items.size

            override fun getItem(position: Int): Any = items[position]

            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: LayoutInflater.from(this@BibleActivity)
                    .inflate(R.layout.grid_spinner_item, parent, false)
                val textItem = view.findViewById<TextView>(R.id.textItem)
                textItem.text = chapterList[position]
                return view
            }
        }

        // Create PopupWindow instead of AlertDialog
        val popupWindow = android.widget.PopupWindow(
            dialogView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Dismiss popup when item is selected
        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            fakeSpinner.setText(chapterList[position], false) // Set selected item text
            popupWindow.dismiss() // Close popup
            setVersesList(position)
        }

        // Optional: Dismiss popup if user taps outside
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        // Show the popup anchored to fakeSpinner, just like a dropdown
        popupWindow.showAsDropDown(fakeSpinner)
    }
    fun setVersesList(position: Int) {
        isMultiSelect = false
        buttonContainer.visibility = View.GONE
        selectedText = ArrayList()
        adapterr = if (position < 0) {
            MyAdapter(chapterMap[chapterList[0]]!!)
        } else {
            MyAdapter(chapterMap[chapterList[position]]!!)
        }
        recyclerView.adapter = adapterr


        if (bookmarkedBible == bibleDropdown.selectedItem.toString() + "," + contentDropdown.text.toString() + "," + fakeSpinner.text.toString()) {
            bookmarkBtn.setImageDrawable(this@BibleActivity.getDrawable(R.drawable.ic_baseline_bookmark_24))
        } else {
            bookmarkBtn.setImageDrawable(this@BibleActivity.getDrawable(R.drawable.ic_baseline_bookmark_border_24))
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
