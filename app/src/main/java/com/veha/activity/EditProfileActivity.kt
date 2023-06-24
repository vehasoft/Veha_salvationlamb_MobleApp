package com.veha.activity

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.veha.activity.R
import com.veha.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import dmax.dialog.SpotsDialog
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
import java.util.*


class EditProfileActivity : AppCompatActivity() {
    private var countrystr: String = ""
    private var statestr: String = ""
    private var citystr: String = ""
    private var religion: String = ""
    private var mobileTxt: String = ""
    private var churchName: String = ""
    private var profilestr: String? = ""
    private var genderbtn: Int = -1
    private var imgStr: String? = ""
    lateinit var saveBtn: Button
    var warriorStr: Boolean = false
    lateinit var warrior: CheckBox
    lateinit var cancelBtn: Button
    lateinit var countrySP: AutoCompleteTextView
    lateinit var stateSp: AutoCompleteTextView
    lateinit var citySp: AutoCompleteTextView
    private var year = 1960
    private var month: Int = 0
    private var day: Int = 1
    private lateinit var userPreferences: UserPreferences
    lateinit var dialog: AlertDialog
    lateinit var logo: ImageView

    private var state: ArrayList<String> = ArrayList()
    private var city: ArrayList<String> = ArrayList()
    private var country: ArrayList<String> = ArrayList()

    private lateinit var myDetails: UserRslt

