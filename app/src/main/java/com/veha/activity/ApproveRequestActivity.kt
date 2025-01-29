package com.veha.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import com.veha.util.Commons
import com.veha.util.ProfileChange
import com.veha.util.UserPreferences
import com.veha.util.UserRslt
import com.veha.util.Util
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApproveRequestActivity : AppCompatActivity() {
    lateinit var exName: TextView
    lateinit var newName: TextView
    lateinit var exFname: TextView
    lateinit var newFname: TextView
    lateinit var close: ImageButton
    lateinit var exLname: TextView
    lateinit var newLname: TextView
    lateinit var exGender: TextView
    lateinit var newGender: TextView
    lateinit var exEmail: TextView
    lateinit var newEmail: TextView
    lateinit var exMobile: TextView
    lateinit var newMobile: TextView
    lateinit var exDOB: TextView
    lateinit var newDOB: TextView
    lateinit var exAddress: TextView
    lateinit var newAddress: TextView
    lateinit var exCity: TextView
    lateinit var newCity: TextView
    lateinit var exState: TextView
    lateinit var newState: TextView
    lateinit var exCountry: TextView
    lateinit var newCountry: TextView
    lateinit var exPincode: TextView
    lateinit var newPincode: TextView
    lateinit var exLang: TextView
    lateinit var newLang: TextView
    lateinit var exReligion: TextView
    lateinit var newReligion: TextView
    lateinit var exChurch: TextView
    lateinit var newChurch: TextView
    lateinit var exGift: TextView
    lateinit var newGift: TextView
    lateinit var newIsWarrior: TextView
    lateinit var exIsWarrior: TextView
    lateinit var newpic: ImageView
    lateinit var expic: ImageView
    lateinit var profile: ImageView
    lateinit var approve: Button
    lateinit var reject: Button
    lateinit var logo: ImageView

    private lateinit var userPreferences: UserPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_approve_request)
        userPreferences = UserPreferences(this)
        exName = findViewById(R.id.ex_name)
        close = findViewById(R.id.close)
        newName = findViewById(R.id.new_name)
        exFname = findViewById(R.id.ex_fname)
        newFname = findViewById(R.id.new_fname)
        exLname = findViewById(R.id.ex_lname)
        newLname = findViewById(R.id.new_lname)
        exGender = findViewById(R.id.ex_gender)
        newGender = findViewById(R.id.new_gender)
        exEmail = findViewById(R.id.ex_email)
        newEmail = findViewById(R.id.new_email)
        exMobile = findViewById(R.id.ex_mobile)
        newMobile = findViewById(R.id.new_mobile)
        exAddress = findViewById(R.id.ex_address)
        newAddress = findViewById(R.id.new_address)
        exDOB = findViewById(R.id.ex_dob)
        newDOB = findViewById(R.id.new_dob)
        exCity = findViewById(R.id.ex_city)
        newCity = findViewById(R.id.new_city)
        exState = findViewById(R.id.ex_state)
        newState = findViewById(R.id.new_state)
        exCountry = findViewById(R.id.ex_country)
        newCountry = findViewById(R.id.new_country)
        exPincode = findViewById(R.id.ex_pincode)
        newPincode = findViewById(R.id.new_pincode)
        exLang = findViewById(R.id.ex_language)
        newLang = findViewById(R.id.new_language)
        exChurch = findViewById(R.id.ex_church)
        newChurch = findViewById(R.id.new_church)
        exReligion = findViewById(R.id.ex_religion)
        newReligion = findViewById(R.id.new_religion)
        exGift = findViewById(R.id.ex_gift)
        newGift = findViewById(R.id.new_gift)
        exIsWarrior = findViewById(R.id.ex_warrior)
        newIsWarrior = findViewById(R.id.new_warrior)
        expic = findViewById(R.id.ex_pic)
        profile = findViewById(R.id.profile_pic)
        newpic = findViewById(R.id.new_pic)
        approve = findViewById(R.id.approve)
        reject = findViewById(R.id.reject)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        close.setOnClickListener {
            finish()
        }
        val userId: String = intent.extras!!.getString("userId").toString()
        getUpdateRequest(this@ApproveRequestActivity, userId)
        approve.setOnClickListener {
            postUpdateRequest("approve", userId)
        }
        reject.setOnClickListener {
            postUpdateRequest("reject", userId)
        }
    }

    fun setValue(existing: TextView, new: TextView, exString: String?, newString: String?) {
        var exString = exString
        var newString = newString
        if (exString.isNullOrEmpty()) {
            exString = ""
        }
        if (newString.isNullOrEmpty()) {
            newString = ""
        }
        if (exString == newString) {
            new.visibility = View.GONE
            existing.visibility = View.VISIBLE
            existing.text = newString
        } else {
            new.visibility = View.VISIBLE
            existing.visibility = View.VISIBLE
            existing.text = exString
            new.text = newString
            new.setTextColor(resources.getColor(R.color.red))
        }
    }

    private fun getUpdateRequest(context: Context, userId: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? =
                            retrofit.getUpdateRequest("Bearer $it", userId)
                        call!!.enqueue(object : Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    val resp = response.body()
                                    val result: JsonObject = Gson().fromJson(
                                        resp?.get("results"),
                                        JsonObject::class.java
                                    )
                                    var exObj: ProfileChange
                                    var newObj: ProfileChange
                                    if (!result.get("user").isJsonNull && !result.get("updateRequest").isJsonNull) {
                                        exObj = Gson().fromJson(
                                            result.get("user"),
                                            ProfileChange::class.java
                                        )
                                        newObj = Gson().fromJson(
                                            result.get("updateRequest"),
                                            ProfileChange::class.java
                                        )
                                        setValue(exName, newName, exObj.name, newObj.name)
                                        setValue(
                                            exFname,
                                            newFname,
                                            exObj.firstName,
                                            newObj.firstName
                                        )
                                        setValue(exLname, newLname, exObj.lastName, newObj.lastName)
                                        setValue(exGender, newGender, exObj.gender, newObj.gender)
                                        setValue(exEmail, newEmail, exObj.email, newObj.email)
                                        setValue(exMobile, newMobile, exObj.mobile, newObj.mobile)
                                        setValue(
                                            exDOB, newDOB,
                                            Util.formatDate(
                                                exObj.dateOfBirth,
                                                "dd MMMM yyyy",
                                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                                            ),
                                            Util.formatDate(
                                                newObj.dateOfBirth,
                                                "dd MMMM yyyy",
                                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                                            )
                                        )
                                        setValue(exCity, newCity, exObj.city, newObj.city)
                                        setValue(exState, newState, exObj.state, newObj.state)
                                        setValue(
                                            exCountry,
                                            newCountry,
                                            exObj.country,
                                            newObj.country
                                        )
                                        setValue(
                                            exPincode,
                                            newPincode,
                                            exObj.pinCode,
                                            newObj.pinCode
                                        )
                                        setValue(exLang, newLang, exObj.language, newObj.language)
                                        setValue(
                                            exReligion,
                                            newReligion,
                                            exObj.religion,
                                            newObj.religion
                                        )
                                        setValue(
                                            exChurch,
                                            newChurch,
                                            exObj.churchName,
                                            newObj.churchName
                                        )
                                        setValue(
                                            exIsWarrior,
                                            newIsWarrior,
                                            exObj.isWarrior,
                                            newObj.isWarrior
                                        )
                                        setValue(
                                            exAddress,
                                            newAddress,
                                            exObj.address,
                                            newObj.address
                                        )
                                        setValue(exGift, newGift, exObj.gift, newObj.gift)
                                        if (!exObj.picture.isNullOrEmpty()) {
                                            Picasso.get().load(exObj.picture).into(profile)
                                        } else {
                                            expic.setImageResource(R.drawable.ic_profile)
                                        }
                                        if (!newObj.picture.isNullOrEmpty()) {
                                            Picasso.get().load(newObj.picture).into(newpic)
                                        } else {
                                            newpic.setImageResource(R.drawable.ic_profile)
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@ApproveRequestActivity,
                                            "This request is already handled",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        finish()
                                    }

                                } else if (response.code() == 401) {
                                    Toast.makeText(
                                        this@ApproveRequestActivity,
                                        resources.getString(R.string.Deleted_account),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val intent = Intent(
                                        this@ApproveRequestActivity,
                                        LoginActivity::class.java
                                    )
                                    startActivity(intent)
                                } else {
                                    Log.e("code", response.code().toString())
                                    Log.e("err", response.errorBody().toString())
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("EditProfileActivity.getMyDetails", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@ApproveRequestActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@ApproveRequestActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EditProfileActivity.getMyDetails", e.toString())
        }
    }

    private fun postUpdateRequest(status: String, userId: String) {
        try {
            if (Commons().isNetworkAvailable(this)) {
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(this) {
                    if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? =
                            retrofit.postUpdateRequest("Bearer $it", userId, status)
                        call!!.enqueue(object : Callback<JsonObject?> {
                            override fun onResponse(
                                call: Call<JsonObject?>,
                                response: Response<JsonObject?>
                            ) {
                                if (response.code() == 200) {
                                    val intent = Intent(
                                        this@ApproveRequestActivity,
                                        MainActivity::class.java
                                    )
                                    startActivity(intent)

                                } else if (response.code() == 401) {
                                    Toast.makeText(
                                        this@ApproveRequestActivity,
                                        resources.getString(R.string.Deleted_account),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val intent = Intent(
                                        this@ApproveRequestActivity,
                                        LoginActivity::class.java
                                    )
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        this@ApproveRequestActivity,
                                        "Something went wrong",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    val intent = Intent(
                                        this@ApproveRequestActivity,
                                        MainActivity::class.java
                                    )
                                    startActivity(intent)
                                    Log.e("code", response.code().toString())
                                    Log.e("err", response.errorBody().toString())
                                }
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("EditProfileActivity.getMyDetails", "fail")
                            }
                        })
                    } else {
                        Toast.makeText(
                            this@ApproveRequestActivity,
                            "Somthing Went Wrong \nLogin again to continue",
                            Toast.LENGTH_LONG
                        ).show()
                        lifecycleScope.launch {
                            userPreferences.deleteAuthToken()
                            userPreferences.deleteUserId()
                        }
                        val intent = Intent(this@ApproveRequestActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EditProfileActivity.getMyDetails", e.toString())
        }
    }

}