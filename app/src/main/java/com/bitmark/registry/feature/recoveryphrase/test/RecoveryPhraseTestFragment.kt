package com.bitmark.registry.feature.recoveryphrase.test

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.DialogController
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.notification.DeleteFirebaseInstanceIdService
import com.bitmark.registry.feature.register.RegisterContainerActivity
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseAdapter
import com.bitmark.registry.logging.Event
import com.bitmark.registry.logging.EventLogger
import com.bitmark.registry.logging.Tracer
import com.bitmark.registry.util.extension.*
import com.bitmark.registry.util.view.ProgressAppCompatDialog
import com.bitmark.sdk.authentication.KeyAuthenticationSpec
import com.google.firebase.iid.FirebaseInstanceId
import io.intercom.android.sdk.Intercom
import kotlinx.android.synthetic.main.fragment_recovery_phrase_test.*
import kotlinx.android.synthetic.main.layout_recovery_phrase_enter.*
import javax.inject.Inject


/**
 * @author Hieu Pham
 * @since 2019-07-25
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class RecoveryPhraseTestFragment : BaseSupportFragment() {

    companion object {
        private const val HIDDEN_ITEM_QUANTITY = 4

        private const val RECOVERY_PHRASE = "recovery_phrase"

        private const val REMOVE_ACCESS = "remove_access"

        private const val TAG = "RecoveryPhraseTestFragment"

        fun newInstance(
            recoveryPhrase: Array<String>,
            removeAccess: Boolean = false
        ): RecoveryPhraseTestFragment {
            val bundle = Bundle()
            bundle.putStringArray(RECOVERY_PHRASE, recoveryPhrase)
            bundle.putBoolean(REMOVE_ACCESS, removeAccess)
            val fragment = RecoveryPhraseTestFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    internal lateinit var viewModel: RecoveryPhraseTestViewModel

    @Inject
    internal lateinit var navigator: Navigator

    @Inject
    internal lateinit var dialogController: DialogController

    @Inject
    internal lateinit var logger: EventLogger

    private lateinit var accountNumber: String

    override fun layoutRes(): Int = R.layout.fragment_recovery_phrase_test

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()

        val recoveryPhrase = arguments?.getStringArray(RECOVERY_PHRASE)!!
        val removeAccess = arguments?.getBoolean(REMOVE_ACCESS) ?: false

        if (removeAccess) {
            toolbarTitle.setText(R.string.recovery_phrase_sign_out)
        } else {
            toolbarTitle.setText(R.string.test_recovery_phrase)
        }

        val adapter = RecoveryPhraseAdapter(editable = false)
        val layoutManager =
            GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
        rvRecoveryPhrase.layoutManager = layoutManager
        rvRecoveryPhrase.isNestedScrollingEnabled = false
        rvRecoveryPhrase.itemAnimator = null
        rvRecoveryPhrase.adapter = adapter
        val hiddenSequences =
            (1..recoveryPhrase.size).shuffled().take(HIDDEN_ITEM_QUANTITY)
                .toIntArray()

        adapter.set(
            recoveryPhrase,
            hiddenSequences
        )
        adapter.requestNextHiddenFocus()

        val hiddenWords = adapter.getHiddenWords().toMutableList().shuffled()
        val tvs = listOf<TextView>(tvWord1, tvWord2, tvWord3, tvWord4)
        tvs.forEachIndexed { i, tv ->
            tv.text = hiddenWords[i]
            tv.setOnClickListener { v ->
                handleRecoveryItemClicked(
                    v,
                    recoveryPhrase,
                    adapter,
                    removeAccess
                )
            }
        }

        btnAction.setSafetyOnclickListener {
            when (btnAction.text.toString()) {

                getString(R.string.retry) -> {
                    tvWord1.visible()
                    tvWord2.visible()
                    tvWord3.visible()
                    tvWord4.visible()

                    hideNotice()
                    btnAction.invisible()

                    adapter.set(
                        recoveryPhrase,
                        hiddenSequences
                    )
                    adapter.requestNextHiddenFocus()

                }

                getString(R.string.done) -> {
                    navigator.popChildFragmentToRoot()
                }

                else -> {
                    // remove access
                    viewModel.getAccountInfo()
                }
            }
        }

        adapter.setOnItemClickListener { item ->
            if (!hiddenSequences.contains(item.sequence)) return@setOnItemClickListener
            val hiddenCount = adapter.countHidden()
            hideNotice()
            btnAction.invisible()
            if (item.isHidden()) {
                adapter.requestFocus(item.sequence)
            } else {
                adapter.hide(item.sequence)
            }
            if (hiddenCount == 0) {
                adapter.setTextColor(R.color.blue_ribbon, hiddenSequences)
            }
            tvs.firstOrNull { tv -> tv.text == item.word }?.visible()
        }

        ivBack.setOnClickListener { navigator.popChildFragmentToRoot() }
    }

    override fun deinitComponents() {
        dialogController.dismiss()
        super.deinitComponents()
    }

    private fun handleRecoveryItemClicked(
        v: View,
        phrase: Array<String>,
        adapter: RecoveryPhraseAdapter,
        removeAccess: Boolean
    ) {
        v.invisible()
        val text = (v as? AppCompatTextView)?.text.toString()
        adapter.show(text)

        if (adapter.isItemsVisible()) {
            if (adapter.compare(phrase)) {
                showSuccess(adapter, removeAccess)
            } else {
                showError(adapter)
            }
        }
    }

    override fun observe() {
        super.observe()

        viewModel.prepareRemoveAccessLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val data = res.data() ?: return@Observer
                    if (activity == null) return@Observer
                    val authorized = data.first
                    accountNumber = data.second
                    val keyAlias = data.third

                    if (authorized) {
                        removeAccount(
                            activity!!,
                            accountNumber,
                            keyAlias,
                            dialogController,
                            navigator
                        ) {
                            getFirebaseToken { token ->
                                viewModel.removeAccess(token)
                            }
                        }
                    } else {
                        dialogController.confirm(
                            R.string.your_data_will_be_lost,
                            R.string.if_you_remove_access_now,
                            false,
                            R.string.ok,
                            {
                                removeAccount(
                                    activity!!,
                                    accountNumber,
                                    keyAlias,
                                    dialogController,
                                    navigator
                                ) {
                                    getFirebaseToken { token ->
                                        viewModel.removeAccess(token)
                                    }
                                }
                            },
                            R.string.cancel, {
                                navigator.popChildFragmentToRoot()
                            }
                        )
                    }
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "prepare removing access failed: ${res.throwable()
                            ?: "unknown"}"
                    )
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error,
                        R.string.ok
                    ) { navigator.popChildFragmentToRoot() }
                }
            }
        })

        var removeAccessProgressDialog: ProgressAppCompatDialog? = null

        viewModel.removeAccessLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    dialogController.dismiss(
                        removeAccessProgressDialog ?: return@Observer
                    )
                    val intent = Intent(
                        context,
                        DeleteFirebaseInstanceIdService::class.java
                    )
                    context?.startService(intent)
                    Intercom.client().logout()
                    navigator.startActivityAsRoot(RegisterContainerActivity::class.java)
                }

                res.isError() -> {
                    Tracer.ERROR.log(
                        TAG,
                        "remove access failed: ${res.throwable() ?: "unknown"}"
                    )
                    logger.logError(Event.ACCOUNT_LOGOUT_ERROR, res.throwable())
                    dialogController.dismiss(
                        removeAccessProgressDialog ?: return@Observer
                    )
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_remove_access_due_to_unexpected_problem,
                        R.string.ok
                    ) {
                        navigator.finishActivity()
                    }
                }

                res.isLoading() -> {
                    removeAccessProgressDialog = ProgressAppCompatDialog(
                        context!!,
                        getString(R.string.removing_access_three_dot),
                        "%s \"%s\"".format(
                            getString(
                                R.string.removing_access_for_account
                            ), accountNumber.shortenAccountNumber()
                        ), true
                    )
                    dialogController.show(
                        removeAccessProgressDialog ?: return@Observer
                    )
                }

            }
        })
    }

    private fun removeAccount(
        activity: Activity,
        accountNumber: String,
        keyAlias: String,
        dialogController: DialogController,
        navigator: Navigator,
        successAction: () -> Unit
    ) {

        val spec = KeyAuthenticationSpec.Builder(activity.applicationContext)
            .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
            .setKeyAlias(keyAlias).build()
        activity.removeAccount(
            accountNumber,
            spec,
            dialogController,
            successAction = successAction,
            setupRequiredAction = { navigator.gotoSecuritySetting() },
            invalidErrorAction = {
                dialogController.alert(
                    R.string.account_is_not_accessible,
                    R.string.sorry_you_have_changed_or_removed
                ) {
                    navigator.startActivityAsRoot(
                        RegisterContainerActivity::class.java,
                        RegisterContainerActivity.getBundle(
                            recoverAccount = true
                        )
                    )
                }
            })
    }

    private fun getFirebaseToken(action: (String?) -> Unit) {
        FirebaseInstanceId.getInstance()
            .instanceId.addOnCompleteListener { task ->
            if (!task.isSuccessful) action.invoke(null)
            else action.invoke(task.result?.token)
        }
    }

    private fun showError(
        adapter: RecoveryPhraseAdapter
    ) {
        tvPrimaryMessage.setTextColorRes(R.color.torch_red)
        tvSecondaryMessage.setTextColorRes(R.color.torch_red)
        tvPrimaryMessage.setText(R.string.error)
        tvSecondaryMessage.setText(R.string.please_try_again)
        btnAction.setText(R.string.retry)
        tvPrimaryMessage.visible()
        tvSecondaryMessage.visible()
        btnAction.visible()
        adapter.setTextColor(R.color.torch_red)
    }

    private fun showSuccess(
        adapter: RecoveryPhraseAdapter,
        removeAccess: Boolean
    ) {
        tvPrimaryMessage.setTextColorRes(android.R.color.black)
        tvSecondaryMessage.setTextColorRes(android.R.color.black)
        tvPrimaryMessage.setText(R.string.success_exclamation)
        tvSecondaryMessage.setText(R.string.keep_your_written_copy_private)
        btnAction.setText(if (removeAccess) R.string.remove_access else R.string.done)
        tvPrimaryMessage.visible()
        tvSecondaryMessage.visible()
        btnAction.visible()
        adapter.setTextColor(R.color.blue_ribbon)
    }

    private fun hideNotice() {
        tvPrimaryMessage.invisible()
        tvSecondaryMessage.invisible()
    }

    override fun onBackPressed() =
        navigator.popChildFragmentToRoot()
}