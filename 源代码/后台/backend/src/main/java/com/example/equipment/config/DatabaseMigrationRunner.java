package com.example.equipment.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时执行幂等数据库迁移，确保存量库结构与最新实体定义一致。
 * init.sql 仅在 Docker postgres 首次初始化时执行，已运行的库需要靠此处补齐字段。
 */
@Slf4j
@Component
@Order(1)
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Resource
    private DataSource dataSource;

    private static final String[] MIGRATIONS = {
            "ALTER TABLE repair_knowledge ADD COLUMN IF NOT EXISTS images TEXT"
    };

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : MIGRATIONS) {
                stmt.execute(sql);
            }
            log.info("数据库迁移执行完成，共 {} 条幂等语句", MIGRATIONS.length);
        } catch (Exception e) {
            log.error("数据库迁移执行失败: {}", e.getMessage(), e);
        }
    }
}
