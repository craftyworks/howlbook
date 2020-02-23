package com.ddam40.example.jjinstagram

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    var firebaseAuth: FirebaseAuth? = null
    var googleSignInClient: GoogleSignInClient? = null
    var callbackManager: CallbackManager? = null
    val GOOGLE_LOGIN_CODE = 9001
    var twitterAuthClient: TwitterAuthClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()

        google_sigin_in_button.setOnClickListener { googleLogin() }
        facebook_login_button.setOnClickListener { facebookLogin() }
        email_login_button.setOnClickListener { emailLogin() }
        twitter_login_button.setOnClickListener { twitterLogin() }
    }

    fun moveMainPage(user: FirebaseUser?) {
        if(user != null) {
            Toast.makeText(this, getString(R.string.signin_complete), Toast.LENGTH_LONG).show()
//            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun googleLogin() {
        progress_bar.visibility = View.VISIBLE
        val signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    fun facebookLogin() {
        progress_bar.visibility = View.VISIBLE
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                handleFacebookAccessToken(result?.accessToken!!)
            }

            override fun onCancel() {
                progress_bar.visibility = View.GONE
            }

            override fun onError(error: FacebookException?) {
                progress_bar.visibility = View.GONE
            }
        })
    }

    fun twitterLogin() {
        progress_bar.visibility = View.VISIBLE
        twitterAuthClient?.authorize(this, object: com.twitter.sdk.android.core.Callback<TwitterSession>() {
            override fun success(result: Result<TwitterSession>?) {
                val credential = TwitterAuthProvider.getCredential(result?.data?.authToken?.token!!, result?.data?.authToken?.secret!!)
                firebaseAuth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
                    progress_bar.visibility = View.GONE
                    if(task.isSuccessful) {
                        moveMainPage(firebaseAuth?.currentUser)
                    }
                }

            }

            override fun failure(exception: TwitterException?) {
                //TODO
            }
        })
    }

    fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth?.signInWithCredential(credential)?.addOnCompleteListener { task->
            progress_bar.visibility = View.GONE
            if(task.isSuccessful) {
                moveMainPage(firebaseAuth?.currentUser)
            }
        }
    }

    fun createAndLoginEmail() {
        firebaseAuth?.createUserWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener {task ->
                progress_bar.visibility = View.GONE
                if(task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.signup_complete), Toast.LENGTH_SHORT).show()
                    moveMainPage(firebaseAuth?.currentUser)
                } else if(task.exception?.message.isNullOrEmpty()) {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                } else {
                    signinEmail()
                }
            }
    }

    fun emailLogin() {
        if(email_edittext.text.toString().isNullOrEmpty() || password_edittext.text.toString().isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.signout_fail_null), Toast.LENGTH_SHORT).show()
        } else {
            progress_bar.visibility = View.VISIBLE
            createAndLoginEmail()
        }
    }

    fun signinEmail() {
        firebaseAuth?.signInWithEmailAndPassword(email_edittext.text.toString(), password_edittext.text.toString())
            ?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    moveMainPage(firebaseAuth?.currentUser)
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        twitterAuthClient?.onActivityResult(requestCode, resultCode, data)
        if(requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess) {
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                progress_bar.visibility = View.GONE
            }
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                progress_bar.visibility = View.GONE
                if(task.isSuccessful) {
                    moveMainPage(firebaseAuth?.currentUser)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        moveMainPage(firebaseAuth?.currentUser)
    }
}
