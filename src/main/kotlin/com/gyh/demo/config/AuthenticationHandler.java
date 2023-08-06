package com.gyh.demo.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyh.demo.common.Constant;
import com.gyh.demo.domain.User;
import com.gyh.demo.dto.ResponseInfo;
import com.gyh.demo.exception.RemoteLoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * create by GYH on 2023/5/12
 */
public class AuthenticationHandler implements ServerAuthenticationSuccessHandler,
        ServerAuthenticationFailureHandler,
        ServerAccessDeniedHandler,
        ServerSecurityContextRepository,
        ServerAuthenticationEntryPoint {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper json = new ObjectMapper();
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public AuthenticationHandler(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 未登录时做的操作，重写不跳转登录页
     */
    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ServerHttpResponse response = exchange.getResponse();
        log.info("未登录 {} {}", exchange.getRequest().getURI(), ex.getLocalizedMessage());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            return response.writeWith(Mono.just(response.bufferFactory().wrap(json.writeValueAsBytes(ResponseInfo.failed(ex.getLocalizedMessage())))));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        ServerHttpResponse response = exchange.getResponse();
        log.info("授权失败 {} {}", exchange.getRequest().getURI(), denied.getLocalizedMessage());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            return response.writeWith(Mono.just(response.bufferFactory().wrap(json.writeValueAsBytes(ResponseInfo.failed(denied.getLocalizedMessage())))));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange, AuthenticationException exception) {
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
        log.info("登录失败", exception);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String msg = exception.getLocalizedMessage();
        try {
            return response.writeWith(Mono.just(response.bufferFactory().wrap(json.writeValueAsBytes(ResponseInfo.failed(msg)))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
        log.info("登录成功 {} {}", authentication, authentication.getCredentials());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) authentication;
        String token = UUID.randomUUID().toString();
        User user = (User) userToken.getPrincipal();
        String redisKey = Constant.tokenKey + user.getId() + Constant.tokenInfix + token;
        try {
            byte[] value = json.writeValueAsBytes(ResponseInfo.ok("成功", token));
            Duration expired = Duration.ofMillis(Constant.tokenTtlMillis);
            return redisTemplate.keys(Constant.tokenKey + user.getId() + Constant.tokenInfix + "*")
                    .flatMap(key -> redisTemplate.opsForValue().get(key).cast(User.class)
                            .flatMap(it -> {
                                it.setCredentialsNonExpired(false);
                                return redisTemplate.getExpire(key)
                                        .flatMap(duration -> redisTemplate.opsForValue().set(key, it, duration.equals(Duration.ZERO) ? expired : duration));
                            }))
                    .then(redisTemplate.opsForValue().set(redisKey, user, expired))
                    .flatMap(it -> response.writeWith(Mono.just(response.bufferFactory().wrap(value))));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String authToken = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            authToken = authHeader.replaceFirst("Bearer ", "");
        } else if (matcher.match("/room", exchange.getRequest().getPath().value())) {
            authToken = getQueryMap(exchange.getRequest().getURI().getQuery()).get("bearer");
        }
        if (authToken != null) {
            log.info("授权 {}", authToken);
            try {
                return redisTemplate.keys(Constant.tokenKey + "*" + Constant.tokenInfix + authToken)
                        .flatMap(key -> {
                            log.info(key);
                            return redisTemplate.opsForValue().get(key).cast(User.class);
                        })
                        .collectList()
                        .filter(it -> !CollectionUtils.isEmpty(it))
                        .handle((user, sink) -> {
                            if (!user.get(0).isCredentialsNonExpired()) {
                                sink.error(new RemoteLoginException("账号异地登录"));
                                return;
                            }
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user.get(0), "", user.get(0).getAuthorities());
                            sink.next(new SecurityContextImpl(authentication));
                        });
            } catch (BadCredentialsException e) {
                log.error(e.getLocalizedMessage());
            }
        }
        return Mono.empty();
    }

    private Map<String, String> getQueryMap(String queryStr) {
        HashMap<String, String> queryMap = new HashMap<>();
        if (StringUtils.hasLength(queryStr)) {
            String[] queryParam = queryStr.split("&");
            for (String s : queryParam) {
                String[] kv = s.split("=", 2);
                String value = kv.length == 2 ? kv[1] : "";
                queryMap.put(kv[0], value);
            }
        }
        return queryMap;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

}
