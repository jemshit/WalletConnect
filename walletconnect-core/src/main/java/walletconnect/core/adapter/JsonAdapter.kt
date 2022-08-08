/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.adapter

import java.lang.reflect.Type

// ParametrizedType and Class<T> are also Type

interface JsonAdapter {

    /** Returns null on error */
    fun <IN> toJson(data: IN,
                    type: Type)
            : String?

    /** Returns null on error */
    fun <OUT> fromJson(data: String,
                       type: Type)
            : OUT?

    /** Store and reuse returned result */
    fun getParametrizedType(rawType: Type,
                            vararg typeArguments: Type)
            : Type

}