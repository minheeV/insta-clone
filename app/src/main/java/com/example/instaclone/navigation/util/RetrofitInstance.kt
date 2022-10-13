package com.example.instaclone.navigation.util

import com.example.instaclone.navigation.util.Constants.Companion.FCM_URL

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(FCM_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: NotificationAPI by lazy {
        retrofit.create(NotificationAPI::class.java)
    }

    /*private fun provideOkHttpClient(
        interceptor: AppInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .run {
            addInterceptor(interceptor)
                .build()
        }

    class AppInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain)
                : Response = with(chain) {
            val newRequest = request().newBuilder()
                .addHeader("Authorization", "key=${BuildConfig.server_key}")
                .addHeader("Content-Type", "application/json")
                .build()
            proceed(newRequest)
        }
    }*/
}