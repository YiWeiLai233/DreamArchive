package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/init")
public class InitController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 初始化梦境统计表
     * POST /api/init/stats-tables
     */
    @PostMapping("/stats-tables")
    public Result<String> initStatsTables() {
        try {
            // 创建 dream_stats 表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS dream_stats (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    stat_date DATE NOT NULL,
                    total_dreams INT DEFAULT 0,
                    happy_count INT DEFAULT 0,
                    sad_count INT DEFAULT 0,
                    scared_count INT DEFAULT 0,
                    angry_count INT DEFAULT 0,
                    peaceful_count INT DEFAULT 0,
                    other_emotion_count INT DEFAULT 0,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_date (user_id, stat_date),
                    INDEX idx_user_id (user_id),
                    INDEX idx_stat_date (stat_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            // 创建 dream_place_stats 表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS dream_place_stats (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    place VARCHAR(100) NOT NULL,
                    dream_count INT DEFAULT 0,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_place (user_id, place),
                    INDEX idx_user_id (user_id),
                    INDEX idx_dream_count (dream_count DESC)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            return Result.success("统计表创建成功");
        } catch (Exception e) {
            return Result.error("创建表失败: " + e.getMessage());
        }
    }
}
