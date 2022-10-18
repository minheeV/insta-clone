package com.example.instaclone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.instaclone.databinding.ActivityMainBinding
import com.example.instaclone.navigation.*
import com.example.instaclone.navigation.util.Constants.Companion.DESTINATION_UID
import com.example.instaclone.navigation.util.Constants.Companion.firebaseAuth
import com.example.instaclone.navigation.util.Constants.Companion.firebaseFirestore
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바 없애기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        binding.bottomNavigation.setOnItemSelectedListener(this)
        //set default screen
        binding.bottomNavigation.selectedItemId = R.id.action_home

        //사진경로 가져올 수 있는 권한 요청
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )
        registerPushToken()
    }

    /**
     * navigationItemSelect 이벤트로 FragmentManager의 replace()함수를 통해 화면 전환을 해준다.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()
        when (item.itemId) {
            R.id.action_home -> {
                val detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, detailViewFragment).commit()
                return true
            }
            R.id.action_search -> {
                val gridFragment = GridFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, gridFragment).commit()
                return true
            }
            R.id.action_add_photo -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                } else {
                    Toast.makeText(this, getString(R.string.text_storage), Toast.LENGTH_LONG).show()
                }
                return true
            }
            R.id.action_favorite_alarm -> {
                val alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, alarmFragment).commit()
                return true
            }
            R.id.action_account -> {
                val userFragment = UserFragment()
                var bundle = Bundle()
                var uid = firebaseAuth.currentUser!!.uid
                bundle.putString(DESTINATION_UID, uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, userFragment).commit()
                return true
            }
        }
        return false
    }

    private fun setToolbarDefault() {
        binding.toolbarUsername.visibility = View.GONE
        binding.toolbarBtnBack.visibility = View.GONE
        binding.toolbarTitleImage.visibility = View.VISIBLE
    }

    /**
     * Push 이벤트 시 등록된 token에만 알림가도록 해주는 메서드
     */
    private fun registerPushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            val token = it.result
            val uid = firebaseAuth.currentUser!!.uid
            val map = mutableMapOf<String, Any>()
            map["pushToken"] = token!!
            firebaseFirestore
                .collection("pushtokens")
                .document(uid!!)
                .set(map)
        }
    }
}