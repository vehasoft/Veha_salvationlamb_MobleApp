package com.veha.activity


import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.veha.util.Commons
import com.veha.util.UserRslt
import com.veha.util.Util
import com.veha.util.UserPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response


class RegisterActivity : AppCompatActivity() {
    lateinit var userPreferences: UserPreferences
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
    private var month: Int = 0
    private var day: Int = 1

    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var mobile: TextInputEditText
    private lateinit var date: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var signInButton: Button
    private lateinit var gender: RadioGroup
    private lateinit var termsAndConditions: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        firstName = findViewById(R.id.fname)
        lastName = findViewById(R.id.lname)
        email = findViewById(R.id.email)
        mobile = findViewById(R.id.mobile)
        date = findViewById(R.id.date)
        password = findViewById(R.id.password)
        registerButton = findViewById(R.id.register_btn)
        signInButton = findViewById(R.id.signin_btn)
        termsAndConditions = findViewById(R.id.terms_conditions)
        gender = findViewById(R.id.gender)

        registerButton.setOnClickListener {
            fNameTxt = firstName.text.toString()
            lNameTxt = lastName.text.toString()
            emailTxt = email.text.toString()
            mobileTxt = mobile.text.toString()
            passwordTxt = password.text.toString()
            genderId = gender.checkedRadioButtonId
            dobTxt = date.text.toString()

            if (doValidation() == SUCCESS) {
                registerButton.isEnabled = false
                val data = JsonObject()
                data.addProperty("firstName", fNameTxt)
                data.addProperty("lastName", lNameTxt)
                data.addProperty("name", "$fNameTxt $lNameTxt")
                data.addProperty("email", emailTxt)
                data.addProperty("mobile", mobileTxt)
                data.addProperty("gender", findViewById<RadioButton>(genderId).text.toString().toLowerCase())
                data.addProperty("password", passwordTxt)
                data.addProperty("dateOfBirth", Util.formatDate(dobTxt, "MM-dd-yyyy","dd-MM-yyyy"))
                data.addProperty("isWarrior", false)
                data.addProperty("isFreshUser", true)
                register(data)
            } else {
                Toast.makeText(this, doValidation(), Toast.LENGTH_LONG).show()
            }
        }
        signInButton.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
        }
        termsAndConditions.setOnClickListener {
            val intent = Intent(this@RegisterActivity, WebViewActivity::class.java)
            intent.putExtra("WebPageName", "terms")
            startActivity(intent)
        }
    }

    fun setDate(view: View?) {
        showDialog(999)
    }

    override fun onCreateDialog(id: Int): Dialog? {
        return if (id == 999) {
            val datePickerDialog = DatePickerDialog(
                this, android.R.style.Theme_Holo_Dialog_MinWidth,
                myDateListener, year, month, day
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis() - 1000
            datePickerDialog
        } else null
    }

    private val myDateListener =
        OnDateSetListener { _, year, month, day ->
            date?.text = Editable.Factory.getInstance().newEditable(StringBuilder().append(day).append("-")
                .append(month + 1).append("-").append(year))
        }

    private fun doValidation(): String {
        if (TextUtils.isEmpty(fNameTxt.trim())) {
            firstName.error = "Enter name"
            return "Enter first name"
        } else if (!Util.isValidName(fNameTxt)) {
            firstName.error = "Enter valid name"
            return "Enter valid name"
        } else if (TextUtils.isEmpty(emailTxt.trim())) {
            email.error = "Enter email"
            return "Enter email"
        } else if (!Util.isValidEmail(emailTxt)) {
            email.error = "Invalid Email"
            return "Invalid Email"
        } else if (TextUtils.isEmpty(mobileTxt.trim())) {
            mobile.error = "Enter mobile number"
            return "Enter mobile number"
        } else if (!Util.isValidMobile(mobileTxt.trim())) {
            mobile.error = "Enter valid mobile number"
            return "Enter valid mobile number"
        } else if (TextUtils.isEmpty(passwordTxt.trim())) {
            password.error = "Enter Password"
            return "Enter Password"
        } else if (!Util.isValidPassword(passwordTxt)) {
            password.error = "Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8"
            return "Password must contain 1 capital, 1 small, 1 number, 1 spl char and length greater than 8"
        } else if (TextUtils.isEmpty(dobTxt.trim()) || dobTxt.equals("Date of birth", true)) {
            date.error = "Select Date of birth"
            return "Select Date of birth"
        } else if (genderId == -1 || TextUtils.isEmpty(genderId.toString())) {
            return "Select Gender"
        }

        return SUCCESS
    }

    private fun register(data: JsonObject) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                userPreferences = UserPreferences(this@RegisterActivity)
                val retrofit = Util.getRetrofit()
                val call: Call<JsonObject?>? = retrofit.postCall("users", data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val registerResp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                            Util.user = registerResp
                            val intent = Intent(this@RegisterActivity, ForgotPasswordActivity::class.java)
                            intent.putExtra("page", "verify")
                            intent.putExtra("email", emailTxt)
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
                        Log.e("REGISTER", "fail")
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("REGISTER", e.toString())
        } finally {
            registerButton.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}