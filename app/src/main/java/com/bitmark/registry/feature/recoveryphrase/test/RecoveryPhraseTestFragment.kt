package com.bitmark.registry.feature.recoveryphrase.test

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitmark.registry.R
import com.bitmark.registry.feature.BaseSupportFragment
import com.bitmark.registry.feature.BaseViewModel
import com.bitmark.registry.feature.Navigator
import com.bitmark.registry.feature.register.recoveryphrase.RecoveryPhraseAdapter
import com.bitmark.registry.util.extension.invisible
import com.bitmark.registry.util.extension.setSafetyOnclickListener
import com.bitmark.registry.util.extension.setTextColorRes
import com.bitmark.registry.util.extension.visible
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

        fun newInstance(recoveryPhrase: Array<String>): RecoveryPhraseTestFragment {
            val bundle = Bundle()
            bundle.putStringArray(RECOVERY_PHRASE, recoveryPhrase)
            val fragment = RecoveryPhraseTestFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    lateinit var viewModel: RecoveryPhraseTestViewModel

    @Inject
    lateinit var navigator: Navigator

    override fun layoutRes(): Int = R.layout.fragment_recovery_phrase_test

    override fun viewModel(): BaseViewModel? = viewModel

    override fun initComponents() {
        super.initComponents()

        val recoveryPhrase = arguments?.getStringArray(RECOVERY_PHRASE)!!

        val adapter = RecoveryPhraseAdapter(editable = false)
        val layoutManager =
            GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
        rvRecoveryPhrase.layoutManager = layoutManager
        rvRecoveryPhrase.isNestedScrollingEnabled = false
        rvRecoveryPhrase.adapter = adapter
        val hiddenPhrases =
            getRandomHiddenPhrase(HIDDEN_ITEM_QUANTITY, recoveryPhrase)
        tvWord1.text = hiddenPhrases[0]
        tvWord2.text = hiddenPhrases[1]
        tvWord3.text = hiddenPhrases[2]
        tvWord4.text = hiddenPhrases[3]

        adapter.set(
            recoveryPhrase,
            hiddenPhrases
        )

        btnAction.setSafetyOnclickListener {
            if (btnAction.text.toString() == getString(R.string.done)) {
                viewModel.removeRecoveryActionRequired()
            } else {
                // retry
                tvWord1.visible()
                tvWord2.visible()
                tvWord3.visible()
                tvWord4.visible()

                tvPrimaryMessage.invisible()
                tvSecondaryMessage.invisible()
                btnAction.invisible()

                adapter.set(
                    recoveryPhrase,
                    hiddenPhrases
                )
            }
        }

        tvWord1.setOnClickListener {
            handleRecoveryItemClicked(
                it,
                recoveryPhrase,
                adapter
            )
        }

        tvWord2.setOnClickListener {
            handleRecoveryItemClicked(
                it,
                recoveryPhrase,
                adapter
            )
        }

        tvWord3.setOnClickListener {
            handleRecoveryItemClicked(
                it,
                recoveryPhrase,
                adapter
            )
        }

        tvWord4.setOnClickListener {
            handleRecoveryItemClicked(
                it,
                recoveryPhrase,
                adapter
            )
        }

        tvCancel.setOnClickListener { navigator.popChildFragmentToRoot() }
    }

    private fun handleRecoveryItemClicked(
        v: View,
        phrase: Array<String>,
        adapter: RecoveryPhraseAdapter
    ) {
        v.invisible()
        val text = (v as? AppCompatTextView)?.text.toString()
        adapter.showHiddenSequentially(text)

        if (adapter.isItemsVisible()) {
            if (adapter.compare(phrase)) {
                showSuccess(adapter)
            } else {
                showError(adapter)
            }
        }
    }

    private fun getRandomHiddenPhrase(
        size: Int,
        phrase: Array<String>
    ): Array<String> {
        val randomIndexes = (0 until phrase.size).shuffled().take(size)
        val hiddenPhrases = Array(size) { "" }
        for (i in 0 until size) {
            hiddenPhrases[i] = phrase[randomIndexes[i]]
        }
        return hiddenPhrases
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
    }

    private fun showError(adapter: RecoveryPhraseAdapter) {
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

    private fun showSuccess(adapter: RecoveryPhraseAdapter) {
        tvPrimaryMessage.setTextColorRes(android.R.color.black)
        tvSecondaryMessage.setTextColorRes(android.R.color.black)
        tvPrimaryMessage.setText(R.string.success_exclamation)
        tvSecondaryMessage.setText(R.string.keep_your_written_copy_private)
        btnAction.setText(R.string.done)
        tvPrimaryMessage.visible()
        tvSecondaryMessage.visible()
        btnAction.visible()
        adapter.setColors(R.color.blue_ribbon)
    }

    override fun onBackPressed() =
        navigator.popChildFragmentToRoot()
}