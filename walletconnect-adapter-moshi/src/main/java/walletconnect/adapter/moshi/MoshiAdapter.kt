/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.adapter.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import walletconnect.core.adapter.JsonAdapter
import walletconnect.core.util.Logger
import walletconnect.core.util.logger_impl.ConsoleLogger
import java.lang.reflect.Type

class MoshiAdapter constructor(private val moshi: Moshi,
                               private val logger: Logger = ConsoleLogger())
    : JsonAdapter {

    private val LogTag = "MoshiAdapter"
    private val adapterCache: MutableMap<Type, com.squareup.moshi.JsonAdapter<*>> = mutableMapOf()

    override fun <IN> toJson(data: IN,
                             type: Type)
            : String? {
        return try {
            val existingAdapter = adapterCache[type] as? com.squareup.moshi.JsonAdapter<IN>
            if (existingAdapter != null) {
                existingAdapter.toJson(data)
            } else {
                var newAdapter = moshi.adapter<IN>(type)
                newAdapter = newAdapter.serializeNulls()
                adapterCache[type] = newAdapter
                newAdapter.toJson(data)
            }
        } catch (error: Exception) {
            logger.error(LogTag, error.stackTraceToString())
            null
        }
    }

    override fun <OUT> fromJson(data: String,
                                type: Type)
            : OUT? {
        return try {
            val existingAdapter = adapterCache[type] as? com.squareup.moshi.JsonAdapter<OUT>
            if (existingAdapter != null) {
                existingAdapter.fromJson(data)
            } else {
                val newAdapter = moshi.adapter<OUT>(type)
                adapterCache[type] = newAdapter
                newAdapter.fromJson(data)
            }
        } catch (error: Exception) {
            logger.error(LogTag, error.stackTraceToString())
            null
        }
    }

    override fun getParametrizedType(rawType: Type,
                                     vararg typeArguments: Type)
            : Type {
        return Types.newParameterizedType(rawType, *typeArguments)
    }

}