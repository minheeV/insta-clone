package com.example.instaclone.navigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instaclone.R
import com.example.instaclone.databinding.FragmentDetailBinding
import com.example.instaclone.databinding.ItemDetailBinding
import com.example.instaclone.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DetailViewFragment : Fragment() {
    private var _binding: FragmentDetailBinding? = null
    var firestore: FirebaseFirestore? = null
    var uid : String? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid
        binding.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter()
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            firestore?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUIDList.clear()
                    if(querySnapshot == null) return@addSnapshotListener
                    for (snapshot in querySnapshot.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUIDList.add(snapshot.id)
                    }
                    notifyDataSetChanged()
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
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .into(binding.detailviewitemProfileImage)

            //This code is when the button is clicked
            binding.detailviewitemFavoriteImageview.setOnClickListener {
                favoriteEvent(position)
            }

            //This code is when the page is loaded
            if(contentDTOs[position].favorites.containsKey(uid)){ // 좋아요 상태에 따라 이미지 적용
                //This is like status
                binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)
            }else{
                //This is unlike status
                binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }
        //프로파일 이미지 클릭하면 상대방 유저 정보로 이동
            binding.detailviewitemProfileImage.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
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
                }
                transaction.set(tsDoc,contentDTO) // 해당 document에 Dto 객체 저장 , 트랜젝션을 다시 서버로 돌려줌
            }
        }
    }
}