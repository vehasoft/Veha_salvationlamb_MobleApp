package com.example.fbproject

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.asLiveData
import com.example.util.Util
import com.example.util.UserPreferences
import com.example.util.UserRslt
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_register.date
import retrofit2.Call
import retrofit2.Response

class EditProfileActivity : AppCompatActivity() {
    private var image : String? = ""
    private var nameStr : String? = ""
    private var addressStr : String? = ""
    private var emailStr : String? = ""
    private var dateOfBirth : String? = ""
    private var genderStr : String? = ""
    private var genderbtn : Int = -1
    private var mobileStr : String? = ""
    lateinit var saveBtn : Button
    lateinit var cancelBtn : Button
    private val year = 0
    private  var month:Int = 0
    private  var day:Int = 0
    private lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        userPreferences = UserPreferences(this)
        saveBtn = findViewById(R.id.save_btn)
        cancelBtn = findViewById(R.id.cancel_btn)
        getmyDetails(this)
        saveBtn.setOnClickListener(){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
            builder.setMessage("Do you want to edit")
            builder.setTitle("Alert !")
            builder.setCancelable(false)
            builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish()}
            builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
            finish()
        }
        cancelBtn.setOnClickListener{finish()}

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
                            if (loginresp.picture.isNullOrEmpty()){
                                Picasso.with(context).load("https://www.gravatar.com/avatar/205e460b479e2e5b48aec07710c08d50").into(profile_pic_edit)
                            }else {
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
                                    "other" -> genderbtn = R.id.other
                                }
                                gender.check(genderbtn)
                            }
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
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
            "other" -> genderbtn = R.id.other
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