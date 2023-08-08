package com.veha.activity

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.veha.activity.R
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import java.io.ByteArrayOutputStream

class AddPostActivity : AppCompatActivity() {

    lateinit var postBtn: Button
    lateinit var imgPostBtn: Button
    lateinit var postPic: ImageView
    lateinit var title: EditText
    lateinit var content: EditText
    lateinit var video: EditText
    lateinit var tags: EditText
    lateinit var postType: RadioGroup
    lateinit var postImage: RadioButton
    lateinit var postVideo: RadioButton
    var postPicStr = ""
    var postTypeStr = "text"

    lateinit var userPreferences: UserPreferences
    lateinit var dialog: android.app.AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)

        userPreferences = UserPreferences(this@AddPostActivity)

        postBtn = findViewById(R.id.post_btn)
        imgPostBtn = findViewById(R.id.add_img_btn)
        postPic = findViewById(R.id.post_img)
        title = findViewById(R.id.title)
        content = findViewById(R.id.content)
        video = findViewById(R.id.video)
        tags = findViewById(R.id.tags)
        postType = findViewById(R.id.post_type)
        postImage = findViewById(R.id.image_btn)
        postVideo = findViewById(R.id.video_btn)
        postImage.setOnClickListener {
            addImg()
        }

        postType.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.image_btn){
                postTypeStr = "image"

            } else if(checkedId == R.id.video_btn){
                video.visibility = View.VISIBLE
                postPic.visibility = View.GONE
                postTypeStr = "video"
            }
        }
        /*postPic.setOnClickListener {

        }*/

        postBtn.isEnabled = true
        postBtn.setOnClickListener {
            if (postPicStr.isNullOrEmpty() && postTypeStr.contentEquals("image")){
                postTypeStr = "text"
            }
            if (content.text.toString().trim().isNullOrEmpty() && postTypeStr.contentEquals("text")){
                content.error = "Content must not be empty"
            }else {
                postBtn.isEnabled = false
                val data = JsonObject()
                data.addProperty("title",title.text.toString())
                data.addProperty("content",content.text.toString())
                data.addProperty("tags",tags.text.toString())
                data.addProperty("image",postPicStr)
                data.addProperty("url",video.text.toString())
                data.addProperty("type",postTypeStr)
                data.addProperty("userId", Util.userId)
                postData(data)
            }
        }
        imgPostBtn.setOnClickListener {
            addImg()
        }
    }
    private fun postData(data: JsonObject) {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            Log.e("data",data.toString())
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(this) {
                if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                    val call1: Call<JsonObject?>? = retrofit.postCallHead("Bearer $it", "post", data)
                    call1!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            if (response.code() == 200) {
                                title.text.clear()
                                content.text.clear()
                                postBtn.isEnabled = true
                                val intent = Intent(this@AddPostActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
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
                            call1.cancel()
                        }

                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            Log.e("AddPostActivity.postData", "fail")
                        }
                    })
                } else {
                    Toast.makeText(
                        this@AddPostActivity,
                        "Somthing Went Wrong \nLogin again to continue",
                        Toast.LENGTH_LONG
                    ).show()
                    lifecycleScope.launch {
                        userPreferences.deleteAuthToken()
                        userPreferences.deleteUserId()
                    }
                    val intent = Intent(this@AddPostActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
    private fun addImg(){
        val result = ContextCompat.checkSelfPermission(applicationContext,READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (result){
            val items = arrayOf<CharSequence>(
                "Take Photo", "Choose from Gallery",
                "Cancel"
            )
            val builder1 = AlertDialog.Builder(this@AddPostActivity)
            builder1.setTitle("Add Picture!")
            builder1.setItems(items) { dialog, item ->
                if (items[item] == "Take Photo") {
                    cameraIntent()
                } else if (items[item] == "Choose from Gallery") {
                    galleryIntent()
                } else if (items[item] == "Cancel") {
                    dialog.dismiss()
                }

            }
            builder1.show()
        } else {
            val builder2 = AlertDialog.Builder(this@AddPostActivity)
            builder2.setTitle("Permission Restricted")
            builder2.setMessage("We need CAMERA and STORAGE permission to serve you for updating your profile picture.\nplease enable this permission through your device's settings")
            builder2.setPositiveButton("cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }
            builder2.setNegativeButton("settings") { _: DialogInterface, _: Int ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri: Uri = Uri.fromParts("package", applicationContext.packageName, null)
                intent.data = uri
                this.startActivity(intent)
            }
            builder2.show()
        }
    }
    private fun galleryIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT //
        startActivityForResult(Intent.createChooser(intent, "Select File"), 100)
    }

    private fun cameraIntent() {
        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE //
        startActivityForResult(intent, 150)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (data?.data != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, data.data)
                if (bitmap != null) {
                    postPic.visibility = View.VISIBLE
                    postPic.setImageBitmap(bitmap)
                }
                postPicStr = encodeTobase64(bitmap).toString()
                postTypeStr = "image"
                postPic.visibility = View.VISIBLE
                video.visibility = View.GONE
            }
        } else if (requestCode == 150) {
            if (data?.extras!!["data"] != null) {
                val bitmap = data.extras!!["data"] as Bitmap?
                val bytes = ByteArrayOutputStream()
                bitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
                if (bitmap != null) {
                    postPic.setImageBitmap(bitmap)
                }
                postPicStr = encodeTobase64(bitmap).toString()
                postTypeStr = "image"
                postPic.visibility = View.VISIBLE
                video.visibility = View.GONE
            }
        }
    }

    fun encodeTobase64(image: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }
}