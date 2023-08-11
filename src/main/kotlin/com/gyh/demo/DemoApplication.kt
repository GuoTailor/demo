package com.gyh.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import reactor.core.publisher.Hooks

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = ["com.gyh.demo.mapper"])
class DemoApplication

fun main(args: Array<String>) {
    val runApplication = runApplication<DemoApplication>(*args)

}
