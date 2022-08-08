/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.socket.model

// increasing id
object FakeSocketMessage {

    val PubEmptyPayload = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = ""
    )
    val SubEmptyPayload = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Sub,
            payload = ""
    )

    val PubJsonRpcError = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = "{\n" +
                      "  \"jsonrpc\": \"2.0\",\n" +
                      "  \"id\": 1,\n" +
                      "  \"error\": {\n" +
                      "    \"code\": -101,\n" +
                      "    \"message\": \"error text\"\n" +
                      "  }\n" +
                      "}"
    )

    val PubJsonRpcResponseInt = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = "{\n" +
                      "  \"jsonrpc\": \"2.0\",\n" +
                      "  \"id\": 2,\n" +
                      "  \"result\": 100\n" +
                      "}"
    )
    val PubJsonRpcResponseIntList = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = "{\n" +
                      "  \"jsonrpc\": \"2.0\",\n" +
                      "  \"id\": 3,\n" +
                      "  \"result\": [100, 101]\n" +
                      "}"
    )
    val PubJsonRpcResponseStringMap = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = "{\n" +
                      "  \"jsonrpc\": \"2.0\",\n" +
                      "  \"id\": 4,\n" +
                      "  \"result\": {\n" +
                      "    \"key\": \"value\",\n" +
                      "    \"key2\": \"value2\"\n" +
                      "  }\n" +
                      "}"
    )

    val PubJsonRpcRequestSessionRequestEmptyParams = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = "{\n" +
                      "  \"jsonrpc\": \"2.0\",\n" +
                      "  \"id\": 5,\n" +
                      "  \"method\": \"wc_sessionRequest\",\n" +
                      "  \"params\": []\n" +
                      "}"
    )
    val PubJsonRpcRequestSessionRequestParamsList = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = "{\n" +
                      "  \"jsonrpc\": \"2.0\",\n" +
                      "  \"id\": 6,\n" +
                      "  \"method\": \"wc_sessionRequest\",\n" +
                      "  \"params\": [\n" +
                      "    {\n" +
                      "      \"peerId\": \"someId1\",\n" +
                      "      \"peerMeta\": {\n" +
                      "        \"name\": \"peerName1\",\n" +
                      "        \"url\": \"https://peerUrl1.com\",\n" +
                      "        \"description\": \"can be null\",\n" +
                      "        \"icons\": [\n" +
                      "          \"can\",\n" +
                      "          \"be\",\n" +
                      "          \"empty\",\n" +
                      "          \"null\"\n" +
                      "        ]\n" +
                      "      },\n" +
                      "      \"chainId\": null\n" +
                      "    },\n" +
                      "    {\n" +
                      "      \"peerId\": \"someId2\",\n" +
                      "      \"peerMeta\": {\n" +
                      "        \"name\": \"peerName2\",\n" +
                      "        \"url\": \"https://peerUrl2.com\",\n" +
                      "        \"description\": null,\n" +
                      "        \"icons\": [\n" +
                      "          \n" +
                      "        ]\n" +
                      "      },\n" +
                      "      \"chainId\": 2\n" +
                      "    },\n" +
                      "    {\n" +
                      "      \"peerId\": \"someId3\",\n" +
                      "      \"peerMeta\": {\n" +
                      "        \"name\": \"peerName3\",\n" +
                      "        \"url\": \"https://peerUrl3.com\",\n" +
                      "        \"description\": null,\n" +
                      "        \"icons\": null\n" +
                      "      },\n" +
                      "      \"chainId\": 3\n" +
                      "    }\n" +
                      "  ]\n" +
                      "}"
    )

    val PubJsonRpcRequestSessionUpdateParamsList = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = "{\n" +
                      "  \"jsonrpc\": \"2.0\",\n" +
                      "  \"id\": 7,\n" +
                      "  \"method\": \"wc_sessionUpdate\",\n" +
                      "  \"params\": [\n" +
                      "    {\n" +
                      "      \"approved\": true,\n" +
                      "      \"chainId\": 123,\n" +
                      "      \"accounts\": null\n" +
                      "    },\n" +
                      "    {\n" +
                      "      \"approved\": false,\n" +
                      "      \"chainId\": null,\n" +
                      "      \"accounts\": []\n" +
                      "    },\n" +
                      "    {\n" +
                      "      \"approved\": false,\n" +
                      "      \"chainId\": 124,\n" +
                      "      \"accounts\": [\n" +
                      "        \"account1\",\n" +
                      "        \"account2\"\n" +
                      "      ]\n" +
                      "    }\n" +
                      "  ]\n" +
                      "}"
    )

    val PubJsonRpcRequestEthSign = SocketMessage(
            topic = "fcfecccf-4930-46b9-9f42-5648579c1658",
            SocketMessageType.Pub,
            payload = "{\n" +
                      "  \"jsonrpc\": \"2.0\",\n" +
                      "  \"id\": 8,\n" +
                      "  \"method\": \"eth_sign\",\n" +
                      "  \"params\": [\n" +
                      "    \"sign1\", \n" +
                      "    \"sign2\"\n" +
                      "  ]\n" +
                      "}"
    )

    val AllFakes: List<SocketMessage> = listOf(
            PubEmptyPayload,
            SubEmptyPayload,
            PubJsonRpcError,
            PubJsonRpcResponseInt,
            PubJsonRpcResponseIntList,
            PubJsonRpcResponseStringMap,
            PubJsonRpcRequestSessionRequestEmptyParams,
            PubJsonRpcRequestSessionRequestParamsList,
            PubJsonRpcRequestSessionUpdateParamsList,
            PubJsonRpcRequestEthSign
    )

}
