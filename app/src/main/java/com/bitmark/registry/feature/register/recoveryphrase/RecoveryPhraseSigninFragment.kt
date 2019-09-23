package com.bitmark.registry.feature.register.recoveryphrase

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bitmark.registry.R
import com.bitmark.registry.logging.Tracer
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.logging.Event
import com.bitmark.registry.logging.EventLogger
import com.bitmark.registry.feature.register.authentication.AuthenticationFragment
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.view.ProgressAppCompatDialog
import com.bitmark.registry.util.view.SimpleRecyclerViewAdapter
import com.bitmark.sdk.features.Account
import com.bitmark.sdk.features.internal.RecoveryPhrase
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.fragment_recovery_phrase_signin.*
import java.util.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 7/5/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseSigninFragment : BaseSupportFragment() {

    companion object {
        private const val URI = "uri"

        private const val RECOVER_ACCOUNT = "recover_account"

        private const val TAG = "RecoveryPhraseSigninFragment"

        fun newInstance(
            uri: String? = null,
            recoverAccount: Boolean = false
        ): RecoveryPhraseSigninFragment {
            val bundle = Bundle()
            if (uri != null) bundle.putString(URI, uri)
            bundle.putBoolean(RECOVER_ACCOUNT, recoverAccount)
            val fragment = RecoveryPhraseSigninFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    internal lateinit var navigator: Navigator

    @Inject
    internal lateinit var viewModel: RecoveryPhraseSigninViewModel

    @Inject
    internal lateinit var dialogController: DialogController

    @Inject
    internal lateinit var logger: EventLogger

    private var uri: String? = null

    private val adapter = RecoveryPhraseAdapter(textColor = R.color.blue_ribbon)

    override fun layoutRes(): Int = R.layout.fragment_recovery_phrase_signin

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()

        uri = arguments?.getString(URI)
        val recoverAccount = arguments?.getBoolean(RECOVER_ACCOUNT) ?: false

        val bip39Words = RecoveryPhrase.getWords(Locale.ENGLISH)

        val phraseLayoutManager =
            GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
        rvRecoveryPhrase.layoutManager = phraseLayoutManager
        rvRecoveryPhrase.isNestedScrollingEnabled = false
        (rvRecoveryPhrase.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false
        rvRecoveryPhrase.adapter = adapter
        adapter.setDefault()

        val suggestionAdapter =
            SimpleRecyclerViewAdapter(R.layout.item_text_suggestion)
        val suggestionLayoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        rvSuggestion.layoutManager = suggestionLayoutManager
        rvSuggestion.isNestedScrollingEnabled = false
        (rvSuggestion.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false
        rvSuggestion.adapter = suggestionAdapter

        if (arguments?.getBoolean(RECOVER_ACCOUNT) == true) {
            ivBack.gone()
        } else {
            ivBack.visible()
        }

        activity?.detectKeyBoardState { showing ->
            if (view == null) return@detectKeyBoardState
            if (showing) {
                tvSwitchWord.gone()
                btnSubmit.gone()
                setSuggestionVisibility(true)
            } else {
                tvSwitchWord.visible()
                btnSubmit.visible()
                setSuggestionVisibility(false)
                if (adapter.isValid()) {
                    adapter.clearFocus()
                }
            }
        }

        btnSubmit.setSafetyOnclickListener {
            submit(adapter.getPhrase(), uri, recoverAccount)
        }

        tvSwitchWord.setOnClickListener {
            setErrorVisibility(false)
            if (adapter.itemCount == Version.TWELVE.value) {
                tvInstruction.setText(R.string.please_type_all_24_word)
                tvSwitchWord.setText(R.string.are_you_using_12_word)
                adapter.setDefault(Version.TWENTY_FOUR)
            } else {
                tvInstruction.setText(R.string.please_type_all_12_word)
                tvSwitchWord.setText(R.string.are_you_using_24_word)
                adapter.setDefault()
            }
        }

        ivBack.setSafetyOnclickListener {
            navigator.popFragment()
        }

        adapter.setOnTextChangeListener(object : OnTextChangeListener {
            override fun onTextChanged(item: Item) {
                if (adapter.isValid()) {
                    btnSubmit.enable()
                } else {
                    btnSubmit.disable()
                }

                if (item.word.isBlank()) {
                    suggestionAdapter.clear()
                } else {
                    val matchedItems = bip39Words.filter { w ->
                        w.startsWith(
                            item.word,
                            ignoreCase = true
                        )
                    }
                    if (matchedItems.isEmpty()) return
                    suggestionAdapter.set(matchedItems, 3)
                }
            }

            override fun afterTextChanged(item: Item) {
            }

        })

        adapter.setOnDoneListener {
            submit(adapter.getPhrase(), uri, recoverAccount)
        }

        suggestionAdapter.setItemClickListener { text ->
            adapter.set(text)
            if (!adapter.requestNextFocus()) {
                activity?.hideKeyBoard()
            }
        }

        ivUp.setOnClickListener {
            if (!adapter.requestPrevFocus()) {
                activity?.hideKeyBoard()
            }
        }

        ivDown.setOnClickListener {
            if (!adapter.requestNextFocus()) {
                activity?.hideKeyBoard()
            }
        }
    }

    override fun deinitComponents() {
        dialogController.dismiss()
        super.deinitComponents()
    }

    override fun observe() {
        super.observe()

        viewModel.checkSameAccountLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val same = res.data() ?: false
                    Tracer.INFO.log(TAG, "check same account result: $same")
                    if (same) {
                        navigateAuthentication(adapter.getPhrase(), uri, true)
                    } else {
                        dialogController.confirm(
                            R.string.access_other_account,
                            R.string.the_recovery_phrase_you_entered_is_not_your,
                            false,
                            R.string.do_it,
                            {
                                getFirebaseToken { token ->
                                    viewModel.clearData(
                                        token
                                    )
                                }
                            },
                            R.string.cancel
                        )
                    }
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "check same account failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error
                    )
                }
            }
        })

        val clearDataProgressDialog = ProgressAppCompatDialog(
            context!!,
            "",
            getString(R.string.clear_data),
            true
        )

        viewModel.clearDataLiveData().observe(this, Observer { res ->

            when {
                res.isSuccess() -> {
                    dialogController.dismiss(clearDataProgressDialog)
                    navigateAuthentication(adapter.getPhrase(), uri, false)
                }

                res.isLoading() -> {
                    dialogController.show(clearDataProgressDialog)
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "clear data failed: ${res.throwable() ?: "unknown"}"
                    )
                    logger.logError(
                        Event.ACCOUNT_RECOVER_CLEAR_DATA_ERROR,
                        res.throwable()
                    )
                    dialogController.dismiss(clearDataProgressDialog)
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error
                    )
                }
            }
        })

    }

    private fun setSuggestionVisibility(visible: Boolean) {
        if (visible) {
            ivUp.visible()
            ivDown.visible()
            rvSuggestion.visible()
        } else {
            ivUp.gone()
            ivDown.gone()
            rvSuggestion.gone()
        }
    }

    private fun submit(
        phrase: Array<String?>,
        uri: String?,
        recoverAccount: Boolean
    ) {
        activity?.hideKeyBoard()
        var account: Account? = null
        val isValid = try {
            account = Account.fromRecoveryPhrase(*phrase)
            true
        } catch (e: Throwable) {
            false
        }

        if (isValid) {
            setErrorVisibility(false)
            if (recoverAccount) {
                viewModel.checkSameAccount(account!!.accountNumber)
            } else {
                navigateAuthentication(phrase, uri, false)
            }
        } else {
            setErrorVisibility(true)
        }
    }

    private fun navigateAuthentication(
        phrase: Array<String?>,
        uri: String?,
        recoverAccount: Boolean
    ) {
        navigator.anim(Navigator.RIGHT_LEFT).replaceFragment(
            R.id.layoutContainer,
            AuthenticationFragment.newInstance(
                phrase,
                uri,
                recoverAccount
            )
        )
    }

    private fun setErrorVisibility(visible: Boolean) {
        if (visible) {
            tvError.visible()
            tvTryAgain.visible()
        } else {
            tvError.gone()
            tvTryAgain.gone()
        }
    }

    override fun onBackPressed() = navigator.popFragment() ?: false

    private fun getFirebaseToken(action: (String?) -> Unit) {
        FirebaseInstanceId.getInstance()
            .instanceId.addOnCompleteListener { task ->
            if (!task.isSuccessful) action.invoke(null)
            else action.invoke(task.result?.token)
        }
    }
}