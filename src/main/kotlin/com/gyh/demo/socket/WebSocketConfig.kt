package com.gyh.demo.socket

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import java.util.*


/**
 * Created by gyh on 2020/4/5.
 */
@Configuration
class WebSocketConfig {

    @Bean
    fun handlerAdapter(handler: SimpleUrlHandlerMapping): WebSocketHandlerAdapter {
        val beanMap = handler.applicationContext!!.getBeansWithAnnotation(WebSocketMapping::class.java)
        val handlerMap = HashMap<String, WebSocketHandler>()
        beanMap.values.forEach { bean ->
            if (bean !is WebSocketHandler) {
                throw RuntimeException(
                    String.format(
                        "Controller [%s] doesn't implement WebSocketHandler interface.",
                        bean.javaClass.name
                    )
                )
            }
            val annotation = AnnotationUtils.getAnnotation(bean.javaClass, WebSocketMapping::class.java)
            //webSocketMapping 映射到管理中
            handlerMap[Objects.requireNonNull(annotation)!!.value] = bean
        }
        handler.setOrder(Ordered.HIGHEST_PRECEDENCE)
        handler.setUrlMap(handlerMap)
        handler.initApplicationContext()
        return WebSocketHandlerAdapter()
    }

}