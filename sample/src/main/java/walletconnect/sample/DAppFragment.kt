/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import walletconnect.core.DApp
import walletconnect.core.cryptography.toHex
import walletconnect.core.requests.eth.EthTransaction
import walletconnect.core.requests.eth.SignType.Companion.toMethod
import walletconnect.core.session.callback.*
import walletconnect.core.session.model.InitialSessionState
import walletconnect.core.session.model.json_rpc.EthRpcMethod
import walletconnect.core.session_state.model.PeerMeta
import walletconnect.requests.CustomRpcMethods
import walletconnect.requests.wallet.SwitchChain
import walletconnect.sample.bnb_provider.BnbViewModel
import walletconnect.sample.databinding.DappFragmentBinding
import walletconnect.sample.sign.SignFragment
import walletconnect.sample.store.SessionListFragment
import java.math.BigInteger
import java.util.*

class DAppFragment : BaseFragment() {

    private var _binding: DappFragmentBinding? = null
    private val binding get() = _binding!!

    override val fragmentTag: String = javaClass.simpleName
    private lateinit var dApp: DApp
    override val initialSessionState by lazy {
        InitialSessionState(
                connectionParams,
                myPeerId = UUID.randomUUID().toString(),
                myPeerMeta = PeerMeta(
                        name = "DApp",
                        url = "https://dapp.com",
                        description = "DApp Description",
                        icons = listOf("https://www.dapp.com/img/Icon_Logotype_2.png")
                )
        )
    }
    private val bnbVM: BnbViewModel by activityViewModels()

