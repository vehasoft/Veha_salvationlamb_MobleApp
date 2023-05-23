package com.veha.util

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

    val isNightModeEnabled: Flow<String>
        get() = dataStorePref.data.map { preferences ->
            preferences[IS_NIGHT].toString()
        }

    suspend fun saveIsNightModeEnabled(isNight: String){
        dataStorePref.edit { preferences ->
            preferences[IS_NIGHT] = isNight
        }
    }
    suspend fun deleteIsNightModeEnabled(){
        dataStorePref.edit {
            it.remove(IS_NIGHT)
        }
    }
    val isFirstTime: Flow<Boolean>
        get() = dataStorePref.data.map { preferences ->
            if (preferences[IS_FIRST] == null){
                return@map true
            }
            preferences[IS_FIRST] as Boolean
        }

    suspend fun saveIsFirstTime(isFirst: Boolean){
        dataStorePref.edit { preferences ->
            preferences[IS_FIRST] = isFirst
        }
    }
    suspend fun deleteIsFirstTime(){
        dataStorePref.edit {
            it.remove(IS_FIRST)
        }
    }


    val textSize: Flow<Float>
        get() = dataStorePref.data.map { preferences ->
            if (preferences[TEXT_SIZE] == null){
                return@map 10.0F
            }
            preferences[TEXT_SIZE]!!
        }

    suspend fun saveTextSize(textSize: Float){
        dataStorePref.edit { preferences ->
            preferences[TEXT_SIZE] = textSize
        }
    }
    suspend fun deleteTextSize(){
        dataStorePref.edit {
            it.remove(TEXT_SIZE)
        }
    }


    companion object{
        private val AUTH_TOKEN = preferencesKey<String>("token")
        private val USER_ID = preferencesKey<String>("userId")
        private val IS_NIGHT = preferencesKey<String>("isNight")
        private val IS_FIRST = preferencesKey<Boolean>("isFirst")
        private val TEXT_SIZE = preferencesKey<Float>("textSize")
    }
}