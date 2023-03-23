package com.example.util

import android.app.Activity
import android.app.ProgressDialog
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
import com.example.fbproject.R
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class Commons {
    fun makeWarrior(context: Context,owner: LifecycleOwner) {
        val data = JsonObject()
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage("If you want to become a warrior, you must strictly follow our Terms & Conditions as you acknowledged earlier during the Signup process.\n" +
                "1.\tINTRODUCTION\n" +
                "A Warrior in “The Army of Salvation Lamb” community, is given the privilege to do the post on this “SalvationLamb” platform. The privilege include (text/image) post. The warrior has the whole responsibility for their post. Once post is done, it cannot be edit/delete. If you wish to edit/delete, you must contact the admin to do such thing. So, we strictly urge the warrior should be keen and careful about the post. Each post is monitored and each post undergo the verification/approval process. The approved post only visible to the platform for the other users. If the post violates our rules, then it will be immediately removed. Even it may cause to terminate your account without any prior notice.\n" +
                "\n" +
                "If you do not read our Terms & Conditions, we strictly advice to read it fully and then decide to become a warrior here. As we mentioned in our Terms & Conditions, the post should not be irrelevant to the “Christianity” content. It should not oppose the “Word of GOD” at any cause.\n" +
                "\n" +
                "2.\tQUALIFICATION\n" +
                "The first and foremost quality to become a warrior is, you must have the holy gift of “Tongues”. It is mandatory for a warrior. If you don’t have this holy gift of “Tongues”, then it is not advisable to become a warrior.\n" +
                "\n" +
                "\n" +
                "\n" +
                "3.\tVERIFICATION\n" +
                "If you want to become a warrior, you are strictly undergoing our verification including your (Account information, Contact information including email id/mobile number, Location, Nationality, Mother Tongue & Church details). If needed we may also contact your over the phone or email. If you wish to co-operate all the verification process taken care from our side, then it would be very helpful to us to know/understand about you in a much better way.\n" +
                "\n" +
                "4.\tWHEN TO CHANGE YOU A WARRIOR\n" +
                "If you are qualified with our minimum requirements and your verification process is satisfied to us, then we will promote you as a warrior by granting the privilege to do the post (text/image post) on our platform. We will also send you a welcome note about you to become as a warrior during your first login after becoming as a warrior.\n")
        builder.setTitle("BECOME A WARRIOR")
        val view = View.inflate(context, R.layout.child_warrior,null)
        builder.setView(view)
        val religion: Spinner = view.findViewById(R.id.religion)
        val church: EditText = view.findViewById(R.id.church)
        val error: TextView = view.findViewById(R.id.err_rel)
        error.setTextColor(Color.RED)
        error.visibility = View.GONE
        val list = Util.getReligion()
        var rel = ""
        val adapter = ArrayAdapter(context, R.layout.spinner_text, list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        religion.adapter = adapter
        religion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                if (list[pos] != "Select"){
                    rel = list[pos].toString()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        builder.setCancelable(false)
        builder.setPositiveButton("I agree") { _: DialogInterface?, _: Int ->
        }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            var wantToCloseDialog = false
            if (rel.isNullOrEmpty() || rel == "Select"){
                error.setTextColor(Color.RED)
                error.visibility = View.VISIBLE
            } else if(church.text.isNullOrEmpty()){
                church.error = "Please Enter ChurchName"
            } else {
                data.addProperty("userId",Util.userId)
                data.addProperty("isWarrior",true)
                data.addProperty("religion",rel)
                data.addProperty("churchName",church.text.toString())
                wantToCloseDialog = true
            }
            if (wantToCloseDialog) alertDialog.dismiss()

            makeMeWarior(data,context,owner)
        }
    }
    private fun makeMeWarior(data: JsonObject,context: Context,owner: LifecycleOwner) {
        if (isNetworkAvailable(context)) {
            val dialog = ProgressDialog(context)
            dialog.setMessage("Please Wait")
            dialog.setCancelable(false)
            dialog.setInverseBackgroundForced(false)
            val userPreferences = UserPreferences(context)
            if (!dialog.isShowing) {
                dialog.show()
            }
            val retrofit = Util.getRetrofit()
            userPreferences.authToken.asLiveData().observe(owner) {
                if (!TextUtils.isEmpty(it) || !it.equals("null") || !it.isNullOrEmpty()) {
                    val call: Call<JsonObject?>? = retrofit.postWarrior("Bearer $it", data)
                    call!!.enqueue(object : retrofit2.Callback<JsonObject?> {
                        override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                            if (response.code() == 200) {
                                Toast.makeText(context, "Waiting for Admin Approval", Toast.LENGTH_LONG).show()
                            } else {
                                val resp = response.errorBody()
                                val loginresp: JsonObject = Gson().fromJson(resp?.string(), JsonObject::class.java)
                                val status = loginresp.get("status").toString()
                                val errorMessage = loginresp.get("errorMessage").toString()
                                Log.e("Status", status)
                                Log.e("result", errorMessage)
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
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
                            Log.e("<Make me warrior>", "fail")
                        }
                    })
                }
            }
        }
    }
    fun getDate(date: String): String{
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
        Toast.makeText(context,"No Internet ",Toast.LENGTH_LONG).show()
       /* val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage("Your are Not connected to Internet")
        builder.setTitle("Alert !")
        builder.setCancelable(false)
        builder.setPositiveButton("Retry") { _: DialogInterface?, _: Int ->
            val activity = context as Activity
            activity.recreate()
        }
        val alertDialog: AlertDialog = builder.create()
        if (alertDialog.isShowing){
            alertDialog.hide()
            alertDialog.dismiss()
        } else {
            alertDialog.show()
        }*/
        return false
    }
    private fun showAlert(context: Context){

    }

}