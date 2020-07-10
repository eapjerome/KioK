package com.eap.kiok.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.eap.kiok.R
import com.eap.kiok.Tools.Companion.auth
import com.eap.kiok.Tools.Companion.checkFieldFilled
import com.eap.kiok.Tools.Companion.checkValidEmailField
import com.eap.kiok.Tools.Companion.checkValidPassword
import com.eap.kiok.Tools.Companion.createUserInDB
import com.eap.kiok.Tools.Companion.dismissLoadingDialog
import com.eap.kiok.Tools.Companion.generateSearchKeywords
import com.eap.kiok.Tools.Companion.questionsBundlesPath
import com.eap.kiok.Tools.Companion.showLoadingDialog
import com.eap.kiok.hideKeyboard
import com.eap.kiok.interfaces.IMainActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_sign_up.*
import java.util.*

private const val TAG = "SignUpFragment"

class SignUpFragment : Fragment() {

    private lateinit var mainActivityListener: IMainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivityListener.hideBottomNavigation()
        signUp()
    }

    private fun signUp() {
        sign_up_btn_sign_up.setOnClickListener {
            val username = sign_up_et_username.text.toString().trim()
            val email = sign_up_et_email.text.toString()
            val password = sign_up_et_password.text.toString()

            if (checkValidSignUpForm(username, email, password)) {
                showLoadingDialog(context!!)
                hideKeyboard()
                auth.currentUser?.let {
                    convertAccount(username, email, password)
                } ?: createUser(username, email, password)
            }
        }
    }

    private fun checkValidSignUpForm(username: String, email: String, password: String): Boolean {
        return checkFieldFilled(
            field = username,
            layout = sign_up_layout_username,
            error = getString(R.string.field_empty_error)
        ) && checkValidEmailField(
            email = email,
            layout = sign_up_layout_email,
            error = getString(R.string.field_email_error)
        ) && checkValidPassword(
            password = password,
            layout = sign_up_layout_password,
            error = getString((R.string.field_password_error))
        )
    }

    private fun convertAccount(username: String, email: String, password: String) {
        val credential = EmailAuthProvider.getCredential(email, password)

        auth.currentUser!!.linkWithCredential(credential).addOnCompleteListener { it ->
            if (it.isSuccessful) {
                val profileUpdates =
                    UserProfileChangeRequest.Builder().setDisplayName(username)
                it.result!!.user!!.updateProfile(profileUpdates.build())

                Firebase.firestore.collection("users").document(auth.currentUser!!.uid).get()
                    .addOnSuccessListener {
                        it.reference.update("username", username)
                        mainActivityListener.navigate(R.id.action_signUpFragment_to_homeFragment)
                        dismissLoadingDialog()
                    }
                Log.d(TAG, "linkWithCredential:success")
            } else {
                dismissLoadingDialog()
                mainActivityListener.setMainSnackbar(
                    getString(R.string.email_already_used_message),
                    Snackbar.LENGTH_LONG
                )
                Log.w(TAG, "linkWithCredential:failure", it.exception)
            }
        }
    }

    private fun createUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                dismissLoadingDialog()
                if (task.isSuccessful) {
                    val profileUpdates =
                        UserProfileChangeRequest.Builder().setDisplayName(username)
                    auth.currentUser!!.updateProfile(profileUpdates.build())
                    createUserInDB(
                        username,
                        getString(R.string.bundle_all), auth.currentUser!!
                    )
                    addBundleAll()
                    mainActivityListener.navigate(R.id.action_signUpFragment_to_homeFragment)
                    Log.d(TAG, "createUserWithEmail:success")
                } else {
                    Toast.makeText(
                        context, getString(R.string.email_already_used_message),
                        Toast.LENGTH_LONG
                    ).show()
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                }
            }
    }

    private fun addBundleAll() {
        val bundleAll = hashMapOf(
            "name" to getString(R.string.bundle_all),
            "quantity" to 0,
            "date" to Timestamp.now(),
            "search_keywords" to generateSearchKeywords(
                getString(R.string.bundle_all).toLowerCase(Locale.FRANCE)
            )
        )

        questionsBundlesPath =
            Firebase.firestore.collection("users").document(auth.currentUser!!.uid)
                .collection("questions bundles")
        questionsBundlesPath!!.add(bundleAll)
            .addOnSuccessListener { documentReference ->
                Log.d(
                    TAG,
                    "DocumentSnapshot added with ID: ${documentReference.id}"
                )
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityListener = context as? IMainActivity
            ?: throw ClassCastException("$context must implement IMainActivity")
    }
}
