package com.example.instaclone.navigation.view.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.instaclone.R
import com.example.instaclone.databinding.ItemDetailBinding
import com.example.instaclone.navigation.view.CommentActivity
import com.example.instaclone.navigation.view.UserFragment
import com.example.instaclone.navigation.model.AlarmDTO
import com.example.instaclone.navigation.model.ContentDTO
import com.example.instaclone.navigation.util.Constants.Companion.DESTINATION_UID
import com.example.instaclone.navigation.util.Constants.Companion.firebaseAuth
import com.example.instaclone.navigation.util.Constants.Companion.firebaseFirestore
import com.example.instaclone.navigation.util.FcmPush
import com.google.firebase.firestore.FirebaseFirestore


/**
 * 사용자의 ID, Profile 이미지, 업로드 한 이미지, 좋아요 버튼, 댓글 버튼, 좋아요 갯수, 글 내용에 대한 정보를 담는 recyclerview
 */

@SuppressLint("NotifyDataSetChanged")
class DetailViewRecyclerViewAdapter :
    RecyclerView.Adapter<DetailViewRecyclerViewAdapter.CustomViewHolder>() {
    var contentDTOs: ArrayList<ContentDTO> = arrayListOf() // 업로드 내용
    var contentUIDs: ArrayList<String> = arrayListOf() // 사용자 정보 List
    lateinit var context: Context

    // 초기에 fireStore 에 업로드 된 정보들을 얻어서 list 에 add 해준다.

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding =
            ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = binding.root.context
        return CustomViewHolder(binding)
    }


    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return contentDTOs.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    /**
     * 좋아요 시 실행할 이벤트
     */
    private fun favoriteEvent(position: Int) {
        val tsDoc = firebaseFirestore.collection("images")
            .document(contentUIDs[position]) // images collection에서 원하는 uid의 document에 대한 정보

        // 데이터를 저장하기 위해 transaction 사용
        firebaseFirestore.runTransaction { transaction ->
            // uid 값 가져옴
            transaction.get(tsDoc) // 해당 document 받아오기
                .toObject(ContentDTO::class.java)?.apply {
                    if (favorites.containsKey(uid)) { // 이미 좋아요를 눌렀을 경욱 -> 좋아요 취소
                        favoriteCount -= 1
                        favorites.remove(uid)
                    } else {
                        favoriteCount += 1
                        favorites[uid] = true
                        favoriteAlarm(firebaseAuth.currentUser!!.uid) // 카운트 올라감
                    }
                    transaction.set(tsDoc, this) // 해당 document에 Dto 객체 저장 , 트랜젝션을 다시 서버로 돌려줌
                }//트랜젝션의 데이터를 ContentDTO로 캐스팅
        }
    }

    private fun favoriteAlarm(destinationUid: String) {
        AlarmDTO().apply {
            this.destinationUid = destinationUid
            userId = firebaseAuth.currentUser!!.email!!
            uid = firebaseAuth.currentUser!!.uid
            kind = 0
            timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(this)
        }

        val message =
            firebaseAuth.currentUser!!.email + context.resources.getString(R.string.alarm_favorite)
        FcmPush.instance.sendMessage(destinationUid, "InstaClone", message)
    }

    inner class CustomViewHolder(private val binding: ItemDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            val position = adapterPosition
            binding.apply {

                //프로파일 이미지 클릭하면 상대방 유저 정보로 이동
                profileImage.setOnClickListener {
                    val fragment = UserFragment()
                    Bundle().apply {
                        putString(DESTINATION_UID, contentDTOs[position].uid)
                        putString("userId", contentDTOs[position].userId)
                        fragment.arguments = this
                    }
                    (context as FragmentActivity).supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content, fragment).commit()
                }
                //user id
                profileTextview.text = contentDTOs[position].userId

                imageUrl = contentDTOs[position].imageUrl

                // Profile Image 가져오기
                firebaseFirestore.collection("profileImages")
                    .document(contentDTOs[position].uid)
                    .get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            this.profileImageUrl = it.result["image"].toString()
                            //this.invalidateAll()
                            Log.d("DetailViewRecyclerView", "position $position")
                        }
                    }

                //explain of content
                explainTextview.text = contentDTOs[position].explain

                //likes
                favoritecounterTextview.text =
                    "Likes ${contentDTOs[position].favoriteCount}"

                //This code is when the button is clicked
                favoriteImageview.setOnClickListener {
                    Log.d("DetailViewRecycler", "setOnClickListener : $position")
                    favoriteEvent(adapterPosition)
                }

                //This code is when the page is loaded
                if (contentDTOs[position].favorites.containsKey(firebaseAuth.currentUser!!.uid)) { // 좋아요 상태에 따라 이미지 적용
                    Log.d("DetailViewRecycler", "like position : $position")
                    //This is like status
                    this.favoriteImageview.setImageResource(R.drawable.ic_favorite)
                } else {
                    Log.d("DetailViewRecycler", "unlike position : $position")
                    //This is unlike status
                    this.favoriteImageview.setImageResource(R.drawable.ic_favorite_border)
                }

                commentImageview.setOnClickListener {
                    Intent(it.context, CommentActivity::class.java).apply {
                        putExtra(
                            "contentUid",
                            contentUIDs[position]
                        ) // 인텐트 안에 컨텐트 내가 선택한 이미지의 uid넘겨줌
                        putExtra(DESTINATION_UID, contentDTOs[position].uid)
                        context.startActivity(this)
                    }
                }
            }
        }
    }

}