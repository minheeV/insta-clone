package com.example.instaclone.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.instaclone.databinding.FragmentAlarmBinding
import com.example.instaclone.navigation.adapter.AlarmRecyclerviewAdapter

class AlarmFragment : Fragment() {
    private lateinit var binding: FragmentAlarmBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlarmBinding.inflate(inflater, container, false)

        binding.alarmfragmentRecyclerview.adapter = AlarmRecyclerviewAdapter(requireActivity())
        binding.alarmfragmentRecyclerview.layoutManager = LinearLayoutManager(requireActivity())
        return binding.root
    }

}