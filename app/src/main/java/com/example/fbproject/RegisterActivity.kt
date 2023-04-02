package com.example.fbproject


import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.util.Commons
import com.example.util.UserRslt
import com.example.util.Util
import com.example.util.UserPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_register.date
import kotlinx.android.synthetic.main.activity_register.email
import kotlinx.android.synthetic.main.activity_register.gender
import kotlinx.android.synthetic.main.activity_register.mobile
import kotlinx.android.synthetic.main.activity_register.fname
import kotlinx.android.synthetic.main.activity_register.lname
import retrofit2.Call
import retrofit2.Response


class RegisterActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    lateinit var dialog: ProgressDialog
    private val SUCCESS = "success"
    private lateinit var fNameTxt: String
    private lateinit var lNameTxt: String
    private lateinit var emailTxt: String
    private lateinit var mobileTxt: String
    private lateinit var passwordTxt: String
    private lateinit var genderTxt: String
    private var genderId: Int = -1
    private lateinit var dobTxt: String
    private val year = 1960
    private  var month:Int = 0
    private  var day:Int = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        dialog = ProgressDialog(this)
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)

        register_btn.setOnClickListener {
            fNameTxt = fname.text.toString()+lname.text.toString()
            lNameTxt = lname.text.toString()+lname.text.toString()
            emailTxt = email.text.toString()
            mobileTxt = mobile.text.toString()
            passwordTxt = password.text.toString()
            genderId = gender.checkedRadioButtonId
            dobTxt = date.text.toString()

            if (doValidation() == SUCCESS){
                val data = JsonObject()
                data.addProperty("firstName",fNameTxt)
                data.addProperty("lastName",fNameTxt)
                data.addProperty("name", fname.text.toString() + " " + lname.text.toString())
                data.addProperty("email",emailTxt)
                data.addProperty("mobile",mobileTxt)
                data.addProperty("gender",findViewById<RadioButton>(genderId).text.toString().toLowerCase())
                data.addProperty("password",passwordTxt)
                data.addProperty("dateOfBirth",dobTxt)
                data.addProperty("isWarrior",false)
                register(data)
            }
            else {
                Toast.makeText(this,doValidation(),Toast.LENGTH_LONG).show()
            }
        }
        signin_btn.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
        }
        terms_conditions.setOnClickListener {

            val intent = Intent(this@RegisterActivity, WebViewActivity::class.java)
            intent.putExtra("WebPageName","terms")
            startActivity(intent)
        }
    }
    fun setDate(view: View?) {
        showDialog(999)
    }

    override fun onCreateDialog(id: Int): Dialog? {
        return if (id == 999) {
            val datePickerDialog = DatePickerDialog(
                this,android.R.style.Theme_Holo_Dialog_MinWidth,
                myDateListener, year, month, day
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis() - 1000
            datePickerDialog
        } else null
    }

    private val myDateListener =
        OnDateSetListener { _, year, month, day ->
            date?.text = StringBuilder().append(day).append("-")
                .append(month+1).append("-").append(year)
        }
    private fun doValidation(): String{
        if(TextUtils.isEmpty(fNameTxt.trim())){
            fname.error = "Enter name"
            return "error"
        } else if(TextUtils.isEmpty(emailTxt.trim())){
            email.error ="Enter email"
            return "error"
        } else if(TextUtils.isEmpty(mobileTxt.trim())){
            mobile.error = "Enter mobile number"
            return "error"
        } else if(TextUtils.isEmpty(passwordTxt.trim())){
            password.error =  "Enter Password"
            return "error"
        } else if(TextUtils.isEmpty(dobTxt.trim()) || dobTxt.equals("Date of birth",true)){
            date.error = "Select Date of birth"
            return "error"
        } else if(genderId == -1 || TextUtils.isEmpty(genderId.toString())){
            return "Select Gender"
        } else if(!Util.isValidEmail(emailTxt)){
            email.error = "Invalid Email"
            return "error"
        } else if(!Util.isValidPassword(passwordTxt)){
            password.error = "Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8"
            return "error"
        }

        return SUCCESS
    }
    private fun register(data: JsonObject) {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            userPreferences = UserPreferences(this@RegisterActivity)
            val retrofit = Util.getRetrofit()
            val call: Call<JsonObject?>? = retrofit.postCall("users", data)
            call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.code() == 200) {
                        val resp = response.body()
                        val registerResp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                        Util.user = registerResp
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                    } else {
                        val resp = response.errorBody()
                        val registerResp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                        val status = registerResp.get("status").toString()
                        val errorMessage = registerResp.get("errorMessage").toString()
                        Log.e("Status", status)
                        Log.e("result", errorMessage)
                        Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    Log.e("REGISTER", "fail")
                }
            })
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}