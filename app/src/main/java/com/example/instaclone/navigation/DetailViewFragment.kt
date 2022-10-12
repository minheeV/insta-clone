package com.example.instaclone.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.R
import com.example.instaclone.databinding.FragmentDetailBinding
import com.example.instaclone.databinding.ItemDetailBinding
import com.example.instaclone.navigation.model.AlarmDTO
import com.example.instaclone.navigation.model.ContentDTO
import com.example.instaclone.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DetailViewFragment : Fragment() {
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var imagesSnapshot: ListenerRegistration? = null
    private lateinit var binding: FragmentDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid
        binding.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter()
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        imagesSnapshot?.remove()
    }


    /**
     * 사용자의 ID, Profile 이미지, 업로드 한 이미지, 좋아요 버튼, 댓글 버튼, 좋아요 갯수, 글 내용에 대한 정보를 담는 recyclerview
     */
    @SuppressLint("NotifyDataSetChanged")
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf() // 업로드 내용
        var contentUIDList: ArrayList<String> = arrayListOf() // 사용자 정보 List
        lateinit var binding: ItemDetailBinding

        // 초기에 firestore에 업로드 된 정보들을 얻어서 list에 add 해준다.
        init {
            firestore?.collection("users")
                ?.document(uid!!)
                ?.get()
                ?.addOnCompleteListener {
                    if (it.isSuccessful) {
                        var userDTO = it.result.toObject(FollowDTO::class.java)
                        if (userDTO?.followings != null) {
                            imagesSnapshot = firestore
                                ?.collection("images")
                                ?.orderBy("timestamp")
                                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    contentDTOs.clear()
                                    contentUIDList.clear()
                                    if (querySnapshot == null) return@addSnapshotListener
                                    for (snapshot in querySnapshot.documents) {
                                        var item = snapshot.toObject(ContentDTO::class.java)
                                        contentDTOs.add(item!!)
                                        contentUIDList.add(snapshot.id)
                                    }
                                    notifyDataSetChanged()
                                }
                        }
                    }
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            binding = ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }

        inner class CustomViewHolder(binding: ItemDetailBinding) :
            RecyclerView.ViewHolder(binding.root) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            // Profile Image 가져오기
            firestore?.collection("profileImages")
                ?.document(contentDTOs[position].uid!!)
                ?.get()
                ?.addOnCompleteListener {
                    if (it.isSuccessful) {
                        val url = it.result["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(binding.detailviewitemProfileImage)
                    }
                }

            //프로파일 이미지 클릭하면 상대방 유저 정보로 이동
            binding.detailviewitemProfileImage.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.main_content, fragment)?.commit()
            }

            //user id
            binding.detailviewitemProfileTextview.text = contentDTOs[position].userId
            //image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .into(binding.detailviewitemImageviewContent)
            //explain of content
            binding.detailviewitemExplainTextview.text = contentDTOs[position].explain
            //likes
            binding.detailviewitemFavoritecounterTextview.text =
                "Likes  ${contentDTOs[position].favoriteCount}"
            //profile
            //Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).into(binding.detailviewitemProfileImage)

            //This code is when the button is clicked
            binding.detailviewitemFavoriteImageview.setOnClickListener {
                favoriteEvent(position)
            }

            //This code is when the page is loaded
            if (contentDTOs[position].favorites.containsKey(uid)) { // 좋아요 상태에 따라 이미지 적용
                //This is like status
                binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)
            } else {
                //This is unlike status
                binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }

            binding.detailviewitemCommentImageview.setOnClickListener {
                var intent = Intent(it.context, CommentActivity::class.java)
                intent.putExtra(
                    "contentUid",
                    contentUIDList[position]
                ) // 인텐트 안에 컨텐트 내가 선택한 이미지의 uid넘겨줌
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }


        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        /**
         * 좋아요 시 실행할 이벤트
         */
        private fun favoriteEvent(position: Int) {
            val tsDoc = firestore?.collection("images")
                ?.document(contentUIDList[position]) // images collection에서 원하는 uid의 document에 대한 정보
            // 데이터를 저장하기 위해 transaction 사용
            firestore?.runTransaction { transaction ->
                uid = FirebaseAuth.getInstance().currentUser?.uid // uid 값 가져옴

                val contentDTO = transaction.get(tsDoc!!) // 해당 document 받아오기
                    .toObject(ContentDTO::class.java)//트랜젝션의 데이터를 ContentDTO로 캐스팅

                if (contentDTO!!.favorites.containsKey(uid)) { //좋아요 버튼이 이미 클릭 되어있으면 -> favorites 값이 true이면
                    //When the button is clicked
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO.favorites.remove(uid);
                } else {
                    //When the button is not clicked
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                    contentDTO.favorites[uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!) // 카운트 올라감
                }
                transaction.set(tsDoc, contentDTO) // 해당 document에 Dto 객체 저장 , 트랜젝션을 다시 서버로 돌려줌
            }
        }

        private fun favoriteAlarm(destinationUid: String) {
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        }
    }
}