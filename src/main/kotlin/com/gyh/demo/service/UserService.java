package com.gyh.demo.service;

import com.gyh.demo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * create by GYH on 2023/7/9
 */
@Service
public class UserService  implements ReactiveUserDetailsService {
    @Resource
    private UserMapper userMapper;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userMapper.findByUsername(username)
                .zipWhen(it -> userMapper.findRolesByUserId(it.getId()).collectList())
                .map(it -> {
                    it.getT1().setRoles(it.getT2());
                    return it.getT1();
                })
                .cast(UserDetails.class);
    }
}
