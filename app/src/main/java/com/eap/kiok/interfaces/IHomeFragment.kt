package com.eap.kiok.interfaces

import android.os.Bundle
import android.view.View

interface IHomeFragment {
    fun navigate(fragment: Int)
    fun navigate(fragment: Int, arguments: Bundle)
    fun isRenameDialogShown(value: Boolean)
    fun scrollToNewBundle()
    fun setMainSnackbar(message: String, duration: Int, actionMessage: String, action: (v: View) -> Unit)
    fun dismissSnackbar()
}