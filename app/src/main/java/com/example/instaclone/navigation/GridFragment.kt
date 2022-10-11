package com.example.instaclone.navigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.R
import com.example.instaclone.databinding.FragmentGridBinding
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// TODO 리사이클러뷰 만들어줄 예정이지만 유저 프레그먼트와 동일
class GridFragment : Fragment() {
    lateinit var binding: FragmentGridBinding

    var imagesSnapshot: ListenerRegistration? = null
    var fireStore: FirebaseFirestore? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGridBinding.inflate(inflater, container, false)
        fireStore = FirebaseFirestore.getInstance()

        binding.gridfragmentRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.gridfragmentRecyclerview.layoutManager = GridLayoutManager(activity, 3)

        return binding.root
    }

    override fun onStop() {
        super.onStop()
        imagesSnapshot?.remove()
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            imagesSnapshot = fireStore?.collection("images")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (querySnapshot == null) return@addSnapshotListener
                    //Get data
                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    notifyDataSetChanged()
                } // 내가 올린 이미지만 내 유아이디일때만 검색
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3 //폭의 3분의 1 값
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

            imageView.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()

                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)

                fragment.arguments = bundle
                activity!!.supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, fragment)
                    .commit()
            }
        }

        override fun getItemCount(): Int {
            return contentDTOs.size

        }
    }
}