package com.gyh.demo.config;

import com.gyh.demo.domain.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * create by GYH on 2023/7/9
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity()
public class WebFluxSecurityConfig {
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        AuthenticationHandler authenticationHandler = new AuthenticationHandler(redisTemplate);
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/login", "/error","/webjars/**",
                                "/swagger-ui/**", "/swagger-resources/*", "/v3/api-docs/*", "/webjars/*",
                                "*.html", "/*/*.html", "/*/*.js", "*.js", "*.css", "*.png", "*.ico"
                        ).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .anyExchange().authenticated()
                        .anyExchange().access((authentication, object) -> authentication
                                .filter(it -> it == null || !AnonymousAuthenticationToken.class.isAssignableFrom(it.getClass()))
                                .map(it -> new AuthorizationDecision(it.isAuthenticated()))
                                .defaultIfEmpty(new AuthorizationDecision(false)))
                )
                .formLogin(form -> form
                        .authenticationSuccessHandler(authenticationHandler)
                        .authenticationFailureHandler(authenticationHandler)
                        .authenticationEntryPoint(authenticationHandler)
                        .requiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/login"))
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(authenticationHandler)
                )
                .securityContextRepository(authenticationHandler)
                .build();
    }

    /**
     * 添加角色继承关系
     *
     * @return RoleHierarchy
     */
    @Bean
    public RoleHierarchy roleHierarchy(DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler) {
        var hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy(Role.SUPER_ADMIN + " > " + Role.ADMIN + "\n" +
                Role.ADMIN + " > " + Role.USER);
        methodSecurityExpressionHandler.setRoleHierarchy(hierarchy);
        return hierarchy;
    }

    /**
     * 解决跨域
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
