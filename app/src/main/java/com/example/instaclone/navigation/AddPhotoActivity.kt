package com.example.instaclone.navigation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.instaclone.R
import com.example.instaclone.databinding.ActivityAddPhotoBinding
import com.example.instaclone.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPhotoBinding

    val PICK_IMAGE_FROM_ALBUM = 0

    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null
    var auth: FirebaseAuth? = null // 유저 정보 가져오기
    var firebaseStore: FirebaseFirestore? = null // firebase 데이터베이스 사용
    private lateinit var getResult: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 성공했을 때 ( = 사진을 선택했을 때) 선택한 이미지 경로가 전달된다.
        getResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

                if (result.resultCode == RESULT_OK) {
                    //이미지 경로 넘어옴
                    photoUri = result.data?.data
                    binding.addphotoImage.setImageURI(photoUri)
                } else {
                    //취소버튼
                    finish()
                }
            }


        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firebaseStore = FirebaseFirestore.getInstance()

        val photoPickIntent = Intent(Intent.ACTION_PICK) // 선택한 이미지를 가져올 수 있도록 생성
        photoPickIntent.type = "image/*"
        getResult.launch(photoPickIntent)

        binding.addphotoImage.setOnClickListener {
            val photoPickIntent2 = Intent(Intent.ACTION_PICK) // 선택한 이미지를 가져올 수 있도록 생성
            photoPickIntent2.type = "image/*"
            getResult.launch(photoPickIntent2)
        }

        binding.addphotoBtnUpload.setOnClickListener {
            contentUpload()
        }
    }

    /**
     * 사진 업로드
     * 이미지 업로드가 완료되면 이미지 주소를 받아오는 코드를 addOnSuccessListener에 작성.
     * 이미지 주소를 받아오자마자 데이터 모델 ContentDTO를 만들어주고 데이터 값을 넣어준다.
     * setResult는 업로드가 완료되면 finish로 창을 닫아주고
     * 정상적으로 창이 닫혔다는 플래그 값을 넘겨주기 위해 RESULT_OK를 사용
     */
    @SuppressLint("SimpleDateFormat")
    private fun contentUpload() {
        //파일 이름 생성 : 이미지 이름이 중복되지 않도록 파일명에 날짜값을 넣어서 지정한다
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "IMAGE_" + timestamp + "_.png"

        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //


        //파일 업로드 1. 콜백방식
        /*storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri->
                var contentDTO = ContentDTO()
                contentDTO.imageUrl = uri.toString()

                contentDTO.uid = auth?.currentUser?.uid
                contentDTO.userId = auth?.currentUser?.email
                contentDTO.explain = binding.addphotoEditExplain.text.toString()
                contentDTO.timestamp= System.currentTimeMillis()

                firebaseStore?.collection("images")?.document()?.set(contentDTO)
                setResult(Activity.RESULT_OK)
                finish()
            }
            //파일 업로드가 성공한 걸 알 수 있도록 토스트 팝업
            Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_LONG).show()
        }*/

        //파일 업로드 2. 프라미스 방식
        storageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()
            contentDTO.imageUrl = uri.toString()

            contentDTO.uid = auth?.currentUser?.uid
            contentDTO.userId = auth?.currentUser?.email
            contentDTO.explain = binding.addphotoEditExplain.text.toString()
            contentDTO.timestamp = System.currentTimeMillis()

            firebaseStore?.collection("images")?.document()?.set(contentDTO) // 데이터베이스에 입력
            setResult(Activity.RESULT_OK)
            finish()
            //파일 업로드가 성공한 걸 알 수 있도록 토스트 팝업
            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
        }

    }
}