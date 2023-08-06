package com.gyh.demo.socket

import com.gyh.demo.common.NotifyOrder
import com.gyh.demo.dto.ResponseInfo
import com.gyh.demo.util.ThreadManager
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by gyh on 2020/4/12.
 */
object SocketSessionStore {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val userInfoMap = ConcurrentHashMap<Int, UserRoomInfo>()

    /**
     * 添加用户
     */
    fun addUser(session: SessionHandler, id: Int): Mono<Unit> {
        val userInfo = UserRoomInfo(session, id)
        val old = userInfoMap.put(id, userInfo)
        logger.info("添加用户 $id ${session.getSessionId()}")
        return if (old != null) {
            logger.info("用户多地登陆 $id ${old.session.getSessionId()}")
            old.session.send(ResponseInfo.ok<Unit>("用户账号在其他地方登陆").toMono(), NotifyOrder.differentPlaceLogin)
                .map { old.session.tryEmitComplete() }.then(Mono.just(Unit))
        } else Mono.just(Unit)
    }


    fun removeUser(userId: Int) {
        val tryEmitComplete = userInfoMap.remove(userId)?.session?.tryEmitComplete()?.subscribeOn(ThreadManager.getScheduler())?.subscribe()
        logger.info("移除用户 $userId {} ", tryEmitComplete)
    }

    fun getRoomInfo(userId: Int): UserRoomInfo? {
        return userInfoMap[userId]
    }

    fun getOnLineSize(): Int {
        return userInfoMap.size
    }


    data class UserRoomInfo(
        val session: SessionHandler,
        val userId: Int
    )
}
