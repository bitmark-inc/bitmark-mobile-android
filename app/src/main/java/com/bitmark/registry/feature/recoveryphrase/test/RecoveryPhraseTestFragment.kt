package com.bitmark.registry.feature.recoveryphrase.test

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
    lateinit var viewModel: RecoveryPhraseTestViewModel

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var dialogController: DialogController

    private lateinit var progressDialog: ProgressAppCompatDialog

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
        rvRecoveryPhrase.adapter = adapter
        val hiddenSequences =
            (1..recoveryPhrase.size).shuffled().take(HIDDEN_ITEM_QUANTITY)
                .toIntArray()

        adapter.set(
            recoveryPhrase,
            hiddenSequences
        )

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

                    tvPrimaryMessage.invisible()
                    tvSecondaryMessage.invisible()
                    btnAction.invisible()

                    adapter.set(
                        recoveryPhrase,
                        hiddenSequences
                    )

                }

                getString(R.string.done) -> {
                    viewModel.removeRecoveryActionRequired()
                }

                else -> {
                    // remove access
                    viewModel.getAccountInfo()
                }
            }
        }

        adapter.setOnItemClickListener { item ->
            if (!hiddenSequences.contains(item.sequence)) return@setOnItemClickListener
            adapter.hide(item.sequence)
            tvs.firstOrNull { tv -> tv.text == item.word }?.visible()
        }

        ivBack.setOnClickListener { navigator.popChildFragmentToRoot() }
    }

    private fun handleRecoveryItemClicked(
        v: View,
        phrase: Array<String>,
        adapter: RecoveryPhraseAdapter,
        removeAccess: Boolean
    ) {
        v.invisible()
        val text = (v as? AppCompatTextView)?.text.toString()
        adapter.showHiddenSequentially(text)

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

        viewModel.removeRecoveryActionRequiredLiveData()
            .observe(this, Observer { res ->
                when {
                    res.isSuccess() -> {
                        navigator.popChildFragmentToRoot()
                    }
                }
            })

        viewModel.getAccountInfoLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    val info = res.data()!!
                    accountNumber = info.first
                    val keyAlias = info.second
                    val spec = KeyAuthenticationSpec.Builder(context)
                        .setAuthenticationDescription(getString(R.string.your_authorization_is_required))
                        .setKeyAlias(keyAlias).build()
                    activity?.removeAccount(
                        accountNumber,
                        spec,
                        dialogController,
                        successAction = {
                            getFirebaseToken { token ->
                                viewModel.removeAccess(token)
                            }
                        },
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

                res.isError() -> {
                    dialogController.alert(
                        R.string.error,
                        R.string.unexpected_error,
                        R.string.ok
                    ) { navigator.popChildFragmentToRoot() }
                }
            }
        })

        viewModel.removeAccessLiveData().observe(this, Observer { res ->
            when {
                res.isSuccess() -> {
                    progressDialog.dismiss()
                    val intent = Intent(
                        context,
                        DeleteFirebaseInstanceIdService::class.java
                    )
                    context?.startService(intent)
                    Intercom.client().logout()
                    navigator.startActivityAsRoot(RegisterContainerActivity::class.java)
                }

                res.isError() -> {
                    progressDialog.dismiss()
                    dialogController.alert(
                        R.string.error,
                        R.string.could_not_remove_access_due_to_unexpected_problem,
                        R.string.ok
                    ) {
                        navigator.finishActivity()
                    }
                }

                res.isLoading() -> {
                    progressDialog = ProgressAppCompatDialog(
                        context!!,
                        getString(R.string.removing_access_three_dot),
                        "%s \"%s\"".format(
                            getString(
                                R.string.removing_access_for_account
                            ), accountNumber.shortenAccountNumber()
                        ), true
                    )
                    progressDialog.show()
                }

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
        adapter.setColors(R.color.torch_red)
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
        adapter.setColors(R.color.blue_ribbon)
    }

    override fun onBackPressed() =
        navigator.popChildFragmentToRoot()
}