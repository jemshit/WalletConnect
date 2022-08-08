/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample.bnb_provider

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import walletconnect.adapter.gson.type_adapter.JsonRpcMethodTypeAdapter
import walletconnect.core.requests.eth.EthTransaction
import walletconnect.core.session.model.json_rpc.CustomRpcMethod
import walletconnect.core.session.model.json_rpc.JsonRpcMethod
import walletconnect.core.session.model.json_rpc.JsonRpcRequest
import java.util.*
import java.util.concurrent.TimeUnit

// https://ethereum.org/en/developers/docs/apis/json-rpc/
class BnbViewModel : ViewModel() {

    private val api: BnbApi by lazy { createApi() }
    private val apiTest: BnbApi by lazy { createTestApi() }
    val gas: MutableStateFlow<String> = MutableStateFlow("")
    val gasPrice: MutableStateFlow<String> = MutableStateFlow("")
    val nonce: MutableStateFlow<String> = MutableStateFlow("")

    fun estimateGas(ethTransaction: EthTransaction,
                    isTestNet: Boolean) {
        // NOTE: Balance of 'from' must be non-empty, otherwise returns jsonRpcError

        viewModelScope.launch {
            val outcome = try {
                getApi(isTestNet).estimateGas(JsonRpcRequest(
                        id = System.currentTimeMillis() * 1000 + Random().nextInt(999),
                        method = CustomRpcMethod("eth_estimateGas"),
                        params = listOf(ethTransaction)
                ))
            } catch (error: Throwable) {
                Log.e("BnbViewModel", error.stackTraceToString())
                null
            }

            // if jsonRpcError, result is null
            if (outcome != null && outcome.result != null) {
                gas.tryEmit(outcome.result)
            }
        }
    }

    fun getGasPrice(isTestNet: Boolean) {
        viewModelScope.launch {
            val outcome = try {
                getApi(isTestNet).getGasPrice(JsonRpcRequest(
                        id = System.currentTimeMillis() * 1000 + Random().nextInt(999),
                        method = CustomRpcMethod("eth_gasPrice"),
                        params = listOf()
                ))
            } catch (error: Throwable) {
                Log.e("BnbViewModel", error.stackTraceToString())
                null
            }

            // if jsonRpcError, result is null
            if (outcome != null && outcome.result != null) {
                gasPrice.tryEmit(outcome.result)
            }
        }
    }

    fun getNextNonce(from: String,
                     isTestNet: Boolean) {
        viewModelScope.launch {
            val outcome = try {
                getApi(isTestNet).getNextNonce(JsonRpcRequest(
                        id = System.currentTimeMillis() * 1000 + Random().nextInt(999),
                        method = CustomRpcMethod("eth_getTransactionCount"),
                        params = listOf(from, "pending")
                ))
            } catch (error: Throwable) {
                Log.e("BnbViewModel", error.stackTraceToString())
                null
            }

            // if jsonRpcError, result is null
            if (outcome != null && outcome.result != null) {
                nonce.tryEmit(outcome.result)
            }
        }
    }

    private fun getApi(isTestNet: Boolean)
            : BnbApi {
        return if (isTestNet) apiTest else api
    }

    private fun createApi(): BnbApi {
        // limit: 8k/5min.
        val gson = GsonBuilder()
                .registerTypeAdapter(JsonRpcMethod::class.java, JsonRpcMethodTypeAdapter())
                .create()

        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addNetworkInterceptor(interceptor)
                .cache(null)
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl("https://bsc-dataseed1.binance.org/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build()

        return retrofit.create(BnbApi::class.java)
    }

    private fun createTestApi(): BnbApi {
        // limit: 8k/5min.
        val gson = GsonBuilder()
                .registerTypeAdapter(JsonRpcMethod::class.java, JsonRpcMethodTypeAdapter())
                .create()

        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addNetworkInterceptor(interceptor)
                .cache(null)
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl("https://data-seed-prebsc-1-s1.binance.org:8545/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build()

        return retrofit.create(BnbApi::class.java)
    }

}