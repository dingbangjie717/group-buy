package com.groupbuy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 鲜购到家 - 启动类
 */
@EnableScheduling
@MapperScan("com.groupbuy.**.mapper")
@SpringBootApplication
public class GroupBuyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupBuyApplication.class, args);
    }
}
