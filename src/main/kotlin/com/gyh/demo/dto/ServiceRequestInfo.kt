package com.gyh.demo.dto

import org.springframework.web.reactive.socket.WebSocketMessage


/**
 * Created by gyh on 2020/4/16.
 */
class ServiceRequestInfo(val order: String, val body: Any? = null, val req: Int) {
    lateinit var message: WebSocketMessage

    private val map: Map<*, *>? = body as? Map<*, *>?

    fun getParameterValues(name: String): Array<Any?> {
        return arrayOf(map?.get(name))
    }
}