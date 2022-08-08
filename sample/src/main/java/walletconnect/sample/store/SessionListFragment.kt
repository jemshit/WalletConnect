/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample.store

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.coroutineScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import walletconnect.core.session_state.SessionStore
import walletconnect.core.util.DispatcherProvider
import walletconnect.core.util.Logger
import walletconnect.sample.OneViewModel
import walletconnect.sample.R
import walletconnect.sample.databinding.SessionListFragmentBinding
import walletconnect.sample.impl.AndroidDispatcherProvider
import walletconnect.sample.impl.AndroidLogger
import walletconnect.store.file.FileSessionStore
import java.io.File

private const val SessionStoreName = "session_store_name"

class SessionListFragment : BottomSheetDialogFragment() {

    private var _binding: SessionListFragmentBinding? = null
    private val binding get() = _binding!!

    private val logger: Logger = AndroidLogger()
    private val dispatcherProvider: DispatcherProvider = AndroidDispatcherProvider()
    private var sessionStoreName: String? = null
    private val viewModel: OneViewModel by activityViewModels()

    companion object {
        @JvmStatic
        fun create(sessionStoreName: String) =
            SessionListFragment().apply {
                arguments = Bundle().apply {
                    putString(SessionStoreName, sessionStoreName)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            sessionStoreName = it.getString(SessionStoreName)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?)
            : View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = SessionListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionStore = createSessionStore(sessionStoreName!!)
        val margin = requireContext().resources.getDimensionPixelSize(R.dimen.margin)

        viewLifecycleOwner.lifecycle.coroutineScope.launch {
            sessionStore.getAll()?.forEach { sessionState ->
                binding.parentLayout.addView(
                        SessionCard(
                                requireContext(),
                                sessionState,
                                openSocket = {
                                    viewModel.openSocket(sessionStoreName!!, it)
                                    dismissNow()
                                },
                                delete = {
                                    viewModel.delete(sessionStoreName!!, it)
                                    dismissNow()
                                }
                        ),
                        LinearLayoutCompat.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                            setMargins(margin)
                        }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createSessionStore(name: String)
            : SessionStore {
        //val sharedPrefs = requireContext().applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

        /*return SharedPrefsSessionStore(
                sharedPrefs,
                dispatcherProvider,
                logger
        )*/
        return FileSessionStore(
                File(requireContext().filesDir, "$name.json"),
                dispatcherProvider,
                logger
        )
    }

}