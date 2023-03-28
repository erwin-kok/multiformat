// Copyright (c) 2022 Erwin Kok. BSD-3-Clause license. See LICENSE file for more details.
package org.erwinkok.multiformat.multistream

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.erwinkok.result.Err
import org.erwinkok.result.Error
import org.erwinkok.result.Errors
import org.erwinkok.result.Ok
import org.erwinkok.result.Result
import org.erwinkok.result.getOrElse
import org.erwinkok.result.map
import org.erwinkok.result.onFailure
import org.erwinkok.result.onSuccess
import java.util.Random

class MultistreamMuxer<T : Utf8Connection> {
    private val handlerLock = ReentrantLock()
    private val handlers = mutableListOf<ProtocolHandlerInfo<T>>()

    suspend fun list(connection: T): Result<List<String>> {
        handshake(connection)
            .onFailure { return Err(it) }
        connection.writeUtf8(LS)
            .onFailure { return Err(it) }
        val result = mutableListOf<String>()
        val token = readNextToken(connection)
            .getOrElse {
                if (it == Errors.EndOfStream) {
                    return Ok(result)
                }
                return Err(it)
            }
        result.addAll(token.split('\n').map { it.trim() })
        return Ok(result)
    }

    suspend fun negotiate(connection: T): Result<ProtocolHandlerInfo<T>> {
        connection.writeUtf8(PROTOCOL_ID)
            .onFailure { return Err(it) }
        val token = readNextToken(connection)
            .getOrElse { return Err(it) }
        if (token != PROTOCOL_ID) {
            connection.close()
            return Err(ErrIncorrectVersion)
        }
        while (true) {
            val nextToken = readNextToken(connection)
                .getOrElse {
                    return Err(it)
                }
            if (nextToken == LS) {
                sendList(connection)
                    .onFailure { return Err(it) }
            } else {
                val handler = findHandler(nextToken)
                if (handler == null) {
                    connection.writeUtf8(NA)
                        .onFailure { return Err(it) }
                } else {
                    connection.writeUtf8(nextToken)
                        .onFailure { return Err(it) }
                    return Ok(handler)
                }
            }
        }
    }

    suspend fun handle(scope: CoroutineScope, connection: T): Result<Unit> {
        return negotiate(connection)
            .map { negotiateResult ->
                val handler = negotiateResult.handler ?: return Err("No Handler registered for ${negotiateResult.protocol}")
                scope.launch {
                    handler(negotiateResult.protocol, connection)
                }
            }
    }

    fun addHandler(protocol: String) {
        addHandlerWithFunc(protocol, { anObject: String -> protocol == anObject }, null)
    }

    fun addHandler(protocol: String, handler: suspend (protocol: String, stream: T) -> Result<Unit>) {
        addHandlerWithFunc(protocol, { anObject: String -> protocol == anObject }, handler)
    }

    fun addHandlerWithFunc(protocol: String, match: (String) -> Boolean, handler: (suspend (protocol: String, stream: T) -> Result<Unit>)?) {
        handlerLock.withLock {
            handlers.add(ProtocolHandlerInfo(match, protocol, handler))
        }
    }

    fun removeHandler(protocol: String) {
        handlerLock.withLock {
            handlers.removeIf { it.protocol == protocol }
        }
    }

    fun clearHandlers() {
        handlers.clear()
    }

    fun protocols(): Set<String> {
        handlerLock.withLock {
            return handlers.map { it.protocol }.toSet()
        }
    }

    private suspend fun sendList(connection: Utf8Connection): Result<Unit> {
        handlerLock.withLock {
            for (handler in handlers) {
                connection.writeUtf8(handler.protocol)
                    .onFailure { return Err(it) }
            }
        }
        return Ok(Unit)
    }

    private fun findHandler(protocol: String): ProtocolHandlerInfo<T>? {
        handlerLock.withLock {
            for (handler in handlers) {
                if (handler.match(protocol)) {
                    return handler
                }
            }
        }
        return null
    }

    private suspend fun handshake(connection: Utf8Connection): Result<Unit> {
        connection.writeUtf8(PROTOCOL_ID)
            .onFailure { return Err(it) }
        return readMultistreamHeader(connection)
    }

