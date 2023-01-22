package com.example.fbproject


import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.activity_register.name
import retrofit2.Call
import retrofit2.Response


class RegisterActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
    private val SUCCESS = "success"
    private lateinit var nameTxt: String
    private lateinit var emailTxt: String
    private lateinit var mobileTxt: String
    private lateinit var passwordTxt: String
    private lateinit var genderTxt: String
    private var genderId: Int = -1
    private lateinit var dobTxt: String
    private val year = 0
    private  var month:Int = 0
    private  var day:Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_btn.setOnClickListener {
            nameTxt = name.text.toString()
            emailTxt = email.text.toString()
            mobileTxt = mobile.text.toString()
            passwordTxt = password.text.toString()
            genderId = gender.checkedRadioButtonId
            dobTxt = date.text.toString()

            if (doValidation() == SUCCESS){
                val data = JsonObject()
                data.addProperty("name",nameTxt)
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
    }
    fun setDate(view: View?) {
        showDialog(999)
        Log.e("dateeeee",date.text.toString())
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
        OnDateSetListener { _, year, month, day ->
            date?.text = StringBuilder().append(year).append("-")
                .append(month+1).append("-").append(day)
        }
    private fun doValidation(): String{
        if(TextUtils.isEmpty(nameTxt.trim())){
            name.error = "Enter name"
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
        userPreferences = UserPreferences(this@RegisterActivity)
        val retrofit = Util.getRetrofit()
        val call: Call<JsonObject?>? = retrofit.postCall("users",data)
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
            }

            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Log.e("responseee", "fail")
            }
        })
    }
}