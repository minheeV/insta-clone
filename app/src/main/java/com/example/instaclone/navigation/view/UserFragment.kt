package com.example.instaclone.navigation.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.instaclone.LoginActivity
import com.example.instaclone.MainActivity
import com.example.instaclone.R
import com.example.instaclone.databinding.FragmentUserBinding
import com.example.instaclone.navigation.view.adapter.UserFragmentRecyclerViewAdapter
import com.example.instaclone.navigation.model.AlarmDTO
import com.example.instaclone.navigation.model.ContentDTO
import com.example.instaclone.navigation.model.FollowDTO
import com.example.instaclone.navigation.util.Constants.Companion.DESTINATION_UID
import com.example.instaclone.navigation.util.Constants.Companion.firebaseAuth
import com.example.instaclone.navigation.util.Constants.Companion.firebaseFirestore
import com.example.instaclone.navigation.util.FcmPush
import com.example.instaclone.navigation.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint

//내 계정, 상대방 계정
@AndroidEntryPoint
class UserFragment : Fragment() {
    lateinit var binding: FragmentUserBinding
    private val userVM: UserViewModel by viewModels()

    private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
    var fireStore: FirebaseFirestore? = null
    var uid: String? = null
    var currentUserUid: String? = null // 내 계정인지 상대방 계정인지 판단
    var imageUrl = "";

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            val imageUrl = it.data?.data
            val uid = firebaseAuth.currentUser!!.uid
            val storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages")
                .child(uid) // userProfileImages 이미지 저장할 폴더명
            storageRef.putFile(imageUrl!!).continueWithTask {
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                val map = HashMap<String, Any>()
                map["image"] = uri.toString()
                firebaseFirestore.collection("profileImages").document(uid).set(map)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user, container, false)
        fireStore = FirebaseFirestore.getInstance()
        currentUserUid = firebaseAuth.currentUser!!.uid

        binding.vm = userVM
        observeUserViewModel()

        if (arguments != null) {
            uid = arguments?.getString(DESTINATION_UID)
            binding.apply {
                if (uid != null && uid == currentUserUid) {
                    //MyPage
                    followChk = activity?.resources?.getString(R.string.signout)

                    accountBtnFollowSignout.setOnClickListener { // 액티비티 종료 및 login 액티비티 이동, firebase auth 값에 signOut
                        activity?.finish()
                        startActivity(Intent(activity, LoginActivity::class.java))
                        firebaseAuth.signOut()
                    }
                } else {
                    //OtherUserPage
                    followChk = activity?.resources?.getString(R.string.follow)
                    (activity as MainActivity).binding.apply {
                        toolbarUsername.text = arguments?.getString("userId")
                        toolbarBtnBack.setOnClickListener {  // 뒤로가기 이벤트
                            bottomNavigation.selectedItemId = R.id.action_home
                        }
                        toolbarTitleImage.visibility = View.GONE
                        toolbarUsername.visibility = View.VISIBLE
                        toolbarBtnBack.visibility = View.VISIBLE
                    } //누구의 유저 페이지인지 텍스트 백버튼 활성화
                    accountBtnFollowSignout.setOnClickListener {
                        requestFollow()
                    }
                }
            }
            userVM.getContentList(uid!!)
        }

        binding.accountIvProfile.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startForResult.launch(photoPickerIntent)
            }

        }

        //getFollowerAndFollowing()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getProfileImage()
        getFollowing()
        getFollower()
    }

    private fun observeUserViewModel() {
        userVM.contentDTOList.observe(viewLifecycleOwner) {
            binding.accountTvPostCount.text = contentDTOs.size.toString()

            binding.accountRecyclerview.adapter =
                UserFragmentRecyclerViewAdapter()
            binding.accountRecyclerview.layoutManager = GridLayoutManager(requireActivity(), 3)
            binding.invalidateAll()
        }
    }

    private fun requestFollow() {
        //Save data to my account
        val tsDocFollowing = fireStore?.collection("users")?.document(currentUserUid!!)
        fireStore?.runTransaction { transaction ->
            var followingDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)

            if (followingDTO == null) {
                followingDTO = FollowDTO()
                followingDTO.followingCount = 1
                followingDTO.followings[uid!!] = true

                transaction.set(tsDocFollowing, followingDTO)//data db
                return@runTransaction
            }

            followingDTO.apply {
                if (followings.containsKey(uid)) {
                    //It remove following third person when a third person follow me //팔로우를 한 상태 -> 취소
                    followingCount -= 1
                    followings.remove(uid)//상대방 uid 제거
                } else {
                    followingCount += 1
                    followings[uid!!] = true //상대방 uid 제거
                }
                transaction.set(tsDocFollowing, this) // 디비 저장
                return@runTransaction // transaction 닫아줌
            }

        }
        //Save data to third person 내가 팔로잉할 상대방 계정 접근
        val tsDocFollower = fireStore?.collection("users")?.document(uid!!)
        fireStore?.runTransaction { transaction ->
            var followerDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)

            if (followerDTO == null) {
                followerDTO = FollowDTO()
                followerDTO.followerCount = 1
                followerDTO.followers[currentUserUid!!] = true
                followAlarm(uid!!)

                transaction.set(tsDocFollower, followerDTO!!)
                return@runTransaction
            }
            followerDTO.apply {
                if (followers.containsKey(currentUserUid)) { // 상대방 계정 팔로우 햇을 경우
                    //It remove following third person when a third person follow me //팔로우를 한 상태 -> 취소
                    followerCount -= 1
                    followers.remove(currentUserUid)//상대방 uid 제거
                } else {
                    followerCount += 1
                    followers[currentUserUid!!] = true //상대방 uid 제거
                    followAlarm(uid!!)
                }
                transaction.set(tsDocFollower, this)
                return@runTransaction
            }
        }
    }

    private fun getFollowing() { // following 하고 있는 count
        fireStore?.collection("users")
            ?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                val followDTO =
                    documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener
                binding.followingCnt = followDTO.followingCount.toString()
            }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getFollower() {
        fireStore?.collection("users")
            ?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                val followDTO =
                    documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener
                binding.followerCnt = followDTO.followerCount.toString()
                if (followDTO.followers.containsKey(currentUserUid!!)) {// 팔로워 하고있으면 버튼 반환
                    binding.accountBtnFollowSignout.text =
                        activity?.resources?.getString(R.string.follow_cancel)
                    binding.accountBtnFollowSignout
                        .background
                        .colorFilter =
                        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            R.color.colorLightGray, BlendModeCompat.MULTIPLY
                        )
                } else {
                    if (uid != currentUserUid) {
                        binding.accountBtnFollowSignout.text =
                            activity?.resources?.getString(R.string.follow)
                        binding.accountBtnFollowSignout.background.colorFilter = null
                    }
                }
            }

    }

    private fun followAlarm(destinationUid: String) {
        AlarmDTO().apply {
            this.destinationUid = destinationUid
            userId = firebaseAuth.currentUser!!.email!!
            kind = 2
            timestamp = System.currentTimeMillis()
            firebaseFirestore.collection("alarms").document().set(this)

        }


        val message =
            firebaseAuth.currentUser!!.email + context?.resources?.getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "InstaClone", message)
    }

    /**
     * 올린 이미지를 다운로드 받는 함수
     */
    private fun getProfileImage() {
        //실시간 변화 체크 snapshot
        fireStore?.collection("profileImages")?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) return@addSnapshotListener
                if (documentSnapshot.data != null) {
                    imageUrl = documentSnapshot.data!!["image"].toString() // image 키값
                }
            }
    }
}