package com.example.fbproject

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.util.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_edit_profile.email
import kotlinx.android.synthetic.main.activity_edit_profile.gender
import kotlinx.android.synthetic.main.activity_edit_profile.menu
import kotlinx.android.synthetic.main.activity_edit_profile.mobile
import kotlinx.android.synthetic.main.activity_edit_profile.name
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_register.date
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream


class EditProfileActivity : AppCompatActivity() {
    private var countrystr : String = ""
    private var statestr : String = ""
    private var citystr : String = ""
    private var profilestr : String? = ""
    private var genderbtn : Int = -1
    private var imgStr : String? = ""
    lateinit var saveBtn : Button
    lateinit var warrior : CheckBox
    lateinit var cancelBtn : Button
    lateinit var countrySP : Spinner
    lateinit var stateSp : Spinner
    lateinit var citySp : Spinner
    private var year = 1960
    private  var month:Int = 0
    private  var day:Int = 1
    private lateinit var userPreferences: UserPreferences

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
            if (data?.data != null){
                val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, data.data)
                profilestr = encodeTobase64(bitmap)
            }
        } else if (requestCode == 150) {
            if (data?.extras!!["data"] != null){
                val bitmap = data.extras!!["data"] as Bitmap?
                val bytes = ByteArrayOutputStream()
                bitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
                profilestr = encodeTobase64(bitmap)
            }
        }
        val data = JsonObject()
        data.addProperty("base64Image",profilestr)
        data.addProperty("name",Util.user.name+" picture")
        updateProfilePic(this,data)

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
        saveBtn = findViewById(R.id.save_btn)
        cancelBtn = findViewById(R.id.cancel_btn)
        warrior = findViewById(R.id.warrior)
        getCountries()
        getMyDetails(this)

        countrySP = findViewById(R.id.country)
        stateSp = findViewById(R.id.state)
        citySp = findViewById(R.id.city)

        if (Util.isWarrior){
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
                if (country[pos] != "Country"){
                    countrystr = country[pos]
                    getStates(countrystr)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        stateSp.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                if (state[pos] != "State"){
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
                val items = arrayOf<CharSequence>(
                "Take Photo", "Choose from Gallery",
                "Cancel"
            )
            val builder1 = AlertDialog.Builder(this@EditProfileActivity)
            builder1.setTitle("Add Picture!")
            builder1.setItems(items) { dialog, item ->
                val result = true
                    //com.sun.org.apache.bcel.internal.classfile.Utility.checkPermission(this@EditProfileActivity)
                if (items[item] == "Take Photo") {
                    if (result) cameraIntent()
                } else if (items[item] == "Choose from Gallery") {
                    if (result) galleryIntent()
                } else if (items[item] == "Cancel") {
                    dialog.dismiss()
                }
            }
            builder1.show()
            }
            builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }

        menu.setOnClickListener {
            val myContext: Context = ContextThemeWrapper(this@EditProfileActivity, R.style.menuStyle)
            val popup = PopupMenu(myContext, menu)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            if (Util.isWarrior){ popup.menu.findItem(R.id.warrior).isVisible = false }
            popup.menu.findItem(R.id.edit_profile).isVisible = false
            val night: MenuItem = popup.menu.findItem(R.id.nightmode)
            if (Util.isNight){ night.title = "Day Mode" } else{ night.title = "Night Mode" }
            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.warrior -> {
                        Commons().makeWarrior(this)
                    }
                    R.id.logout ->{
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
                        builder.setMessage("Do you want to Logout ?")
                        builder.setTitle("Alert !")
                        builder.setCancelable(false)
                        builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int -> finish()
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
                    }
                    R.id.fav ->{
                        val intent = Intent(this@EditProfileActivity, FavoritesActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.nightmode ->{
                        if (Util.isNight){
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
                }
                true
            })
            popup.show()
    }
        saveBtn.setOnClickListener{
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@EditProfileActivity)
            builder.setMessage("Do you want to edit")
            builder.setTitle("Alert")
            builder.setCancelable(false)
            builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                val data = JsonObject()
                data.addProperty("name",name.text.toString())
                data.addProperty("email",email.text.toString())
                data.addProperty("mobile",mobile.text.toString())
                data.addProperty("address",address.text.toString())
                data.addProperty("gender",findViewById<RadioButton>(gender.checkedRadioButtonId).text.toString().toLowerCase())
                data.addProperty("dateOfBirth",date.text.toString())
                data.addProperty("isWarrior",warrior.isChecked)
                data.addProperty("country",countrystr)
                data.addProperty("state",statestr)
                data.addProperty("city",citystr)
                data.addProperty("picture",imgStr)
                data.addProperty("language",language.text.toString())
                data.addProperty("pinCode",pincode.text.toString())
                edit(data)
            }
            builder.setNegativeButton("No") { dialog: DialogInterface, _: Int -> dialog.cancel() }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
        cancelBtn.setOnClickListener{finish()}
        warrior.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                Commons().makeWarrior(this)
            }
        }

    }
    private fun edit(data: JsonObject) {
        userPreferences = UserPreferences(this@EditProfileActivity)
        val retrofit = Util.getRetrofit()

        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.putUser("Bearer $it",Util.userId, data)
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
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("responseee", "fail")
                    }
                })
            }
        }
    }
    private fun getMyDetails(context: Context) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.getUser("Bearer $it", Util.userId)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            val resp = response.body()
                            val loginresp: UserRslt = Gson().fromJson(resp?.get("result"), UserRslt::class.java)
                            if (!loginresp.picture.isNullOrEmpty()){
                                imgStr = loginresp.picture
                                Picasso.with(context).load(loginresp.picture).into(profile_pic_edit)
                            }
                            if(!TextUtils.isEmpty(loginresp.name)) name.text = Editable.Factory.getInstance().newEditable(loginresp.name)
                            if(!TextUtils.isEmpty(loginresp.address)) address.text = Editable.Factory.getInstance().newEditable(loginresp.address)
                            if(!TextUtils.isEmpty(loginresp.email)) email.text = Editable.Factory.getInstance().newEditable(loginresp.email)
                            if(!TextUtils.isEmpty(loginresp.mobile)) mobile.text = Editable.Factory.getInstance().newEditable(loginresp.mobile)
                            if(!TextUtils.isEmpty(loginresp.dateOfBirth)) {
                                date.text = Editable.Factory.getInstance().newEditable(Util.formatDate(loginresp.dateOfBirth))
                                var dateArr = Util.formatDate(loginresp.dateOfBirth).split("-")
                                year = dateArr[0].toInt()
                                month = dateArr[1].toInt()-1
                                day = dateArr[2].toInt()
                            }
                            if(!TextUtils.isEmpty(loginresp.pinCode)) pincode.text = Editable.Factory.getInstance().newEditable(loginresp.pinCode)
                            if(!TextUtils.isEmpty(loginresp.language)) language.text = Editable.Factory.getInstance().newEditable(loginresp.language)
                            if(!TextUtils.isEmpty(loginresp.country)) {
                                Log.e("country",country.indexOf(loginresp.country).toString())
                                countrySP.setSelection(country.indexOf(loginresp.country))
                            }
                            if(!TextUtils.isEmpty(loginresp.state)) {
                                Log.e("state",state.indexOf(loginresp.state).toString())
                                stateSp.setSelection(state.indexOf(loginresp.state))
                            }
                            if(!TextUtils.isEmpty(loginresp.city)) {
                                Log.e("country",city.indexOf(loginresp.city).toString())
                                citySp.setSelection(city.indexOf(loginresp.city))
                            }
                            if(!TextUtils.isEmpty(loginresp.gender)){
                                when(loginresp.gender) {
                                    "male" -> genderbtn = R.id.male
                                    "female" -> genderbtn = R.id.female
                                }
                                gender.check(genderbtn)
                            }
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("fail ", "Posts")
                    }
                })
            } else {
                Toast.makeText(this@EditProfileActivity,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(this@EditProfileActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }
    private fun updateProfilePic(context: Context,data: JsonObject) {
        val retrofit = Util.getRetrofit()
        userPreferences.authToken.asLiveData().observe(this) {
            if (!TextUtils.isEmpty(it) && !it.equals("null") && !it.isNullOrEmpty()) {
                val call: Call<JsonObject?>? = retrofit.postProfilePic("Bearer $it", Util.userId,data)
                call!!.enqueue(object : retrofit2.Callback<JsonObject?> {

                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.code() == 200) {
                            Toast.makeText(context,"Waiting For Admin Approval",Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Toast.makeText(context,"No Internet",Toast.LENGTH_LONG).show()
                        Log.e("fail ", "Posts")
                    }
                })
            } else {
                Toast.makeText(this@EditProfileActivity,"Somthing Went Wrong \nLogin again to continue",Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    userPreferences.deleteAuthToken()
                    userPreferences.deleteUserId()
                }
                val intent = Intent(this@EditProfileActivity, LoginActivity::class.java)
                startActivity(intent)
            }
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
        DatePickerDialog.OnDateSetListener { _, year, month, day ->
            date?.text = StringBuilder().append(year).append("-")
                .append(month+1).append("-").append(day)
        }
    private fun getCountries() {
        val call = Util.getRetrofit().getCountries()
        call!!.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
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

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {}
        })
    }
    private fun getStates(countryId: String) {
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

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {}
        })
    }
    private fun getCities(stateId: String) {
        val call = Util.getRetrofit().getCity(stateId)
        call!!.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val data = response.body()!!["results"] as JsonObject
                val array = data["cities"] as JsonArray
                city = ArrayList()
                city.add("City")
                for (i in 0 until array.size()) {
                    val cityobj: City = Gson().fromJson(array[i], City::class.java)
                    city.add(cityobj.name)
                }
                val adapter = ArrayAdapter(this@EditProfileActivity, R.layout.spinner_text, city)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                citySp.adapter = adapter
                getMyDetails(this@EditProfileActivity)
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {}
        })
    }
}