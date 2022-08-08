/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample.store

import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import walletconnect.core.session_state.model.SessionState
import walletconnect.sample.R
import walletconnect.sample.databinding.SessionCardBinding
import walletconnect.sample.loadSvgImage

class SessionCard : MaterialCardView {

    private var _binding: SessionCardBinding? = null
    private val binding get() = _binding!!

    private val sessionState: SessionState
    private val openSocket: (SessionState) -> Unit
    private val delete: (SessionState) -> Unit

    constructor(context: Context,
                sessionState: SessionState,
                openSocket: (SessionState) -> Unit,
                delete: (SessionState) -> Unit)
            : super(context) {
        _binding = SessionCardBinding.inflate(LayoutInflater.from(context), this)

        radius = context.resources.getDimensionPixelSize(R.dimen.card_radius).toFloat()
        cardElevation = context.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))

        this.sessionState = sessionState
        this.openSocket = openSocket
        this.delete = delete

        setClickActions()
        render()
    }

    private fun setClickActions() {
        binding.root.setOnClickListener {
            openSocket(sessionState)
        }
        binding.imageDeleteSession.setOnClickListener {
            delete(sessionState)
        }
    }

    private fun render() {
        binding.imagePeerLogo.loadSvgImage(sessionState.remotePeerMeta.icons?.firstOrNull())

        binding.textTopic.text = "Topic: " + sessionState.connectionParams.topic
        binding.textPeerId.text = "PeerId: " + sessionState.myPeerId
        binding.textRPeerId.text = "R.PeerId: " + sessionState.remotePeerId

        binding.textPeerName.text = sessionState.remotePeerMeta.name
        binding.textPeerDesc.text = sessionState.remotePeerMeta.description ?: ""
        binding.textPeerUrl.text = sessionState.remotePeerMeta.url

        binding.textChainId.text = sessionState.chainId.toString()
        binding.textAccounts.text = sessionState.accounts?.joinToString("\n") {
            it.take(32)
        } ?: ""
    }

}