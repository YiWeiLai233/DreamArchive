-- 梦境内容表
-- 存储用户记录的梦境详细内容

CREATE TABLE IF NOT EXISTS dream_content (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID',
    user_id INT NOT NULL COMMENT '用户ID',
    title VARCHAR(100) DEFAULT '' COMMENT '梦境标题',
    content TEXT NOT NULL COMMENT '梦境内容',
    emotion VARCHAR(50) DEFAULT 'mysterious' COMMENT '情绪标签',
    place VARCHAR(100) DEFAULT '未知' COMMENT '梦境地点',
    time VARCHAR(50) DEFAULT '' COMMENT '梦境时间',
    interpretation TEXT COMMENT 'AI解析结果',
    analysis_status VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '解析状态: NONE/PENDING/SUCCESS/FAILED',
    analysis_error TEXT COMMENT '解析失败原因',
    image_url TEXT COMMENT '梦境图片访问地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='梦境内容表';
