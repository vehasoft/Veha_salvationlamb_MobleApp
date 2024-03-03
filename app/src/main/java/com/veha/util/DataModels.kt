package com.veha.util

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
    val churchName: String,
    val religion: String,
    val language: String,
    val isFreshUser: String,
    val isVerifiedUser: String,
    val blocked: String,
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
data class FilesAndFolders(
    val id: String,
    val parentId: String,
    val name: String,
    val type: String,
    val size: String,
    val url: String,
    val permission: String,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
)
data class Countries(
    val id: String,
    val name: String,
    val iso3: String,
    val iso2: String,
    val numeric_code: String,
    val phone_code: String,
    val capital: String,
    val currency: String,
    val currency_name: String,
    val currency_symbol: String,
    val tld: String,
    val native: String,
    val region: String,
    val subregion: String,
)
data class State(
    val id: String,
    val name: String,
    val country_id: String,
    val country_code: String,
    val country_name: String,
    val state_code: String,
    val type: String,
    val latitude: String,
    val longitude: String,
)
data class City(
    val id: String,
    val name: String,
    val state_id: String,
    val state_code: String,
    val state_name: String,
    val country_id: String,
    val country_code: String,
    val country_name: String,
    val latitude: String,
    val longitude: String,
    val wikidataid: String,
)
data class UserRslt(
    val id: String,
    val name: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val email: String,
    val mobile: String,
    val picture: String,
    val coverPicture: String,
    val address: String,
    val dateOfBirth: String,
    val loginSource: String,
    val role: String,
    val createdAt: String,
    val updatedAt: String,
    val isWarrior: String,
    val isReviewState: String,
    val state: String,
    val pinCode: String,
    val country: String,
    val churchName: String,
    val religion: String,
    val city: String,
    val language: String,
    val userGroup: String,
    val isVerified: String,
    val isFreshUser: String,
    val blocked: String,
)
data class Posts(
    val id: String,
    val title: String,
    val content: String,
    val tags: String,
    val userId: String,
    val picture: String,
    val type: String,
    val url: String,
    val contentURL: String,
    val likesCount: String,
    val shareCount: String,
    val createdAt: String,
    val updatedAt: String,
    var user: PostUser
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
data class NotificationList(
    val id: String,
    val name: String,
    val content: String,
    val createdAt: String,
)
