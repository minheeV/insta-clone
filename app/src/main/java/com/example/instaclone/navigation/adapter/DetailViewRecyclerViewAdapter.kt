package com.example.instaclone.navigation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.R
import com.example.instaclone.databinding.ItemDetailBinding
import com.example.instaclone.navigation.CommentActivity
import com.example.instaclone.navigation.UserFragment
import com.example.instaclone.navigation.model.AlarmDTO
import com.example.instaclone.navigation.model.ContentDTO
import com.example.instaclone.navigation.model.FollowDTO
import com.example.instaclone.navigation.util.Constants.Companion.DESTINATION_UID
import com.example.instaclone.navigation.util.Constants.Companion.firebaseAuth
import com.example.instaclone.navigation.util.Constants.Companion.firebaseFirestore
import com.example.instaclone.navigation.util.FcmPush
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


/**
 * 사용자의 ID, Profile 이미지, 업로드 한 이미지, 좋아요 버튼, 댓글 버튼, 좋아요 갯수, 글 내용에 대한 정보를 담는 recyclerview
 */

@SuppressLint("NotifyDataSetChanged")
class DetailViewRecyclerViewAdapter(context: Context) :
    RecyclerView.Adapter<DetailViewRecyclerViewAdapter.CustomViewHolder>() {
    private var contentDTOs: ArrayList<ContentDTO> = arrayListOf() // 업로드 내용
    private var contentUIDList: ArrayList<String> = arrayListOf() // 사용자 정보 List
    var uid: String
    var context: Context

    // 초기에 fireStore 에 업로드 된 정보들을 얻어서 list 에 add 해준다.
    init {
        uid = firebaseAuth.currentUser!!.uid
        this.context = context
        firebaseFirestore.collection("users")
            .document(uid)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val userDTO = it.result.toObject(FollowDTO::class.java)
                    if (userDTO?.followings != null) {
                        firebaseFirestore
                            .collection("images")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                contentDTOs.clear()
                                contentUIDList.clear()
                                if (querySnapshot == null) return@addSnapshotListener
                                for (snapshot in querySnapshot.documents) {
                                    val item = snapshot.toObject(ContentDTO::class.java)
                                    contentDTOs.add(item!!)
                                    contentUIDList.add(snapshot.id)
                                }
                                notifyDataSetChanged()
                            }
                    }
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding = ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(binding)
    }

    inner class CustomViewHolder(private val binding: ItemDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            val position = adapterPosition
            //프로파일 이미지 클릭하면 상대방 유저 정보로 이동
            binding.detailviewitemProfileImage.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString(DESTINATION_UID, contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                (context as FragmentActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, fragment).commit()
            }

            //user id
            binding.detailviewitemProfileTextview.text = contentDTOs[position].userId
            //image
            Glide.with(itemView.context)
                .load(contentDTOs[position].imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.detailviewitemImageviewContent)

            // Profile Image 가져오기
            firebaseFirestore.collection("profileImages")
                .document(contentDTOs[position].uid)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val url = it.result["image"]
                        Log.d("DetailViewRecyclerView", "position $position : $url")
                        Glide.with(itemView.context)
                            .load(url)
                            .apply(
                                RequestOptions().circleCrop()
                                    .skipMemoryCache(true)
                                    .diskCacheStrategy(
                                        DiskCacheStrategy.NONE
                                    )
                            ).into(binding.detailviewitemProfileImage)
                    }
                }

            //explain of content
            binding.detailviewitemExplainTextview.text = contentDTOs[position].explain
            //likes
            binding.detailviewitemFavoritecounterTextview.text =
                "Likes ${contentDTOs[position].favoriteCount}"
            //profile
            //Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(binding.detailviewitemProfileImage)

            //This code is when the button is clicked
            binding.detailviewitemFavoriteImageview.setOnClickListener {
                Log.d("DetailViewRecycler", "setOnClickListener : $position")
                favoriteEvent(adapterPosition)
            }

            //This code is when the page is loaded
            if (contentDTOs[position].favorites.containsKey(uid)) { // 좋아요 상태에 따라 이미지 적용
                Log.d("DetailViewRecycler", "like position : $position")
                //This is like status
                binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)
            } else {
                Log.d("DetailViewRecycler", "unlike position : $position")
                //This is unlike status
                binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }

            binding.detailviewitemCommentImageview.setOnClickListener {
                val intent = Intent(it.context, CommentActivity::class.java)
                intent.putExtra(
                    "contentUid",
                    contentUIDList[position]
                ) // 인텐트 안에 컨텐트 내가 선택한 이미지의 uid넘겨줌
                intent.putExtra(DESTINATION_UID, contentDTOs[position].uid)
                context.startActivity(intent)
            }
        }

        fun bind2() {
            val pos = adapterPosition
            binding.detailviewitemProfileImage.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString(DESTINATION_UID, contentDTOs[pos].uid)
                bundle.putString("userId", contentDTOs[pos].userId)
                fragment.arguments = bundle
                (context as FragmentActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, fragment).commit()
            }

            //user id
            binding.detailviewitemProfileTextview.text = contentDTOs[pos].userId
            binding.detailviewitemFavoriteImageview.setOnClickListener {
                Log.d("DetailViewRecycler", "postio : $pos")
                binding.detailviewitemExplainTextview.text = "뭐지"
            }
        }
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
            .document(contentUIDList[position]) // images collection에서 원하는 uid의 document에 대한 정보
        // 데이터를 저장하기 위해 transaction 사용
        firebaseFirestore.runTransaction { transaction ->
            uid = firebaseAuth.currentUser!!.uid // uid 값 가져옴

            val contentDTO = transaction.get(tsDoc) // 해당 document 받아오기
                .toObject(ContentDTO::class.java)//트랜젝션의 데이터를 ContentDTO로 캐스팅

            if (contentDTO!!.favorites.containsKey(uid)) { //좋아요 버튼이 이미 클릭 되어있으면 -> favorites 값이 true이면
                //When the button is clicked
                contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                contentDTO.favorites.remove(uid);
            } else {
                //When the button is not clicked
                contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                contentDTO.favorites[uid] = true
                favoriteAlarm(contentDTOs[position].uid) // 카운트 올라감
            }
            transaction.set(tsDoc, contentDTO) // 해당 document에 Dto 객체 저장 , 트랜젝션을 다시 서버로 돌려줌
            //notifyItemChanged(position)
        }
    }

    private fun favoriteAlarm(destinationUid: String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = firebaseAuth.currentUser!!.email!!
        alarmDTO.uid = firebaseAuth.currentUser!!.uid
        alarmDTO.kind = 0
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        val message =
            firebaseAuth.currentUser!!.email + context.resources.getString(R.string.alarm_favorite)
        FcmPush.instance.sendMessage(destinationUid, "InstaClone", message)
    }
}