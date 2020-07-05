package com.example.kiok.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.kiok.R
import com.example.kiok.Tools.Companion.bundleAllID
import com.example.kiok.Tools.Companion.currentBundleID
import com.example.kiok.Tools.Companion.currentBundleName
import com.example.kiok.Tools.Companion.generateSearchKeywords
import com.example.kiok.Tools.Companion.questionsBundlesPath
import com.example.kiok.interfaces.ICollectionFragment
import com.example.kiok.models.Question
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import kotlinx.android.synthetic.main.view_edit_question.view.*
import kotlinx.android.synthetic.main.view_question.view.*
import java.util.*

class QuestionAdapter(
    options: FirestoreRecyclerOptions<Question>,
    private val collectionFragmentListener: ICollectionFragment
) :
    FirestoreRecyclerAdapter<Question, QuestionHolder>(options) {

    private lateinit var context: Context

    override fun onBindViewHolder(
        holder: QuestionHolder,
        position: Int,
        model: Question
    ) {
        context = holder.itemView.context
        setQuestionUI(holder, model)
        holder.itemView.setOnClickListener {
            if (currentBundleName == context.getString(R.string.bundle_all))
                getQuestionOriginalBundle(position)
            else
                questionOnClick(holder, position)
        }
    }

    private fun getQuestionOriginalBundle(position: Int) {
        questionsBundlesPath!!.document(bundleAllID).collection("questions")
            .document(snapshots.getSnapshot(position).id).get().addOnSuccessListener {
                collectionFragmentListener.showQuestionOriginalBundle(it["original_bundle"] as String)
            }
    }

    private fun setQuestionUI(holder: QuestionHolder, model: Question) {
        holder.itemView.apply {
            question_view_tv_question.text = model.question
            question_view_tv_answer.text = model.answer
        }
    }

    private fun questionOnClick(holder: QuestionHolder, position: Int) {
        val editQuestionDialogBuilder = setEditQuestionDialogBuilder()
        val editQuestionDialog = editQuestionDialogBuilder.create()
        val editQuestionView = setEditQuestionView(holder, editQuestionDialog)
        val prevQuestion = holder.itemView.question_view_tv_question.text.toString()

        editQuestionDialog.apply {
            setView(editQuestionView)
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = false
                setOnClickListener {
                    editQuestion(editQuestionDialog, editQuestionView, position, prevQuestion)
                }
            }
        }
    }

    private fun setEditQuestionDialogBuilder(): AlertDialog.Builder {
        return (
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.editing))
                    .setNegativeButton(context.getText(R.string.cancel)) { _, _ ->

                    }
                    .setPositiveButton(context.getText(R.string.edit)) { _, _ ->

                    }
                )
    }

    private fun setEditQuestionView(
        holder: QuestionHolder,
        editQuestionDialog: AlertDialog
    ): View {
        val editQuestionView =
            LayoutInflater.from(context).inflate(R.layout.view_edit_question, null)

        editQuestionView.apply {
            edit_question_layout_question.hint = context.getString(R.string.question)
            edit_question_layout_answer.hint = context.getString(R.string.answer)
            edit_question_et_question.apply {
                setText(holder.itemView.question_view_tv_question.text)
                requestFocus()
                doAfterTextChanged {
                    editQuestionDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                        this.text!!.isNotEmpty() && editQuestionView.edit_question_et_answer.text!!.isNotEmpty()
                }
            }
            edit_question_et_answer.apply {
                setText(holder.itemView.question_view_tv_answer.text)
                doAfterTextChanged {
                    editQuestionDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                        editQuestionView.edit_question_et_question.text!!.isNotEmpty() && this.text!!.isNotEmpty()
                }
            }
        }
        return editQuestionView
    }

    private fun editQuestion(
        editQuestionDialog: AlertDialog,
        editQuestionView: View,
        position: Int,
        prevQuestion: String
    ) {
        val question = editQuestionView.edit_question_et_question.text.toString().trim()
        val answer = editQuestionView.edit_question_et_answer.text.toString().trim()
        val questionSearchKeywords =
            arrayOf(question.toLowerCase(Locale.FRANCE), answer.toLowerCase(Locale.FRANCE))
        val currentBundleQuestionsRef =
            questionsBundlesPath!!.document(currentBundleID).collection("questions")

        currentBundleQuestionsRef.whereEqualTo("question", question).whereEqualTo("answer", answer)
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    currentBundleQuestionsRef.document(snapshots.getSnapshot(position).id)
                        .update(
                            "question", question,
                            "answer", answer,
                            "search_keywords", generateSearchKeywords(questionSearchKeywords)
                        )
                    editQuestionInBundleAll(question, answer, questionSearchKeywords, prevQuestion)
                    editQuestionDialog.dismiss()
                } else
                    editQuestionView.edit_question_layout_question.error =
                        context.getString(R.string.question_already_exists)
            }
    }

    private fun editQuestionInBundleAll(
        question: String,
        answer: String,
        questionSearchKeywords: Array<String>,
        prevQuestion: String
    ) {
        val bundleAllQuestionsRef =
            questionsBundlesPath!!.document(bundleAllID).collection("questions")

        bundleAllQuestionsRef
            .whereEqualTo("question", prevQuestion)
            .whereEqualTo("original_bundle", currentBundleName)
            .get().addOnSuccessListener {
                bundleAllQuestionsRef.document(it.documents[0].id).update(
                    "question", question,
                    "answer", answer,
                    "search_keywords", generateSearchKeywords(questionSearchKeywords)
                )
            }
    }

    override fun onCreateViewHolder(group: ViewGroup, i: Int): QuestionHolder {
        val view = LayoutInflater.from(group.context)
            .inflate(R.layout.view_question, group, false)

        return QuestionHolder(view)
    }
}

class QuestionHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