    private fun galleryIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT //
        startActivityForResult(Intent.createChooser(intent, "Select File"), 100)
    }
    fun getImageUri( inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(this.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    private fun cameraIntent() {
        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE //
        startActivityForResult(intent, 150)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e(requestCode.toString(), CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE.toString())
        Log.e(resultCode.toString(), RESULT_OK.toString())
        Log.e(resultCode.toString(), CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE.toString())
        if (requestCode == 100) {
            if (data?.data != null) {
                CropImage.activity(data.data).start(this@EditProfileActivity)

            }
        } else if (requestCode == 150) {
            if (data != null) {
                if (data.extras!!["data"] != null) {
                    CropImage.activity(getImageUri(data.extras!!["data"] as Bitmap)).start(this@EditProfileActivity)
                }
            }
        } else if (requestCode === CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                val result = CropImage.getActivityResult(data)
                if (resultCode === RESULT_OK) {
                    val resultUri = result.uri
                    val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, resultUri)
                    profilestr = encodeTobase64(bitmap)
                    val data = JsonObject()
                    data.addProperty("base64Image", profilestr)
                    data.addProperty("name", Util.user.name + " picture")
                    updateProfilePic(this, data)
                } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    val error = result.error
                    Log.e("errorrr", error.toString())
                }
            }
    }
    fun encodeTobase64(image: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        setContentView(R.layout.activity_edit_profile)
        userPreferences = UserPreferences(this)
        dialog = SpotsDialog.Builder().setContext(this).build()
        dialog.setMessage("Please Wait")
        dialog.setCancelable(false)
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
        stateSp.setAdapter(adapter1)
        city.add("City")
        val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, city)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySp.setAdapter(adapter)

        countrySP.onItemClickListener = OnItemClickListener { parent, view, pos, id ->
            if (countrySP.text.toString() != "Country" && !countrySP.text.toString().isNullOrEmpty()) {
                countrystr = countrySP.text.toString()
                getStates(countrystr)
            }
        }
        stateSp.onItemClickListener = OnItemClickListener { parent, view, pos, id ->
            if (stateSp.text.toString() != "Country" && !stateSp.text.toString().isNullOrEmpty()) {
                statestr = stateSp.text.toString()
                getCities(statestr)
            }
        }
        citySp.onItemClickListener = OnItemClickListener { parent, view, pos, id ->
            if (citySp.text.toString() != "Country" && !citySp.text.toString().isNullOrEmpty()) {
                citystr = citySp.text.toString()
            }
        }
        profile_pic_edit.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
            builder.setMessage("Modified profile picture can be updated only after the admin's approval.")
            builder.setTitle("Do you want to change your profile picture?")
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
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.warrior -> {
                        Commons().makeWarrior(this, this)
                    }
                    R.id.fav -> {
                        val intent = Intent(this@EditProfileActivity, FavoritesActivity::class.java)
                        startActivity(intent)
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
        countrySP.setOnFocusChangeListener { _, _ ->
            countrySP.showDropDown()
        }
        stateSp.setOnFocusChangeListener { _, _ ->
            stateSp.showDropDown()
        }
        citySp.setOnFocusChangeListener { _, _ ->
            citySp.showDropDown()
        }
        saveBtn.setOnClickListener {
            if (isDataChanged()){
                val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
                builder.setMessage("Do you want to save the changes?")
                builder.setTitle("Edit - Profile")
                builder.setCancelable(false)
                builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                    if(doValidation().contentEquals("success",true)){
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
            } else {
                val intent = Intent(this@EditProfileActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

        }
        cancelBtn.setOnClickListener { finish() }
        warrior.setOnCheckedChangeListener { buttonView, isChecked ->
            Commons().makeWarrior(this, this)
        }
    }

    private fun isDataChanged(): Boolean {
        if (!fname.text!!.contentEquals(myDetails.firstName,false)){
            return true
        }else if (!lname.text!!.contentEquals(myDetails.lastName,false)){
            return true
        }else if (!mobile.text!!.contentEquals(myDetails.mobile,false)){
            return true
        }else if (!address.text!!.contentEquals(myDetails.address,false)){
            return true
        }else if (!date.text!!.contentEquals(Util.formatDate(myDetails.dateOfBirth,"dd-MM-yyyy"),false)){
            return true
        }else if (!pincode.text!!.contentEquals(myDetails.pinCode,false)){
            return true
        }else if (!language.text!!.contentEquals(myDetails.language,false)){
            return true
        }else if (!countrySP.text!!.contentEquals(myDetails.country)){
            return true
        }else if (!stateSp.text!!.contentEquals(myDetails.state)){
            return true
        }else if (!citySp.text!!.contentEquals(myDetails.city)){
            return true
        }else if (gender.checkedRadioButtonId == R.id.male && myDetails.gender != "male"){
            return true
        }else if (gender.checkedRadioButtonId == R.id.female && myDetails.gender != "female"){
            return true
        }
        return false
    }

    private fun doValidation(): String{
        if(TextUtils.isEmpty(fname.text.toString().trim())){
            fname.error = "Enter name"
            return "Enter name"
        } else if(!Util.isValidName(fname.text.toString())){
            fname.error = "Enter valid name"
            return "Enter valid name"
        } else if(TextUtils.isEmpty(email.text.toString().trim())){
            email.error ="Enter email"
            return "Enter email"
        } else if(!Util.isValidEmail(email.text.toString())){
            email.error = "Invalid Email"
            return "Invalid Email"
        } else if(TextUtils.isEmpty(mobile.text.toString().trim())){
            mobile.error = "Enter mobile number"
            return "Enter mobile number"
        } else if(!Util.isValidMobile(mobile.text.toString().trim())){
            mobile.error = "Enter valid mobile number"
            return "Enter valid mobile number"
        } else if(TextUtils.isEmpty(date.text.toString().trim()) || date.text.toString().equals("Date of birth",true)){
            date.error = "Select Date of birth"
            return "Select Date of birth"
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
                                finish()
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
                                myDetails = loginresp
                                Log.e("mydetails",loginresp.toString())
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
                                if (!TextUtils.isEmpty(loginresp.mobile)) {
                                    mobile.text = Editable.Factory.getInstance().newEditable(loginresp.mobile)
                                    mobileTxt = loginresp.mobile
                                }
                                if (!TextUtils.isEmpty(loginresp.religion)) religion = loginresp.religion
                                if (!TextUtils.isEmpty(loginresp.churchName)) churchName = loginresp.churchName
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
                                    countrySP.setText(loginresp.country)
                                    getStates(loginresp.country)
                                }
                                if (!TextUtils.isEmpty(loginresp.state)) {
                                    stateSp.setText(loginresp.state)
                                    getCities(loginresp.state)
                                }
                                if (!TextUtils.isEmpty(loginresp.city)) {
                                    citySp.setText(loginresp.city)
                                }
                                if (!TextUtils.isEmpty(loginresp.gender)) {
                                    when (loginresp.gender) {
                                        "male" -> genderbtn = R.id.male
                                        "female" -> genderbtn = R.id.female
                                    }
                                    gender.check(genderbtn)
                                }
                            }
                            if (dialog.isShowing) {
                                dialog.dismiss()
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
                    for (i in 0 until array.size()) {
                        val cityobj: City = Gson().fromJson(array[i], City::class.java)
                        country.add(cityobj.name)
                    }
                    val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, country)
                    adapter.setDropDownViewResource(android.R.layout.simple_gallery_item)
                    countrySP.setAdapter(adapter)
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
            val call = Util.getRetrofit().getState(countryId)
            call!!.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    val data = response.body()!!["results"] as JsonObject
                    state = ArrayList()
                    val array = data["states"] as JsonArray
                    for (i in 0 until array.size()) {
                        val stateobj: State = Gson().fromJson(array[i], State::class.java)
                        state.add(stateobj.name)
                    }
                    val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, state)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    stateSp.setAdapter(adapter)
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
            val call = Util.getRetrofit().getCity(stateId)
            call!!.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    val data = response.body()!!["results"] as JsonObject
                    val array = data["cities"] as JsonArray
                    city = ArrayList()
                    for (i in 0 until array.size()) {
                        val cityObj: City = Gson().fromJson(array[i], City::class.java)
                        city.add(cityObj.name)
                    }
                    val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, city)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    citySp.setAdapter(adapter)
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