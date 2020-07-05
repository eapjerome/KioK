package com.example.kiok.fragments

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.kiok.R
import com.example.kiok.Tools.Companion.auth
import com.example.kiok.Tools.Companion.checkValidPassword
import com.example.kiok.Tools.Companion.dismissLoadingDialog
import com.example.kiok.Tools.Companion.showLoadingDialog
import com.example.kiok.hideKeyboard
import com.example.kiok.interfaces.IMainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.android.synthetic.main.fragment_edit_password.*

class EditPasswordFragment : Fragment() {

    private lateinit var mainActivityListener: IMainActivity
    private val currentUser = auth.currentUser!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivityListener.hideBottomNavigation()
        setEditPasswordEditButtonListener()
        setEditPasswordForgotPasswordButton()
    }

    private fun setEditPasswordEditButtonListener() {
        edit_password_btn_edit.setOnClickListener {
            val formerPassword = edit_password_et_former_password.text.toString()
            val newPassword = edit_password_et_new_password.text.toString()
            val confirmPassword = edit_password_et_confirm_password.text.toString()

            edit_password_layout_former_password.error = null
            if (checkEditPasswordForm(newPassword, confirmPassword)) {
                showLoadingDialog(context!!)
                val credential =
                    EmailAuthProvider.getCredential(currentUser.email!!, formerPassword)

                currentUser.reauthenticate(credential).addOnCompleteListener { reauthenticate ->
                    if (reauthenticate.isSuccessful) {
                        hideKeyboard()
                        currentUser.updatePassword(newPassword).addOnSuccessListener {
                            dismissLoadingDialog()
                            mainActivityListener.setMainSnackbar(
                                getString(R.string.password_modified),
                                Snackbar.LENGTH_INDEFINITE,
                                getString(R.string.ok)
                            ) { }
                            mainActivityListener.goBack()
                        }
                    } else {
                        dismissLoadingDialog()
                        edit_password_layout_former_password.error =
                            getString(R.string.former_password_error)
                    }
                }
            }
        }
    }

    private fun checkEditPasswordForm(newPassword: String, confirmPassword: String): Boolean {
        return checkValidPassword(
            password = newPassword,
            layout = edit_password_layout_new_password,
            error = getString(R.string.field_password_error)
        ) && checkPasswordsEquality(newPassword, confirmPassword)
    }

    private fun checkPasswordsEquality(newPassword: String, confirmPassword: String): Boolean {
        return if (confirmPassword != newPassword) {
            edit_password_layout_confirm_password.error = getString(R.string.passwords_not_equals)
            false
        } else {
            edit_password_layout_confirm_password.error = null
            true
        }
    }

    private fun setEditPasswordForgotPasswordButton() {
        edit_btn_forgot_password.setOnClickListener {
            MaterialAlertDialogBuilder(context, R.style.CustomMaterialAlertDialogTheme)
                .setTitle(R.string.forgot_password)
                .setMessage(
                    Html.fromHtml(
                        getString(
                            R.string.send_password_reset_email_alert_message,
                            currentUser.email
                        ), Html.FROM_HTML_MODE_LEGACY
                    )
                )
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->

                }
                .setPositiveButton(resources.getString(R.string.send)) { _, _ ->
                    showLoadingDialog(context!!)
                    auth.sendPasswordResetEmail(currentUser.email!!).addOnSuccessListener {
                        dismissLoadingDialog()
                        mainActivityListener.setMainSnackbar(
                            getString(R.string.password_reset_email_sent),
                            Snackbar.LENGTH_INDEFINITE,
                            getString(R.string.ok)
                        ) { }
                    }
                }
                .show()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityListener = context as? IMainActivity
            ?: throw ClassCastException("$context must implement IMainActivity")
    }
}
