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
import com.eap.kiok.Tools.Companion.createUserInDB
import com.eap.kiok.Tools.Companion.dismissLoadingDialog
import com.eap.kiok.Tools.Companion.generateSearchKeywords
import com.eap.kiok.Tools.Companion.questionsBundlesPath
import com.eap.kiok.Tools.Companion.showLoadingDialog
import com.eap.kiok.interfaces.IMainActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_welcome.*
import java.util.*

private const val TAG = "WelcomeFragment"

class WelcomeFragment : Fragment() {

    private lateinit var mainActivityListener: IMainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        welcome_appbar.outlineProvider = null

        buttonListener()
    }

    private fun buttonListener() {
        welcome_btn_sign_up.setOnClickListener {
            mainActivityListener.navigate(R.id.action_welcomeFragment_to_signUpFragment)
        }

        welcome_btn_sign_in.setOnClickListener {
            mainActivityListener.navigate(R.id.action_welcomeFragment_to_signInFragment)
        }

        welcome_btn_direct_start.setOnClickListener {
            showLoadingDialog(context!!)
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    dismissLoadingDialog()
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInAnonymously:success")
                        createUserInDB(null, getString(R.string.bundle_all), auth.currentUser!!)
                        addBundleAll()
                        mainActivityListener.navigate(R.id.action_welcomeFragment_to_homeFragment)
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.exception)
                        Toast.makeText(
                            context, "Authentification échouée.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityListener = context as? IMainActivity
            ?: throw ClassCastException("$context must implement IMainActivity")
    }

    override fun onStart() {
        super.onStart()
        checkAlreadySignIn()
    }

    private fun checkAlreadySignIn() {
        auth.currentUser?.let {
            questionsBundlesPath =
                Firebase.firestore.collection("users").document(it.uid)
                    .collection("questions bundles")
            mainActivityListener.navigate(R.id.action_welcomeFragment_to_homeFragment)
        }
    }
}
