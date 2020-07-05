package com.example.kiok.interfaces

import android.os.Bundle
import android.view.View

interface IMainActivity {
    fun navigate(fragment: Int)
    fun navigate(fragment: Int, arguments: Bundle)
    fun showMainFAB()
    fun hideMainFAB()
    fun isMainFABShown(): Boolean
    fun setMainSnackbar(message: String, duration: Int)
    fun setMainSnackbar(message: String, duration: Int, actionMessage: String, action: (v: View) -> Unit)
    fun setHomeFragmentListener(listener: IHomeFragment)
    fun setCollectionFragmentListener(listener: ICollectionFragment)
    fun isCollectionSearching(value: Boolean)
    fun hideBottomNavigation()
    fun dismissSnackbar()
    fun goBack()
    fun isRevising(value: Boolean)
}