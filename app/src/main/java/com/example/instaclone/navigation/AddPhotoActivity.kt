package com.example.instaclone.navigation

import android.annotation.SuppressLint
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
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPhotoBinding

    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null

    private lateinit var getResult: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    //이미지 경로 넘어옴
                    photoUri = result.data?.data
                    binding.addphotoImage.setImageURI(photoUri)
                }else{
                    //취소버튼
                    finish()
                }
            }


        storage = FirebaseStorage.getInstance()
        val photoPickIntent = Intent(Intent.ACTION_PICK)
        photoPickIntent.type = "image/*"
        getResult.launch(photoPickIntent)

        binding.addphotoBtnUpload.setOnClickListener {
            contentUpload()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun contentUpload(){
        //파일 이름 생성
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "IMAGE_"+ timestamp + "_.png"

        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //파일 업로드
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_LONG).show()
        }

    }
}