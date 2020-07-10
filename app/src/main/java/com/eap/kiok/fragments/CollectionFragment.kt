package com.eap.kiok.fragments

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eap.kiok.R
import com.eap.kiok.SwipeToDeleteCallback
import com.eap.kiok.Tools.Companion.auth
import com.eap.kiok.Tools.Companion.bundleAllID
import com.eap.kiok.Tools.Companion.currentBundleID
import com.eap.kiok.Tools.Companion.currentBundleName
import com.eap.kiok.Tools.Companion.currentQuestionSort
import com.eap.kiok.Tools.Companion.getSortedBundlesQuery
import com.eap.kiok.Tools.Companion.questionsBundlesPath
import com.eap.kiok.adapters.QuestionAdapter
import com.eap.kiok.adapters.QuestionHolder
import com.eap.kiok.hideKeyboard
import com.eap.kiok.interfaces.ICollectionFragment
import com.eap.kiok.interfaces.IMainActivity
import com.eap.kiok.models.Question
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_collection.*
import kotlinx.android.synthetic.main.view_question.view.*
import java.util.*

class CollectionFragment : Fragment(), ICollectionFragment {

    private lateinit var mainActivityListener: IMainActivity
    private var collectionAdapter: FirestoreRecyclerAdapter<Question, QuestionHolder>? = null
    private lateinit var collectionSwipe: ItemTouchHelper

    //ICollectionFragment
    override fun navigate(fragment: Int) {
        mainActivityListener.navigate(fragment)
    }

    override fun navigate(fragment: Int, arguments: Bundle) {
        mainActivityListener.navigate(fragment, arguments)
    }

    override fun showQuestionOriginalBundle(questionOriginalBundle: String) {
        mainActivityListener.setMainSnackbar(
            questionOriginalBundle,
            Snackbar.LENGTH_LONG,
            getString(R.string.see)
        ) {
            updateCurrentBundleName(questionOriginalBundle)
            updateQuestionsRecycler()
        }
    }

    override fun scrollToNewQuestion() {
        when (currentQuestionSort) {
            0 -> collection_recycler_questions.smoothScrollToPosition(collectionAdapter!!.itemCount)
            1 -> collection_recycler_questions.smoothScrollToPosition(0)
        }
    }

    override fun clearCollectionSearchFocus() {
        removeCollectionSearchET()
        setMainFabVisible()
        collection_et_search.text.clear()
    }

    override fun checkNoQuestionBundle() {
        questionsBundlesPath!!.document(currentBundleID).collection("questions").get()
            .addOnSuccessListener {
                collection_tv_no_question?.visibility = if (it.isEmpty) View.VISIBLE else View.GONE
            }
    }

