package com.gyh.demo.socket

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.gyh.demo.common.NotifyOrder
import com.gyh.demo.config.LocalDateTimeDeserializer
import com.gyh.demo.config.LocalDateTimeSerializer
import com.gyh.demo.dto.ResponseInfo
import com.gyh.demo.dto.ServiceRequestInfo
import com.gyh.demo.dto.ServiceResponseInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Created by gyh on 2020/5/19.
 */
abstract class SocketHandler : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val json = jacksonObjectMapper()

    @Autowired
    private lateinit var dispatcherServlet: DispatcherHandler

    init {
        val javaTimeModule = JavaTimeModule()
        javaTimeModule.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer())
        javaTimeModule.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer())
        json.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        json.registerModule(javaTimeModule)
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        logger.info("连接")
        val sessionHandler = SessionHandler(session, json)
        sessionHandler.setSessionId(session.id)
        val input = session.receive()
            .map { it.payloadAsText }
            .map(::toServiceRequestInfo)
            .filter { it.order != "/ping" }
            .filter { filterConfirm(it, sessionHandler) }
            .doOnNext(::printLog)
            .flatMap {
//                DefaultServerWebExchangeBuilder
//                dispatcherServlet.handle()


                val resp = ServiceResponseInfo(req = it.req, order = NotifyOrder.requestReq)
                logger.info("返回数据order:{} req:{} data:{}", resp.order, resp.req, it.body)
                resp.data = Mono.just(it.body ?: "ok")
//                dispatcherServlet.doDispatch(it, resp)
                resp.getMono().onErrorResume { e ->
                    logger.info("错误 {}", e.message)
                    ServiceResponseInfo(
                        ResponseInfo.failed<Unit>("错误 ${e.message}").toMono(),
                        NotifyOrder.errorNotify,
                        NotifyOrder.requestReq
                    ).getMono()
                }
            }
            .flatMap { sessionHandler.send(it, true) }
            .doOnTerminate { onDisconnected(sessionHandler) }
            .then()//.log()
        val onCon = onConnect(sessionHandler)
        return Mono.zip(onCon, input).then()
    }

    /**
     * 当socket连接时
     */
    abstract fun onConnect(sessionHandler: SessionHandler): Mono<*>

    /**
     * 当socket断开连接时
     */
    abstract fun onDisconnected(sessionHandler: SessionHandler)

    private fun toServiceRequestInfo(data: String): ServiceRequestInfo {
        return this.json.readValue(data)
    }

    private fun printLog(info: ServiceRequestInfo): ServiceRequestInfo {
        if (info.order != "/echo")
            logger.info("接收到数据order:{} req:{} data:{}", info.order, info.req, info.body)
        return info
    }

    private fun filterConfirm(info: ServiceRequestInfo, sessionHandler: SessionHandler): Boolean {
        if (info.order == "/ok") {
            sessionHandler.reqIncrement(info.req)
            return false
        }
        return true
    }
}