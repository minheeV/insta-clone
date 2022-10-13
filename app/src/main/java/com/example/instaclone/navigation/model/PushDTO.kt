package com.example.instaclone.navigation.model


data class PushDTO(
    var to : String? = null, // 받는 uid
    var notification: Notification = Notification()
){
    data class Notification(
        var body : String? = null,
        var title : String? = null
    )
}
