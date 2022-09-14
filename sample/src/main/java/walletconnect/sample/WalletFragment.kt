/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import walletconnect.core.Wallet
import walletconnect.core.session.callback.CallbackData
import walletconnect.core.session.callback.RequestCallback
import walletconnect.core.session.callback.SessionCallback
import walletconnect.core.session.callback.SocketCallback
import walletconnect.core.session.model.InitialSessionState
import walletconnect.core.session_state.model.PeerMeta
import walletconnect.sample.databinding.WalletFragmentBinding
import walletconnect.sample.sign.SignFragment
import walletconnect.sample.store.SessionListFragment
import java.util.*

class WalletFragment : BaseFragment() {

    private var _binding: WalletFragmentBinding? = null
    private val binding get() = _binding!!

    override val fragmentTag: String = javaClass.simpleName
    private lateinit var wallet: Wallet
    override val initialSessionState by lazy {
        InitialSessionState(
                connectionParams,
                myPeerId = UUID.randomUUID().toString(),
                myPeerMeta = PeerMeta(
                        name = "Wallet",
                        url = "https://wallet.com",
                        description = "Wallet Description",
                        icons = listOf("https://img.favpng.com/1/20/24/wallet-icon-png-favpng-TQrAD3mHXn7Yey6wnt6aa97YF.jpg")
                )
        )
    }

    // region lifecycle
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?)
            : View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = WalletFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wallet = createWallet(sessionStoreName = fragmentTag)
        sessionLifecycle = wallet

        setClickActions()
        observeSignResponseEvents()
    }

    override fun consoleTextView(): TextView = binding.textConsole

    override fun onConsoleUpdate() {
        binding.scrollView.post {
            // then wait for the child of the scroll view to be laid out
            binding.scrollView.getChildAt(0).postDelayed(
                    {
                        // finally scroll
                        binding.scrollView.smoothScrollBy(0, binding.scrollView.bottom)
                    },
                    500L
            )
        }
    }

    override fun onDestroyView() {
        // [super.onDestroyView] calls [sessionManager.close], which expects caller to trigger [SessionCallback]
        onSessionCallback(SessionCallback.SessionClosedLocally)
        super.onDestroyView()
        _binding = null
    }
    // endregion

    private fun setClickActions() {
        binding.buttonClear.setOnClickListener {
            binding.textConsole.text = ""
        }
        binding.buttonOpen.setOnClickListener {
            wallet.openSocketAsync(initialSessionState,
                                   callback = ::onSessionCallback,
                                   onOpened = null)
        }
        binding.buttonClose.setOnClickListener {
            wallet.closeAsync(deleteLocal = false,
                              deleteRemote = false)
        }
        binding.buttonDelete.setOnClickListener {
            wallet.closeAsync(deleteLocal = true,
                              deleteRemote = true)
        }

        binding.buttonAcceptSessionRequest.setOnClickListener {
            wallet.approveSession(1,
                                  listOf("0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
                                         "0x621261D26847B423Df639848Fb53530025a008e8"))
        }
        binding.buttonRejectSessionRequest.setOnClickListener {
            wallet.rejectSession()
        }
        binding.buttonUpdateSessionRequest.setOnClickListener {
            wallet.updateSession(chainId = 2,
                                 accounts = listOf("0x621261D26847B423Df639848Fb53530025a008e8"),
                                 approved = true)
        }
        binding.buttonUpdateSessionRequestFalse.setOnClickListener {
            wallet.updateSession(chainId = 2,
                                 accounts = listOf("0x621261D26847B423Df639848Fb53530025a008e8"),
                                 approved = false)
        }

        binding.buttonDisconnect.setOnClickListener {
            wallet.disconnectSocketAsync()
        }
        binding.buttonReconnect.setOnClickListener {
            wallet.reconnectSocketAsync()
        }

        binding.imageSessionList.setOnClickListener {
            SessionListFragment
                    .create(fragmentTag)
                    .show(requireActivity().supportFragmentManager, fragmentTag)
        }
    }

    override fun onSessionCallback(callbackData: CallbackData) {
        viewLifecycleOwner.lifecycle.coroutineScope.launch(dispatcherProvider.ui()) {
            val stateText = updateLatestState(callbackData)
            if (stateText != null) {
                binding.textCallbacks.text = stateText
            }

            when (callbackData) {
                is SessionCallback -> {
                    when (callbackData) {
                        is SessionCallback.LocalSessionStateUpdated -> {
                            if (callbackData.sessionState == null) {
                                binding.imageMyLogo.setImageDrawable(null)
                                binding.textMyId.text = "Me(null)"
                                binding.imageRemoteLogo.setImageDrawable(null)
                                binding.textRemoteId.text = "Remote(null)"
                                binding.textChainId.text = "null"
                                binding.textAccounts.text = "null"
                            } else {
                                binding.imageMyLogo.loadSvgImage(callbackData.sessionState!!.myPeerMeta.icons?.firstOrNull())
                                binding.textMyId.text = "Me(${callbackData.sessionState!!.myPeerId.take(6)})"
                                binding.imageRemoteLogo.loadSvgImage(callbackData.sessionState!!.remotePeerMeta.icons?.firstOrNull())
                                binding.textRemoteId.text = "Rem.(${callbackData.sessionState!!.remotePeerId.take(6)})"
                                binding.textChainId.text = "Chain:${callbackData.sessionState!!.chainId?.toString()}"
                                binding.textAccounts.text =
                                    "Acc:${callbackData.sessionState!!.accounts?.joinToString { it.take(6) }}"
                            }
                        }
                        else -> {}
                    }
                }

                is SocketCallback -> {
                    when (callbackData) {
                        SocketCallback.SocketConnecting -> {
                            binding.textTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))

                        }
                        SocketCallback.SocketClosed -> {
                            binding.textTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        }
                        SocketCallback.SocketConnected -> {
                            binding.textTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                        }
                        SocketCallback.SocketDisconnected -> {
                            binding.textTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                        }
                        is SocketCallback.SocketMessage -> {

                        }
                    }
                }

                is RequestCallback -> {
                    when (callbackData) {
                        is RequestCallback.EthSignRequested -> {
                            SignFragment
                                    .create(messageId = callbackData.messageId,
                                            callbackData.data.address,
                                            message = callbackData.data.getRawMessage())
                                    .show(requireActivity().supportFragmentManager, fragmentTag)
                        }
                        is RequestCallback.EthSignTypedDataRequested -> {
                            //
                        }
                        is RequestCallback.EthSignResponse -> {
                            // NoOp
                        }

                        is RequestCallback.CustomRequested -> {

                        }
                        is RequestCallback.CustomResponse -> {

                        }

                        is RequestCallback.EthSignTxRequested -> {

                        }
                        is RequestCallback.EthSignTxResponse -> {

                        }
                        is RequestCallback.EthSendRawTxRequested -> {

                        }
                        is RequestCallback.EthSendRawTxResponse -> {

                        }
                        is RequestCallback.EthSendTxRequested -> {

                        }
                        is RequestCallback.EthSendTxResponse -> {

                        }

                        is RequestCallback.RequestRejected -> {
                            // NoOp
                        }
                    }
                }

                else -> {

                }
            }
        }
    }

    private fun observeSignResponseEvents() {
        viewModel.signResponses
                .onEach { (messageId, signature) ->
                    if (signature != null) {
                        wallet.approveRequest(messageId,
                                              result = signature,
                                              resultType = String::class.java)
                    } else {
                        wallet.rejectRequest(messageId, null)
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

}