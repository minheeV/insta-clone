package com.example.instaclone.navigation.adapter

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore

class UserFragmentRecyclerViewAdapter(fireStore: FirebaseFirestore, contentDTOs: ArrayList<ContentDTO>, uid: String, context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var contentDTOs: ArrayList<ContentDTO>

    private var fireStore: FirebaseFirestore
    var uid : String
    var context: Context


    init {
        this.fireStore = fireStore
        this.uid = uid
        this.context = context
        this.contentDTOs = contentDTOs
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val width = context.resources.displayMetrics.widthPixels / 3 //폭의 3분의 1 값
        val imageview = ImageView(parent.context)
        imageview.layoutParams = ConstraintLayout.LayoutParams(width, width)
        return CustomViewHolder(imageview)
    }

    inner class CustomViewHolder(var imageview: ImageView) :
        RecyclerView.ViewHolder(imageview) {
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val imageView = (holder as CustomViewHolder).imageview
        Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
            .apply(RequestOptions().centerCrop()).into(imageView)
    }

    override fun getItemCount(): Int {
        return contentDTOs.size

    }
}