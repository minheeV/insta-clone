package com.example.instaclone.navigation.model

data class AlarmDTO (
    var destinationUid: String? = null,
    var userId: String? = null,
    var uid: String? = null,
    //0 : like alarm
    //1 : comment alarm
    //2 : follow alarm
    var kind: Int? = null, // 어떤 타입의 메세지 종류
    var message: String? = null, // comment
    var timestamp: Long? = null
)