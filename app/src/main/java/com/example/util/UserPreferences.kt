package com.example.util

import android.content.Context
import androidx.datastore.DataStore
import androidx.datastore.preferences.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences (context: Context) {
    private val applicationContext = context.applicationContext
    private val dataStorePref: DataStore<Preferences> = applicationContext.createDataStore(name = "SalvationLamb")

    val authToken: Flow<String>
        get() = dataStorePref.data.map { preferences ->
            preferences[AUTH_TOKEN].toString()
        }

    suspend fun saveAuthToken(token: String){
        dataStorePref.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }
    suspend fun deleteAuthToken(){
        dataStorePref.edit {
            it.remove(AUTH_TOKEN)
        }
    }
    val userId: Flow<String>
        get() = dataStorePref.data.map { preferences ->
            preferences[USER_ID].toString()
        }

    suspend fun saveUserId(token: String){
        dataStorePref.edit { preferences ->
            preferences[USER_ID] = token
        }
    }
    suspend fun deleteUserId(){
        dataStorePref.edit {
            it.remove(USER_ID)
        }
    }


    companion object{
        private val AUTH_TOKEN = preferencesKey<String>("token")
        private val USER_ID = preferencesKey<String>("userId")
    }
}