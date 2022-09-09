package com.example.util

data class Login(val email: String, val password: String)
data class User(val userId: String,val userType: String)
data class Register(val name: String, val email: String, val phoneNumber: String, val userid: String, val userType: String, val gender: String, val dob: String)
data class ForgotPassword(val email: String, val otp: String)
data class ChangePassword(val email: String,val otp: String, val password: String)


