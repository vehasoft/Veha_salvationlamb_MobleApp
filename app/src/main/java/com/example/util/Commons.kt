package com.example.util

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.fbproject.LoginActivity
import com.example.fbproject.MainActivity
import com.example.fbproject.R
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response

class Commons {
    fun makeWarrior(context: Context): JsonObject{
        val data = JsonObject()
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage("You will become warrior after the admin approval")
        builder.setTitle("Alert !")
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
                data.addProperty("church",church.text.toString())
                wantToCloseDialog = true
            }
            if (wantToCloseDialog) alertDialog.dismiss()
        }
        return data
    }
}