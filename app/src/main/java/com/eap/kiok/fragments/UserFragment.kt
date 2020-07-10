package com.eap.kiok.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.eap.kiok.R
import com.eap.kiok.Tools.Companion.auth
import com.eap.kiok.Tools.Companion.checkFieldFilled
import com.eap.kiok.Tools.Companion.checkValidEmailField
import com.eap.kiok.Tools.Companion.currentUsername
import com.eap.kiok.interfaces.IMainActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.view_simple_edit_text.view.*

class UserFragment : Fragment() {

    private lateinit var mainActivityListener: IMainActivity
    private lateinit var currentUser: FirebaseUser
    private var passwordConfirmed = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUsername?.let {
            setSignedInUserUI()
        } ?: setAnonymousUserUI()
    }

    private fun setSignedInUserUI() {
        user_btn_sign_up?.visibility = View.GONE
        user_tv_sign_up?.visibility = View.GONE
        currentUser = auth.currentUser!!
        setUserInfoUI()
        setUserSaveButtonListener()
        setUserSignOutButton()
        setUserEditPassWordListener()
    }

    private fun setUserInfoUI() {
        user_et_username.setUserInfoField(currentUsername!!)
        user_et_email.setUserInfoField(currentUser.email!!)
        user_layout_username.visibility = View.VISIBLE
        user_layout_email.visibility = View.VISIBLE
        user_btn_edit_password.visibility = View.VISIBLE
        user_btn_sign_out.visibility = View.VISIBLE
    }

    private fun TextInputEditText.setUserInfoField(info: String) {
        setText(info)
        setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                if (!passwordConfirmed) {
                    askPassword()
                    v.clearFocus()
                } else
                    setUserSaveButton()
            }
        }
    }

    private fun askPassword() {
        val askPasswordView = layoutInflater.inflate(R.layout.view_simple_edit_text, null)
        val askPasswordDialogBuilder = AlertDialog.Builder(context!!)
            .setTitle(R.string.ask_password_dialog_title)
            .setView(askPasswordView)
            .setNegativeButton(getText(R.string.cancel)) { _, _ ->

            }
            .setPositiveButton(getText(R.string.confirm)) { _, _ ->

            }
        val askPasswordDialog = askPasswordDialogBuilder.create()

        askPasswordView.apply {
            simple_edit_text_layout.apply {
                hint = getString(R.string.form_field_password)
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                editText!!.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            simple_edit_text_et.apply {
                requestFocus()
                inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                doAfterTextChanged {
                    askPasswordDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                        askPasswordView.simple_edit_text_et.text!!.trim().isNotEmpty()
                    askPasswordView.simple_edit_text_layout.error = null
                }
            }
        }
        askPasswordDialog.apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = false
                setOnClickListener {
                    checkPassword(askPasswordDialog, askPasswordView)
                }
            }
        }
    }

    private fun checkPassword(askPasswordDialog: AlertDialog, askPasswordView: View) {
        val credential =
            EmailAuthProvider.getCredential(
                currentUser.email!!,
                askPasswordView.simple_edit_text_et.text.toString()
            )

        currentUser.reauthenticate(credential).addOnCompleteListener { reauthenticate ->
            if (reauthenticate.isSuccessful) {
                askPasswordDialog.dismiss()
                passwordConfirmed = true
                mainActivityListener.setMainSnackbar(
                    getString(R.string.can_now_edit_message),
                    Snackbar.LENGTH_LONG
                )
            } else {
                askPasswordView.simple_edit_text_layout.error =
                    getString(R.string.incorrect_password)
            }
        }
    }

    private fun setUserSaveButton() {
        Handler().postDelayed({
            user_btn_edit_password?.visibility = View.GONE
            user_btn_sign_out?.visibility = View.GONE
            user_btn_save?.visibility = View.VISIBLE
        }, 750)
    }

    private fun setUserSaveButtonListener() {
        user_btn_save.setOnClickListener {
            val usernameET = user_et_username.text.toString().trim()
            val emailET = user_et_email.text.toString()

            if (checkValidUserInfo(usernameET, emailET)) {
                if (usernameET != currentUsername) {
                    Firebase.firestore.collection("users").document(currentUser.uid)
                        .update("username", usernameET)
                    currentUsername = usernameET
                }
                if (emailET != currentUser.email)
                    currentUser.updateEmail(emailET)
                user_btn_save.visibility = View.GONE
                user_btn_sign_out.visibility = View.VISIBLE
                user_btn_edit_password.visibility = View.VISIBLE
                user_et_username.clearFocus()
                user_et_email.clearFocus()
            }
        }
    }

    private fun checkValidUserInfo(username: String, email: String): Boolean {
        return checkFieldFilled(
            field = username,
            layout = user_layout_username,
            error = getString(R.string.field_empty_error)
        ) && checkValidEmailField(
            email = email,
            layout = user_layout_email,
            error = getString(R.string.field_email_error)
        )
    }

    private fun setUserEditPassWordListener() {
        user_btn_edit_password.setOnClickListener {
            mainActivityListener.navigate(R.id.action_userFragment_to_editPasswordFragment)
        }
    }

    private fun setUserSignOutButton() {
        user_btn_sign_out.setOnClickListener {
            mainActivityListener.dismissSnackbar()
            auth.signOut()
            mainActivityListener.navigate(R.id.action_userFragment_to_welcomeFragment)
        }
    }

    private fun setAnonymousUserUI() {
        user_tv_sign_up.visibility = View.VISIBLE
        user_btn_sign_up.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                mainActivityListener.navigate(R.id.action_userFragment_to_signUpFragment)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityListener = context as? IMainActivity
            ?: throw ClassCastException("$context must implement IMainActivity")
    }
}

