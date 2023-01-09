package com.example.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adapter.HomeAdapter
import com.example.fbproject.LoginActivity
import com.example.fbproject.R
import com.example.models.FavPost
import com.example.models.Posts
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response


class HomeFragment(private val contexts: Context,private var type: String) : Fragment() {
    lateinit var userPreferences: UserPreferences
    lateinit var list : RecyclerView
    lateinit var nodata: LinearLayout
    lateinit var createpostLinear: LinearLayout
    lateinit var postBtn: Button
    lateinit var title: EditText
    lateinit var content: EditText
    lateinit var userType: String

    //private lateinit var mEditor: RichEditor

    private lateinit var likeslist: ArrayList<PostLikes>
    private lateinit var myFollowMap: HashMap<String,String>
    private lateinit var myFavMap: HashMap<String,String>
    private var page: Int = 1
    private var showCreatePost: Boolean = false


    private var myLikes: String = ""
    private lateinit var myLikesMap: HashMap<String,String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        likeslist = ArrayList()
        myFollowMap = HashMap()
        myFavMap = HashMap()
        myLikesMap = HashMap()
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        userPreferences = UserPreferences(contexts)
        list = view.findViewById(R.id.list)
        postBtn = view.findViewById(R.id.post_btn)
        title = view.findViewById(R.id.title)
        content = view.findViewById(R.id.content)
        nodata = view.findViewById(R.id.no_data)
        createpostLinear = view.findViewById(R.id.createpostLinear)
        //mEditor = view.findViewById(R.id.editor)
        getMyDetails()
/*

        var post1 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath1","https://www.gstatic.com/webp/gallery/1.jpg","",""))
        var post2 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath2","https://www.gstatic.com/webp/gallery/2.jpg","",""))
        var post3 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath3","https://www.gstatic.com/webp/gallery/3.jpg","",""))
        var post4 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath4","https://www.gstatic.com/webp/gallery/4.jpg","",""))
        var post5 = Posts("Temp1","M. Hari prasath1","temp test","#veha,#salvationlamb","1","https://www.gstatic.com/webp/gallery/1.jpg","12","false","1234","", PostUser("","M. Hari prasath5","https://www.gstatic.com/webp/gallery/5.jpg","",""))

        var posts : ArrayList<Posts> = ArrayList()
        posts.add(post1)
        posts.add(post2)
        posts.add(post3)
        posts.add(post4)
        posts.add(post5)
*/

        //list.layoutManager = LinearLayoutManager(activity)
        //list.adapter = HomeAdapter(posts, contexts,"home")

        postBtn.setOnClickListener {
            postData()
        }

        list.layoutManager = LinearLayoutManager(activity)
        getallLikes()

