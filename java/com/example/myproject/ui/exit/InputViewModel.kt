package com.example.myproject.ui.exit

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myproject.ApiService
import com.example.myproject.User
import com.example.myproject.UserIdResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InputViewModel(private val apiService: ApiService) : ViewModel() {

    var formSubmitted: Boolean = false

    private val _isFormVisible = MutableLiveData(true)
    val isFormVisible: LiveData<Boolean> = _isFormVisible

    fun resetFormState() {
        _isFormVisible.value = true
    }

    suspend fun login(phone: String, password: String): Result<Boolean> {
        if (phone.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Поля не должны быть пустыми"))
        }

        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.login(phone, password)
            }

            if (response.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.message ?: "Ошибка входа"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(phone: String, password: String): Result<Boolean> {
        if (phone.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Поля не должны быть пустыми"))
        }

        return try {
            val response = apiService.register(phone, password)
            if (response.success) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.message ?: "Ошибка регистрации"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserId(phone: String): UserIdResponse {
        return apiService.getUserId(phone)
    }
}