    // region lifecycle
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?)
            : View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = DappFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dApp = createDApp(sessionStoreName = fragmentTag)
        sessionLifecycle = dApp

        setClickActions()
        observeSignRequestEvents()
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
        // [super.onDestroyView] calls [dApp.close], which expects caller to trigger [SessionCallback]
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
            dApp.openSocket(initialSessionState,
                            callback = ::onSessionCallback,
                            onOpen = null)
        }
        binding.buttonClose.setOnClickListener {
            dApp.close(deleteLocal = false,
                       deleteRemote = false)
        }
        binding.buttonDelete.setOnClickListener {
            dApp.close(deleteLocal = true,
                       deleteRemote = true)
        }

        binding.buttonSendSessionRequest.setOnClickListener {
            dApp.sendSessionRequest(null)
        }
        binding.buttonSwitchChain.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(dispatcherProvider.io()) {
                if (!approvedAddress.isNullOrBlank()) {
                    dApp.sendRequest(CustomRpcMethods.SwitchEthChain,
                                     data = listOf(SwitchChain("0x" + 56.toHex())),
                                     itemType = SwitchChain::class.java)
                }
            }
        }

        binding.buttonSendSignRequest.setOnClickListener {
            if (!approvedAddress.isNullOrBlank()) {
                SignFragment
                        .create(messageId = null, approvedAddress!!, message = null)
                        .show(requireActivity().supportFragmentManager, fragmentTag)
            }
        }
        binding.buttonSignTx.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(dispatcherProvider.io()) {
                if (!approvedAddress.isNullOrBlank()) {
                    dApp.sendRequest(EthRpcMethod.SignTransaction,
                                     data = listOf(createTransaction()),
                                     itemType = EthTransaction::class.java)
                }
            }
        }
        binding.buttonSendTx.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(dispatcherProvider.io()) {
                if (!approvedAddress.isNullOrBlank()) {
                    dApp.sendRequest(EthRpcMethod.SendTransaction,
                                     data = listOf(createTransaction()),
                                     itemType = EthTransaction::class.java)
                }
            }
        }
        binding.buttonSendTokenTx.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(dispatcherProvider.io()) {
                if (!approvedAddress.isNullOrBlank()) {
                    if (bnbVM.gas.value.isBlank()) {
                        bnbVM.estimateGas(createTokenTransaction("", ""),
                                          isTestNet = approvedChainId!! == 97)
                        withContext(dispatcherProvider.ui()) {
                            Toast.makeText(requireContext(), "Fetching GAS. Try Again", LENGTH_SHORT).show()
                        }

                    } else if (bnbVM.gasPrice.value.isBlank()) {
                        bnbVM.getGasPrice(isTestNet = approvedChainId!! == 97)
                        withContext(dispatcherProvider.ui()) {
                            Toast.makeText(requireContext(), "Fetching GAS PRICE. Try Again", LENGTH_SHORT).show()
                        }

                    } else {
                        dApp.sendRequest(EthRpcMethod.SendTransaction,
                                         data = listOf(createTokenTransaction(bnbVM.gas.value,
                                                                              bnbVM.gasPrice.value)),
                                         itemType = EthTransaction::class.java)
                    }
                }
            }
        }
        binding.buttonDeepLink.setOnClickListener {
            triggerDeepLink()
        }

        binding.imageSessionList.setOnClickListener {
            SessionListFragment
                    .create(fragmentTag)
                    .show(requireActivity().supportFragmentManager, fragmentTag)
        }
    }

    private fun triggerDeepLink() {
        val currentSessionState = dApp.getInitialSessionState() ?: return
        try {
            val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentSessionState.connectionParams.toUri()))
            startActivity(myIntent)
        } catch (_: ActivityNotFoundException) {
            Toast
                    .makeText(requireContext(),
                              "No application can handle this request. Please install a wallet app",
                              Toast.LENGTH_LONG)
                    .show()
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
                                approvedAddress = null
                                approvedChainId = null
                            } else {
                                binding.imageMyLogo.loadSvgImage(callbackData.sessionState!!.myPeerMeta.icons?.firstOrNull())
                                binding.textMyId.text = "Me(${callbackData.sessionState!!.myPeerId.take(6)})"
                                binding.imageRemoteLogo.loadSvgImage(callbackData.sessionState!!.remotePeerMeta.icons?.firstOrNull())
                                binding.textRemoteId.text = "Rem.(${callbackData.sessionState!!.remotePeerId.take(6)})"
                                binding.textChainId.text = "Chain:${callbackData.sessionState!!.chainId?.toString()}"
                                binding.textAccounts.text =
                                    "Acc:${callbackData.sessionState!!.accounts?.joinToString { it.take(6) }}"
                                approvedAddress = callbackData.sessionState!!.accounts?.firstOrNull()
                                approvedChainId = callbackData.sessionState!!.chainId
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
                            //triggerDeepLink()
                        }
                        is RequestCallback.EthSignTypedDataRequested -> {
                            //triggerDeepLink()
                        }
                        is RequestCallback.EthSignResponse -> {
                            BottomSheetDialog(requireContext()).apply {
                                setContentView(R.layout.text_item)
                                val textView = findViewById<TextView>(R.id.textContent)
                                textView?.text = "Signature:\n" + callbackData.signature
                                show()
                            }
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
                            BottomSheetDialog(requireContext()).apply {
                                setContentView(R.layout.text_item)
                                val textView = findViewById<TextView>(R.id.textContent)
                                textView?.text = "Rejected:\n" + callbackData.error.message
                                show()
                            }
                        }
                    }
                }
                is FailureCallback -> {

                }
            }
        }
    }

    // region Sign/Transaction
    private fun observeSignRequestEvents() {
        viewModel.signRequests
                .onEach { ethSign ->
                    try {
                        ethSign.validate()
                        dApp.sendRequest(
                                method = ethSign.type.toMethod(),
                                data = ethSign.toList(),
                                itemType = String::class.java
                        )
                    } catch (error: Exception) {
                        Toast.makeText(requireContext(), error.message, LENGTH_LONG).show()
                    }
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun createTransaction()
            : EthTransaction {
        return EthTransaction(
                from = approvedAddress!!,
                to = "0x621261D26847B423Df639848Fb53530025a008e8",
                data = "",
                chainId = if (approvedChainId == null) null else "0x" + approvedChainId!!.toHex(),

                gas = null,
                gasPrice = null,
                gasLimit = null,
                maxFeePerGas = null,
                maxPriorityFeePerGas = null,

                value = "0x" + BigInteger("10000000000000000").toString(16), // 1_000_000_000_000_000_000L.toHex(),
                nonce = null
        )
    }

    private fun createTokenTransaction(gas: String,
                                       gasPrice: String)
            : EthTransaction {
        // bnb=56, bnb_test=97
        val chainId = "0x" + approvedChainId!!.toHex()
        // on bnb_test
        val smartContractAddressMain = "0xb465f3cb6Aba6eE375E12918387DE1eaC2301B05"
        val smartContractAddress = "0xcD0FC5078Befa4701C3692F4268641A468DEB8d5"
        // keccak256(transactionMethod).take(4 bytes)
        val smartContractTransactionMethod = "a9059cbb"
        // must be 32 bytes
        val to = "621261D26847B423Df639848Fb53530025a008e8".padStart(64, '0')
        // 3 decimal; must be 32 bytes; 1_000L.toHex()
        val amount = BigInteger("1000").toString(16).padStart(64, '0')

        return EthTransaction(
                from = approvedAddress!!,
                to = if (approvedChainId!! == 56) smartContractAddressMain else smartContractAddress,
                data = "0x$smartContractTransactionMethod$to$amount",
                chainId = chainId,

                gas = gas, // can't auto calculate for smart contract
                gasPrice = gasPrice, // can auto calculate
                gasLimit = null,
                maxFeePerGas = null,
                maxPriorityFeePerGas = null,

                value = "", // this is in 'data' field now
                nonce = null // ignored by metamask
        )
    }
    // endregion
}