package com.example.fbproject

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.util.UserPreferences
import com.example.util.UserRslt
import com.example.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_edit_profile.email
import kotlinx.android.synthetic.main.activity_edit_profile.gender
import kotlinx.android.synthetic.main.activity_edit_profile.mobile
import kotlinx.android.synthetic.main.activity_edit_profile.name
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_register.date
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import java.io.*


class EditProfileActivity : AppCompatActivity() {
    private var image : String? = ""
    private var nameStr : String? = ""
    private var addressStr : String? = ""
    private var emailStr : String? = ""
    private var dateOfBirth : String? = ""
    private var genderStr : String? = ""
    private var passwordstr : String? = ""
    private var profilestr : String? = ""
    private var genderbtn : Int = -1
    private var mobileStr : String? = ""
    lateinit var saveBtn : Button
    lateinit var warrior : CheckBox
    lateinit var cancelBtn : Button
    private val year = 0
    private  var month:Int = 0
    private  var day:Int = 0
    private lateinit var userPreferences: UserPreferences
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
            Log.e("image",data!!.data.toString())
            /*val imageStream = data.data?.let { this.contentResolver.openInputStream(it) }
            val yourSelectedImage = BitmapFactory.decodeStream(imageStream)*/
            var bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, data.data)
            profilestr = encodeTobase64(bitmap)
            }else if (requestCode == 150) {
            Log.e("image",data!!.data.toString())
            /*val imageStream = data.data?.let { this.contentResolver.openInputStream(it) }
            val yourSelectedImage = BitmapFactory.decodeStream(imageStream)*/
            var bitmap = data.extras!!["data"] as Bitmap?
            val bytes = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
            profilestr = encodeTobase64(bitmap)
            }
        }
    fun encodeTobase64(image: Bitmap): String? {
        val immagex = image
        val baos = ByteArrayOutputStream()
        immagex.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        val imageEncoded = Base64.encodeToString(b, Base64.DEFAULT)
        Log.e("LOOK", imageEncoded)
        return imageEncoded
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        userPreferences = UserPreferences(this)
        saveBtn = findViewById(R.id.save_btn)
        cancelBtn = findViewById(R.id.cancel_btn)
        warrior = findViewById(R.id.warrior)
        getmyDetails(this)

        val dropdown = findViewById<Spinner>(R.id.country)
        val items = Util.getCountries()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter



        profile_pic_edit.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
            builder.setMessage("Do you want to edit")
            builder.setTitle("Alert !")
            builder.setCancelable(false)
            builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                val items = arrayOf<CharSequence>(
                "Take Photo", "Choose from Library",
                "Cancel"
            )

            val builder1 = AlertDialog.Builder(this@EditProfileActivity)
            builder1.setTitle("Add Photo!")
            builder1.setItems(items) { dialog, item ->
                val result = true
                    //com.sun.org.apache.bcel.internal.classfile.Utility.checkPermission(this@EditProfileActivity)
                if (items[item] == "Take Photo") {
                    if (result) cameraIntent()
                } else if (items[item] == "Choose from Library") {
                    if (result) galleryIntent()
                } else if (items[item] == "Cancel") {
                    dialog.dismiss()
                }
            }
            builder1.show()
            }
            builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
        saveBtn.setOnClickListener{
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
            builder.setMessage("Do you want to edit")
            builder.setTitle("Alert !")
            builder.setCancelable(false)
            builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                val data = JsonObject()
                data.addProperty("name",name.text.toString())
                data.addProperty("email",email.text.toString())
                data.addProperty("mobile",mobile.text.toString())
                data.addProperty("gender",findViewById<RadioButton>(gender.checkedRadioButtonId).text.toString().toLowerCase())
                //data.addProperty("password",password.text.toString())
                data.addProperty("dateOfBirth",date.text.toString())
                data.addProperty("picture",profilestr)
                data.addProperty("isWarrior",warrior.isChecked)
                edit(data)
                finish()
            }
            builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
        cancelBtn.setOnClickListener{finish()}
        
        warrior.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
                builder.setMessage("Make me warrior")
                builder.setTitle("Alert !")
                builder.setCancelable(false)
                builder.setPositiveButton("I agree") { _: DialogInterface?, _: Int -> warrior.isChecked = true}
                builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> warrior.isChecked = true
                    dialog.cancel() }

                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
                Toast.makeText(this@EditProfileActivity, "You Clicked ", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun edit(data: JsonObject) {
        userPreferences = UserPreferences(this@EditProfileActivity)
        Log.e("dataaa",data.toString())
        val retrofit = Util.getRetrofit()

        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {

                val call: Call<JsonObject?>? = retrofit.putUser("Bearer $it",Util.userId, data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Log.e("respppppp",resp.toString())
                            Log.e("respppppp",resp?.get("result").toString())
                            /*val registerResp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                    Util.user = registerResp*/
                            /*val token = resp?.get("token").toString()
                    lifecycleScope.launch {
                        userPreferences.saveAuthToken(token)
                        userPreferences.saveUserId(registerResp.id)
                    }

                    Log.e("Status", resp?.get("status").toString())
                    Log.e("result", resp?.get("result").toString())
                    Log.e("result", registerResp.toString())*/
                            /*val intent = Intent(this@EditProfileActivity, MainActivity::class.java)
                            startActivity(intent)*/
                        } else {
                            val resp = response.errorBody()
                            val registerResp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                            val status = registerResp.get("status").toString()
                            val errorMessage = registerResp.get("errorMessage").toString()
                            Log.e("respppStatus", status)
                            Log.e("respppresult", errorMessage)
                            Toast.makeText(this@EditProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("responseee", "fail")
                    }
                })
            }
        }
    }
    private fun getmyDetails(context: Context) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            Log.e("token################", it)
            if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            Log.e("userrrrr",resp.toString())
                            val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                            if (!loginresp.picture.isNullOrEmpty()){
                                Picasso.with(context).load(loginresp.picture).into(profile_pic_edit)
                            }
                            if(!TextUtils.isEmpty(loginresp.name)) name.text = Editable.Factory.getInstance().newEditable(loginresp.name)
                            if(!TextUtils.isEmpty(loginresp.address)) address.text = Editable.Factory.getInstance().newEditable(loginresp.address)
                            if(!TextUtils.isEmpty(loginresp.email)) email.text = Editable.Factory.getInstance().newEditable(loginresp.email)
                            if(!TextUtils.isEmpty(loginresp.mobile)) mobile.text = Editable.Factory.getInstance().newEditable(loginresp.mobile)
                            if(!TextUtils.isEmpty(loginresp.dateOfBirth)) date.text = Editable.Factory.getInstance().newEditable(loginresp.dateOfBirth)
                            if(!TextUtils.isEmpty(loginresp.gender)){
                                when(loginresp.gender) {
                                    "male" -> genderbtn = R.id.male
                                    "female" -> genderbtn = R.id.female
                                }
                                gender.check(genderbtn)
                            }
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            } else {
                Toast.makeText(this@EditProfileActivity,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(this@EditProfileActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun getData() {
        /*Log.e("user@@@@@@@@@",Util.user.toString())
        nameStr = if(!TextUtils.isEmpty(Util.user.name)) Util.user.name else "Hari"
        addressStr = if(!TextUtils.isEmpty(Util.user.address)) Util.user.address else "Madurai"
        emailStr = if(!TextUtils.isEmpty(Util.user.email)) Util.user.email else "temp@temp.com"
        mobileStr = if(!TextUtils.isEmpty(Util.user.mobile)) Util.user.mobile else "9876543211"
        image = if(!TextUtils.isEmpty(Util.user.picture)) Util.user.picture else "https://www.gstatic.com/webp/gallery/1.jpg"
        dateOfBirth = if(!TextUtils.isEmpty(Util.user.dateOfBirth)) Util.user.dateOfBirth else "22-10-1998"
        genderStr = if(!TextUtils.isEmpty(Util.user.gender)) Util.user.gender else "male"*/

        Picasso.with(this).load(image).into(profile_pic_edit)
        name.text = Editable.Factory.getInstance().newEditable(nameStr)
        address.text = Editable.Factory.getInstance().newEditable(addressStr)
        email.text = Editable.Factory.getInstance().newEditable(emailStr)
        mobile.text = Editable.Factory.getInstance().newEditable(mobileStr)
        date.text = Editable.Factory.getInstance().newEditable(dateOfBirth)
        when(genderStr) {
            "male" -> genderbtn = R.id.male
            "female" -> genderbtn = R.id.female
        }
        gender.check(genderbtn)
    }
    fun setDate(view: View?) {
        showDialog(999)
    }

    override fun onCreateDialog(id: Int): Dialog? {
        return if (id == 999) {
            DatePickerDialog(
                this,
                myDateListener, year, month, day
            )
        } else null
    }
    private val myDateListener =
        DatePickerDialog.OnDateSetListener { _, year, month, day ->
            date?.text = StringBuilder().append(day).append("-")
                .append(month).append("-").append(year)
        }
}