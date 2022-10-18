package com.example.instaclone.navigation.util

import android.util.Log
import com.example.instaclone.navigation.model.PushDTO
import com.example.instaclone.navigation.util.Constants.Companion.firebaseFirestore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmPush {
    companion object {
        var instance = FcmPush()
    }

    fun sendMessage(destinationUid: String, title: String, message: String) {
        firebaseFirestore
            .collection("pushtokens")
            .document(destinationUid)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val token = it.result.get("pushToken").toString()
                    val pushDTO = PushDTO()
                    pushDTO.to = token
                    pushDTO.notification.title = title
                    pushDTO.notification.body = message
                    sendNotification(pushDTO)
                }
            }
    }

    private fun sendNotification(pushDTO: PushDTO) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.sendNotification(pushDTO)
                if (response.isSuccessful) {
                    Log.d("", "sendNotification() isSuccessful ")
                } else {
                    Log.e("", "${response.errorBody()}")
                }
            } catch (e: Exception) {
                println(e.stackTrace)
            }
        }
}