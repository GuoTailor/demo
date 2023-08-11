package com.gyh.demo.socket

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import java.util.*

/**
 * Created by gyh on 2020/4/8.
 */
@Component
@ConditionalOnMissingBean(SimpleUrlHandlerMapping::class)
class WebSocketMappingHandler : SimpleUrlHandlerMapping() {

    override fun initApplicationContext() {
        val beanMap = obtainApplicationContext().getBeansWithAnnotation(WebSocketMapping::class.java)
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
        super.setOrder(Ordered.HIGHEST_PRECEDENCE)
        super.setUrlMap(handlerMap)
        super.initApplicationContext()
    }
}