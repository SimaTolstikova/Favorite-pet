package com.example.myproject

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("input/login.php")
    suspend fun login(@Query("phone") phone: String, @Query("password") password: String): LoginResponse

    @POST("input/register.php")
    @FormUrlEncoded
    suspend fun register(
        @Field("phone") phone: String,
        @Field("password") password: String
    ): LoginResponse

    @GET("id.php")
    suspend fun getUserId(@Query("phone") phone: String): UserIdResponse

    @GET("animal/getAnimalsByUserId.php")
    suspend fun getAnimalsByUserId(@Query("id_user") userId: Int): Response<List<Animal>>

    @POST("animal/insertAnimal.php")
    suspend fun insertAnimal(@Body animal: Animal): Response<ApiResponse<Unit>>

    @POST("animal/updateAnimal.php")
    suspend fun updateAnimal(@Body animal: Animal): Response<Void>

    @POST("animal/deleteAnimal.php")
    suspend fun deleteAnimal(@Body requestBody: JsonObject): Response<Void>

    // --- Напоминания ---
    @GET("reminder/getRemindersByUser.php")
    suspend fun getRemindersByUser(@Query("id_user") idUser: Int): Response<ApiResponse<List<Reminder>>>

    @POST("reminder/insertReminder.php")
    suspend fun insertReminder(@Body reminder: Reminder): Response<Unit>

    @POST("reminder/updateReminder.php")
    suspend fun updateReminder(@Body reminder: Reminder): Response<Void>

    @HTTP(method = "DELETE", path = "reminder/deleteReminder.php", hasBody = true)
    @Headers("Content-Type: application/json")
    suspend fun deleteReminder(@Body body: JsonObject): Response<Unit>

    // --- Документы ---

    @GET("document/getDocumentByUser.php")
    suspend fun getDocumentByUser(
        @Query("id_user") userId: Int
    ): Response<List<Document>>

    @Multipart
    @POST("document/insertDocument.php")
    suspend fun insertDocument(
        @Part("id_user") userId: RequestBody,
        @Part("text") description: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @POST("document/deleteDocument.php")
    suspend fun deleteDocument(
        @Body request: JsonObject
    ): Response<Unit>

    @Streaming
    @GET("document/{filename}")
    suspend fun downloadDocument(
        @Path("filename") filename: String
    ): Response<ResponseBody>

    // --- История заболеваний ---
    @GET("history/getHistorysByUser.php")
    suspend fun getHistorysByUser(@Query("id_user") idUser: Int): Response<ApiResponse<List<History>>>

    @POST("history/insertHistory.php")
    suspend fun insertHistory(@Body reminder: History): Response<Unit>

    @POST("history/updateHistory.php")
    suspend fun updateHistory(@Body reminder: History): Response<Void>

    @HTTP(method = "DELETE", path = "history/deleteHistory.php", hasBody = true)
    @Headers("Content-Type: application/json")
    suspend fun deleteHistory(@Body body: JsonObject): Response<Unit>

    // --- Загрузка изображения ---
    @Multipart
    @POST("animal/uploadImage.php")  // Убедитесь, что endpoint правильный
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<ResponseBody>
}