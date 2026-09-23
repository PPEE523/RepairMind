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
 * init.sql 仅在 Docker postgres 首次初始化时执行，已运行的库需要靠此处补齐字段和表。
 */
@Slf4j
@Component
@Order(1)
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Resource
    private DataSource dataSource;

    private static final String[] MIGRATIONS = {
            "ALTER TABLE repair_knowledge ADD COLUMN IF NOT EXISTS images TEXT",

            // ===== AI智能问答系列表（对应 sql/chat_tables.sql，全部幂等）=====
            // 对话历史表
            """
            CREATE TABLE IF NOT EXISTS chat_history (
                id BIGSERIAL PRIMARY KEY,
                conversation_id VARCHAR(64),
                user_question TEXT NOT NULL,
                device_model VARCHAR(128),
                answer_content TEXT,
                message_id VARCHAR(64),
                status VARCHAR(20) DEFAULT 'success',
                round_num INTEGER DEFAULT 1,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                user_id BIGINT
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_chat_history_conversation_id ON chat_history(conversation_id)",
            "CREATE INDEX IF NOT EXISTS idx_chat_history_user_id ON chat_history(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_chat_history_create_time ON chat_history(create_time)",

            // AI反馈表
            """
            CREATE TABLE IF NOT EXISTS chat_feedback (
                id BIGSERIAL PRIMARY KEY,
                chat_history_id BIGINT NOT NULL,
                feedback_status VARCHAR(20) NOT NULL DEFAULT 'pending',
                feedback_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                user_id BIGINT
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_chat_feedback_history_id ON chat_feedback(chat_history_id)",

            // 文档资源表
            """
            CREATE TABLE IF NOT EXISTS chat_document_resource (
                id BIGSERIAL PRIMARY KEY,
                chat_history_id BIGINT NOT NULL,
                dataset_id VARCHAR(64),
                dataset_name VARCHAR(256),
                document_id VARCHAR(64),
                document_name VARCHAR(256),
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_chat_doc_resource_history_id ON chat_document_resource(chat_history_id)",
            "CREATE INDEX IF NOT EXISTS idx_chat_doc_resource_document_id ON chat_document_resource(document_id)",

            // 分段信息表
            """
            CREATE TABLE IF NOT EXISTS chat_segment (
                id BIGSERIAL PRIMARY KEY,
                document_resource_id BIGINT NOT NULL,
                segment_id VARCHAR(64),
                content TEXT,
                score DOUBLE PRECISION DEFAULT 0,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_chat_segment_doc_resource_id ON chat_segment(document_resource_id)",
            "CREATE INDEX IF NOT EXISTS idx_chat_segment_segment_id ON chat_segment(segment_id)"
    };

    @Override
    public void run(String... args) {
        int success = 0;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : MIGRATIONS) {
                stmt.execute(sql);
                success++;
            }
            log.info("数据库迁移执行完成，共 {}/{} 条幂等语句", success, MIGRATIONS.length);
        } catch (Exception e) {
            log.error("数据库迁移执行失败，已完成 {}/{}: {}", success, MIGRATIONS.length, e.getMessage(), e);
        }
    }
}
