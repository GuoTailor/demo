package com.gyh.demo.socket

import com.gyh.demo.common.NotifyOrder
import com.gyh.demo.domain.User
import com.gyh.demo.dto.ResponseInfo
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono

/**
 * Created by gyh on 2021/7/5.
 */
@WebSocketMapping("/room")
class RoomSocketHandler : SocketHandler() {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun onConnect(sessionHandler: SessionHandler): Mono<*> {
        return ReactiveSecurityContextHolder.getContext()
            .map { (it.authentication.principal as User) }
            .flatMap {
                sessionHandler.dataMap["id"] = it.id
                SocketSessionStore.addUser(sessionHandler, it.id)
            }.onErrorResume {
                it.printStackTrace()
                sessionHandler.send(ResponseInfo.failed<Unit>("错误: ${it.message}").toMono(), NotifyOrder.errorNotify)
            }
    }

    override fun onDisconnected(sessionHandler: SessionHandler) {
        val id = sessionHandler.dataMap["id"] as Int
        SocketSessionStore.removeUser(id)
    }

}
