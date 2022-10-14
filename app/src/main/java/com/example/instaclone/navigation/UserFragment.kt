package com.example.instaclone.navigation

import android.Manifest
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instaclone.LoginActivity
import com.example.instaclone.MainActivity
import com.example.instaclone.R
import com.example.instaclone.databinding.FragmentUserBinding
import com.example.instaclone.navigation.adapter.UserFragmentRecyclerViewAdapter
import com.example.instaclone.navigation.model.AlarmDTO
import com.example.instaclone.navigation.model.ContentDTO
import com.example.instaclone.navigation.model.FollowDTO
import com.example.instaclone.navigation.util.Constants
import com.example.instaclone.navigation.util.Constants.Companion.firebaseAuth
import com.example.instaclone.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

//내 계정, 상대방 계정
class UserFragment : Fragment() {
    lateinit var binding: FragmentUserBinding
    private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserBinding.inflate(inflater, container, false)
        fireStore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid
        if (arguments != null) {
            uid = arguments?.getString("destinationUid")
            if (uid != null && uid == currentUserUid) {
                //MyPage
                binding.accountBtnFollowSignout.text =
                    activity?.resources?.getString(R.string.signout)
                binding.accountBtnFollowSignout.setOnClickListener { // 액티비티 종료 및 login 액티비티 이동, firebase auth 값에 signOut
                    activity?.finish()
                    startActivity(Intent(activity, LoginActivity::class.java))
                    auth?.signOut()
                }
            } else {
                //OtherUserPage
                binding.accountBtnFollowSignout.text =
                    activity?.resources?.getString(R.string.follow)
                val mainActivity = (activity as MainActivity) //누구의 유저 페이지인지 텍스트 백버튼 활성화
                mainActivity.binding.toolbarUsername.text = arguments?.getString("userId")
                mainActivity.binding.toolbarBtnBack.setOnClickListener {  // 뒤로가기 이벤트
                    mainActivity.binding.bottomNavigation.selectedItemId = R.id.action_home
                }
                mainActivity.binding.toolbarTitleImage.visibility = View.GONE
                mainActivity.binding.toolbarUsername.visibility = View.VISIBLE
                mainActivity.binding.toolbarBtnBack.visibility = View.VISIBLE
                binding.accountBtnFollowSignout.setOnClickListener {
                    requestFollow()
                }
            }
        }

        fireStore!!.collection("images").whereEqualTo("uid", uid)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (querySnapshot == null) return@addSnapshotListener
                //Get data
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                binding.accountTvPostCount.text = contentDTOs.size.toString()
            }

        binding.accountRecyclerview.adapter =
            UserFragmentRecyclerViewAdapter(fireStore!!, contentDTOs, uid!!, requireActivity())
        binding.accountRecyclerview.layoutManager = GridLayoutManager(requireActivity(), 3)

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

    private fun requestFollow() {
        //Save data to my account
        val tsDocFollowing = fireStore?.collection("users")?.document(currentUserUid!!)
        fireStore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)//data db
                return@runTransaction
            }
            if (followDTO.followings.containsKey(uid)) {
                //It remove following third person when a third person follow me //팔로우를 한 상태 -> 취소
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(uid)//상대방 uid 제거
            } else {
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true //상대방 uid 제거
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
                followAlarm(uid!!)

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if (followDTO!!.followers.containsKey(currentUserUid)) {// 상대방 계정 팔로우 햇을 경우
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true //상대방 uid 제거
                followAlarm(uid!!)
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    private fun getFollowing() { // following 하고 있는 count
        fireStore?.collection("users")
            ?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                val followDTO =
                    documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener
                binding.accountTvFollowingCount.text = followDTO.followingCount.toString()
            }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getFollower() {
        fireStore?.collection("users")
            ?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                val followDTO =
                    documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener
                binding.accountTvFollowerCount.text = followDTO.followerCount.toString()
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
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        val message =
            firebaseAuth.currentUser?.email + context?.resources?.getString(R.string.alarm_follow)
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
                    val url = documentSnapshot.data!!["image"] // image 키값
                    Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop())
                        .into(
                            binding.accountIvProfile
                        )
                }
            }
    }
}