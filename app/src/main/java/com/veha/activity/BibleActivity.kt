package com.veha.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.veha.adapter.BibleAdapter
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.Util
import kotlinx.coroutines.launch
import org.chromium.base.Log
import retrofit2.Call
import retrofit2.Response

class BibleActivity : AppCompatActivity() {
    lateinit var logo: ImageView
    lateinit var recyclerView: RecyclerView
    lateinit var bibleLinear: LinearLayout
    lateinit var copy: ImageView
    lateinit var share: ImageView
    lateinit var next: ImageView
    lateinit var previous: ImageView
    lateinit var post: Button
    lateinit var contentDropdown: Spinner
    lateinit var chapterDropdown: Spinner
    lateinit var userPreferences: UserPreferences
    lateinit var shimmerFrameLayout: ShimmerFrameLayout
    val bibleMap: HashMap<String, JsonArray> = HashMap()
    val keyList: ArrayList<String> = ArrayList()
    val chapterMap: HashMap<String, JsonArray> = HashMap()
    val chapterList: ArrayList<String> = ArrayList()

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
        contentDropdown = findViewById(R.id.heading)
        chapterDropdown = findViewById(R.id.chapter)
        bibleLinear = findViewById(R.id.bible_linear)
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
        var obj: JsonArray
        if (type == "old") {
            obj = Gson().fromJson(Util.bible.get("Old").toString(), JsonArray::class.java)
        } else {
            obj = Gson().fromJson(Util.bible.get("New").toString(), JsonArray::class.java)
        }
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
                Log.e("hgvhgzdv", selectedTexts)
                text = text + selectedTexts + "\n"
            }
            Log.e("selected text", text)
            val clipBoardManager: ClipboardManager =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData = ClipData.newPlainText("bible", text)
            clipBoardManager.setPrimaryClip(clipData)
        }
        share.setOnClickListener {
            text = ""
            for (selectedTexts in selectedText) {
                Log.e("hgvhgzdv", selectedTexts)
                text = text + selectedTexts + "\n"
            }
            Log.e("selected text", text)
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
            if (selectedText.size <= 0){
                Toast.makeText(this,"Please select atleast one",Toast.LENGTH_LONG).show()
            } else {
                text = ""
                var i = 0
                for (selectedTexts in selectedText) {
                    text = text + ++i + ". " + selectedTexts + "\n" + "\n"
                }


                val intent = Intent(this, BiblePostActivity::class.java)
                intent.putExtra("edition", type)
                intent.putExtra("content", text)
                intent.putExtra(
                    "tags",
                    contentDropdown.selectedItem.toString() + "," + chapterDropdown.selectedItem.toString()
                )
                startActivity(intent)
            }

//            val data = JsonObject()
//            data.addProperty("title", "bible content")
//            data.addProperty("content", text)
//            data.addProperty("tags", "Bible")
//            data.addProperty("image", "")
//            data.addProperty("url", "")
//            data.addProperty("type", "image")
//            data.addProperty("userId", Util.userId)
//            postData(data)
            selectedText = ArrayList()
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
        }

        for (bibleContent in obj) {
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
        var chapter = 0
        for (bibleContent in list) {
            chapter++
            val key = Gson().fromJson(bibleContent, JsonObject::class.java)
            chapterList.add("ch $chapter")
            chapterMap.put("ch $chapter", key.get("V").asJsonArray)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, chapterList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chapterDropdown.adapter = adapter
        chapterDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                // Log.e(chapterList[pos],chapterMap.get(chapterList[pos]).toString())
                selectedText = ArrayList()
                recyclerView.adapter = BibleAdapter(
                    this@BibleActivity,
                    chapterMap[chapterList[pos]]!!,
                    this@BibleActivity
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        //recyclerView.adapter = BibleAdapter(this, chapterMap[chapterList[0]]!!, "", this, "")
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

    private fun postData(data: JsonObject) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call1: Call<JsonObject?>? =
                            retrofit.postCallHead("Bearer $it", "post", data)
                        call1!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    post.isEnabled = true
                                    val intent =
                                        Intent(this@BibleActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    post.isEnabled = true
                                    val resp = response.errorBody()
                                    val loginresp: JsonObject =
                                        Gson().fromJson(resp?.string(), JsonObject::class.java)
                                    val status = loginresp.get("status").toString()
                                    val errorMessage = loginresp.get("errorMessage").toString()
                                    Log.e("Status", status)
                                    Log.e("result", errorMessage)
                                }
                                call1.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                android.util.Log.e("AddPostActivity.postData", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@BibleActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@BibleActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddPostActivity.postData", e.toString())
        }
    }

}