    companion object {
        private const val PROTOCOL_ID = "/multistream/1.0.0"
        private const val simOpenProtocol = "/libp2p/simultaneous-connect"
        private const val NA = "na"
        private const val LS = "ls"
        private const val tieBreakerPrefix = "select:"
        private const val initiator = "initiator"
        private const val responder = "responder"

        private val ErrNoProtocols = Error("no protocols specified")
        private val ErrNotSupported = Error("Peer does not support any of the given protocols")
        private val ErrIncorrectVersion = Error("client connected with incorrect version")

        suspend fun selectOneOf(protocols: Set<String>, connection: Utf8Connection): Result<String> {
            if (protocols.isEmpty()) {
                return Err(ErrNoProtocols)
            }
            val protoList = protocols.toMutableList()
            val firstProto = protoList.removeAt(0)
            selectProtoOrFail(firstProto, connection)
                .onSuccess { return Ok(firstProto) }
                .onFailure {
                    if (it != ErrNotSupported) {
                        return Err(it)
                    }
                }
            return selectProtosOrFail(protoList, connection)
        }

        suspend fun selectProtoOrFail(protocol: String, connection: Utf8Connection): Result<Unit> {
            connection.writeUtf8(PROTOCOL_ID, protocol)
                .onFailure { return Err(it) }
            readMultistreamHeader(connection)
                .onFailure { return Err(it) }
            return readProto(protocol, connection)
        }

        suspend fun selectWithSimopenOrFail(protocols: Set<String>, connection: Utf8Connection): Result<SimOpenInfo> {
            if (protocols.isEmpty()) {
                return Err(ErrNoProtocols)
            }
            val protoList = protocols.toMutableList()
            connection.writeUtf8(PROTOCOL_ID, simOpenProtocol, protoList[0])
                .onFailure { return Err(it) }
            readMultistreamHeader(connection)
                .onFailure { return Err(it) }
            val token = readNextToken(connection)
                .getOrElse { return Err(it) }
            if (token == simOpenProtocol) {
                return simOpen(protoList, connection)
            } else if (token == NA) {
                val protocol = clientOpen(protoList, connection)
                    .getOrElse { return Err(it) }
                return Ok(SimOpenInfo(protocol, false))
            }
            return Err("unexpected response: $token")
        }

        private suspend fun selectProtosOrFail(protocols: List<String>, connection: Utf8Connection): Result<String> {
            for (protocol in protocols) {
                trySelect(protocol, connection)
                    .onSuccess {
                        return Ok(protocol)
                    }
                    .onFailure {
                        if (it != ErrNotSupported) {
                            return Err(it)
                        }
                    }
            }
            return Err(ErrNotSupported)
        }

        private suspend fun clientOpen(protocols: List<String>, connection: Utf8Connection): Result<String> {
            val token = readNextToken(connection)
                .getOrElse { return Err(it) }
            val protoList = protocols.toMutableList()
            val firstProto = protoList.removeAt(0)
            if (token == firstProto) {
                return Ok(firstProto)
            } else if (token == NA) {
                return selectProtosOrFail(protoList, connection)
            }
            return Err("unexpected response: $token")
        }

        private suspend fun simOpen(protocols: List<String>, connection: Utf8Connection): Result<SimOpenInfo> {
            val randBytes = ByteArray(8)
            Random().nextBytes(randBytes)
            val myNonce = toLong(randBytes)
            connection.writeUtf8(tieBreakerPrefix + myNonce.toString(10))
                .onFailure { return Err(it) }
            // skip one protocol
            readNextToken(connection)
                .getOrElse { return Err(it) }
            val token = readNextToken(connection)
                .getOrElse { return Err(it) }
            if (!token.startsWith(tieBreakerPrefix)) {
                return Err("tie breaker nonce not sent with the correct prefix")
            }
            val peerNonce = token.substring(tieBreakerPrefix.length).toULong(radix = 10)
            if (peerNonce == myNonce) {
                return Err("failed client selection; identical nonces")
            }
            val iamserver = peerNonce > myNonce
            if (iamserver) {
                val protocol = simOpenSelectServer(protocols, connection)
                    .getOrElse { return Err(it) }
                return Ok(SimOpenInfo(protocol, true))
            } else {
                val protocol = simOpenSelectClient(protocols, connection)
                    .getOrElse { return Err(it) }
                return Ok(SimOpenInfo(protocol, false))
            }
        }

        private suspend fun simOpenSelectServer(protocols: List<String>, connection: Utf8Connection): Result<String> {
            connection.writeUtf8(responder)
                .onFailure { return Err(it) }
            val token = readNextToken(connection)
                .getOrElse { return Err(it) }
            if (token != initiator) {
                return Err("unexpected response: $token")
            }
            while (true) {
                val nextToken = readNextToken(connection)
                    .getOrElse {
                        if (it == Errors.EndOfStream) {
                            return Err(ErrNotSupported)
                        }
                        return Err(it)
                    }
                for (protocol in protocols) {
                    if (nextToken == protocol) {
                        connection.writeUtf8(protocol)
                            .onFailure { return Err(it) }
                        return Ok(protocol)
                    }
                }
                connection.writeUtf8(NA)
                    .onFailure { return Err(it) }
            }
        }

        private suspend fun simOpenSelectClient(protocols: List<String>, connection: Utf8Connection): Result<String> {
            connection.writeUtf8(initiator)
                .onFailure { return Err(it) }
            val token = readNextToken(connection)
                .getOrElse { return Err(it) }
            if (token != responder) {
                return Err("unexpected response: $token")
            }
            return selectProtosOrFail(protocols, connection)
        }

        private suspend fun readMultistreamHeader(connection: Utf8Connection): Result<Unit> {
            val token = readNextToken(connection)
                .getOrElse { return Err(it) }
            if (token != PROTOCOL_ID) {
                return Err("received mismatch in protocol id")
            }
            return Ok(Unit)
        }

        private suspend fun trySelect(protocol: String, connection: Utf8Connection): Result<Unit> {
            connection.writeUtf8(protocol)
                .onFailure { return Err(it) }
            return readProto(protocol, connection)
        }

        private suspend fun readProto(protocol: String, connection: Utf8Connection): Result<Unit> {
            val token = readNextToken(connection)
                .getOrElse { return Err(it) }
            if (token == protocol) {
                return Ok(Unit)
            } else if (token == NA) {
                return Err(ErrNotSupported)
            }
            return Err("unrecognized response: $token")
        }

        private fun toLong(b: ByteArray): ULong {
            return b[0].toULong() or (b[1].toULong() shl 8) or (b[2].toULong() shl 16) or (b[3].toULong() shl 24) or
                (b[4].toULong() shl 32) or (b[5].toULong() shl 40) or (b[6].toULong() shl 48) or (b[7].toULong() shl 56)
        }

        private suspend fun readNextToken(connection: Utf8Connection) = connection.readUtf8()
    }
}
