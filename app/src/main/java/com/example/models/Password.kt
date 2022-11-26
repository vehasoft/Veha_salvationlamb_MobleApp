package com.example.models

data class ForgotPasswodResp(
    val status: String,
    val message: String
)
data class ForgotPasswodVerify(
    val email: String,
    val otp: String
)
