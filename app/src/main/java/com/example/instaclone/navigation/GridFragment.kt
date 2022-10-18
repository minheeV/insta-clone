package com.example.instaclone.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.instaclone.databinding.FragmentGridBinding
import com.example.instaclone.navigation.adapter.GridFragmentRecyclerViewAdapter
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore

class GridFragment : Fragment() {
    lateinit var binding: FragmentGridBinding
    private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGridBinding.inflate(inflater, container, false)

        binding.gridfragmentRecyclerview.adapter =
            GridFragmentRecyclerViewAdapter(contentDTOs, requireActivity())
        binding.gridfragmentRecyclerview.layoutManager = GridLayoutManager(requireActivity(), 3)

        return binding.root
    }
}