-- Metrics 模拟数据
-- 用途：填充 Dashboard 看板数据，验证前后端联调

-- ========== 1. 用户数据 ==========
INSERT INTO metrics_users (user_id, username, username_hash, hostname, team, created_stime, modified_stime, is_del) VALUES
('a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', '张三', 'hash_zhangsan', 'mac-zhangsan', '平台组', NOW() - INTERVAL 30 DAY, NOW(), 0),
('b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', '李四', 'hash_lisi', 'mac-lisi', '后端组', NOW() - INTERVAL 25 DAY, NOW(), 0),
('c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3', '王五', 'hash_wangwu', 'mac-wangwu', '前端组', NOW() - INTERVAL 20 DAY, NOW(), 0),
('d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', '赵六', 'hash_zhaoliu', 'mac-zhaoliu', 'AI组', NOW() - INTERVAL 15 DAY, NOW(), 0),
('e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5', '孙七', 'hash_sunqi', 'mac-sunqi', '数据组', NOW() - INTERVAL 10 DAY, NOW(), 0),
('f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6', '周八', 'hash_zhouba', 'mac-zhouba', '平台组', NOW() - INTERVAL 5 DAY, NOW(), 0);

-- ========== 2. 每日汇总（最近 30 天） ==========
INSERT IGNORE INTO metrics_daily_summary (summary_date, dau, total_sessions, total_tokens, total_duration_seconds, active_projects, agent_tasks_completed, agent_tasks_failed, agent_first_pass_rate, avg_tokens_per_task, top_skill, top_agent, skill_call_count, created_stime, modified_stime, is_del)
SELECT
    d.date AS summary_date,
    FLOOR(3 + RAND() * 5) AS dau,
    FLOOR(8 + RAND() * 15) AS total_sessions,
    FLOOR(50000 + RAND() * 100000) AS total_tokens,
    FLOOR(3000 + RAND() * 5000) AS total_duration_seconds,
    FLOOR(2 + RAND() * 4) AS active_projects,
    FLOOR(5 + RAND() * 10) AS agent_tasks_completed,
    FLOOR(RAND() * 3) AS agent_tasks_failed,
    ROUND(65 + RAND() * 25, 2) AS agent_first_pass_rate,
    FLOOR(3000 + RAND() * 5000) AS avg_tokens_per_task,
    ELT(FLOOR(1 + RAND() * 3), 'commit', 'review-pr', 'code-review') AS top_skill,
    'claude-code' AS top_agent,
    FLOOR(15 + RAND() * 30) AS skill_call_count,
    d.date AS created_stime,
    d.date AS modified_stime,
    0 AS is_del
FROM (
    SELECT DATE(DATE_SUB(CURDATE(), INTERVAL n DAY)) AS date
    FROM (
        SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
        UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
        UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
        UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
        UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
    ) nums
) d;

