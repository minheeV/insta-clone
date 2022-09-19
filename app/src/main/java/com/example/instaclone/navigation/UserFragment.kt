package com.example.instaclone.navigation

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
import com.example.instaclone.databinding.FragmentUserBinding
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    var fireStore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        uid = arguments?.getString("destinationUid")
        fireStore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.accountRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview.layoutManager = GridLayoutManager(requireActivity(), 3)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            fireStore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (querySnapshot == null) return@addSnapshotListener
                    //Get data
                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    binding.accountTvPostCount.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                } // 내가 올린 이미지만 내 유아이디일때만 검색
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3 //폭의 3분의 1 값
            var imageview = ImageView(parent.context)
            imageview.layoutParams = ConstraintLayout.LayoutParams(width, width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) :
            RecyclerView.ViewHolder(imageview) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size

        }
    }
}