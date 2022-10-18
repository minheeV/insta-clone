package com.example.instaclone.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instaclone.databinding.FragmentDetailBinding
import com.example.instaclone.navigation.adapter.DetailViewRecyclerViewAdapter
import com.example.instaclone.navigation.util.Constants.Companion.firebaseAuth

class DetailViewFragment : Fragment() {
    var uid: String? = null
    private lateinit var binding: FragmentDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailBinding.inflate(inflater, container, false)

        uid = firebaseAuth.currentUser!!.uid
        binding.detailviewfragmentRecyclerview.adapter =
            DetailViewRecyclerViewAdapter(requireActivity())
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)
        return binding.root
    }


}