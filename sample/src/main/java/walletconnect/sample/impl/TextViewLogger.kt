/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample.impl

import android.annotation.SuppressLint
import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import walletconnect.core.util.Logger
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SetTextI18n")
class TextViewLogger(private val coroutineScope: CoroutineScope,
                     private val textView: TextView,
                     private val onConsoleUpdate: () -> Unit)
    : Logger {

    private val dateFormatter = SimpleDateFormat("mm:ss.SSS", Locale.US)
    private val logQueue = MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.SUSPEND
    )

    init {
        logQueue
                .onEach {
                    textView.text = textView.text.toString() + "\n" + it
                    onConsoleUpdate()
                }
                .catch {
                    Log.e("TextViewLogger", it.stackTraceToString())
                }
                .launchIn(coroutineScope)
    }

    override fun debug(tag: String, parameters: String?) {
        val time = dateFormatter.format(Date(System.currentTimeMillis()))
        logQueue.tryEmit("$time: Debug | $tag | $parameters\n")
    }

    override fun info(tag: String, parameters: String?) {
        val time = dateFormatter.format(Date(System.currentTimeMillis()))
        logQueue.tryEmit("$time: Info  | $tag | $parameters\n")
    }

    override fun warning(tag: String, parameters: String?) {
        val time = dateFormatter.format(Date(System.currentTimeMillis()))
        logQueue.tryEmit("$time: WARN  | $tag | $parameters\n")
    }

    override fun error(tag: String, parameters: String?) {
        val time = dateFormatter.format(Date(System.currentTimeMillis()))
        logQueue.tryEmit("$time: ERROR | $tag | $parameters\n")
    }

}