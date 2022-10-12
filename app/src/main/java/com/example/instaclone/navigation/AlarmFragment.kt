package com.example.instaclone.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.R
import com.example.instaclone.databinding.FragmentAlarmBinding
import com.example.instaclone.databinding.ItemCommentBinding
import com.example.instaclone.navigation.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AlarmFragment : Fragment() {
    lateinit var binding: FragmentAlarmBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlarmBinding.inflate(inflater, container, false)

        binding.alarmfragmentRecyclerview.adapter = AlarmRecyclerviewAdapter()
        Log.d("민희", "AlarmRecyclerviewAdapter bidn")
        binding.alarmfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)
        return binding.root
    }

    inner class AlarmRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var alarmDTOList: ArrayList<AlarmDTO> = arrayListOf() //알람 저장하는 리스트 변수
        lateinit var binding: ItemCommentBinding

        init {
            Log.d("민희", "init bidn")
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            Log.d("민희", "init bidn $uid")

            FirebaseFirestore.getInstance() //나에게 도착한 메세지만 필터링
                .collection("alarms")
                .whereEqualTo("destinationUid", uid)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    alarmDTOList.clear()
                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                    }
                    alarmDTOList.sortByDescending { it.timestamp }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            Log.d("민희", "onCreateViewHolder bidn")

            return CustomViewHolder(binding)
        }

        inner class CustomViewHolder(binding: ItemCommentBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            Log.d("민희", "recyclerview bidn")
            FirebaseFirestore.getInstance().collection("profilesImages")
                .document(alarmDTOList[position].uid!!)
                .get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val url = it.result!!["image"]
                        Glide.with(binding.root.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(binding.commentviewitemImageviewProfile)
                    }
                }
            when (alarmDTOList[position].kind) {
                0 -> {
                    var str_0 =
                        alarmDTOList[position].userId + activity?.resources?.getString(R.string.alarm_favorite)
                    binding.commentviewitemTextviewProfile.text = str_0
                }//좋아요 이벤트 알람
                1 -> {
                    var str_1 = alarmDTOList[position].userId + " " +
                            activity?.resources?.getString(R.string.alarm_comment) + " of " + alarmDTOList[position].message
                    binding.commentviewitemTextviewProfile.text = str_1
                }//코멘트 이벤트 알람
                2 -> {
                    var str_2 =
                        alarmDTOList[position].userId + " " + activity?.resources?.getString(R.string.alarm_follow)
                    binding.commentviewitemTextviewProfile.text = str_2
                }//팔로우 이벤트 알람
            }
            binding.commentviewitemTextviewComment.visibility = View.INVISIBLE
        }

        override fun getItemCount(): Int {
            Log.d("민희", "getItemCount bidn : ${alarmDTOList.size}")
            return alarmDTOList.size
        }

    }
}