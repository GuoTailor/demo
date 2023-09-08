package com.gyh.demo.mapper;

import com.gyh.demo.domain.Menu;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

/**
 * create by GYH on 2023/8/16
 */
public interface MenuMapper extends R2dbcRepository<Menu, Integer> {

    @Query("select distinct m1.*,m2.`id` as id2,m2.`component` as component2,m2.`enabled` as enabled2,m2.`iconCls` as iconCls2,m2.`name` as name2,m2.`parentId` as parentId2,m2.`path` as path2 " +
            "from menu m1,menu m2,user_role hrr,menu_role mr " +
            "where m1.`id`=m2.`parentId` and hrr.`id`=:userId and hrr.`role_id`=mr.`rid` and mr.`mid`=m2.`id` and m2.`enabled`=true " +
            "order by m1.`id`,m2.`id`")
    Flux<Menu> getMenusByUserId(Integer userId);
}
