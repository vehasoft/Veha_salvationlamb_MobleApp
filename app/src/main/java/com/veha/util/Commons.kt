package com.veha.util

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import com.veha.activity.R
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class Commons {
    fun makeWarrior(context: Context, owner: LifecycleOwner): String {
        var status = ""
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val data = JsonObject()
        builder.setTitle("BECOME A WARRIOR")
        val view = View.inflate(context, R.layout.child_warrior, null)
        builder.setView(view)
        var gift = ""
        val religion: Spinner = view.findViewById(R.id.religion)
        val church: EditText = view.findViewById(R.id.church)
        val error: TextView = view.findViewById(R.id.err_rel)
        val gift1: CheckBox = view.findViewById(R.id.gift1)
        val gift2: CheckBox = view.findViewById(R.id.gift2)
        val gift3: CheckBox = view.findViewById(R.id.gift3)
        val gift4: CheckBox = view.findViewById(R.id.gift4)
        val gift5: CheckBox = view.findViewById(R.id.gift5)
        error.setTextColor(Color.RED)
        error.visibility = View.GONE
        val list = Util.getReligion()
        var rel = ""
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        religion.adapter = adapter
        religion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                if (list[pos] != "Select") {
                    rel = list[pos].toString()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        builder.setCancelable(false)
        builder.setPositiveButton("I agree") { _: DialogInterface?, _: Int ->
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }
        val builder1: AlertDialog.Builder = AlertDialog.Builder(context)
        builder1.setMessage(R.string.make_me_warrior)
        builder1.setTitle("BECOME A WARRIOR")
        builder1.setNegativeButton("Skip") { dialog: DialogInterface, _: Int -> dialog.cancel() }
        builder1.setPositiveButton("I agree") { _: DialogInterface?, _: Int ->
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
            gift1.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    gift2.isChecked = false
                    gift3.isChecked = false
                    gift4.isChecked = false
                    gift5.isChecked = false
                    gift2.isEnabled = false
                    gift3.isEnabled = false
                    gift4.isEnabled = false
                    gift5.isEnabled = false
                } else {
                    gift2.isEnabled = true
                    gift3.isEnabled = true
                    gift4.isEnabled = true
                    gift5.isEnabled = true
                }
            }
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                var wantToCloseDialog = false
                if (rel.isNullOrEmpty() || rel == "Select") {
                    error.setTextColor(Color.RED)
                    error.visibility = View.VISIBLE
                } else if (church.text.isNullOrEmpty()) {
                    church.error = "Please Enter ChurchName"
                } else {

                    if (gift1.isChecked) {
                        gift += gift1.text.toString()
                        gift2.isChecked = false
                        gift3.isChecked = false
                        gift4.isChecked = false
                        gift5.isChecked = false
                        gift2.isEnabled = false
                        gift3.isEnabled = false
                        gift4.isEnabled = false
                        gift4.isEnabled = false
                    }
                    if (gift2.isChecked)
                        gift += gift2.text.toString() + ","
                    if (gift3.isChecked)
                        gift += gift3.text.toString() + ","
                    if (gift4.isChecked)
                        gift += gift4.text.toString() + ","
                    if (gift5.isChecked)
                        gift += gift5.text.toString() + ","
                    data.addProperty("userId", Util.userId)
                    data.addProperty("isWarrior", true)
                    data.addProperty("religion", rel)
                    data.addProperty("churchName", church.text.toString())
                    data.addProperty("gift", gift)
                    wantToCloseDialog = true
                }
                if (wantToCloseDialog) {
                    alertDialog.dismiss()
                    status = makeMeWarior(data, context, owner)
                }
            }
        }
        val alertDialog: AlertDialog = builder1.create()
        alertDialog.show()
        return status
    }

    private fun makeMeWarior(data: JsonObject, context: Context, owner: LifecycleOwner): String {
        var status: String = ""
        try {
            if (isNetworkAvailable(context)) {
                val userPreferences = UserPreferences(context)
                val retrofit = Util.getRetrofit()
                userPreferences.authToken.asLiveData().observe(owner) {
                    if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                        val call: Call<JsonObject?>? = retrofit.postWarrior("Bearer $it", data)
                        call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                if (response.code() == 200) {
                                    Toast.makeText(context, "Waiting for Admin Approval", Toast.LENGTH_LONG).show()
                                    status = "success"
                                }
                                else{
                                    status = "fail"
                                }
                                call.cancel()
                            }

                            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                Log.e("<Make me warrior>", "fail")
                                status = "fail"
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("<Make me warrior>", e.toString())
        }
        return status
    }

    fun getDate(date: String): String {
        return date.toDate().formatTo()
    }

    fun String.toDate(): Date {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        return parser.parse(this)
    }

    fun Date.formatTo(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        formatter.timeZone = TimeZone.getDefault()
        return formatter.format(this)
    }

    fun isNetworkAvailable(context: Context?): Boolean {
        val connectivityManager = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        Toast.makeText(context, "No Internet ", Toast.LENGTH_LONG).show()
        return false
    }

    private fun showAlert(context: Context) {

    }

}