package com.example.fbproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.ImageView
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso

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
}