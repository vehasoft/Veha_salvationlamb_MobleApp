package com.veha.activity

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.veha.activity.R
import com.veha.util.Commons
import com.veha.util.UserPreferences
import com.veha.util.UserRslt
import com.veha.util.Util
import com.google.gson.Gson
import com.google.gson.JsonObject
import dmax.dialog.SpotsDialog
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class AboutActivity : AppCompatActivity() {

    private lateinit var name: TextView
    private lateinit var dob: TextView
    private lateinit var phone: TextView
    private lateinit var gender: TextView
    private lateinit var address: TextView
    private lateinit var email: TextView
    private lateinit var join: TextView
    private lateinit var edit: ImageButton
    private lateinit var changePass: Button

    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        userPreferences = UserPreferences(this)
        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        name = findViewById(R.id.about_name)
        dob = findViewById(R.id.about_dob)
        phone = findViewById(R.id.about_phone)
        gender = findViewById(R.id.about_gender)
        address = findViewById(R.id.about_address)
        email = findViewById(R.id.about_email)
        join = findViewById(R.id.about_join)
        edit = findViewById(R.id.edit)
        changePass = findViewById(R.id.change_pwd_btn)
        getmyDetails()
        edit.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
        changePass.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getmyDetails() {
        try {
            if (Commons().isNetworkAvailable(this)) {
                if (!dialog.isShowing) {
                    dialog.show()
                }
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", Util.userId)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val loginResp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                                    var add = ""
                                    gender.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                                    if (!TextUtils.isEmpty(loginResp.name)) name.text = loginResp.name
                                    if (!TextUtils.isEmpty(loginResp.dateOfBirth)) dob.text =
                                        Util.formatDate(loginResp.dateOfBirth, "dd MMMM yyyy")
                                    if (!TextUtils.isEmpty(loginResp.mobile)) phone.text = loginResp.mobile
                                    if (!TextUtils.isEmpty(loginResp.gender)) gender.text =
                                        loginResp.gender.capitalize()
                                    if (!TextUtils.isEmpty(loginResp.address)) add = "${loginResp.address}, "
                                    if (!TextUtils.isEmpty(loginResp.city)) add += "${loginResp.city}"
                                    if (!TextUtils.isEmpty(loginResp.pinCode)) add += " - ${loginResp.pinCode}\n"
                                    if (!TextUtils.isEmpty(loginResp.state)) add += "${loginResp.state}, "
                                    if (!TextUtils.isEmpty(loginResp.country)) add += "${loginResp.country}"
                                    address.text = add
                                    if (!TextUtils.isEmpty(loginResp.email)) email.text = loginResp.email
                                    if (!TextUtils.isEmpty(loginResp.createdAt)) join.text =
                                        Util.formatDate(loginResp.createdAt, "dd MMMM yyyy")

                                }
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                if (dialog.isShowing) {
                                    dialog.dismiss()
                                }
                                Log.e("AboutActivity.getMyDetails", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@AboutActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@AboutActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AboutActivity.getMyDetails", e.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}