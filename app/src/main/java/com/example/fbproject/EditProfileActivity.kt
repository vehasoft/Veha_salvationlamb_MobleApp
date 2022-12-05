package com.example.fbproject

import android.app.DatePickerDialog
import android.app.Dialog
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
import com.example.util.Util
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_register.date

class EditProfileActivity : AppCompatActivity() {
    private var image : String? = null
    private var nameStr : String? = null
    private var addressStr : String? = null
    private var emailStr : String? = null
    private var dateOfBirth : String? = null
    private var genderStr : String? = null
    private var genderbtn : Int = -1
    private var mobileStr : String? = null
    lateinit var saveBtn : Button
    lateinit var cancelBtn : Button
    private val year = 0
    private  var month:Int = 0
    private  var day:Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        saveBtn = findViewById(R.id.save_btn)
        cancelBtn = findViewById(R.id.cancel_btn)
        getData()
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
    fun getData() {
        Log.e("user@@@@@@@@@",Util.user.toString())
        nameStr = if(!TextUtils.isEmpty(Util.user.name)) Util.user.name else "Hari"
        addressStr = if(!TextUtils.isEmpty(Util.user.address)) Util.user.address else "Madurai"
        emailStr = if(!TextUtils.isEmpty(Util.user.email)) Util.user.email else "temp@temp.com"
        mobileStr = if(!TextUtils.isEmpty(Util.user.mobile)) Util.user.mobile else "9876543211"
        image = if(!TextUtils.isEmpty(Util.user.picture)) Util.user.picture else "https://www.gstatic.com/webp/gallery/1.jpg"
        dateOfBirth = if(!TextUtils.isEmpty(Util.user.dateOfBirth)) Util.user.dateOfBirth else "22-10-1998"
        genderStr = if(!TextUtils.isEmpty(Util.user.gender)) Util.user.gender else "male"

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