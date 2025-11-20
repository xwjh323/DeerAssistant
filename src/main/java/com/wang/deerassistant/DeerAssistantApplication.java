package com.wang.deerassistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan("com.wang.deerassistant.mapper")
@SpringBootApplication
public class DeerAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeerAssistantApplication.class, args);
    }
}
