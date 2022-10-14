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
import com.example.instaclone.navigation.adapter.GridFragmentRecyclerViewAdapter
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// TODO 리사이클러뷰 만들어줄 예정이지만 유저 프레그먼트와 동일
class GridFragment : Fragment() {
    lateinit var binding: FragmentGridBinding
    var fireStore: FirebaseFirestore? = null
    private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGridBinding.inflate(inflater, container, false)
        fireStore = FirebaseFirestore.getInstance()

        fireStore!!.collection("images")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (querySnapshot == null) return@addSnapshotListener
                //Get data
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
            }

        binding.gridfragmentRecyclerview.adapter =
            GridFragmentRecyclerViewAdapter(fireStore!!, contentDTOs, requireActivity())
        binding.gridfragmentRecyclerview.layoutManager = GridLayoutManager(requireActivity(), 3)

        return binding.root
    }
}