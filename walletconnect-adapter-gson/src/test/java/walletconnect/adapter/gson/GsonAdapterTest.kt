/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.adapter.gson

import com.google.gson.GsonBuilder
import org.junit.Before
import walletconnect.adapter.gson.type_adapter.JsonRpcMethodTypeAdapter
import walletconnect.adapter.gson.type_adapter.SocketMessageTypeAdapter
import walletconnect.core.adapter.JsonAdapterTest
import walletconnect.core.session.model.json_rpc.JsonRpcMethod
import walletconnect.core.socket.model.SocketMessageType

class GsonAdapterTest : JsonAdapterTest() {

    @Before
    fun before() {
        val gson = GsonBuilder()
                .registerTypeAdapter(SocketMessageType::class.java, SocketMessageTypeAdapter())
                .registerTypeAdapter(JsonRpcMethod::class.java, JsonRpcMethodTypeAdapter())
                .create()
        jsonAdapter = GsonAdapter(gson)

        mapType = jsonAdapter.getParametrizedType(Map::class.java, String::class.java, Any::class.java)
    }

}