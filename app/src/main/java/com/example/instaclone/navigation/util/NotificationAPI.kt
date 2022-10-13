package com.example.instaclone.navigation.util

import com.example.instaclone.BuildConfig.*
import com.example.instaclone.navigation.model.PushDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationAPI {
    @Headers("Authorization: key=$server_key", "Content-Type:application/json")
    @POST("fcm/send")
    suspend fun sendNotification(
        @Body notification: PushDTO
    ): Response<ResponseBody>
}