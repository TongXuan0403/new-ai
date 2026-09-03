package org.example.aispingboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.aispingboot.mapper")
public class AiSpingbootApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiSpingbootApplication.class, args);
    }
}