-- ========== 3. 会话数据（最近 7 天，每天若干条） ==========
INSERT INTO metrics_sessions (session_id, user_id, project_name, started_at, ended_at, duration_seconds, total_tokens, tool_read_count, tool_edit_count, tool_bash_count, tool_grep_count, created_stime, modified_stime, is_del)
SELECT
    CONCAT('sess_', d.date, '_', n.num) AS session_id,
    ELT(1 + FLOOR(RAND() * 6), 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', 'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3', 'd4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', 'e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5', 'f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6') AS user_id,
    ELT(1 + FLOOR(RAND() * 5), 'funny-harness', 'data-pipeline', 'web-admin', 'api-gateway', 'ai-service') AS project_name,
    TIMESTAMP(d.date, MAKETIME(FLOOR(8 + RAND() * 12), FLOOR(RAND() * 60), 0)) AS started_at,
    TIMESTAMP(d.date, MAKETIME(FLOOR(8 + RAND() * 12), FLOOR(RAND() * 60), FLOOR(RAND() * 60))) AS ended_at,
    FLOOR(300 + RAND() * 2000) AS duration_seconds,
    FLOOR(2000 + RAND() * 15000) AS total_tokens,
    FLOOR(5 + RAND() * 20) AS tool_read_count,
    FLOOR(3 + RAND() * 15) AS tool_edit_count,
    FLOOR(2 + RAND() * 10) AS tool_bash_count,
    FLOOR(1 + RAND() * 8) AS tool_grep_count,
    d.date AS created_stime,
    d.date AS modified_stime,
    0 AS is_del
FROM (
    SELECT DATE(DATE_SUB(CURDATE(), INTERVAL n DAY)) AS date FROM (
        SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
    ) days
) d
CROSS JOIN (
    SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
) n;

-- ========== 4. Agent Trace 数据（最近 30 天） ==========
INSERT INTO metrics_agent_traces (trace_id, session_id, agent_type, task_type, task_desc, status, total_tokens, total_duration_ms, files_changed, loop_count, quality_score, revision_count, started_at, completed_at, created_stime, modified_stime, is_del)
SELECT
    CONCAT('T-', d.date, '-', n.num) AS trace_id,
    CONCAT('sess_', d.date, '_', n.num) AS session_id,
    ELT(1 + FLOOR(RAND() * 4), 'claude-code', 'plan-agent', 'code-reviewer', 'architect') AS agent_type,
    ELT(1 + FLOOR(RAND() * 5), 'feature', 'bugfix', 'refactor', 'test', 'docs') AS task_type,
    ELT(1 + FLOOR(RAND() * 5), '新增用户认证模块', '修复登录超时问题', '重构数据访问层', '编写单元测试', '更新API文档') AS task_desc,
    IF(RAND() > 0.15, 'completed', 'failed') AS status,
    FLOOR(3000 + RAND() * 20000) AS total_tokens,
    FLOOR(5000 + RAND() * 60000) AS total_duration_ms,
    FLOOR(1 + RAND() * 8) AS files_changed,
    IF(RAND() > 0.3, 1, FLOOR(2 + RAND() * 4)) AS loop_count,
    FLOOR(70 + RAND() * 30) AS quality_score,
    FLOOR(RAND() * 3) AS revision_count,
    TIMESTAMP(d.date, MAKETIME(FLOOR(9 + RAND() * 10), FLOOR(RAND() * 60), 0)) AS started_at,
    TIMESTAMP(d.date, MAKETIME(FLOOR(10 + RAND() * 10), FLOOR(RAND() * 60), 0)) AS completed_at,
    d.date AS created_stime,
    d.date AS modified_stime,
    0 AS is_del
FROM (
    SELECT DATE(DATE_SUB(CURDATE(), INTERVAL n DAY)) AS date FROM (
        SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
        UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
        UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
        UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
        UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
    ) days
) d
CROSS JOIN (
    SELECT 1 AS num UNION SELECT 2 UNION SELECT 3
) n;

-- ========== 5. Skill 使用数据（最近 7 天） ==========
INSERT INTO metrics_skill_usage (session_id, user_id, project_name, capability_type, capability_name, triggered_at, tokens_consumed, duration_ms, result_status, trace_id, created_stime, modified_stime, is_del)
SELECT
    CONCAT('sess_', d.date, '_', n.num) AS session_id,
    ELT(1 + FLOOR(RAND() * 6), 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', 'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3', 'd4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', 'e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5', 'f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6') AS user_id,
    ELT(1 + FLOOR(RAND() * 5), 'funny-harness', 'data-pipeline', 'web-admin', 'api-gateway', 'ai-service') AS project_name,
    ELT(1 + FLOOR(RAND() * 4), 'skill', 'agent', 'command', 'slash_command') AS capability_type,
    ELT(1 + FLOOR(RAND() * 8), 'commit', 'review-pr', 'code-review', 'test', 'deploy', 'format', 'search', 'refactor') AS capability_name,
    TIMESTAMP(d.date, MAKETIME(FLOOR(9 + RAND() * 10), FLOOR(RAND() * 60), 0)) AS triggered_at,
    FLOOR(500 + RAND() * 5000) AS tokens_consumed,
    FLOOR(1000 + RAND() * 30000) AS duration_ms,
    IF(RAND() > 0.1, 'success', 'failed') AS result_status,
    CONCAT('T-', d.date, '-', n.num) AS trace_id,
    d.date AS created_stime,
    d.date AS modified_stime,
    0 AS is_del
FROM (
    SELECT DATE(DATE_SUB(CURDATE(), INTERVAL n DAY)) AS date FROM (
        SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
    ) days
) d
CROSS JOIN (
    SELECT 1 AS num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
) n;

-- ========== 6. 告警数据（最近 7 天） ==========
INSERT INTO metrics_alerts (alert_type, severity, trace_id, details, notified, created_stime, modified_stime, is_del) VALUES
('LOOP_EXCEEDED', 'warning', 'T-2026-04-10-2', '{"actual":4,"threshold":3}', 1, NOW() - INTERVAL 1 DAY, NOW(), 0),
('TOKEN_OVERUSE', 'critical', 'T-2026-04-09-1', '{"actual":85000,"avg":12000,"ratio":7.1}', 1, NOW() - INTERVAL 2 DAY, NOW(), 0),
('DAU_DROP', 'critical', NULL, '{"today":2,"yesterday":6,"ratio":0.33}', 1, NOW() - INTERVAL 3 DAY, NOW(), 0),
('FAILURE_RATE', 'warning', NULL, '{"weekRate":0.18,"threshold":0.15}', 0, NOW() - INTERVAL 2 DAY, NOW(), 0),
('AGENT_STUCK', 'critical', 'T-2026-04-11-1', '{"minutesElapsed":8,"threshold":5}', 0, NOW(), NOW(), 0),
('LOOP_EXCEEDED', 'warning', 'T-2026-04-08-3', '{"actual":5,"threshold":3}', 1, NOW() - INTERVAL 4 DAY, NOW(), 0);

-- ========== 7. 报告数据 ==========
INSERT INTO metrics_reports (report_type, report_date, content_json, content_md, created_stime, modified_stime, is_del) VALUES
('weekly', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '{"suggestions":["推广 commit skill 到后端组，使用率提升 40%","关注 Agent 一次通过率下降趋势","数据组建议增加 code-review skill 使用"]}', '## 本周建议\n- 推广 commit skill 到后端组\n- 关注 Agent 通过率\n- 数据组增加 code-review', NOW() - INTERVAL 1 DAY, NOW(), 0),
('monthly', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '{"suggestions":["全团队安装覆盖率已达 83%，建议推动剩余人员","Token 成本月环比下降 12%，继续优化 prompt 策略","Agent loop_count>3 占比 18%，需关注 prompt 质量"]}', '## 月度建议\n- 推动剩余人员安装\n- 优化 prompt 策略降成本\n- 关注 Agent 循环次数', NOW() - INTERVAL 1 DAY, NOW(), 0);
