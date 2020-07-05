package com.example.kiok

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Tools {
    companion object {
        private lateinit var loadingDialog: AlertDialog

        // FirebaseAuth instance
        val auth = FirebaseAuth.getInstance()

        // Firestore path to current user's questions bundles
        var questionsBundlesPath: CollectionReference? = null

        // User Firestore preferences
        var currentUsername: String? = null
        var currentBundleSort: Int = -1
        var currentBundleName: String = ""
        var currentQuestionSort: Int = -1

        // Current bundle ID
        var currentBundleID: String = ""

        // Bundle all ID
        var bundleAllID: String = ""

        // Show a loading Dialog
        fun showLoadingDialog(context: Context) {
            val progressBarView =
                LayoutInflater.from(context).inflate(R.layout.view_progress_bar, null)

            loadingDialog = AlertDialog.Builder(context)
                .setView(progressBarView)
                .show()

            loadingDialog.window!!.setLayout(400, 400)
        }

        // Dismiss a loading Dialog
        fun dismissLoadingDialog() {
            loadingDialog.dismiss()
        }

        // Check if a field is filled and set an error if not
        fun checkFieldFilled(field: String, layout: TextInputLayout, error: String): Boolean {
            if (field.isEmpty()) {
                layout.error = error
                return false
            }
            layout.error = null
            return true
        }

        // Check if an email address is valid and return a Boolean accordingly
        private fun isEmailValid(email: CharSequence): Boolean {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

        // Check if an email address field is valid and set an error if not
        fun checkValidEmailField(email: String, layout: TextInputLayout, error: String): Boolean {
            if (!isEmailValid(email)) {
                layout.error = error
                return false
            }
            layout.error = null
            return true
        }

        // Check if a password field is valid and set an error if not
        fun checkValidPassword(password: String, layout: TextInputLayout, error: String): Boolean {
            if (password.length < 8) {
                layout.error = error
                return false
            }
            layout.error = null
            return true
        }

        /* Create a new user Document inside a "users" Collection
        The Document ID is the user's UID
        It contains :
        - the username
        - the current bundle sort of the home fragment
        - the current bundle displayed in the collection fragment
        */
        fun createUserInDB(username: String?, bundleAllName: String, currentUser: FirebaseUser) {
            val db = Firebase.firestore
            val user = hashMapOf(
                "username" to username,
                "current_bundle_sort" to 1,
                "current_bundle_name" to bundleAllName,
                "current_question_sort" to 0
            )

            db.collection("users").document(currentUser.uid)
                .set(user)
                .addOnSuccessListener { Log.d("createUserInDB", "DocumentSnapshot successfully written!") }
                .addOnFailureListener { e -> Log.w("createUserInDB", "Error writing document", e) }
        }

        // Generates search keywords for a string
        fun generateSearchKeywords(string: String): MutableList<String> {
            var searchString = string
            val keywords = mutableListOf<String>()
            val words = searchString.split(" ")

            for (word in words) {
                var keyword = ""

                for (charPosition in searchString.indices) {
                    keyword += searchString[charPosition].toString()
                    keywords.add(keyword)
                }
                searchString = searchString.replace("$word ", "")
            }
            return keywords
        }

        // Generates search keywords for an array of strings
        fun generateSearchKeywords(strings: Array<String>): MutableList<String> {
            val keywords = mutableListOf<String>()

            for (string in strings) {
                var searchString = string
                val words = searchString.split(" ")

                for (word in words) {
                    var keyword = ""

                    for (charPosition in searchString.indices) {
                        keyword += searchString[charPosition].toString()
                        keywords.add(keyword)
                    }
                    searchString = searchString.replace("$word ", "")
                }
            }
            return keywords
        }

        // Get user preferences
        fun getUserPreferences(documentSnapshot: DocumentSnapshot) {
            currentUsername = documentSnapshot["username"] as String?
            currentBundleSort = (documentSnapshot["current_bundle_sort"] as Long).toInt()
            currentBundleName = documentSnapshot["current_bundle_name"] as String
            currentQuestionSort = (documentSnapshot["current_question_sort"] as Long).toInt()
        }

        fun getBundleAllID(bundleAll: String) {
            questionsBundlesPath!!.whereEqualTo("name", bundleAll).get()
                .addOnSuccessListener {
                    bundleAllID = it.documents[0].id
                }
        }

        // Get the correct bundle query according to the current bundle sort
        fun getSortedBundlesQuery(): Query {
            return (
                    when (currentBundleSort) {
                        0 -> questionsBundlesPath!!.orderBy("name")
                        1 -> questionsBundlesPath!!.orderBy("date")
                        2 -> questionsBundlesPath!!.orderBy("date", Query.Direction.DESCENDING)
                        else -> {
                            Log.e(
                                "Database data not valid",
                                " The current bundle sort value is not valid."
                            )
                            questionsBundlesPath!!.orderBy("date")
                        }
                    }
                    )
        }
    }
}

// Function to call in a Fragment to hide the keyboard
fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

// Function to call in an Activity to hide the keyboard
fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

// Hide the keyboard
private fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}
