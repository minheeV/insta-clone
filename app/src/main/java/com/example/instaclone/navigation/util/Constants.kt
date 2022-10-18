package com.example.instaclone.navigation.util

import android.annotation.SuppressLint
import android.util.Log
import com.example.instaclone.navigation.model.PushDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Constants {
    companion object{
        // FCM URL
        const val FCM_URL = "https://fcm.googleapis.com"
        const val DESTINATION_UID = "destinationUid"

        val firebaseAuth = FirebaseAuth.getInstance() //Firebase 로그인 통합 관리하는 Object
        @SuppressLint("StaticFieldLeak")
        val firebaseFirestore = FirebaseFirestore.getInstance()
    }
}