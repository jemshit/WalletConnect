/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.adapter.gson

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import walletconnect.core.adapter.JsonAdapter
import walletconnect.core.util.Logger
import walletconnect.core.util.logger_impl.ConsoleLogger
import java.lang.reflect.Type

class GsonAdapter constructor(private val gson: Gson,
                              private val logger: Logger = ConsoleLogger())
    : JsonAdapter {

    private val LogTag = "GsonAdapter"

    override fun <IN> toJson(data: IN,
                             type: Type)
            : String? {
        return try {
            gson.toJson(data)!!
        } catch (error: Exception) {
            logger.error(LogTag, error.stackTraceToString())
            null
        }
    }

    override fun <OUT> fromJson(data: String,
                                type: Type)
            : OUT? {
        return try {
            gson.fromJson<OUT>(data, type)!!
        } catch (error: Exception) {
            logger.error(LogTag, error.stackTraceToString())
            null
        }
    }

    override fun getParametrizedType(rawType: Type,
                                     vararg typeArguments: Type)
            : Type {
        return TypeToken.getParameterized(rawType, *typeArguments).type
    }

}