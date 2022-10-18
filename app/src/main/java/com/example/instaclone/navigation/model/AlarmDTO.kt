package com.example.instaclone.navigation.model

data class AlarmDTO(
    var destinationUid: String = "",
    var userId: String = "",
    var uid: String = "",
    //0 : like alarm
    //1 : comment alarm
    //2 : follow alarm
    var kind: Int = 0, // 어떤 타입의 메세지 종류
    var message: String = "", // comment
    var timestamp: Long = 0
)