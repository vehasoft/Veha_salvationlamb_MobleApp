package com.example.fbproject

import android.app.DatePickerDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_register.*

class EditProfileActivity : AppCompatActivity() {
    lateinit var image : String
    lateinit var name : String
    lateinit var address : String
    lateinit var email : String
    lateinit var mobile : String
    lateinit var profilePic : ImageView
    lateinit var nameEdit : TextInputEditText
    lateinit var addressEdit : TextInputEditText
    lateinit var emailEdit : TextInputEditText
    lateinit var mobileEdit : TextInputEditText
    lateinit var saveBtn : Button
    lateinit var cancelBtn : Button
    private val year = 0
    private  var month:Int = 0
    private  var day:Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        profilePic = findViewById(R.id.profile_pic_edit)
        nameEdit = findViewById(R.id.name)
        addressEdit = findViewById(R.id.address)
        emailEdit = findViewById(R.id.email)
        mobileEdit = findViewById(R.id.mobile)
        saveBtn = findViewById(R.id.save_btn)
        cancelBtn = findViewById(R.id.cancel_btn)
        getData()
        saveBtn.setOnClickListener(){
            finish()
        }
        cancelBtn.setOnClickListener(){
            finish()
        }

    }
    fun getData() {
        name = "Hari prasath"
        address = "Madurai"
        email = "temp@temp.com"
        mobile = "9876543211"
        image = "https://www.gstatic.com/webp/gallery/1.jpg";

        Picasso.with(this).load(image).into(profilePic)
        nameEdit.text = Editable.Factory.getInstance().newEditable(name)
        addressEdit.text = Editable.Factory.getInstance().newEditable(address)
        emailEdit.text = Editable.Factory.getInstance().newEditable(email)
        mobileEdit.text = Editable.Factory.getInstance().newEditable(mobile)
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
        DatePickerDialog.OnDateSetListener { arg0, year, month, day ->
            date?.text = StringBuilder().append(day).append("-")
                .append(month).append("-").append(year)
        }
}