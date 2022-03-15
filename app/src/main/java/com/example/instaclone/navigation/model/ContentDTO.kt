package com.example.instaclone.navigation.model

data class ContentDTO(
    var explain: String? = null, // 컨텐츠 설명 관리
    var imageUrl: String? = null, // 이미지 주소 관리
    var uid: String? = null, // 어느 유저가 올렸는지 관리
    var userId: String? = null, // 올린 유저의 이미지를 관리
    var timestamp: Long? = null, // 몇시 몇분에 컨텐츠를 올렸는지
    var favoriteCount: Int = 0, // 좋아요를 몇 개 눌렀는지
    var favorites: Map<String, Boolean> = HashMap() // 중복 좋아요 방지할 수 있는 유저 확인
) {
    data class Comment( // 댓글관리
        var uid: String? = null, // uid 관리
        var userId: String? = null, // 이메일 관리
        var comment: String? = null, // 댓글 관리
        var timestamp: Long? = null // 시간
    )
}
