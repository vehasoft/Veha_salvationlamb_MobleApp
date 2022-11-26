package com.example.models

data class User(
    val name: String,
    val email: String,
    val gender: String,
    val password: String,
    val mobile: String,
    val dateOfBirth: String
)
data class UserRslt(
    val id: String,
    val loginSource: String,
    val role: String,
    val name: String,
    val email: String,
    val gender: String,
    val password: String,
    val mobile: String,
    val dateOfBirth: String,
    val updatedAt: String,
    val createdAt: String,
    val picture: String,
    val coverPicture: String,
    val address: String,
    val isWarrior: String,
    val blocked: String,
)
data class UserResponse(
    val status: String,
    val errorMessage: String,
    val rslt: UserRslt,
    val token: String,
)
