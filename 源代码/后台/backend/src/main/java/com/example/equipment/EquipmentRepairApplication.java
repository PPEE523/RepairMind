package com.example.equipment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.equipment.mapper")
@EnableScheduling
public class EquipmentRepairApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentRepairApplication.class, args);
    }
}
