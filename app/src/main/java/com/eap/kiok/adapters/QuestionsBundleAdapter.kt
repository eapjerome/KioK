package com.eap.kiok.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.eap.kiok.R
import com.eap.kiok.Tools.Companion.auth
import com.eap.kiok.Tools.Companion.currentBundleName
import com.eap.kiok.Tools.Companion.generateSearchKeywords
import com.eap.kiok.Tools.Companion.questionsBundlesPath
import com.eap.kiok.Tools.Companion.showLoadingDialog
import com.eap.kiok.interfaces.IHomeFragment
import com.eap.kiok.models.QuestionsBundle
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.view_questions_bundle.view.*
import kotlinx.android.synthetic.main.view_simple_edit_text.view.*
import java.util.*

class QuestionsBundleAdapter(
    options: FirestoreRecyclerOptions<QuestionsBundle>,
    private val homeFragmentListener: IHomeFragment
) :
    FirestoreRecyclerAdapter<QuestionsBundle, QuestionsBundleHolder>(options) {

    private lateinit var context: Context

    override fun onBindViewHolder(
        holder: QuestionsBundleHolder,
        position: Int,
        model: QuestionsBundle
    ) {
        context = holder.itemView.context
        setQuestionsBundleUI(holder, model)
        holder.itemView.apply {
            setOnClickListener {
                questionsBundleOnClick(holder)
            }
            setOnLongClickListener {
                if (holder.itemView.questions_bundle_view_tv_name.text == it.context.getString(R.string.bundle_all))
                    homeFragmentListener.setMainSnackbar(
                        context.getString(R.string.cannot_rename_bundle_message),
                        Snackbar.LENGTH_SHORT,
                        context.getString(R.string.ok)
                    ) { }
                else
                    setBundleRenameDialog(position, holder)
                homeFragmentListener.isRenameDialogShown(true)
                true
            }
            questions_bundle_view_btn_revise.setOnClickListener {
                if (model.quantity == 0)
                    homeFragmentListener.setMainSnackbar(
                        context.getString(R.string.cannot_revise_bundle_message),
                        Snackbar.LENGTH_SHORT,
                        context.getString(R.string.ok)
                    ) { }
                else {
                    homeFragmentListener.dismissSnackbar()
                    showLoadingDialog(context)
                    val bundleToRevise = bundleOf(
                        "bundleToRevise" to holder.itemView.questions_bundle_view_tv_name.text
                    )

                    homeFragmentListener.navigate(
                        R.id.action_homeFragment_to_reviseFragment,
                        bundleToRevise
                    )
                }
            }
        }
    }

    private fun setQuestionsBundleUI(holder: QuestionsBundleHolder, model: QuestionsBundle) {
        holder.itemView.apply {
            questions_bundle_view_tv_name.apply {
                text = model.name
                typeface =
                    if (model.name == context.getString(R.string.bundle_all))
                        Typeface.DEFAULT_BOLD
                    else
                        Typeface.DEFAULT
            }
            questions_bundle_view_tv_quantity.text =
                if (model.quantity == 0)
                    "aucune question"
                else
                    resources.getQuantityString(
                        R.plurals.quantity_of_questions, model.quantity, model.quantity
                    )
        }
    }

    private fun questionsBundleOnClick(holder: QuestionsBundleHolder) {
        val selectedBundle = bundleOf(
            "selectedBundle" to holder.itemView.questions_bundle_view_tv_name.text
        )

        homeFragmentListener.navigate(
            R.id.action_homeFragment_to_collectionFragment,
            selectedBundle
        )
    }

    private fun setBundleRenameDialog(position: Int, holder: QuestionsBundleHolder) {
        val renameBundleDialogBuilder =
            setRenameBundleDialogBuilder(holder)
        val renameBundleDialog = renameBundleDialogBuilder.create()
        val renameBundleView = setRenameBundleView(holder, renameBundleDialog)

        renameBundleDialog.apply {
            setView(renameBundleView)
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = false
                setOnClickListener {
                    renameBundle(renameBundleDialog, renameBundleView, position)
                    homeFragmentListener.isRenameDialogShown(false)
                }
            }
        }
    }

    private fun setRenameBundleDialogBuilder(holder: QuestionsBundleHolder): AlertDialog.Builder {
        return (
                AlertDialog.Builder(context)
                    .setTitle(
                        context.getString(
                            R.string.rename_bundle,
                            holder.itemView.questions_bundle_view_tv_name.text
                        )
                    )
                    .setNegativeButton(context.getText(R.string.cancel)) { _, _ ->
                        homeFragmentListener.isRenameDialogShown(false)
                    }
                    .setPositiveButton(context.getText(R.string.rename)) { _, _ ->

                    }
                    .setOnCancelListener {
                        homeFragmentListener.isRenameDialogShown(false)
                    }
                )
    }

    private fun setRenameBundleView(
        holder: QuestionsBundleHolder,
        renameBundleDialog: AlertDialog
    ): View {
        val renameBundleView =
            LayoutInflater.from(context).inflate(R.layout.view_simple_edit_text, null)

        renameBundleView.simple_edit_text_layout.hint = context.getString(R.string.new_name)
        renameBundleView.simple_edit_text_et.apply {
            setText(holder.itemView.questions_bundle_view_tv_name.text)
            requestFocus()
            doAfterTextChanged {
                renameBundleDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    text!!.isNotEmpty()
            }
        }
        return renameBundleView
    }

    private fun renameBundle(
        renameBundleDialog: AlertDialog,
        renameBundleView: View,
        position: Int
    ) {
        val newNameBundle = renameBundleView.simple_edit_text_et.text.toString().trim()

        questionsBundlesPath!!.whereEqualTo("name", newNameBundle).get().addOnSuccessListener {
            if (it.isEmpty) {
                questionsBundlesPath!!.document(snapshots.getSnapshot(position).id)
                    .update(
                        "name",
                        newNameBundle,
                        "search_keywords",
                        generateSearchKeywords(newNameBundle.toLowerCase(Locale.FRANCE))
                    )
                checkCurrentBundleName(newNameBundle)
                renameBundleDialog.dismiss()
            } else
                renameBundleView.simple_edit_text_layout.error =
                    renameBundleView.context.getString(R.string.bundle_already_exists)
        }
    }

    private fun checkCurrentBundleName(newNameBundle: String) {
        currentBundleName = newNameBundle
        Firebase.firestore.collection("users")
            .document(auth.currentUser!!.uid)
            .update("current_bundle_name", currentBundleName)
    }

    override fun onCreateViewHolder(group: ViewGroup, i: Int): QuestionsBundleHolder {
        val view = LayoutInflater.from(group.context)
            .inflate(R.layout.view_questions_bundle, group, false)

        return QuestionsBundleHolder(view)
    }
}

class QuestionsBundleHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
