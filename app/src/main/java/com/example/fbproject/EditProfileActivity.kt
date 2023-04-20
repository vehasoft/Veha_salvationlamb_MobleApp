package com.example.fbproject

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_edit_profile.email
import kotlinx.android.synthetic.main.activity_edit_profile.fname
import kotlinx.android.synthetic.main.activity_edit_profile.gender
import kotlinx.android.synthetic.main.activity_edit_profile.lname
import kotlinx.android.synthetic.main.activity_edit_profile.mobile
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_register.date
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream


class EditProfileActivity : AppCompatActivity() {
    private var countrystr: String = ""
    private var statestr: String = ""
    private var citystr: String = ""
    private var religion: String = ""
    private var churchName: String = ""
    private var profilestr: String? = ""
    private var genderbtn: Int = -1
    private var imgStr: String? = ""
    lateinit var saveBtn: Button
    var warriorStr: Boolean = false
    lateinit var warrior: CheckBox
    lateinit var cancelBtn: Button
    lateinit var countrySP: Spinner
    lateinit var stateSp: Spinner
    lateinit var citySp: Spinner
    private var year = 1960
    private var month: Int = 0
    private var day: Int = 1
    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: ProgressDialog
    lateinit var logo: ImageView

    private var state: ArrayList<String> = ArrayList()
    private var city: ArrayList<String> = ArrayList()
    private var country: ArrayList<String> = ArrayList()


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
            if (data?.data != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, data.data)
                profilestr = encodeTobase64(bitmap)
            }
        } else if (requestCode == 150) {
            if (data?.extras!!["data"] != null) {
                val bitmap = data.extras!!["data"] as Bitmap?
                val bytes = ByteArrayOutputStream()
                bitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
                profilestr = encodeTobase64(bitmap)
            }
        }
        val data = JsonObject()
        data.addProperty("base64Image", profilestr)
        data.addProperty("name", Util.user.name + " picture")
        updateProfilePic(this, data)
    }

    fun encodeTobase64(image: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        userPreferences = UserPreferences(this)
        dialog = ProgressDialog(this)
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        saveBtn = findViewById(R.id.save_btn)
        cancelBtn = findViewById(R.id.cancel_btn)
        warrior = findViewById(R.id.warrior)
        getCountries()
        getMyDetails(this)

        countrySP = findViewById(R.id.country)
        stateSp = findViewById(R.id.state)
        citySp = findViewById(R.id.city)
        logo = findViewById(R.id.prod_logo)
        logo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        if (Util.isWarrior) {
            warrior.visibility = View.GONE
        }
        state.add("State")
        val adapter1 = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, state)
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        stateSp.adapter = adapter1
        city.add("City")
        val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, city)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySp.adapter = adapter

        countrySP.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                if (country[pos] != "Country") {
                    countrystr = country[pos]
                    getStates(countrystr)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        stateSp.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                if (state[pos] != "State") {
                    statestr = state[pos]
                    getCities(statestr)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        citySp.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                if (city[pos] != "City") {
                    citystr = city[pos]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        profile_pic_edit.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
            builder.setMessage("Edit Profile pic only updated after admin approval")
            builder.setTitle("Do you want to update")
            builder.setCancelable(false)
            builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                val result = ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
               if (result){
                    val items = arrayOf<CharSequence>(
                        "Take Photo", "Choose from Gallery",
                        "Cancel"
                    )
                    val builder1 = AlertDialog.Builder(this@EditProfileActivity)
                    builder1.setTitle("Add Picture!")
                    builder1.setItems(items) { dialog, item ->
                        if (items[item] == "Take Photo") {
                            cameraIntent()
                        } else if (items[item] == "Choose from Gallery") {
                            galleryIntent()
                        } else if (items[item] == "Cancel") {
                            dialog.dismiss()
                        }

                    }
                    builder1.show()
                } else {
                    val builder2 = AlertDialog.Builder(this@EditProfileActivity)
                    builder2.setTitle("Permission Restricted")
                    builder2.setMessage("We need CAMERA and STORAGE permission to serve you for updating your profile picture.\nplease enable this permission through your device's settings")
                    builder2.setPositiveButton("cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }
                    builder2.setNegativeButton("settings") { dialog: DialogInterface, _: Int ->
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri: Uri = Uri.fromParts("package", applicationContext.packageName, null)
                        intent.data = uri
                        this.startActivity(intent)
                    }
                    builder2.show()
                }
            }
            builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }

        menu.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@EditProfileActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            if (Util.isWarrior) {
                popup.menu.findItem(R.id.warrior).isVisible = false
            }
            if (Util.user.isReviewState.toBoolean()) { popup.menu.findItem(R.id.warrior).isVisible = false }
            popup.menu.findItem(R.id.edit_profile).isVisible = false
            popup.menu.findItem(R.id.logout).isVisible = false
            val night: MenuItem = popup.menu.findItem(R.id.nightmode)
            if (Util.isNight) {
                night.title = "Day Mode"
            } else {
                night.title = "Night Mode"
            }
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.warrior -> {
                        Commons().makeWarrior(this, this)
                    }

                    /*R.id.logout -> {
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
                        builder.setMessage("Do you want to Logout ?")
                        builder.setTitle("Alert !")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                            finish()
                            lifecycleScope.launch {
                                userPreferences.deleteAuthToken()
                                userPreferences.deleteUserId()
                            }
                            val intent = Intent(this@EditProfileActivity, LoginActivity::class.java)
                            startActivity(intent)
                        }
                        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

                        val alertDialog: AlertDialog = builder.create()
                        alertDialog.show()
                    }*/

                    R.id.fav -> {
                        val intent = Intent(this@EditProfileActivity, FavoritesActivity::class.java)
                        startActivity(intent)
                    }

                    R.id.nightmode -> {
                        if (Util.isNight) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            Util.isNight = false
                            night.title = "Day Mode"
                            lifecycleScope.launch { userPreferences.saveIsNightModeEnabled(false) }
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            Util.isNight = true
                            night.title = "Night Mode"
                            lifecycleScope.launch { userPreferences.saveIsNightModeEnabled(true) }
                        }
                    }
                    R.id.settings -> {
                        val intent = Intent(this@EditProfileActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                }
                true
            })
            popup.show()
        }
        saveBtn.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
            builder.setMessage("Do you want to edit")
            builder.setTitle("Alert")
            builder.setCancelable(false)
            builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                if(doValidation().contentEquals("success")){
                    val data = JsonObject()
                    data.addProperty("firstName", fname.text.toString())
                    data.addProperty("lastName", lname.text.toString())
                    data.addProperty("name", fname.text.toString() + " " + lname.text.toString())
                    data.addProperty("email", email.text.toString())
                    data.addProperty("mobile", mobile.text.toString())
                    data.addProperty("address", address.text.toString())
                    data.addProperty(
                        "gender",
                        findViewById<RadioButton>(gender.checkedRadioButtonId).text.toString().toLowerCase()
                    )
                    data.addProperty("dateOfBirth", date.text.toString())
                    data.addProperty("isWarrior", warriorStr)
                    data.addProperty("country", countrystr)
                    data.addProperty("state", statestr)
                    data.addProperty("city", citystr)
                    data.addProperty("religion", religion)
                    data.addProperty("churchName", churchName)
                    data.addProperty("picture", imgStr)
                    data.addProperty("language", language.text.toString())
                    data.addProperty("pinCode", pincode.text.toString())
                    edit(data)
                }
            }
            builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
        cancelBtn.setOnClickListener { finish() }
        warrior.setOnCheckedChangeListener { buttonView, isChecked ->
            Commons().makeWarrior(this, this)
        }
    }

    private fun doValidation(): String{
        if(TextUtils.isEmpty(fname.text.toString().trim())){
            fname.error = "Enter name"
            return "error"
        } else if(TextUtils.isEmpty(lname.text.toString().trim())){
            lname.error = "Enter name"
            return "error"
        }  else if(TextUtils.isEmpty(mobile.text.toString().trim())){
            mobile.error = "Enter mobile number"
            return "error"
        } else if(TextUtils.isEmpty(date.text.toString().trim()) || date.text.toString().equals("Date of birth",true)){
            date.error = "Select Date of birth"
            return "error"
        }
        return "SUCCESS"
    }

    private fun edit(data: JsonObject) {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            userPreferences = UserPreferences(this@EditProfileActivity)
            val retrofit = Util.getRetrofit()

            userPreferences.authToken.asLiveData().observe(this) {
                if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? = retrofit.putUser("Bearer $it", Util.userId, data)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            if (response.code() == 200) {
                                val intent = Intent(this@EditProfileActivity, MainActivity::class.java)
                                startActivity(intent)
                            } else {
                                val resp = response.errorBody()
                                val registerResp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                val status = registerResp.get("status").toString()
                                val errorMessage = registerResp.get("errorMessage").toString()
                                Log.e("respppStatus", status)
                                Log.e("respppresult", errorMessage)
                                Toast.makeText(this@EditProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                            }
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                        }

                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            Log.e("EditProfileActivity.edit", "fail")
                        }
                    })
                }
            }
        }
    }

    private fun getMyDetails(context: Context) {
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
                                val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                                if (!loginresp.picture.isNullOrEmpty()) {
                                    imgStr = loginresp.picture
                                    Picasso.with(context).load(loginresp.picture).into(profile_pic_edit)
                                }
                                if (!TextUtils.isEmpty(loginresp.firstName)) fname.text =
                                    Editable.Factory.getInstance().newEditable(loginresp.firstName)
                                if (!TextUtils.isEmpty(loginresp.lastName)) lname.text =
                                    Editable.Factory.getInstance().newEditable(loginresp.lastName)
                                if (!TextUtils.isEmpty(loginresp.address)) address.text =
                                    Editable.Factory.getInstance().newEditable(loginresp.address)
                                if (!TextUtils.isEmpty(loginresp.email)) email.text =
                                    Editable.Factory.getInstance().newEditable(loginresp.email)
                                if (!TextUtils.isEmpty(loginresp.mobile)) mobile.text =
                                    Editable.Factory.getInstance().newEditable(loginresp.mobile)
                                if (!TextUtils.isEmpty(loginresp.religion)) religion = loginresp.religion
                                if (!TextUtils.isEmpty(loginresp.churchName)) churchName = loginresp.churchName
                                if (!TextUtils.isEmpty(loginresp.isWarrior)) warriorStr =
                                    loginresp.isWarrior.toBoolean()
                                if (warriorStr) {
                                    warrior.visibility = View.GONE
                                } else {
                                    warrior.visibility = View.VISIBLE
                                }
                                loginresp.isWarrior.toBoolean()
                                if (!TextUtils.isEmpty(loginresp.dateOfBirth)) {
                                    var dateArr = Util.formatDate(loginresp.dateOfBirth,"dd-MM-yyyy").split("-")
                                    year = dateArr[2].toInt()
                                    month = dateArr[1].toInt() - 1
                                    day = dateArr[0].toInt()
                                    //val viewDate: String = day + "" + month + "" + year
                                    date.text =
                                        Editable.Factory.getInstance()
                                            .newEditable(Util.formatDate(loginresp.dateOfBirth,"dd-MM-yyyy"))
                                }
                                if (!TextUtils.isEmpty(loginresp.pinCode)) pincode.text =
                                    Editable.Factory.getInstance().newEditable(loginresp.pinCode)
                                if (!TextUtils.isEmpty(loginresp.language)) language.text =
                                    Editable.Factory.getInstance().newEditable(loginresp.language)
                                if (!TextUtils.isEmpty(loginresp.country)) {
                                    Log.e("country", country.indexOf(loginresp.country).toString())
                                    countrySP.setSelection(country.indexOf(loginresp.country))
                                } else{
                                    if (dialog.isShowing) {
                                        dialog.dismiss()
                                    }
                                }
                                if (!TextUtils.isEmpty(loginresp.state)) {
                                    Log.e("state", state.indexOf(loginresp.state).toString())
                                    stateSp.setSelection(state.indexOf(loginresp.state))
                                } else{
                                    if (dialog.isShowing) {
                                        dialog.dismiss()
                                    }
                                }
                                if (!TextUtils.isEmpty(loginresp.city)) {
                                    Log.e("country", city.indexOf(loginresp.city).toString())
                                    citySp.setSelection(city.indexOf(loginresp.city))
                                } else{
                                    if (dialog.isShowing) {
                                        dialog.dismiss()
                                    }
                                }
                                if (!TextUtils.isEmpty(loginresp.gender)) {
                                    when (loginresp.gender) {
                                        "male" -> genderbtn = R.id.male
                                        "female" -> genderbtn = R.id.female
                                    }
                                    gender.check(genderbtn)
                                }
                            }
                        }

                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            Log.e("EditProfileActivity.getMyDetails", "fail")
                        }
                    })
                } else {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Somthing Went Wrong \nLogin again to continue",
                        Toast.LENGTH_LONG
                    ).show()
                    lifecycleScope.launch {
                        userPreferences.deleteAuthToken()
                        userPreferences.deleteUserId()
                    }
                    val intent = Intent(this@EditProfileActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun updateProfilePic(context: Context, data: JsonObject) {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(this) {
                if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? = retrofit.postProfilePic("Bearer $it", Util.userId, data)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            if (response.code() == 200) {
                                Toast.makeText(context, "Waiting For Admin Approval", Toast.LENGTH_LONG).show()
                            }
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                        }

                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            Log.e("EditProfileActivity.UpdateProfilepic ", "Fail")
                        }
                    })
                } else {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Somthing Went Wrong \nLogin again to continue",
                        Toast.LENGTH_LONG
                    ).show()
                    lifecycleScope.launch {
                        userPreferences.deleteAuthToken()
                        userPreferences.deleteUserId()
                    }
                    val intent = Intent(this@EditProfileActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
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
        DatePickerDialog.OnDateSetListener { _, year, month, day ->
            date?.text = StringBuilder().append(day).append("-")
                .append(month+1).append("-").append(year)
        }

    private fun getCountries() {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val call = Util.getRetrofit().getCountries()
            call!!.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    Log.e("country",response.body().toString())
                    val data = response.body()!!["results"] as JsonObject
                    val array = data["countries"] as JsonArray
                    country = ArrayList()
                    country.add("Country")
                    for (i in 0 until array.size()) {
                        val cityobj: City = Gson().fromJson(array[i], City::class.java)
                        country.add(cityobj.name)
                    }
                    val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, country)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    countrySP.adapter = adapter
                    getMyDetails(this@EditProfileActivity)
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    Log.e("EditProfileActivity.getCountries", "fail")
                }
            })
        }
    }

    private fun getStates(countryId: String) {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val call = Util.getRetrofit().getState(countryId)
            call!!.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    val data = response.body()!!["results"] as JsonObject
                    state = ArrayList()
                    val array = data["states"] as JsonArray
                    state.add("State")
                    for (i in 0 until array.size()) {
                        val stateobj: State = Gson().fromJson(array[i], State::class.java)
                        state.add(stateobj.name)
                    }
                    val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, state)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    stateSp.adapter = adapter
                    getMyDetails(this@EditProfileActivity)
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    Log.e("EditProfileActivity.getStates", "fail")
                }
            })
        }
    }

    private fun getCities(stateId: String) {
        if (Commons().isNetworkAvailable(this)) {
            if (!dialog.isShowing) {
                dialog.show()
            }
            val call = Util.getRetrofit().getCity(stateId)
            call!!.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    val data = response.body()!!["results"] as JsonObject
                    val array = data["cities"] as JsonArray
                    city = ArrayList()
                    city.add("City")
                    for (i in 0 until array.size()) {
                        val cityObj: City = Gson().fromJson(array[i], City::class.java)
                        city.add(cityObj.name)
                    }
                    val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, city)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    citySp.adapter = adapter
                    getMyDetails(this@EditProfileActivity)
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    Log.e("EditProfileActivity.getCities", "fail")
                }
            })
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}