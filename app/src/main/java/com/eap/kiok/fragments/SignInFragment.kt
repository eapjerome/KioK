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
import com.eap.kiok.Tools.Companion.dismissLoadingDialog
import com.eap.kiok.Tools.Companion.questionsBundlesPath
import com.eap.kiok.Tools.Companion.showLoadingDialog
import com.eap.kiok.hideKeyboard
import com.eap.kiok.interfaces.IMainActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_sign_in.*

private const val TAG = "SignInFragment"

class SignInFragment : Fragment() {

    private lateinit var mainActivityListener: IMainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        signIn()
        setForgotPasswordButtonListener()
    }

    private fun signIn() {
        sign_in_btn_sign_in.setOnClickListener {
            val email = sign_in_et_email.text.toString()
            val password = sign_in_et_password.text.toString()

            if (checkValidSignInForm(email, password)) {
                showLoadingDialog(context!!)
                hideKeyboard()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        dismissLoadingDialog()
                        if (task.isSuccessful) {
                            Log.d(TAG, "signInWithEmail:success")
                            questionsBundlesPath =
                                Firebase.firestore.collection("users")
                                    .document(auth.currentUser!!.uid)
                                    .collection("questions bundles")
                            mainActivityListener.navigate(R.id.action_signInFragment_to_homeFragment)
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.exception)
                            Toast.makeText(
                                context, "Nom d'utilisateur et/ou mot de passe incorrects",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        }
    }

    private fun checkValidSignInForm(email: String, password: String): Boolean {
        return checkValidEmailField(
            email = email,
            layout = sign_in_layout_email,
            error = getString(R.string.field_email_error)
        ) && checkFieldFilled(
            field = password,
            layout = sign_in_layout_password,
            error = getString(R.string.field_empty_error)
        )
    }

    private fun setForgotPasswordButtonListener() {
        sign_in_btn_forgot_password.setOnClickListener {
            val email = sign_in_et_email.text.toString()

            sign_in_layout_email.error = null
            if (checkValidEmailField(
                    email,
                    sign_in_layout_email,
                    getString(R.string.field_email_error)
                )
            ) {
                showLoadingDialog(context!!)
                auth.sendPasswordResetEmail(email).addOnCompleteListener {
                    if (it.isSuccessful) {
                        dismissLoadingDialog()
                        mainActivityListener.setMainSnackbar(
                            getString(R.string.password_reset_email_sent),
                            Snackbar.LENGTH_INDEFINITE,
                            getString(R.string.ok)
                        ) { }
                    } else {
                        dismissLoadingDialog()
                        sign_in_layout_email.error = getString(R.string.email_not_recognized)
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityListener = context as? IMainActivity
            ?: throw ClassCastException("$context must implement IMainActivity")
    }
}