    //Fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("selectedBundle")?.let {
            updateCurrentBundleName(arguments?.getString("selectedBundle"))
        }
        questionsBundlesPath!!.whereEqualTo("name", currentBundleName).get()
            .addOnSuccessListener {
                currentBundleID = if (!it.isEmpty)
                    it.documents[0].id
                else
                    bundleAllID
                setCurrentBundleTV()
                setQuestionsRecycler()
                setMainFabVisible()
                setCollectionSortListener()
                setCollectionSearch()
                mainActivityListener.setCollectionFragmentListener(this)
            }
    }

    private fun setMainFabVisible() {
        if (currentBundleName == getString(R.string.bundle_all))
            mainActivityListener.hideMainFAB()
        else
            mainActivityListener.showMainFAB()
    }

    private fun setCurrentBundleTV() {
        val userBundles: ArrayAdapter<String> =
            ArrayAdapter(context!!, R.layout.view_dialog_singlechoice)

        getSortedBundlesQuery().get().addOnSuccessListener {
            for (document in it.documents) {
                userBundles.add(document["name"] as String)
            }
        }
        collection_tv_current_bundle?.apply {
            text = currentBundleName
            setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(
                        Html.fromHtml(
                            getString(R.string.select_a_bundle),
                            Html.FROM_HTML_MODE_LEGACY
                        )
                    )
                    .setSingleChoiceItems(
                        userBundles,
                        userBundles.getPosition(currentBundleName)
                    ) { dialog, which ->
                        if (userBundles.getItem(which) != currentBundleName) {
                            updateCurrentBundleName(userBundles.getItem(which))
                            updateQuestionsRecycler()
                        }
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun updateCurrentBundleName(selectedBundleName: String?) {
        Firebase.firestore.collection("users")
            .document(auth.currentUser!!.uid)
            .update("current_bundle_name", selectedBundleName)
        currentBundleName = selectedBundleName!!
        collection_tv_current_bundle.text = currentBundleName
        setMainFabVisible()
    }

    private fun setQuestionsRecycler() {
        collectionAdapter =
            QuestionAdapter(getSortedQuestionsOptions(), this)
        collectionAdapter!!.startListening()
        collection_recycler_questions?.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = collectionAdapter
            setQuestionsRecyclerFeatures()
        }
    }

    private fun setQuestionsRecyclerFeatures() {
        checkNoQuestionBundle()
        collectionSwipe = setCollectionSwipe()

        collection_recycler_questions?.apply {
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
                        collection_et_search.clearFocus()
                        hideKeyboard()
                        if (collection_et_search.text.isEmpty())
                            removeCollectionSearchET()
                        when {
                            dy > 0 -> mainActivityListener.hideMainFAB()
                            dy < 0 -> setMainFabVisible()
                        }
                    }
                }
            })
        }
        attachSwipeToRecycler()
    }

    private fun attachSwipeToRecycler() {
        if (currentBundleName == getString(R.string.bundle_all))
            collectionSwipe.attachToRecyclerView(null)
        else
            collectionSwipe.attachToRecyclerView(collection_recycler_questions)
    }

    private fun setCollectionSwipe(): ItemTouchHelper {
        val swipeDeleteCallback = object : SwipeToDeleteCallback(context!!) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deletedDocID =
                    collectionAdapter!!.snapshots.getSnapshot(viewHolder.adapterPosition).id

                questionsBundlesPath!!.document(currentBundleID).apply {
                    collection("questions").document(deletedDocID).apply {
                        get().addOnSuccessListener {
                            this.delete()
                            checkNoQuestionBundle()
                            removeQuestionFromBundleAll(viewHolder)
                            setQuestionDeletedSnackbar(it.reference, it.data as HashMap)
                        }
                    }
                    update("quantity", FieldValue.increment(-1))
                }
            }
        }
        return ItemTouchHelper(swipeDeleteCallback)
    }

    private fun removeQuestionFromBundleAll(viewHolder: RecyclerView.ViewHolder) {
        val bundleAllRef = questionsBundlesPath!!.document(bundleAllID)

        bundleAllRef.apply {
            get().addOnSuccessListener {
                this.collection("questions")
                    .whereEqualTo("question", viewHolder.itemView.question_view_tv_question.text)
                    .whereEqualTo("original_bundle", currentBundleName)
                    .get().addOnSuccessListener {
                        questionsBundlesPath!!.document(bundleAllID).collection("questions")
                            .document(it.documents[0].id)
                            .delete()
                    }
                update("quantity", FieldValue.increment(-1))
            }
        }
    }

    private fun setQuestionDeletedSnackbar(
        deletedDocRef: DocumentReference,
        deletedDocData: HashMap<String, Any>
    ) {
        mainActivityListener.setMainSnackbar(
            getString(R.string.deleted_question_message),
            Snackbar.LENGTH_LONG,
            getString(R.string.cancel)
        ) {
            deletedDocRef.set(deletedDocData)
            questionsBundlesPath!!.document(currentBundleID)
                .update("quantity", FieldValue.increment(1))
            restoreQuestionInBundleAll(deletedDocData)
        }
    }

    private fun restoreQuestionInBundleAll(deletedDocData: HashMap<String, Any>) {
        val bundleAllRef = questionsBundlesPath!!.document(bundleAllID)
        val newQuestionRef = bundleAllRef.collection("questions").document()

        newQuestionRef.apply {
            set(deletedDocData)
            update("original_bundle", currentBundleName)
        }
        bundleAllRef.update("quantity", FieldValue.increment(1))
    }

    private fun updateQuestionsRecycler() {
        questionsBundlesPath!!.whereEqualTo("name", currentBundleName).get()
            .addOnSuccessListener {
                if (collection_et_search.text.isNotEmpty()) {
                    val collectionSearchText =
                        collection_et_search.text.toString().trimEnd().toLowerCase(
                            Locale.FRANCE
                        )
                    val collectionSearchQuery =
                        getSortedQuestionsQuery().whereArrayContains(
                            "search_keywords",
                            collectionSearchText
                        )
                    val collectionSearchOptions = FirestoreRecyclerOptions.Builder<Question>()
                        .setQuery(collectionSearchQuery, Question::class.java)
                        .build()
                    collectionAdapter!!.updateOptions(collectionSearchOptions)
                } else {
                    currentBundleID = it.documents[0].id
                    collectionAdapter!!.updateOptions(getSortedQuestionsOptions())
                    checkNoQuestionBundle()
                    attachSwipeToRecycler()
                }
            }
    }

    private fun getSortedQuestionsOptions(): FirestoreRecyclerOptions<Question> {
        val sortedQuestionsQuery = getSortedQuestionsQuery()

        return FirestoreRecyclerOptions.Builder<Question>()
            .setQuery(sortedQuestionsQuery, Question::class.java)
            .build()
    }

    private fun getSortedQuestionsQuery(): Query {
        val questionsPath = questionsBundlesPath!!
            .document(currentBundleID).collection("questions")
        val sortedQuestionsQuery =
            when (currentQuestionSort) {
                0 -> questionsPath.orderBy("date")
                1 -> questionsPath.orderBy("date", Query.Direction.DESCENDING)
                else -> {
                    Log.e(
                        "Database data not valid",
                        " The current bundle sort value is not valid."
                    )
                    questionsPath.orderBy("date")
                }
            }

        return (sortedQuestionsQuery)
    }

    private fun setCollectionSortListener() {
        val questionsSortType =
            arrayOf(
                getString(R.string.sort_oldest),
                getString(R.string.sort_newest)
            )

        collection_iv_sort?.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.sort_by)
                .setSingleChoiceItems(questionsSortType, currentQuestionSort) { dialog, which ->
                    if (which != currentQuestionSort) {
                        currentQuestionSort = which
                        Firebase.firestore.collection("users")
                            .document(auth.currentUser!!.uid)
                            .update("current_question_sort", currentQuestionSort)
                        updateQuestionsRecycler()
                        mainActivityListener.showMainFAB()
                    }
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setCollectionSearch() {
        collection_et_search.setOnFocusChangeListener { _, hasFocus ->
            when (hasFocus) {
                true -> mainActivityListener.isCollectionSearching(true)
                false -> mainActivityListener.isCollectionSearching(false)
            }
        }
        setCollectionSearchSwitch()
        collection_et_search?.doAfterTextChanged {
            updateQuestionsRecycler()
        }
    }

    private fun setCollectionSearchSwitch() {
        collection_iv_search?.setOnClickListener {
            collection_iv_search.visibility = View.GONE
            collection_tv_current_bundle.visibility = View.GONE
            collection_et_search.visibility = View.VISIBLE
            collection_et_search.requestFocus()
            (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                collection_et_search,
                InputMethodManager.SHOW_IMPLICIT
            )
            collection_iv_search_back.visibility = View.VISIBLE
        }
        collection_iv_search_back?.setOnClickListener {
            removeCollectionSearchET()
            setMainFabVisible()
            collection_et_search.text.clear()
            hideKeyboard()
        }
    }

    private fun removeCollectionSearchET() {
        collection_iv_search_back.visibility = View.GONE
        collection_et_search.visibility = View.GONE
        collection_iv_search.visibility = View.VISIBLE
        collection_tv_current_bundle.visibility = View.VISIBLE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityListener = context as? IMainActivity
            ?: throw ClassCastException("$context must implement IMainActivity")
    }

    override fun onStart() {
        super.onStart()
        collectionAdapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        collectionAdapter?.stopListening()
    }
}
