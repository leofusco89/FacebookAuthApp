package com.example.facebookauthapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private var mCallbackManager: CallbackManager? = null
    private var mFirebaseAuth: FirebaseAuth? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var accessTokenTracker: AccessTokenTracker? = null
    private var TAG = "FacebookAuthentication"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Get FirebaseAuth and FabookSDK instance
        mFirebaseAuth = FirebaseAuth.getInstance()
        FacebookSdk.sdkInitialize(applicationContext)

        //Create a callbackManager to handle login responses
        mCallbackManager = CallbackManager.Factory.create()

        //Ask for read permission for email and public profile
        fb_loginButton.setReadPermissions("email", "public_profile")

        //Register callback to respond to success, cancel or error result on login try
        fb_loginButton.registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                //If login succeeds, the LoginResult parameter has the new AccessToken, and the
                //most recently granted or declined permissions.
                Log.d(TAG, "onSuccess: $loginResult")
                //Handle token to login
                handleFacebookToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "onCancel")
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "onError: $error")
            }
        })

//        For AuthStateListener, its listener will be called when there is a change in the
//        authentication state, will be call when:
//          - Right after the listener has been registered
//          - When a user is signed in
//          - When the current user is signed out
//          - When the current user changes
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            //Check if there is a user has already logged in, if so, update Facebook name and picture
            val user = firebaseAuth.currentUser
            if (user != null) {
                updateUI(user)
            } else {
                updateUI(null)
            }
        }

        //Check Login Status with an AccessTokenTracker. This methedo is triggered when token
        //change, so if its empty or expired, log out current user
        accessTokenTracker = object : AccessTokenTracker() {
            override fun onCurrentAccessTokenChanged(
                oldAccessToken: AccessToken?,
                currentAccessToken: AccessToken?
            ) {
                //If access token is not initialized or is expired, sign out currenr Firebase user
                if (currentAccessToken == null || currentAccessToken?.isExpired) {
                    mFirebaseAuth?.signOut()
                }
            }
        }
    }

    private fun handleFacebookToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookToken: $token")

        //Get token credential to sign in Facebook account
        val credential = FacebookAuthProvider.getCredential(token.token)
        mFirebaseAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(this, OnCompleteListener {
                fun onComplete(task: Task<AuthResult?>) {
                    if (task.isSuccessful) {
                        //Update current Firebase user as Facebook user
                        Log.d(TAG, "sign in with credential: Successful")
                        val user = mFirebaseAuth!!.currentUser
                        //Update Facebook name and profile picture of current user
                        updateUI(user)
                    } else {
                        //Inform if fails
                        Log.d(TAG, "sign in with credential: Failed")
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT)
                        //Update name and profile picture to default
                        updateUI(null)

                    }
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Call this method to pass the login results to the LoginManager via callbackManager
        mCallbackManager?.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        //Register listener in for Firebase Authentication
        mFirebaseAuth?.addAuthStateListener(authStateListener!!)
    }

    override fun onStop() {
        super.onStop()

        //Unregister listener in for Firebase Authentication
        if (authStateListener != null)
            mFirebaseAuth?.removeAuthStateListener(authStateListener!!)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            //Set Facebook user fullname in TextView
            tv_fullname.text = user.displayName

            //Set Facebook user profile picture in ImageView by using Picasso
            if (user.photoUrl != null) {
                var photoUrl = user.photoUrl.toString()
                photoUrl = "$photoUrl?type=large"
                Picasso.get().load(photoUrl).into(iv_logo)
            }

        } else {
            //Set default text and picture
            tv_fullname.text = "No user logged in"
            iv_logo.setImageResource(R.drawable.logo)
        }
    }
}
