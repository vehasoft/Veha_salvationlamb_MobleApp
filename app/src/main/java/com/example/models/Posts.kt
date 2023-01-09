package com.example.models

data class Posts(
    val id: String,
    val title: String,
    val content: String,
    val tags: String,
    val userId: String,
    val picture: String,
    val likesCount: String,
    val shareCount: String,
    val createdAt: String,
    val updatedAt: String,
    var user: PostUser
)
data class Post(
    val id: String,
    val title: String,
    val content: String,
    val tags: String,
    val userId: String,
    val picture: String,
    val likesCount: String,
    val shareCount: String,
    val createdAt: String,
    val updatedAt: String,
)
data class PostUser(
    val id: String,
    val name: String,
    val picture: String,
    val isWarrior: String,
    val email: String
)
data class FavPost (
    val id: String,
    val userId: String,
    val postId: String,
    val createdAt: String,
    val updatedAt: String,
    val posts: Posts,
)