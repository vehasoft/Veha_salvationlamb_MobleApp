package com.example.fbproject

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.util.UserPreferences
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class AddPostActivity : AppCompatActivity() {

    lateinit var postBtn: Button
    lateinit var title: EditText
    lateinit var content: EditText
    lateinit var tags: EditText

    lateinit var userPreferences: UserPreferences
    lateinit var dialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        dialog = ProgressDialog(this)
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)

        userPreferences = UserPreferences(this@AddPostActivity)

        postBtn = findViewById(R.id.post_btn)
        title = findViewById(R.id.title)
        content = findViewById(R.id.content)
        tags = findViewById(R.id.tags)
        postBtn.isEnabled = true
        postBtn.setOnClickListener {
            if (content.text.toString().trim().isNullOrEmpty()){
                content.error = "Content must not be empty"
            }else {
                postBtn.isEnabled = false
                val data = JsonObject()
                data.addProperty("title",title.text.toString())
                data.addProperty("content",content.text.toString())
                data.addProperty("tags",tags.text.toString())
                data.addProperty("userId", Util.userId)
                postData(data)
            }
        }
    }
    private fun postData(data: JsonObject) {
        if (!dialog.isShowing) {
            dialog.show()
        }
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it","post",data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code()==200){
                            title.text.clear()
                            content.text.clear()
                            postBtn.isEnabled = true
                            val intent = Intent(this@AddPostActivity, MainActivity::class.java)
                            startActivity(intent)
                        }
                        else{
                            postBtn.isEnabled = true
                            val resp = response.errorBody()
                            val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val status = loginresp.get("status").toString()
                            val errorMessage = loginresp.get("errorMessage").toString()
                            Log.e("Status", status)
                            Log.e("result", errorMessage)
                        }
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                        Toast.makeText(this@AddPostActivity, "No Internet", Toast.LENGTH_LONG).show()
                        Log.e("responseee", "fail")
                    }
                })
            } else {
                Toast.makeText(this@AddPostActivity,"Somthing Went Wrong \nLogin again to continue", Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(this@AddPostActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}