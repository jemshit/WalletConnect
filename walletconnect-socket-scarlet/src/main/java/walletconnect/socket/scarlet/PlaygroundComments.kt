package walletconnect.socket.scarlet

//for (event in service.observeAllEvents()) {
//    when (event) {
//// Lifecycle
//        is Event.OnLifecycle.StateChange<*> -> when (event.state) {
//            is Lifecycle.State.Started -> {
//                logger.log(LOG_WEB_SOCKET, "lifecycle: started", LogLevel.DEBUG)
//            }
//            is Lifecycle.State.Stopped -> {
//                logger.log(LOG_WEB_SOCKET, "lifecycle: stopped", LogLevel.DEBUG)
//            }
//            is Lifecycle.State.Destroyed -> {
//                logger.log(LOG_WEB_SOCKET, "lifecycle: destroyed", LogLevel.DEBUG)
//            }
//        }
//        is Event.OnLifecycle.Terminate -> {
//            logger.log(LOG_WEB_SOCKET, "lifecycle: terminated", LogLevel.DEBUG)
//        }
//
//// Connection
//        is Event.OnWebSocket.Event<*> -> when (event.event) {
//            is WebSocket.Event.OnConnectionOpened<*> -> {
//                logger.log(LOG_WEB_SOCKET, "connection: opened", LogLevel.DEBUG)
//            }
//            is WebSocket.Event.OnMessageReceived -> {
//                logger.log(LOG_WEB_SOCKET, "connection: on message", LogLevel.DEBUG)
//            }
//            is WebSocket.Event.OnConnectionClosing -> {
//                logger.log(LOG_WEB_SOCKET, "connection: closing", LogLevel.DEBUG)
//            }
//            is WebSocket.Event.OnConnectionClosed -> {
//                logger.log(LOG_WEB_SOCKET, "connection: closed", LogLevel.DEBUG)
//            }
//            is WebSocket.Event.OnConnectionFailed -> {
//                logger.log(LOG_WEB_SOCKET, "connection: failed", LogLevel.DEBUG)
//            }
//        }
//        is Event.OnWebSocket.Terminate -> {
//            logger.log(LOG_WEB_SOCKET, "connection: terminated", LogLevel.DEBUG)
//        }
//        is Event.OnRetry -> {
//            logger.log(LOG_WEB_SOCKET, "connection: retry", LogLevel.DEBUG)
//        }
//
//// State
//        is Event.OnStateChange<*> -> when (event.state) {
//            is State.WaitingToRetry -> {
//                logger.log(LOG_WEB_SOCKET, "state: waiting to retry", LogLevel.DEBUG)
//            }
//            is State.Connecting -> {
//                logger.log(LOG_WEB_SOCKET, "state: connecting", LogLevel.DEBUG)
//            }
//            is State.Connected -> {
//                logger.log(LOG_WEB_SOCKET, "state: connected", LogLevel.DEBUG)
//            }
//            is State.Disconnecting -> {
//                logger.log(LOG_WEB_SOCKET, "state: disconnecting", LogLevel.DEBUG)
//            }
//            is State.Disconnected -> {
//                logger.log(LOG_WEB_SOCKET, "state: disconnected", LogLevel.DEBUG)
//            }
//            is State.Destroyed -> {
//                logger.log(LOG_WEB_SOCKET, "state: destroyed", LogLevel.DEBUG)
//            }
//        }
//    }
//}

/**
//  command: start
//    lifecycle: started
//    state: connecting
//    connection: opened
//    state: connected

//  command: stop
//    lifecycle: stopped
//    state: disconnecting
//    connection: failed
//    connection: terminated
//    state: disconnected

//  command: destroyed
//    lifecycle: destroyed
//    lifecycle: terminated
//    state: destroyed
//    connection: failed
//    connection: terminated
// HAVE TO REPLACE LifecycleRegistry to reuse socket

// auto disconnect:
//      connection: failed
//      connection: terminated
//      state: waiting to retry
//      connection: retry
//      state: connecting
 */

// if connected, Lifecycle.Start does not change anything
// if waitingToRetry, Lifecycle.Start triggers retry
// if disconnected, Lifecycle.Start does not change anything
// if destroyed, Lifecycle.Start triggers connecting