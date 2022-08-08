/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.socket.scarlet

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import walletconnect.core.CoroutineTestRule
import walletconnect.core.socket.model.FakeSocketMessage
import walletconnect.core.socket.model.SocketMessage
import walletconnect.core.util.DispatcherProvider
import walletconnect.core.util.Logger
import walletconnect.core.util.logger_impl.ConsoleLogger

class SocketManagerTest : ClientServerFactory() {

    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    override val logger: Logger = ConsoleLogger()

    override val dispatcherProvider: DispatcherProvider = coroutineTestRule.dispatcherProvider

    @Before
    override fun before() {
        super.before()
    }

    @After
    override fun after() {
        super.after()
    }

    @Test
    fun openCloseConcurrency() {
        val threadList = mutableListOf<Thread>()
        var exceptionFound = false

        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()

        // run Threads concurrently
        for (counter in 0 until 100) {
            val thread = Thread {
                try {
                    clientSocket.open({}, { _, _ -> })
                    clientSocket.close()
                } catch (error: Throwable) {
                    error.printStackTrace()
                    exceptionFound = true
                }
            }
            threadList.add(thread)
            thread.start()
        }

        // wait for Threads to finish
        threadList.forEach {
            try {
                it.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        assertFalse(exceptionFound)
    }

    @Test
    fun openIdempotent() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        clientSocket.open({}, { _, _ -> })
        blockUntilIsConnected()

        for (counter in 0 until 100) {
            clientSocket.open({}, { _, _ -> })
        }
        assertTrue(clientSocket.isConnected())

        clientSocket.close()
        blockUntilClientNotConnected()
    }

    @Test
    fun reconnectWhenProhibited() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        // should ignore
        clientSocket.reconnect()

        clientSocket.open({}, { _, _ -> })
        blockUntilIsConnected()
        // should happen nothing
        clientSocket.reconnect()
        assertTrue(clientSocket.isConnected())

        clientSocket.close()
        blockUntilClientNotConnected()
        // should ignore
        clientSocket.reconnect()
        assertFalse(clientSocket.isConnected())
    }

    @Test
    fun closeIdempotent() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        clientSocket.open({}, { _, _ -> })
        blockUntilIsConnected()

        val serverReceived = mutableListOf<SocketMessage>()
        serverSocket.subscribeToAll { serverReceived.add(it) }

        for (counter in 0 until 100) {
            clientSocket.close()
        }
        blockUntilClientNotConnected()

