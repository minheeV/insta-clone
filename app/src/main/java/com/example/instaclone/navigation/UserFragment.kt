package com.example.instaclone.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.LoginActivity
import com.example.instaclone.MainActivity
import com.example.instaclone.R
import com.example.instaclone.databinding.FragmentUserBinding
import com.example.instaclone.navigation.model.ContentDTO
import com.example.instaclone.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

//내 계정, 상대방 계정
class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    var fireStore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var currentUserUid: String? = null // 내 계정인지 상대방 계정인지 판단

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            val imageUrl = it.data?.data
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages")
                .child(uid!!) // userProfileImages 이미지 저장할 폴더명
            storageRef.putFile(imageUrl!!).continueWithTask {
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                val map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }
    }

    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        uid = arguments?.getString("destinationUid")
        fireStore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if (uid == currentUserUid) {
            //MyPage
            binding.accountBtnFollowSignout.text = getString(R.string.signout)
            binding.accountBtnFollowSignout.setOnClickListener { // 액티비티 종료 및 login 액티비티 이동, firebase outh 값에 signout
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        } else {
            //OtherUserPage
            binding.accountBtnFollowSignout.text = getString(R.string.follow)
            val mainActivity = (activity as MainActivity) //누구의 유저 페이지인지 텍스트 백버튼 활성화
            mainActivity.binding.toolbarUsername.text = arguments?.getString("userId")
            mainActivity.binding.toolbarBtnBack.setOnClickListener {  // 뒤로가기 이벤트
                mainActivity.binding.bottomNavigation.selectedItemId = R.id.action_home
            }
            mainActivity.binding.toolbarTitleText.visibility = View.GONE
            mainActivity.binding.toolbarUsername.visibility = View.VISIBLE
            mainActivity.binding.toolbarBtnBack.visibility = View.VISIBLE
            binding.accountBtnFollowSignout.setOnClickListener {
                requestFollow()
            }
        }

        binding.accountRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview.layoutManager = GridLayoutManager(requireActivity(), 3)

        binding.accountIvProfile.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"

            startForResult.launch(photoPickerIntent)
        }

        getProfileImage()
        getFollowerAndFollowing()
        return binding.root
    }

    private fun requestFollow() {
        //Save data to my account
        val tsDocFollowing = fireStore?.collection("users")?.document(currentUserUid!!)
        fireStore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followers[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)//data db
                return@runTransaction
            }
            if (followDTO.followings.containsKey(uid)) {
                //It remove following third person when a third person follow me //팔로우를 한 상태 -> 취소
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followers.remove(uid)//상대방 uid 제거
            } else {
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followers[uid!!] = true //상대방 uid 제거
            }
            transaction.set(tsDocFollowing, followDTO) // 디비 저장
            return@runTransaction // transaction 닫아줌
        }
        //Save data to third person 내가 팔로잉할 상대방 계정 접근
        val tsDocFollower = fireStore?.collection("users")?.document(uid!!)
        fireStore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if (followDTO!!.followers.containsKey(currentUserUid)) {// 상대방 계정 팔로우 햇을 경우
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true //상대방 uid 제거
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    /**
     * 화면에 카운터 보여주는 함수
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getFollowerAndFollowing() {
        fireStore?.collection("users")?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) return@addSnapshotListener
                val followDTO = documentSnapshot.toObject(FollowDTO::class.java)
                if (followDTO?.followingCount != null) {
                    binding.accountTvFollowingCount.text = followDTO.followingCount.toString()
                }
                if (followDTO?.followerCount != null) {
                    binding.accountTvFollowerCount.text = followDTO.followerCount.toString()
                    if (followDTO.followers.containsKey(currentUserUid!!)) {// 팔로워 하고있으면 버튼 반환
                        binding.accountBtnFollowSignout.text = getString(R.string.follow_cancel)
                        binding.accountBtnFollowSignout.background.colorFilter =
                            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                R.color.colorLightGray, BlendModeCompat.MULTIPLY
                            )
                    } else {
                        binding.accountBtnFollowSignout.text = getString(R.string.follow)
                        if (uid != currentUserUid) {
                            binding.accountBtnFollowSignout.background.colorFilter = null
                        }
                    }
                }
            } // 내페이지 클릭햇을 땐 내 uid, 상대방 클릭할 시 상대방 uid
    }

    /**
     * 올린 이미지를 다운로드 받는 함수
     */
    fun getProfileImage() {
        //실시간 변화 체크 snapshot
        fireStore?.collection("profileImages")?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) return@addSnapshotListener
                if (documentSnapshot.data != null) {
                    val url = documentSnapshot.data!!["image"] // image 키값
                    Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop())
                        .into(
                            binding.accountIvProfile
                        )
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            fireStore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (querySnapshot == null) return@addSnapshotListener
                    //Get data
                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    binding.accountTvPostCount.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                } // 내가 올린 이미지만 내 유아이디일때만 검색
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3 //폭의 3분의 1 값
            val imageview = ImageView(parent.context)
            imageview.layoutParams = ConstraintLayout.LayoutParams(width, width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) :
            RecyclerView.ViewHolder(imageview) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size

        }
    }
}