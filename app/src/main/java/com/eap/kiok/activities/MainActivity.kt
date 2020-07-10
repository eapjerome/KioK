package com.eap.kiok.activities

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.eap.kiok.R
import com.eap.kiok.Tools.Companion.bundleAllID
import com.eap.kiok.Tools.Companion.currentBundleID
import com.eap.kiok.Tools.Companion.currentBundleName
import com.eap.kiok.Tools.Companion.generateSearchKeywords
import com.eap.kiok.Tools.Companion.questionsBundlesPath
import com.eap.kiok.interfaces.ICollectionFragment
import com.eap.kiok.interfaces.IHomeFragment
import com.eap.kiok.interfaces.IMainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_edit_question.view.*
import kotlinx.android.synthetic.main.view_simple_edit_text.view.*
import java.util.*

class MainActivity : AppCompatActivity(), IMainActivity {

    private lateinit var navController: NavController
    private var mainSnackbar: Snackbar? = null
    private var homeFragmentListener: IHomeFragment? = null
    private var collectionFragmentListener: ICollectionFragment? = null
    private var isCollectionSearching: Boolean = false
    private var isRevising: Boolean = false

    // IMainActivity
    override fun navigate(fragment: Int) {
        navController.navigate(fragment)
    }

    override fun navigate(fragment: Int, arguments: Bundle) {
        navController.navigate(fragment, arguments)
    }

    override fun showMainFAB() {
        main_activity_fab.show()
    }

    override fun hideMainFAB() {
        main_activity_fab.hide()
    }

    override fun isMainFABShown(): Boolean {
        return main_activity_fab.isShown
    }

    override fun setMainSnackbar(message: String, duration: Int) {
        mainSnackbar = Snackbar
            .make(main_activity_coordinator, message, duration)
        mainSnackbar!!.show()
    }

    override fun setMainSnackbar(
        message: String,
        duration: Int,
        actionMessage: String,
        action: (v: View) -> Unit
    ) {
        mainSnackbar = Snackbar
            .make(main_activity_coordinator, message, duration)
            .setAction(actionMessage, action)
        mainSnackbar!!.show()
    }

    override fun setHomeFragmentListener(listener: IHomeFragment) {
        homeFragmentListener = listener
    }

    override fun setCollectionFragmentListener(listener: ICollectionFragment) {
        collectionFragmentListener = listener
    }

    override fun isCollectionSearching(value: Boolean) {
        isCollectionSearching = value
    }

    override fun hideBottomNavigation() {
        bottom_nav.visibility = View.GONE
    }

    override fun dismissSnackbar() {
        mainSnackbar?.dismiss()
    }

    override fun goBack() {
        onBackPressed()
    }

    override fun isRevising(value: Boolean) {
        isRevising = value
    }

    // Actvity
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = findNavController(R.id.welcome_fragment_host)

