/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.adapter.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Before
import walletconnect.adapter.moshi.type_adapter.JsonRpcMethodTypeAdapter
import walletconnect.adapter.moshi.type_adapter.SocketMessageTypeAdapter
import walletconnect.core.adapter.JsonAdapterTest

class MoshiAdapterTest : JsonAdapterTest() {

    @Before
    fun before() {
        val moshi = Moshi.Builder()
                .add(SocketMessageTypeAdapter())
                .add(JsonRpcMethodTypeAdapter())
                .addLast(KotlinJsonAdapterFactory())
                .build()
        jsonAdapter = MoshiAdapter(moshi)

        mapType = jsonAdapter.getParametrizedType(Map::class.java, String::class.java, Any::class.java)
    }

}