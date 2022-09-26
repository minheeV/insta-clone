package com.example.instaclone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.instaclone.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding

    var auth: FirebaseAuth? = null // firebase 인증 라이브러리 객체
    var googleSignInClient:GoogleSignInClient? = null // google 계정 로그인

    lateinit var getResult : ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.emailLoginButton.setOnClickListener {
            signInAndSignUp()
        }
        binding.googleSignInButton.setOnClickListener {
            googleLogin()
        }

        // 사용자 ID 및 기본 프로필 정보 요청하도록 구글 로그인 구성
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.web_client_id)
            .requestEmail() // 이메일 주소 요청
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)

        getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result:ActivityResult->
            if(result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // 로그인에 성공하게 되면 구글에서는 로그인한 사용자의 정보를 얻을 때 필요한 IdToken을 전달
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account.idToken)
                    Log.d("GoogleLogin", "firebaseAuthWithGoogle: " + account.id)
                } catch (e: ApiException) {
                    Log.d("GoogleLogin", "Google sign in failed: " + e.message)
                }
            } else if(result.resultCode == RESULT_CANCELED){
                println(result.resultCode)
            }
        }
    }

    //자동 로그인 기능
    override fun onStart() {
        super.onStart()
        moveMainPage(auth?.currentUser)
    }

    /**
     * 구글 로그인 화면
     */
    private fun googleLogin(){
        val signInIntent = googleSignInClient?.signInIntent
        getResult.launch(signInIntent)
    }


    /**
     * IdToken을 활용하여 Firebase 인증하기
     * IdToken으로 Firebase 사용자 인증 정보로 교환을 한 후 교환된 정보를 이용해 Firebase에 인증할 수 있다.
     */
    private fun firebaseAuthWithGoogle(account: String?){
        val credential = GoogleAuthProvider.getCredential(account,null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //아이디 패스워드 맞았을 때
                    moveMainPage(task.result?.user)
                } else {
                    //로그인 실패 틀렸을 때
                    Toast.makeText(this,task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * 입력한 이메일과 비밀번호가 기존에 없었던 이메일이라면 createUserWithEmailAndPassword를 통해 회원가입하고
     * 있었던 이메일이라면 signInwithEmailAndPassword를 통해 로그인 하게 된다.
     */
    private fun signInAndSignUp() {
        auth?.createUserWithEmailAndPassword(
            binding.emailEdittext.text.toString().trim(),
            binding.passwordEdittext.text.toString()
        )?.addOnCompleteListener { task ->
                when {
                    task.isSuccessful -> { // id 생성 성공
                        Toast.makeText(this,"회원가입 성공", Toast.LENGTH_LONG).show()
                        moveMainPage(task.result?.user)
                    }
                    else -> { // 회원가입도 아니고 에러도 아니기 때문에 로그인 성공
                        signInEmail()
                    }
                }
            }
    }

    /**
     * 기존에 있었던 이메일이므로 로그인하게 된다.
     */
    private fun signInEmail() {
        auth?.signInWithEmailAndPassword(
            binding.emailEdittext.text.toString().trim(),
            binding.passwordEdittext.text.toString()
        )?.addOnCompleteListener { task ->
                if (task.isSuccessful) { //로그인 성공
                    Toast.makeText(this,"로그인 성공", Toast.LENGTH_LONG).show()
                    moveMainPage(task.result?.user)
                } else { //로그인 실패
                    Toast.makeText(this,task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * 로그인에 성공하게 되면 메인화면으로 넘어가도록 하는 함수
     */
    private fun moveMainPage(user:FirebaseUser?){
        if(user!=null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }
}