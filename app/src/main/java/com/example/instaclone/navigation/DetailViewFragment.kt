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
import com.google.firebase.firestore.FirebaseFirestore

class DetailViewFragment : Fragment() {
    private var _binding: FragmentDetailBinding? = null
    var firestore: FirebaseFirestore? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
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
                ?.addSnapshotListener { querySnapshot, firebaseFirestorException ->
                    contentDTOs.clear()
                    contentUIDList.clear()
                    for (snapshot in querySnapshot!!.documents) {
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

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }
    }
}