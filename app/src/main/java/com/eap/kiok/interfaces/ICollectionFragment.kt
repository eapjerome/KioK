package com.eap.kiok.interfaces

import android.os.Bundle

interface ICollectionFragment {
    fun navigate(fragment: Int)
    fun navigate(fragment: Int, arguments: Bundle)
    fun showQuestionOriginalBundle(questionOriginalBundle: String)
    fun scrollToNewQuestion()
    fun clearCollectionSearchFocus()
    fun checkNoQuestionBundle()
}