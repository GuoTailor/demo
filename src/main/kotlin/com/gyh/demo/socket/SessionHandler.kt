package com.gyh.demo.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.gyh.demo.dto.ServiceResponseInfo
import com.gyh.demo.util.ThreadManager
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by gyh on 2021/7/9
 */
class SessionHandler(private var session: WebSocketSession, private val json: ObjectMapper) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val responseCount = AtomicInteger(1)
    private val responseMap = ConcurrentHashMap<Int, SendInfo>()
    private var retryCount = 3
    private var retryTimeout = 1L
    val dataMap = HashMap<String, Any>()

    /**
     * 被动回应调用
     */
    fun send(message: ServiceResponseInfo.DataResponse, confirm: Boolean = false): Mono<Unit> {
        if (confirm) {
            val cycle = Flux.interval(Duration.ofSeconds(retryTimeout))
                .flatMap {
                    logger.info("重试 {} {} {}", responseMap[message.req]?.ack == true, it >= retryCount, message)
                    if (responseMap[message.req]?.ack == true || it >= retryCount) {
                        val remove = responseMap.remove(message.req)
                        remove?.cycle?.dispose()
                        reqIncrement(message.req)
                        Mono.just(message)
                    } else {
                        session.send(Mono.defer {
                            val body = message.body.toString()
                            message.body = Any()
                            var writeValueAsString = json.writeValueAsString(message)
                            writeValueAsString = writeValueAsString.replace("{}", body)
                            Mono.just(session.textMessage(writeValueAsString))
                        })
                    }
                }.subscribeOn(ThreadManager.getScheduler())
                .subscribe()
            responseMap[message.req] = SendInfo(message.req, cycle)
        }
        return session.send(Mono.defer {
            val body = message.body.toString()
            message.body = Any()
            var writeValueAsString = json.writeValueAsString(message)
            writeValueAsString = writeValueAsString.replace("{}", body)
            Mono.just(session.textMessage(writeValueAsString))
        }).then(Mono.just(Unit))
    }

    /**
     * 主动发送请调用该方法
     */
    fun <T> send(data: Mono<T>, order: Int, confirm: Boolean = false): Mono<Unit> {
        val req = responseCount.getAndIncrement()
        return ServiceResponseInfo(data, req, order).getMono()
            .flatMap { send(it, confirm) }
    }

    fun <T> send(data: Mono<T>, req: Int, order: Int, confirm: Boolean = false): Mono<Unit> {
        return ServiceResponseInfo(data, req, order).getMono()
            .flatMap { send(it, confirm) }
    }

    fun <T : Any> send(data: T, order: Int, confirm: Boolean = false): Mono<Unit> {
        val req = responseCount.getAndIncrement()
        val msg = ServiceResponseInfo.DataResponse(data, req, order)
        return send(msg, confirm)
    }

    fun tryEmitComplete(): Mono<Void> {
        return session.close()
    }

    fun setSessionId(sessionId: String) {
        dataMap["sessionId"] = sessionId
    }

    fun getSessionId(): String {
        return dataMap["sessionId"].toString()
    }

    fun reqIncrement(req: Int) {
        val value = responseMap[req]
        if (value != null) {
            value.ack = true
            value.cycle.dispose()
            logger.info("取消 {} ", req)
            responseMap.remove(req)
        }
    }

    data class SendInfo(
        val req: Int,
        val cycle: Disposable,
        var ack: Boolean = false
    )
}