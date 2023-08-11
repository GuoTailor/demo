package com.gyh.demo.controller;

import com.gyh.demo.dto.ResponseInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

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

    @Operation(summary = "test", security = {@SecurityRequirement(name = "Authorization")})
    @PostMapping("/hello")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseInfo<Set<Map.Entry<String, Object>>>> hello2(@RequestBody Map<String, Object> map) {
        return Mono.just(ResponseInfo.ok(map.entrySet()));
    }
}