        return  view
    }

    private fun postData() {
        val data = JsonObject()
        data.addProperty("title",title.text.toString())
        data.addProperty("content",content.text.toString())
        data.addProperty("tags","test")
        data.addProperty("userId",Util.userId)
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(viewLifecycleOwner) {
            //Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it","post",data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val ft: FragmentTransaction = requireFragmentManager().beginTransaction()
                            ft.setReorderingAllowed(false)
                            ft.detach(this@HomeFragment).attach(this@HomeFragment).commit()
                            title.text.clear()
                            content.text.clear()
                            //val resp = response.body()
                            //val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            //Log.e("Status", resp?.get("status").toString())
                            //Log.e("result", resp?.get("results").toString())
                            //Log.e("result", loginresp.toString())


                            /*val postlist: ArrayList<Posts> = ArrayList()

                            for (post in loginresp){
                                val pos = Gson().fromJson(post,Posts::class.java)
                                postlist.add(pos)
                            }*/
                            //Log.e("postlist",postlist.toString())
                        }
                        else{
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("result", errorMessage)
                            list.visibility = View.GONE
                            nodata.visibility = View.VISIBLE
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    /*fun richTextEditer (view: View){
        mEditor.setEditorHeight(400)
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
    }*/
    fun getfavPosts(context: Context){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(viewLifecycleOwner) {
            //Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                Log.e("favvvv",(type == "fav").toString())
                val call: Call<JsonObject?>? = retrofit.getMyFav("Bearer $it",Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            val postlist: ArrayList<Posts> = ArrayList()

                            for (post in loginresp){
                                val pos = Gson().fromJson(post,FavPost::class.java)
                                postlist.add(pos.posts)
                            }
                            if (postlist.size <= 0){
                                list.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                list.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                list.adapter = HomeAdapter(postlist, context,"home",myLikesMap,myFollowMap,myFavMap,this@HomeFragment)
                            }
                            Log.e("postlist",postlist.toString())
                        }
                        else{
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("result", errorMessage)
                            list.visibility = View.GONE
                            nodata.visibility = View.VISIBLE
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }

    }
    fun getallPosts(context: Context){
        var count: Int
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(viewLifecycleOwner) {
            //Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                Log.e("favvvv",(type == "fav").toString())
                val call: Call<JsonObject?>? = retrofit.getPost("Bearer $it",page,10)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            count = Integer.parseInt(resp?.get("count").toString())
                            count /= 10
                            //Log.e("Status", resp?.get("status").toString())
                            //Log.e("result", resp?.get("results").toString())
                            //Log.e("result", loginresp.toString())


                            val postlist: ArrayList<Posts> = ArrayList()

                            for (post in loginresp){
                                val pos = Gson().fromJson(post,Posts::class.java)
                                postlist.add(pos)
                            }
                            if (postlist.size <= 0){
                                list.visibility = View.GONE
                                nodata.visibility = View.VISIBLE
                            } else{
                                list.visibility = View.VISIBLE
                                nodata.visibility = View.GONE
                                list.adapter = HomeAdapter(postlist, context,"home",myLikesMap,myFollowMap,myFavMap,this@HomeFragment)
                                list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                                    override fun onScrollStateChanged(recyclerView: RecyclerView, dx: Int) {
                                        if (!recyclerView.canScrollVertically(1)) {
                                            if(count > page){
                                                page++
                                                getallPosts(context)
                                            }
                                            //Toast.makeText(context, "Last", Toast.LENGTH_LONG).show();

                                        }
                                    }
                                })
                            }
                            Log.e("postlist",postlist.toString())
                        }
                        else{
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(),JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("result", errorMessage)
                            list.visibility = View.GONE
                            nodata.visibility = View.VISIBLE
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }

    }
    fun getallLikes(){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(viewLifecycleOwner) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getUserLikes("Bearer $it",Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)

                            for (likes in loginresp){
                                val pos = Gson().fromJson(likes,PostLikes::class.java)
                                likeslist.add(pos)
                                myLikes += pos.postId + " , "
                                myLikesMap.put(pos.postId,pos.reaction)
                            }
                            Log.e("postlist",likeslist.toString())
                        }

                        getallFav()
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun getallFollowers(){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(viewLifecycleOwner) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFollowers("Bearer $it",Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)

                            for (likes in loginresp){
                                val pos = Gson().fromJson(likes, AllFollowerList::class.java)
                                myFollowMap.put(pos.followerId,pos.id)
                                //Log.e("MYFOLLOWMAP", pos.followerId+"-----"+pos.id)
                            }
                        }
                        if (type == "fav") getfavPosts(contexts) else getallPosts(contexts)
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun getallFav(){
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(viewLifecycleOwner) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getFav("Bearer $it",Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            val resp = response.body()
                            val loginresp: JsonArray = Gson().fromJson(resp?.get("results"), JsonArray::class.java)
                            for (likes in loginresp){
                                val pos = Gson().fromJson(likes, AllFavList::class.java)
                                //Log.e("MYLIKES all",pos.toString())
                                myFavMap.put(pos.postId,pos.userId)
                            }
                        }
                        getallFollowers()
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ","Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun getMyDetails() {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(viewLifecycleOwner) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Log.e("userrrrr",resp.toString())
                            val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                            Util.user = loginresp
                            val isWarrior: Boolean = loginresp.isWarrior.isNullOrEmpty() || loginresp.isWarrior != "false"
                            userType = if(isWarrior) Util.WARRIOR else Util.USER
                            showCreatePost = userType == Util.WARRIOR
                            if(showCreatePost) {
                                createpostLinear.visibility = View.VISIBLE
                            } else{
                                createpostLinear.visibility = View.GONE
                            }
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            } else {
                Toast.makeText(contexts,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(contexts, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
}