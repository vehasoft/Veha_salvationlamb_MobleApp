package com.example.util

import com.example.models.PostUser
import com.example.models.Posts

data class UserRslt(
    val id: String?,
    val loginSource: String?,
    val role: String?,
    val name: String?,
    val email: String?,
    val gender: String?,
    val password: String?,
    val mobile: String?,
    val dateOfBirth: String?,
    val updatedAt: String?,
    val createdAt: String?,
    val picture: String?,
    val coverPicture: String?,
    val address: String?,
    val isWarrior: String?,
    val isReviewState: String?,
    val state: String?,
    val pinCode: String?,
    val country: String?,
    val religion: String?,
    val language: String?,
    val blocked: String?,
)

data class Loginresp(
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
    val isReviewState: String,
    val state: String,
    val pinCode: String,
    val country: String,
    val religion: String,
    val language: String,
    val blocked: String,
    val token: String,
)
data class UserResponse(
    val status: String,
    val errorMessage: String,
    val rslt: UserRslt,
    val token: String,
)

data class PostLikes(
    val id: String,
    val userId: String,
    val postId: String,
    val reaction: String,
    val createdAt: String,
    val updatedAt: String,
    val user: PostUser,
)
data class AllFavList(
    val id: String,
    val userId: String,
    val postId: String,
    val createdAt: String,
    val updatedAt: String,
    val posts: Posts
)
data class AllFollowerList(
    val id: String,
    val userId: String,
    val followerId: String,
    val createdAt: String,
    val updatedAt: String,
    val user: PostUser,
)
