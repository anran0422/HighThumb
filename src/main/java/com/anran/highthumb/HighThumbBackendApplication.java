package com.anran.highthumb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.anran.highthumb.mapper")
public class HighThumbBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighThumbBackendApplication.class, args);
    }

}
