package com.example.instaclone.navigation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.databinding.ItemCommentBinding
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore

class CommentRecyclerviewAdapter(contentUid: String) :
    RecyclerView.Adapter<CommentRecyclerviewAdapter.CustomViewHolder>() {
    private var comments: ArrayList<ContentDTO.Comment> = arrayListOf()

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding =
            ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(binding)
    }

    inner class CustomViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            val position = adapterPosition
            binding.commentviewitemTextviewComment.text = comments[position].comment
            binding.commentviewitemTextviewProfile.text = comments[position].userId

            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result!!["image"]// url주소 받아옴
                        Glide.with(itemView.context)
                            .load(url)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .apply(RequestOptions().circleCrop())
                            .into(binding.commentviewitemImageviewProfile)
                    }
                }
        }
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return comments.size
    }

}