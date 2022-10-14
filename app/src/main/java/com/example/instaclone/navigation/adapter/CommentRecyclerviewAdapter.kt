package com.example.instaclone.navigation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.databinding.ItemCommentBinding
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore

class CommentRecyclerviewAdapter(contentUid : String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var comments: ArrayList<ContentDTO.Comment> = arrayListOf()
    private lateinit var commentBinding: ItemCommentBinding

    init {
        FirebaseFirestore.getInstance()
            .collection("images")
            .document(contentUid)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                comments.clear()
                if (querySnapshot == null) return@addSnapshotListener
                for (snapshot in querySnapshot.documents!!) {
                    comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                }
                notifyDataSetChanged()
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        commentBinding =
            ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(commentBinding)
    }

    private inner class CustomViewHolder(view: ItemCommentBinding) :
        RecyclerView.ViewHolder(view.root)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        commentBinding.commentviewitemTextviewComment.text = comments[position].comment
        commentBinding.commentviewitemTextviewProfile.text = comments[position].userId

        FirebaseFirestore.getInstance()
            .collection("profileImages")
            .document(comments[position].uid!!)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val url = task.result!!["image"]// url주소 받아옴
                    Glide.with(holder.itemView.context).load(url)
                        .apply(RequestOptions().circleCrop())
                        .into(commentBinding.commentviewitemImageviewProfile)
                }
            }

    }

    override fun getItemCount(): Int {
        return comments.size
    }

}