        // no message is published to server
        clientSocket.publish(FakeSocketMessage.PubEmptyPayload,
                             queueIfDisconnected = false)
        delay(1_000L)
        assertTrue(serverReceived.isEmpty())
    }

    @Test
    fun disconnect() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        val serverReceived = mutableListOf<SocketMessage>()
        serverSocket.subscribeToAll { serverReceived.add(it) }
        clientSocket.open({}, { _, _ -> })
        blockUntilIsConnected()

        // publish works
        clientSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        clientSocket.publish(FakeSocketMessage.SubEmptyPayload, queueIfDisconnected = false)
        waitUntilReceived(
                count = 2,
                items = serverReceived,
                assertions = {
                    assertTrue(serverReceived.contains(FakeSocketMessage.PubEmptyPayload))
                    assertTrue(serverReceived.contains(FakeSocketMessage.SubEmptyPayload))
                }
        )
        serverReceived.clear()

        // idempotent
        for (counter in 0 until 100) {
            clientSocket.disconnect()
        }
        blockUntilClientNotConnected()

        // no message is published to server
        clientSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        delay(1_000L)
        assertTrue(serverReceived.isEmpty())
    }

    @Test
    fun subscribeToAll() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        val clientReceived = mutableListOf<SocketMessage>()

        // should ignore
        clientSocket.subscribeToAll { clientReceived.add(it) }

        // should work
        clientSocket.open({}, { _, _ -> })
        clientSocket.subscribeToAll { clientReceived.add(it) }
        blockUntilIsConnected()
        serverSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        waitUntilReceived(1, clientReceived) {
            assertTrue(clientReceived[0] == FakeSocketMessage.PubEmptyPayload)
        }
        clientReceived.clear()

        clientSocket.close()
        blockUntilClientNotConnected()
        // should ignore
        clientSocket.subscribeToAll { clientReceived.add(it) }
    }

    @Test
    fun unsubscribeFromAll() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        val clientReceived = mutableListOf<SocketMessage>()
        // should ignore
        clientSocket.unsubscribeFromAll()

        // should work
        clientSocket.open({}, { _, _ -> })
        clientSocket.subscribeToAll { clientReceived.add(it) }
        blockUntilIsConnected()
        serverSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        waitUntilReceived(1, clientReceived) {
            assertTrue(clientReceived.contains(FakeSocketMessage.PubEmptyPayload))
        }
        clientReceived.clear()
        clientSocket.unsubscribeFromAll()
        serverSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        blockUntilIsConnected()
        delay(1_000)
        assertTrue(clientReceived.isEmpty())

        clientSocket.close()
        blockUntilClientNotConnected()
        // should ignore
        clientSocket.unsubscribeFromAll()
    }

    @Test
    fun publishWhenProhibited() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        val serverReceived = mutableListOf<SocketMessage>()
        serverSocket.subscribeToAll { serverReceived.add(it) }

        // should ignore
        clientSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        delay(1_000)
        assertTrue(serverReceived.isEmpty())

        clientSocket.open({}, { _, _ -> })
        blockUntilIsConnected()
        // should work
        clientSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        clientSocket.publish(FakeSocketMessage.SubEmptyPayload, queueIfDisconnected = false)
        waitUntilReceived(2, serverReceived) {
            assertTrue(serverReceived.contains(FakeSocketMessage.PubEmptyPayload))
            assertTrue(serverReceived.contains(FakeSocketMessage.SubEmptyPayload))
        }
        serverReceived.clear()

        clientSocket.close()
        blockUntilClientNotConnected()
        // should ignore
        clientSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        delay(1_000)
        assertTrue(serverReceived.isEmpty())
    }

    @Test
    fun publishAndSubscribe() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        clientSocket.open({}, { _, _ -> })
        blockUntilIsConnected()

        val serverReceived = mutableListOf<SocketMessage>()
        serverSocket.subscribeToAll { serverReceived.add(it) }

        clientSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        clientSocket.publish(FakeSocketMessage.SubEmptyPayload, queueIfDisconnected = false)

        waitUntilReceived(
                count = 2,
                items = serverReceived,
                assertions = {
                    assertTrue(serverReceived.contains(FakeSocketMessage.PubEmptyPayload))
                    assertTrue(serverReceived.contains(FakeSocketMessage.SubEmptyPayload))
                }
        )

        serverSocket.unsubscribeFromAll()
        serverReceived.clear()
        clientSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = false)
        clientSocket.publish(FakeSocketMessage.SubEmptyPayload, queueIfDisconnected = false)
        delay(1_000)
        assertTrue(serverReceived.isEmpty())

        clientSocket.close()
        blockUntilClientNotConnected()
    }

    @Test
    fun publishAndSubscribeQueued() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        clientSocket.open({}, { _, _ -> })
        val serverReceived = mutableListOf<SocketMessage>()
        serverSocket.subscribeToAll { serverReceived.add(it) }
        blockUntilIsConnected()

        clientSocket.disconnect()
        blockUntilClientNotConnected()
        clientSocket.publish(FakeSocketMessage.PubEmptyPayload, queueIfDisconnected = true)
        clientSocket.publish(FakeSocketMessage.SubEmptyPayload, queueIfDisconnected = true)

        clientSocket.reconnect()
        blockUntilIsConnected()
        waitUntilReceived(
                count = 2,
                items = serverReceived,
                assertions = {
                    assertTrue(serverReceived.contains(FakeSocketMessage.PubEmptyPayload))
                    assertTrue(serverReceived.contains(FakeSocketMessage.SubEmptyPayload))
                }
        )

        clientSocket.close()
        blockUntilClientNotConnected()
    }

    @Test
    fun reuseSocketAfterClosing() = runTest {
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        clientSocket.open({}, { _, _ -> })
        blockUntilIsConnected()

        clientSocket.close()
        serverSocket.close()
        blockUntilClientNotConnected()
        blockUntilServerNotConnected()

        // try to reuse
        serverSocket.open({}, { _, _ -> })
        mockWebServer.start()
        clientSocket.open({}, { _, _ -> })
        blockUntilIsConnected()
    }

}