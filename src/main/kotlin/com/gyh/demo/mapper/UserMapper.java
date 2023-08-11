package com.gyh.demo.mapper;

import com.gyh.demo.domain.Role;
import com.gyh.demo.domain.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * create by GYH on 2023/7/9
 */
public interface UserMapper extends R2dbcRepository<User, Integer> {

    @Query("select * from user where username = :username")
    Mono<User> findByUsername(String username);

    @Query("select role.* from role left join user_role ur on role.id = ur.role_id where user_id = :userId")
    Flux<Role> findRolesByUserId(Integer userId);
}
