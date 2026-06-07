-- AI provider pool storage.
-- Runtime state such as fail_count, circuit_open, and avg_latency_ms remains in memory.

CREATE TABLE IF NOT EXISTS ai_provider (
    name VARCHAR(80) PRIMARY KEY COMMENT 'Provider unique name',
    url VARCHAR(500) NOT NULL COMMENT 'OpenAI-compatible base URL',
    api_key VARCHAR(512) NOT NULL COMMENT 'Provider API key',
    model VARCHAR(120) NOT NULL COMMENT 'Model name',
    weight INT NOT NULL DEFAULT 10 COMMENT 'Weighted round-robin base weight',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Whether provider is enabled',
    vision_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Use this provider for image analysis',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',

    INDEX idx_enabled (enabled),
    INDEX idx_vision_enabled (vision_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI provider pool';
