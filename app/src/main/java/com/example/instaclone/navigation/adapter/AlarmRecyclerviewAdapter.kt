package com.example.instaclone.navigation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.R
import com.example.instaclone.databinding.ItemCommentBinding
import com.example.instaclone.navigation.model.AlarmDTO
import com.example.instaclone.navigation.util.Constants
import com.example.instaclone.navigation.util.Constants.Companion.DESTINATION_UID
import com.example.instaclone.navigation.util.Constants.Companion.firebaseAuth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.reflect.InvocationTargetException

@SuppressLint("NotifyDataSetChanged")
class AlarmRecyclerviewAdapter(context: Context) : RecyclerView.Adapter<AlarmRecyclerviewAdapter.CustomViewHolder>() {
    private var alarmDTOList: ArrayList<AlarmDTO> = arrayListOf() //알람 저장하는 리스트 변수
    private var context: Context

    init {
        this.context = context
        val uid = firebaseAuth.currentUser!!.uid

        try {
            FirebaseFirestore.getInstance() //나에게 도착한 메세지만 필터링
                .collection("alarms")
                .whereEqualTo(DESTINATION_UID, uid)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    alarmDTOList.clear()
                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                    }
                    alarmDTOList.sortByDescending { it.timestamp }
                    notifyDataSetChanged()
                }
        } catch (e: InvocationTargetException) {
            Log.e("AlarmRecycler", "Invocation Target Exception : $e")
        } catch (e: IllegalAccessException) {
            Log.e("AlarmRecycler", "Illegal Access Exception : $e")
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return CustomViewHolder(binding)
    }

    inner class CustomViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root){
            fun bind(){
                val position = adapterPosition
                FirebaseFirestore.getInstance().collection("profilesImages")
                    .document(alarmDTOList[position].uid)
                    .get().addOnCompleteListener {
                        if (it.isSuccessful) {
                            val url = it.result!!["image"]
                            Glide.with(binding.root.context)
                                .load(url)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .apply(RequestOptions().circleCrop())
                                .into(binding.commentviewitemImageviewProfile)
                        }
                    }
                when (alarmDTOList[position].kind) {
                    0 -> {
                        var str_0 =
                            alarmDTOList[position].userId + context.resources.getString(R.string.alarm_favorite)
                        binding.commentviewitemTextviewProfile.text = str_0
                    }//좋아요 이벤트 알람
                    1 -> {
                        var str_1 = alarmDTOList[position].userId + " " +
                                context.resources.getString(R.string.alarm_comment) + " of " + alarmDTOList[position].message
                        binding.commentviewitemTextviewProfile.text = str_1
                    }//코멘트 이벤트 알람
                    2 -> {
                        var str_2 =
                            alarmDTOList[position].userId + " " + context.resources.getString(R.string.alarm_follow)
                        binding.commentviewitemTextviewProfile.text = str_2
                    }//팔로우 이벤트 알람
                }
                binding.commentviewitemTextviewComment.visibility = View.INVISIBLE
            }
        }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return alarmDTOList.size
    }

}