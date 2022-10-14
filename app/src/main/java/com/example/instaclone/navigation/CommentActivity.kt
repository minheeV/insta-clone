package com.example.instaclone.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instaclone.R
import com.example.instaclone.databinding.ActivityCommentBinding
import com.example.instaclone.navigation.adapter.CommentRecyclerviewAdapter
import com.example.instaclone.navigation.model.AlarmDTO
import com.example.instaclone.navigation.model.ContentDTO
import com.example.instaclone.navigation.util.Constants
import com.example.instaclone.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentBinding
    private var contentUid = " "
    private var destinationUid = " "

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contentUid = intent.getStringExtra("contentUid")!!
        destinationUid = intent.getStringExtra("destinationUid")!!

        binding.commentRecyclerview.adapter = CommentRecyclerviewAdapter(contentUid)
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)

        binding.commentBtnSend.setOnClickListener {
            val comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = binding.commentEditMessage.text.toString()
            comment.timestamp = System.currentTimeMillis()

            //DB 저장
            FirebaseFirestore.getInstance().collection("images")
                .document(contentUid)
                .collection("comments")
                .document()
                .set(comment)
            //커멘트 받는 부분
            commentAlarm(destinationUid, binding.commentEditMessage.text.toString())
            binding.commentEditMessage.setText("") // 보내고 나서 edit 초기화

        }
    }

    //커멘트 달았을 때 알려주는 커멘트 알림 함수
    private fun commentAlarm(destinationUid: String, message: String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind = 1
        alarmDTO.message = message
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        val msg =
            Constants.firebaseAuth.currentUser?.email + " " + resources.getString(R.string.alarm_comment) + " of " + message
        FcmPush.instance.sendMessage(destinationUid, "InstaClone", msg)
    }
}