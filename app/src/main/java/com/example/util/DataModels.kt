package com.example.util

data class Login(val email: String,
                 val password: String,
                 val isMobile: Boolean
)
data class LoginRslt(val status: String,
                     val result: Loginresp,
                     val errorMessage: String
)
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
data class Register(val name: String,
                    val email: String,
                    val phoneNumber: String,
                    val userid: String,
                    val userType: String,
                    val gender: String,
                    val dob: String)
data class ForgotPassword(val email: String,
                          val otp: String)
data class ChangePassword(val email: String,
                          val otp: String,
                          val password: String)
data class Post(var heading: String,
                var tags: String,
                var image: String,
                var name: String,
                var time: String,
                var noOfReacts: String,
                var liked: Boolean,
                var following: Boolean,
                var content: String)
data class Profile(val name: String,
                val followers: String,
                val following: String,
                val image: String,
                val id: String,
                val about: String,
                val noOfPosts: String)

data class Followers(val name: String,
                     val image: String)




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
