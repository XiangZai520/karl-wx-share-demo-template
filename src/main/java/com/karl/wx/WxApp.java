package com.karl.wx;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author liuweilong
 * @description
 * @date 2019/5/22 14:05
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class WxApp {
    public static void main(String[] args) {
        SpringApplication.run(WxApp.class, args);
    }
}
