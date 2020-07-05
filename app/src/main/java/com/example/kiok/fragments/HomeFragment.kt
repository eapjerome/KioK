package com.example.kiok.fragments

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kiok.R
import com.example.kiok.SwipeToDeleteCallback
import com.example.kiok.Tools.Companion.auth
import com.example.kiok.Tools.Companion.bundleAllID
import com.example.kiok.Tools.Companion.currentBundleName
import com.example.kiok.Tools.Companion.currentBundleSort
import com.example.kiok.Tools.Companion.getBundleAllID
import com.example.kiok.Tools.Companion.getSortedBundlesQuery
import com.example.kiok.Tools.Companion.getUserPreferences
import com.example.kiok.Tools.Companion.questionsBundlesPath
import com.example.kiok.adapters.QuestionsBundleAdapter
import com.example.kiok.adapters.QuestionsBundleHolder
import com.example.kiok.hideKeyboard
import com.example.kiok.interfaces.IHomeFragment
import com.example.kiok.interfaces.IMainActivity
import com.example.kiok.models.QuestionsBundle
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.view_questions_bundle.view.*
import java.util.*

class HomeFragment : Fragment(), IHomeFragment {

    private lateinit var mainActivityListener: IMainActivity
    private var homeAdapter: FirestoreRecyclerAdapter<QuestionsBundle, QuestionsBundleHolder>? =
        null
    private var isRenameDialogShown = false

    // IHomeFragment
    override fun navigate(fragment: Int) {
        mainActivityListener.navigate(fragment)
    }

    override fun navigate(fragment: Int, arguments: Bundle) {
        mainActivityListener.navigate(fragment, arguments)
    }

    override fun isRenameDialogShown(value: Boolean) {
        isRenameDialogShown = value
    }

    override fun scrollToNewBundle() {
        when (currentBundleSort) {
            1 -> home_recycler_questions_bundles.smoothScrollToPosition(homeAdapter!!.itemCount)
            2 -> home_recycler_questions_bundles.smoothScrollToPosition(0)
        }
    }

    override fun setMainSnackbar(
        message: String,
        duration: Int,
        actionMessage: String,
        action: (v: View) -> Unit
    ) {
        mainActivityListener.setMainSnackbar(message, duration, actionMessage, action)
    }

    override fun dismissSnackbar() {
        mainActivityListener.dismissSnackbar()
    }

    // Fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Firebase.firestore.collection("users").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener {
                getUserPreferences(it)
                getBundleAllID(getString(R.string.bundle_all))
                setQuestionsBundlesRecycler()
                setHomeSearchListener()
                setHomeSortListener()
                mainActivityListener.setHomeFragmentListener(this)
            }
    }

    private fun setQuestionsBundlesRecycler() {
        homeAdapter = QuestionsBundleAdapter(getSortedBundlesOptions(), this)
        homeAdapter!!.startListening()
        home_recycler_questions_bundles?.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = homeAdapter
        }
        setBundlesRecyclerFeatures()
    }

    private fun getSortedBundlesOptions(): FirestoreRecyclerOptions<QuestionsBundle> {
        val sortedBundlesQuery = getSortedBundlesQuery()

        return FirestoreRecyclerOptions.Builder<QuestionsBundle>()
            .setQuery(sortedBundlesQuery, QuestionsBundle::class.java)
            .build()
    }

    private fun setBundlesRecyclerFeatures() {
        val homeSwipe = setHomeSwipe()

        home_recycler_questions_bundles?.apply {
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL
                )
            )
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy != 0) {
                        home_et_search.clearFocus()
                        hideKeyboard()
                        when {
                            dy > 0 -> mainActivityListener.hideMainFAB()
                            dy < 0 -> mainActivityListener.showMainFAB()
                        }
                    }
                }
            })
        }
        homeSwipe.attachToRecyclerView(home_recycler_questions_bundles)
    }

    private fun setHomeSwipe(): ItemTouchHelper {
        val swipeDeleteCallback = object : SwipeToDeleteCallback(context!!) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (viewHolder.itemView.questions_bundle_view_tv_name.text == getString(R.string.bundle_all))
                    cannotDeleteBundle(viewHolder)
                else
                    when (!isRenameDialogShown) {
                        true -> setDeleteBundleDialog(viewHolder)
                        false -> homeAdapter!!.notifyItemChanged(viewHolder.adapterPosition)
                    }
            }
        }
        return ItemTouchHelper(swipeDeleteCallback)
    }

    private fun cannotDeleteBundle(viewHolder: RecyclerView.ViewHolder) {
        mainActivityListener.setMainSnackbar(
            getString(R.string.cannot_delete_bundle_message),
            Snackbar.LENGTH_SHORT,
            getString(R.string.ok)
        ) { }
        homeAdapter!!.notifyItemChanged(viewHolder.adapterPosition)
    }

    private fun setDeleteBundleDialog(viewHolder: RecyclerView.ViewHolder) {
        MaterialAlertDialogBuilder(context, R.style.CustomMaterialAlertDialogTheme)
            .setTitle(getString(R.string.delete_bundle_alert_title))
            .setMessage(
                Html.fromHtml(
                    getString(
                        R.string.delete_bundle_alert_message,
                        viewHolder.itemView.questions_bundle_view_tv_name.text
                    ), Html.FROM_HTML_MODE_LEGACY
                )
            )
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                homeAdapter!!.notifyItemChanged(viewHolder.adapterPosition)
            }
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ ->
                questionsBundlesPath!!.document(homeAdapter!!.snapshots.getSnapshot(viewHolder.adapterPosition).id)
                    .delete()
                setBundleDeletedSnackbar(viewHolder)
                removeQuestionsFromBundleAll(viewHolder)
                checkCurrentBundleName(viewHolder)
            }
            .setOnCancelListener {
                homeAdapter!!.notifyItemChanged(viewHolder.adapterPosition)
            }
            .show()
    }

    private fun setBundleDeletedSnackbar(viewHolder: RecyclerView.ViewHolder) {
        mainActivityListener.setMainSnackbar(
            getString(
                R.string.deleted_bundle_message,
                viewHolder.itemView.questions_bundle_view_tv_name.text
            ),
            Snackbar.LENGTH_SHORT,
            getString(R.string.ok)
        ) { }
    }

    private fun removeQuestionsFromBundleAll(viewHolder: RecyclerView.ViewHolder) {
        questionsBundlesPath!!.document(bundleAllID).collection("questions")
            .whereEqualTo("original_bundle", viewHolder.itemView.questions_bundle_view_tv_name.text)
            .get().addOnSuccessListener {
                for (document in it.documents) {
                    document.reference.delete()
                    questionsBundlesPath!!.document(bundleAllID)
                        .update("quantity", FieldValue.increment(-1))
                }
            }
    }

    private fun checkCurrentBundleName(viewHolder: RecyclerView.ViewHolder) {
        if (viewHolder.itemView.questions_bundle_view_tv_name.text == currentBundleName) {
            currentBundleName = getString(R.string.bundle_all)
            Firebase.firestore.collection("users")
                .document(auth.currentUser!!.uid)
                .update("current_bundle_name", currentBundleName)
        }
    }

    private fun setHomeSearchListener() {
        home_et_search?.doAfterTextChanged {
            updateQuestionsBundlesRecycler()
        }
    }

    private fun setHomeSortListener() {
        val bundlesSortType =
            arrayOf(
                getString(R.string.sort_alpha),
                getString(R.string.sort_oldest),
                getString(R.string.sort_newest)
            )
        home_iv_sort?.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.sort_by)
                .setSingleChoiceItems(bundlesSortType, currentBundleSort) { dialog, which ->
                    if (which != currentBundleSort) {
                        Firebase.firestore.collection("users")
                            .document(auth.currentUser!!.uid)
                            .update("current_bundle_sort", which)
                        currentBundleSort = which
                        updateQuestionsBundlesRecycler()
                        mainActivityListener.showMainFAB()
                    }
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun updateQuestionsBundlesRecycler() {
        if (home_et_search.text.isNotEmpty()) {
            home_et_search.requestFocus()
            val homeSearchText = home_et_search.text.toString().trimEnd().toLowerCase(Locale.FRANCE)
            val homeSearchQuery =
                getSortedBundlesQuery().whereArrayContains("search_keywords", homeSearchText)
            val homeSearchOptions = FirestoreRecyclerOptions.Builder<QuestionsBundle>()
                .setQuery(homeSearchQuery, QuestionsBundle::class.java)
                .build()
            homeAdapter!!.updateOptions(homeSearchOptions)
        } else
            homeAdapter!!.updateOptions(getSortedBundlesOptions())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityListener = context as? IMainActivity
            ?: throw ClassCastException("$context must implement IMainActivity")
    }

    override fun onStart() {
        super.onStart()
        homeAdapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        homeAdapter?.stopListening()
        hideKeyboard()
    }
}