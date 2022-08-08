/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample.sign

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import walletconnect.core.cryptography.toHex
import walletconnect.core.requests.eth.SignType
import walletconnect.sample.databinding.SignFragmentBinding
import walletconnect.sample.OneViewModel

private const val Address = "address"
private const val Message = "message"
private const val MessageId = "message_id"

class SignFragment : BottomSheetDialogFragment() {

    private var _binding: SignFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OneViewModel by activityViewModels()

    private var address: String = ""
    private var message: String? = null
    private var messageId: Long = -1L

    companion object {
        @JvmStatic
        fun create(messageId: Long?,
                   address: String,
                   message: String?) =
            SignFragment().apply {
                arguments = Bundle().apply {
                    putString(Address, address)
                    putString(Message, message)
                    putLong(MessageId, messageId ?: 1L)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            address = it.getString(Address, "")!!
            message = it.getString(Message)
            messageId = it.getLong(MessageId)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?)
            : View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = SignFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!message.isNullOrBlank()) {
            // wallet
            binding.inputAddress.isEnabled = false
            binding.inputMessage.isEnabled = false

            binding.inputAddress.setText(address)
            binding.inputMessage.setText(message)

            binding.buttonSignRequest.visibility = View.GONE
            binding.buttonPersonalSignRequest.visibility = View.GONE
            binding.buttonApproveRequest.visibility = View.VISIBLE
            binding.buttonRejectRequest.visibility = View.VISIBLE
            binding.buttonApproveRequest.setOnClickListener {
                viewModel.approveSignRequest(messageId,
                                             "0x4e1ce8ea60bc6dfd4068a35462612495850cb645a1c9f475eb969bff21d0b0fb414112aaf13f01dd18a3527cb648cdd51b618ae49d4999112c33f86b7b26e9731b")
                dismissNow()
            }
            binding.buttonRejectRequest.setOnClickListener {
                viewModel.rejectSignRequest(messageId)
                dismissNow()
            }

        } else {
            // dApp
            binding.inputAddress.isEnabled = true
            binding.inputMessage.isEnabled = true

            binding.inputAddress.setText(address)

            binding.buttonApproveRequest.visibility = View.GONE
            binding.buttonRejectRequest.visibility = View.GONE
            binding.buttonSignRequest.visibility = View.VISIBLE
            binding.buttonPersonalSignRequest.visibility = View.VISIBLE
            binding.buttonSignRequest.setOnClickListener {
                val address = binding.inputAddress.text.toString()
                val message = binding.inputMessage.text.toString()
                if (message.isNotBlank()) {
                    viewModel.signRequest(address,
                                          message, // raw
                                          SignType.Sign)
                    dismissNow()
                }
            }
            binding.buttonPersonalSignRequest.setOnClickListener {
                val address = binding.inputAddress.text.toString()
                val message = binding.inputMessage.text.toString()
                if (message.isNotBlank()) {
                    viewModel.signRequest(address,
                                          "0x" + message.toHex(),
                                          SignType.PersonalSign)
                    dismissNow()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}