package com.example.instaclone.navigation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.R
import com.example.instaclone.databinding.ActivityCommentBinding
import com.example.instaclone.databinding.ItemCommentBinding
import com.example.instaclone.navigation.model.AlarmDTO
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentBinding
    var contentUid : String? = null
    var destinationUid : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        binding.commentRecyclerview.adapter = CommentRecyclerviewAdapter()
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)

        binding.commentBtnSend.setOnClickListener {
            var comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = binding.commentEditMessage.text.toString()
            comment.timestamp = System.currentTimeMillis()

            //DB 저장
            FirebaseFirestore.getInstance().collection("images")
                .document(contentUid!!)
                .collection("comments")
                .document()
                .set(comment)
            //커멘트 받는 부분
            commentAlarm(destinationUid!!, binding.commentEditMessage.text.toString())
            binding.commentEditMessage.setText("") // 보내고 나서 edit 초기화

        }
    }

    //커멘트 달았을 때 알려주는 커멘트 알림 함수
    fun commentAlarm(destinationUid : String, message : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind = 1
        alarmDTO.message = message
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }
    inner class  CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()
        private lateinit var commentBinding : ItemCommentBinding
        init {
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    if(querySnapshot == null) return@addSnapshotListener
                    for(snapshot in querySnapshot.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            commentBinding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(commentBinding)
        }

        private inner class CustomViewHolder(view: ItemCommentBinding) :  RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            commentBinding.commentviewitemTextviewComment.text = comments[position].comment
            commentBinding.commentviewitemTextviewProfile.text = comments[position].userId

            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        var url = task.result!!["image"]// url주소 받아옴
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(commentBinding.commentviewitemImageviewProfile)
                    }
                }

        }

        override fun getItemCount(): Int {
            return comments.size
        }

    }
}