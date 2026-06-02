-- 梦境解析状态补丁
-- 为 dream_content 表新增 analysis_status 和 analysis_error 字段
-- 状态枚举：NONE（未请求解析）/ PENDING（解析中）/ SUCCESS（成功）/ FAILED（失败）

ALTER TABLE dream_content
    ADD COLUMN analysis_status VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER interpretation,
    ADD COLUMN analysis_error TEXT AFTER analysis_status;

-- 回填历史数据
-- 有正常解析内容的标记 SUCCESS
UPDATE dream_content
SET analysis_status = 'SUCCESS'
WHERE interpretation IS NOT NULL
  AND interpretation != ''
  AND interpretation NOT LIKE '%解析失败%'
  AND interpretation NOT LIKE '%解析中%'
  AND interpretation NOT LIKE '%稍后重新解析%';

-- 解析失败文案的标记 FAILED
UPDATE dream_content
SET analysis_status = 'FAILED',
    analysis_error = interpretation
WHERE interpretation LIKE '%解析失败%'
   OR interpretation LIKE '%稍后重新解析%';

-- 解析中哨兵的标记 PENDING（这些应该很少，因为是异步的）
UPDATE dream_content
SET analysis_status = 'PENDING'
WHERE interpretation LIKE '%解析中%';
