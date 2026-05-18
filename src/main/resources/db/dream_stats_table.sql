-- 梦境统计汇总表
-- 用于存储预计算的统计数据，提高查询性能

CREATE TABLE IF NOT EXISTS dream_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_dreams INT DEFAULT 0 COMMENT '总梦境数',
    happy_count INT DEFAULT 0 COMMENT '开心梦境数',
    sad_count INT DEFAULT 0 COMMENT '难过梦境数',
    scared_count INT DEFAULT 0 COMMENT '恐惧梦境数',
    angry_count INT DEFAULT 0 COMMENT '愤怒梦境数',
    peaceful_count INT DEFAULT 0 COMMENT '平静梦境数',
    other_emotion_count INT DEFAULT 0 COMMENT '其他情绪梦境数',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_date (user_id, stat_date),
    INDEX idx_user_id (user_id),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='梦境统计汇总表';

-- 梦境地点统计表
CREATE TABLE IF NOT EXISTS dream_place_stats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    place VARCHAR(100) NOT NULL COMMENT '地点',
    dream_count INT DEFAULT 0 COMMENT '梦境数量',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_place (user_id, place),
    INDEX idx_user_id (user_id),
    INDEX idx_dream_count (dream_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='梦境地点统计表';
