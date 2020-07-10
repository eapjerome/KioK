package com.eap.kiok.fragments

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.eap.kiok.R
import com.eap.kiok.Tools.Companion.dismissLoadingDialog
import com.eap.kiok.Tools.Companion.questionsBundlesPath
import com.eap.kiok.hideKeyboard
import com.eap.kiok.interfaces.IMainActivity
import com.eap.kiok.models.Question
import com.google.firebase.firestore.ktx.toObject
import kotlinx.android.synthetic.main.fragment_revise.*

class ReviseFragment : Fragment() {

    private lateinit var mainActivityListener: IMainActivity
    private lateinit var bundleToRevise: String
    private var questionsList = mutableListOf<Question>()
    private var questionCounter = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_revise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bundleToRevise = arguments?.getString("bundleToRevise")!!
        revise_tv_bundle_name.text = bundleToRevise

        mainActivityListener.hideBottomNavigation()
        questionsBundlesPath!!.whereEqualTo("name", bundleToRevise).get().addOnSuccessListener {
            questionsBundlesPath!!.document(it.documents[0].id).collection("questions").get()
                .addOnSuccessListener { questionsQuery ->
                    for (document in questionsQuery.documents.shuffled())
                        questionsList.add(document.toObject<Question>()!!)
                    dismissLoadingDialog()
                    setReviseFragmentUI()
                    setReviseListeners()
                }
        }
    }

    private fun setReviseListeners() {
        setKeyboardDoneListener()
        setReviseButtonCheckListener()
        setReviseIVNextListener()
        setReviseButtonReplayListener()
        setReviseButtonHomeListener()
    }

    private fun setReviseFragmentUI() {
        revise_layout_user_answer?.visibility = View.VISIBLE
        revise_btn_check?.visibility = View.VISIBLE
        revise_iv_next?.visibility = View.VISIBLE
        setQuestion()
        setQuestionsCounterTV()
    }

    private fun setQuestion() {
        revise_tv_question.text = questionsList[questionCounter].question
    }

    private fun setKeyboardDoneListener() {
        revise_et_user_answer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (revise_et_user_answer.text?.isNotEmpty()!!)
                    checkUserAnswer()
            }
            false
        }
    }

    private fun setReviseButtonCheckListener() {
        revise_btn_check.setOnClickListener {
            if (revise_et_user_answer.text?.isNotEmpty()!!)
                checkUserAnswer()
        }
    }

    private fun checkUserAnswer() {
        hideKeyboard()
        val isCorrectAnswer =
            revise_et_user_answer.text.toString().trim() == questionsList[questionCounter].answer

        clearUserAnswerETFocus()
        revise_btn_check.visibility = View.GONE
        revise_layout_result.visibility = View.VISIBLE
        revise_iv_result_icon.setImageResource(android.R.color.transparent)
        if (isCorrectAnswer)
            revise_iv_result_icon.setImageResource(R.drawable.ic_correct_green_60dp)
        else {
            revise_iv_result_icon.setImageResource(R.drawable.ic_wrong_red_60dp)
            revise_tv_correct_answer.apply {
                visibility = View.VISIBLE
                text =
                    Html.fromHtml(
                        getString(R.string.correct_answer, questionsList[questionCounter].answer),
                        Html.FROM_HTML_MODE_LEGACY
                    )
            }
        }
        if (!isLastQuestion())
            setIsRevisingValue(true)
        else {
            setEndUI()
            setIsRevisingValue(false)
        }
    }

    private fun setEndUI() {
        revise_tv_questions_counter.visibility = View.INVISIBLE
        revise_iv_next.visibility = View.INVISIBLE
        revise_iv_replay.visibility = View.VISIBLE
        revise_iv_home.visibility = View.VISIBLE
    }

    private fun isLastQuestion(): Boolean {
        return (questionCounter + 1 == questionsList.size)
    }

    private fun nextQuestion() {
        questionCounter++
        updateQuestion()
        setIsRevisingValue(true)
    }

    private fun updateQuestion() {
        setQuestion()
        setQuestionsCounterTV()
        setUserAnswerETFocus()
        revise_btn_check.visibility = View.VISIBLE
        revise_layout_result.visibility = View.GONE
        if (!isLastQuestion())
            revise_iv_next.visibility = View.VISIBLE
        revise_tv_correct_answer.visibility = View.GONE
    }

    private fun setIsRevisingValue(value: Boolean) {
        mainActivityListener.isRevising(value)
    }

    private fun setUserAnswerETFocus() {
        revise_et_user_answer.apply {
            text?.clear()
            isFocusableInTouchMode = true
            isLongClickable = true
            requestFocus()
            (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                this,
                InputMethodManager.SHOW_IMPLICIT
            )
        }
    }

    private fun clearUserAnswerETFocus() {
        revise_et_user_answer.apply {
            isFocusable = false
            isLongClickable = false
        }
    }

    private fun setQuestionsCounterTV() {
        revise_tv_questions_counter.apply {
            visibility = View.VISIBLE
            text = getString(R.string.question_counter, questionCounter + 1, questionsList.size)
        }
    }

    private fun setReviseIVNextListener() {
        if (isLastQuestion())
            revise_iv_next.visibility = View.INVISIBLE
        revise_iv_next.apply {
            setOnClickListener {
                nextQuestion()
                if (isLastQuestion())
                    this.visibility = View.INVISIBLE
            }
        }
    }

    private fun setReviseButtonReplayListener() {
        revise_iv_replay.setOnClickListener {
            revise_iv_replay.visibility = View.GONE
            revise_iv_home.visibility = View.GONE
            setQuestionsCounterTV()
            questionCounter = 0
            questionsList.shuffle()
            updateQuestion()
        }
    }

    private fun setReviseButtonHomeListener() {
        revise_iv_home.setOnClickListener {
            mainActivityListener.goBack()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityListener = context as? IMainActivity
            ?: throw ClassCastException("$context must implement IMainActivity")
    }
}