        setSoftInputMode()
        setBottomNavigation()
        setFloatingActionButon()
    }

    private fun setSoftInputMode() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.welcomeFragment, R.id.reviseFragment, R.id.editPasswordFragment, R.id.signUpFragment -> window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                )
                R.id.homeFragment, R.id.userFragment -> window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            }
        }
    }

    private fun setBottomNavigation() {
        bottom_nav.setupWithNavController(navController)
        bottom_nav.setOnNavigationItemReselectedListener {}

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.welcomeFragment -> hideBottomNavigation()
                R.id.homeFragment, R.id.userFragment -> bottom_nav.visibility = View.VISIBLE
            }
        }
    }

    private fun setFloatingActionButon() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.collectionFragment && currentBundleName == getString(R.string.bundle_all))
                showMainFAB()
            else
                when (destination.id) {
                    R.id.welcomeFragment, R.id.userFragment, R.id.reviseFragment -> hideMainFAB()
                    R.id.homeFragment, R.id.collectionFragment -> showMainFAB()
                }
        }
        main_activity_fab.setOnClickListener {
            when (navController.currentDestination?.id) {
                R.id.homeFragment -> homeFABOnClick()
                R.id.collectionFragment ->
                    if (currentBundleName != getString(R.string.bundle_all))
                        collectionFABOnClick()
            }
        }
    }

    private fun homeFABOnClick() {
        val createBundleView = layoutInflater.inflate(R.layout.view_simple_edit_text, null)
        val createBundleDialogBuilder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.create_a_bundle))
            .setView(createBundleView)
            .setNegativeButton(getText(R.string.cancel)) { _, _ ->

            }
            .setPositiveButton(getText(R.string.create)) { _, _ ->

            }
        val createBundleDialog = createBundleDialogBuilder.create()

        createBundleView.apply {
            simple_edit_text_layout.hint = getString(R.string.name)
            simple_edit_text_et.apply {
                requestFocus()
                doAfterTextChanged {
                    createBundleDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                        createBundleView.simple_edit_text_et.text!!.trim().isNotEmpty()
                }
            }
        }
        createBundleDialog.apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = false
                setOnClickListener {
                    createBundle(createBundleDialog, createBundleView)
                }
            }
        }
    }

    private fun createBundle(createBundleDialog: AlertDialog, createBundleView: View) {
        val newBundleName = createBundleView.simple_edit_text_et.text.toString().trim()

        questionsBundlesPath!!.whereEqualTo("name", newBundleName).get().addOnSuccessListener {
            if (it.isEmpty) {
                val newBundle =
                    hashMapOf(
                        "name" to newBundleName,
                        "quantity" to 0,
                        "date" to Timestamp.now(),
                        "search_keywords" to generateSearchKeywords(newBundleName.toLowerCase(Locale.FRANCE))
                    )
                questionsBundlesPath!!.add(newBundle).addOnSuccessListener {
                    homeFragmentListener!!.scrollToNewBundle()
                }
                createBundleDialog.dismiss()
            } else
                createBundleView.simple_edit_text_layout.error =
                    getString(R.string.bundle_already_exists)
        }
    }

    private fun collectionFABOnClick() {
        val addQuestionView = layoutInflater.inflate(R.layout.view_edit_question, null)
        val addQuestionDialogBuilder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_a_question))
            .setView(addQuestionView)
            .setNegativeButton(getText(R.string.cancel)) { _, _ ->

            }
            .setPositiveButton(getText(R.string.add)) { _, _ ->

            }
        val addQuestionDialog = addQuestionDialogBuilder.create()
        addQuestionView.apply {
            edit_question_layout_question.hint = getString(R.string.question)
            edit_question_layout_answer.hint = getString(R.string.answer)
            edit_question_et_question.apply {
                doAfterTextChanged {
                    addQuestionDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                        addQuestionView.edit_question_et_question.text!!.isNotEmpty() && addQuestionView.edit_question_et_answer.text!!.isNotEmpty()

                }
                requestFocus()
            }
            edit_question_et_answer.doAfterTextChanged {
                addQuestionDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    addQuestionView.edit_question_et_question.text!!.isNotEmpty() && addQuestionView.edit_question_et_answer.text!!.isNotEmpty()

            }
        }

        addQuestionDialog.apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = false
                setOnClickListener {
                    addQuestion(addQuestionDialog, addQuestionView)
                }
            }
        }
    }

    private fun addQuestion(addQuestionDialog: AlertDialog, addQuestionView: View) {
        val question = addQuestionView.edit_question_et_question.text.toString().trim()
        val answer = addQuestionView.edit_question_et_answer.text.toString().trim()
        val currentBundleQuestionsRef =
            questionsBundlesPath!!.document(currentBundleID).collection("questions")

        currentBundleQuestionsRef.whereEqualTo("question", question).get().addOnSuccessListener {
            if (it.isEmpty) {
                val newQuestionSearchKeywords =
                    arrayOf(question.toLowerCase(Locale.FRANCE), answer.toLowerCase(Locale.FRANCE))
                val newQuestion =
                    hashMapOf(
                        "question" to question,
                        "answer" to answer,
                        "date" to Timestamp.now(),
                        "search_keywords" to generateSearchKeywords(newQuestionSearchKeywords)
                    )

                currentBundleQuestionsRef.add(newQuestion).addOnSuccessListener {
                    collectionFragmentListener!!.checkNoQuestionBundle()
                    collectionFragmentListener!!.scrollToNewQuestion()
                }
                questionsBundlesPath!!.document(currentBundleID)
                    .update("quantity", FieldValue.increment(1))
                addQuestionToBundleAll(newQuestion)
                addQuestionDialog.dismiss()
            } else
                addQuestionView.edit_question_layout_question.error =
                    getString(R.string.question_already_exists)
        }
    }

    private fun addQuestionToBundleAll(newQuestion: HashMap<String, Any>) {
        val bundleAllRef = questionsBundlesPath!!.document(bundleAllID)
        val newQuestionRef = bundleAllRef.collection("questions").document()

        newQuestionRef.apply {
            set(newQuestion)
            update("original_bundle", currentBundleName)
        }
        bundleAllRef.update("quantity", FieldValue.increment(1))
    }

    private fun setExitDialogAlert() {
        MaterialAlertDialogBuilder(this, R.style.CustomMaterialAlertDialogTheme)
            .setTitle(R.string.confirmation_dialog_title)
            .setMessage(R.string.quit_revise_dialog_message)
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->

            }
            .setPositiveButton(resources.getString(R.string.quit)) { _, _ ->
                isRevising = false
                onBackPressed()
            }
            .show()
    }

    override fun onBackPressed() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment, R.id.welcomeFragment -> finish()
            R.id.collectionFragment ->
                if (isCollectionSearching)
                    collectionFragmentListener?.clearCollectionSearchFocus()
                else
                    navController.navigate(R.id.homeFragment)
            R.id.userFragment -> navController.navigate(R.id.homeFragment)
            R.id.reviseFragment ->
                if (isRevising)
                    setExitDialogAlert()
                else
                    super.onBackPressed()
            else -> super.onBackPressed()
        }
    }
}
