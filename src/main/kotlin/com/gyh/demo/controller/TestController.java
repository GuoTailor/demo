package com.gyh.demo.controller;

import com.gyh.demo.dto.ResponseInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * create by GYH on 2023/7/9
 */
@RestController
public class TestController {
    @Operation(summary = "test", security = {@SecurityRequirement(name = "Authorization")})
    @GetMapping("/hello")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseInfo<String>> hello() {
        return Mono.just(ResponseInfo.ok("hello nm"));
    }

}
