package com.gyh.demo.dto

import reactor.core.publisher.Mono

/**
 * Created by gyh on 2020/4/16.
 */
class ServiceResponseInfo (var data: Mono<*> = Mono.empty<Any>(), var req: Int, var order: Int) {

    fun getMono(): Mono<DataResponse> {
        return data.map { DataResponse(it, req, order) }
    }

    data class DataResponse(var body: Any, val req: Int, val order: Int)
}