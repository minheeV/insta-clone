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
    var auth: FirebaseAuth? = null
    var googleSignInClient:GoogleSignInClient? = null

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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.web_client_id)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        getResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result:ActivityResult->
            if(result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
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

    private fun googleLogin(){
        val signInIntent = googleSignInClient?.signInIntent
        getResult.launch(signInIntent)
    }


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

    private fun signInAndSignUp() {
        auth?.createUserWithEmailAndPassword(
            binding.emailEdittext.text.toString(),
            binding.passwordEdittext.text.toString()
        )
            ?.addOnCompleteListener { task ->
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

    private fun signInEmail() {
        auth?.signInWithEmailAndPassword(
            binding.emailEdittext.text.toString(),
            binding.passwordEdittext.text.toString()
        )
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) { //로그인 성공
                    Toast.makeText(this,"로그인 성공", Toast.LENGTH_LONG).show()
                    moveMainPage(task.result?.user)
                } else { //로그인 실패
                    Toast.makeText(this,task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun moveMainPage(user:FirebaseUser?){
        if(user!=null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

    }
}