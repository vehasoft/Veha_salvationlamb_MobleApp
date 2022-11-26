package com.example.models

data class Loginresp(val id: String,
                     val name: String,
                     val password: String,
                     val gender: String,
                     val email: String,
                     val picture: String,
                     val coverPicture: String,
                     val address: String,
                     val dateOfBirth: String,
                     val mobile: String,
                     val loginSource: String,
                     val role: String,
                     val createdAt: String,
                     val updatedAt: String,
                     val isWarrior: String,
                     val blocked: String,
                     val token: String
)