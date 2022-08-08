/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.adapter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import walletconnect.core.session.model.SessionRequest
import walletconnect.core.session.model.json_rpc.JsonRpcError
import walletconnect.core.session.model.json_rpc.JsonRpcRequest
import walletconnect.core.session.model.json_rpc.JsonRpcResponse
import walletconnect.core.session.model.json_rpc.SessionRpcMethod
import walletconnect.core.session_state.model.*
import walletconnect.core.socket.model.FakeSocketMessage
import walletconnect.core.socket.model.SocketMessage
import java.lang.reflect.Type

abstract class JsonAdapterTest {

    protected lateinit var jsonAdapter: JsonAdapter

    /** Map::class.java, String::class.java, Any::class.java */
    protected lateinit var mapType: Type

    @Test
    fun peerMeta() {
        val inputs = listOf(FakePeerMeta.Normal,
                            FakePeerMeta.EmptyName,
                            FakePeerMeta.NullDesc,
                            FakePeerMeta.EmptyIcons,
                            FakePeerMeta.NullIcons)

        inputs.forEach { expectedModel ->
            val json = jsonAdapter.toJson(expectedModel, PeerMeta::class.java)
            val actual: PeerMeta? = jsonAdapter.fromJson(json!!, PeerMeta::class.java)
            assertEquals(expectedModel, actual!!)
        }
    }

    @Test
    fun connectionParams() {
        val inputs = listOf(FakeConnectionParams.Normal,
                            FakeConnectionParams.EmptyTopic,
                            FakeConnectionParams.InvalidBridgeUrl,
                            FakeConnectionParams.NonHexSymmetricKey)

        inputs.forEach { expectedModel ->
            val json = jsonAdapter.toJson(expectedModel, ConnectionParams::class.java)
            val actual: ConnectionParams? = jsonAdapter.fromJson(json!!, ConnectionParams::class.java)
            assertEquals(expectedModel, actual!!)
        }
    }

    @Test
    fun sessionStates() {
        FakeSessionStates.AllFakes.forEach { expectedModel ->
            val json = jsonAdapter.toJson(expectedModel, SessionState::class.java)
            val actual: SessionState? = jsonAdapter.fromJson(json!!, SessionState::class.java)
            assertEquals(expectedModel, actual!!)
        }
    }

    @Test
    fun socketMessages() {
        FakeSocketMessage.AllFakes.forEach { expectedModel ->
            val json = jsonAdapter.toJson(expectedModel, SocketMessage::class.java)
            val actual: SocketMessage? = jsonAdapter.fromJson(json!!, SocketMessage::class.java)
            assertEquals(expectedModel, actual!!)
        }

        // payload
        FakeSocketMessage.AllFakes.forEach { expectedModel ->
            val payloadJson = expectedModel.payload
            if (payloadJson.isNotBlank()) {
                val payloadAsMap: Map<String, Any?> = jsonAdapter.fromJson(payloadJson, mapType)!!

                if (payloadAsMap["method"] != null && payloadAsMap["params"] != null) {
                    if (payloadAsMap["method"] == "wc_sessionRequest") {
                        val jsonRpcRequestType = jsonAdapter.getParametrizedType(JsonRpcRequest::class.java,
                                                                                 SessionRequest::class.java)
                        val payload: JsonRpcRequest<SessionRequest> = jsonAdapter.fromJson(payloadJson,
                                                                                           jsonRpcRequestType)!!
                        assertEquals("2.0", payload.jsonrpc)
                        assertTrue(payload.id > 0L)
                        assertEquals(SessionRpcMethod.Request, payload.method)
                    }

                } else if (payloadAsMap["result"] != null) {
                    val jsonRpcResponseType = jsonAdapter.getParametrizedType(JsonRpcResponse::class.java,
                                                                              Any::class.java)
                    val payload: JsonRpcResponse<Any> = jsonAdapter.fromJson(payloadJson, jsonRpcResponseType)!!

                    assertEquals("2.0", payload.jsonrpc)
                    assertTrue(payload.id > 0L)

                } else if (payloadAsMap["error"] != null) {
                    val payload: JsonRpcError = jsonAdapter.fromJson(payloadJson, JsonRpcError::class.java)!!

                    assertEquals("2.0", payload.jsonrpc)
                    assertTrue(payload.id > 0L)
                }
            }
        }
    }

    @Test
    fun socketMessage_sessionRequest() {
        val jsonRpcRequestType = jsonAdapter.getParametrizedType(JsonRpcRequest::class.java,
                                                                 SessionRequest::class.java)
        val payload: JsonRpcRequest<SessionRequest> = jsonAdapter.fromJson(
                FakeSocketMessage.PubJsonRpcRequestSessionRequestParamsList.payload,
                jsonRpcRequestType
        )!!

        assertEquals(SessionRpcMethod.Request, payload.method)
        assertEquals(3, payload.params.size)
        assertEquals(3, payload.params.size)
        assertEquals("someId1", payload.params[0].peerId)
        assertEquals("peerName1", payload.params[0].peerMeta.name)
        assertEquals("https://peerUrl1.com", payload.params[0].peerMeta.url)
        assertEquals("can", payload.params[0].peerMeta.icons!![0])
        assertEquals(null, payload.params[0].chainId)

        assertEquals(null, payload.params[1].peerMeta.description)
        assertTrue(payload.params[1].peerMeta.icons!!.isEmpty())
        assertEquals(2, payload.params[1].chainId)
    }

    @Test
    fun socketMessage_sessionRequest2() {
        val jsonRpcRequestType = jsonAdapter.getParametrizedType(JsonRpcRequest::class.java,
                                                                 SessionRequest::class.java)
        val payload: JsonRpcRequest<SessionRequest> = jsonAdapter.fromJson(
                FakeSocketMessage.PubJsonRpcRequestSessionRequestEmptyParams.payload,
                jsonRpcRequestType
        )!!

        assertEquals(SessionRpcMethod.Request, payload.method)
        assertEquals(0, payload.params.size)
    }

    @Test
    fun socketMessage_sessionResponseInt() {
        val jsonRpcResponseType = jsonAdapter.getParametrizedType(JsonRpcResponse::class.java,
                                                                  Integer::class.java)
        val payload: JsonRpcResponse<Int> = jsonAdapter.fromJson(
                FakeSocketMessage.PubJsonRpcResponseInt.payload,
                jsonRpcResponseType
        )!!

        assertEquals(100, payload.result)
    }

    @Test
    fun socketMessage_sessionResponseMap() {
        val jsonRpcResponseType = jsonAdapter.getParametrizedType(JsonRpcResponse::class.java,
                                                                  jsonAdapter.getParametrizedType(
                                                                          Map::class.java,
                                                                          String::class.java,
                                                                          String::class.java
                                                                  ))
        val payload: JsonRpcResponse<Map<String, String>> = jsonAdapter.fromJson(
                FakeSocketMessage.PubJsonRpcResponseStringMap.payload,
                jsonRpcResponseType
        )!!

        assertEquals("value", payload.result["key"])
        assertEquals("value2", payload.result["key2"])
    }

}