-- 清空 mock 数据后重新插入
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE metrics_reports;
TRUNCATE TABLE metrics_alerts;
TRUNCATE TABLE metrics_skill_usage;
TRUNCATE TABLE metrics_agent_traces;
TRUNCATE TABLE metrics_sessions;
TRUNCATE TABLE metrics_daily_summary;
TRUNCATE TABLE metrics_users;
SET FOREIGN_KEY_CHECKS = 1;

-- 用户数据
INSERT INTO metrics_users (user_id, username, username_hash, hostname, team, created_stime, modified_stime, is_del) VALUES
('a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', '张三', 'hash_zhangsan', 'mac-zhangsan', '平台组', NOW() - INTERVAL 30 DAY, NOW(), 0),
('b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', '李四', 'hash_lisi', 'mac-lisi', '后端组', NOW() - INTERVAL 25 DAY, NOW(), 0),
('c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3', '王五', 'hash_wangwu', 'mac-wangwu', '前端组', NOW() - INTERVAL 20 DAY, NOW(), 0),
('d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4', '赵六', 'hash_zhaoliu', 'mac-zhaoliu', 'AI组', NOW() - INTERVAL 15 DAY, NOW(), 0),
('e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5', '孙七', 'hash_sunqi', 'mac-sunqi', '数据组', NOW() - INTERVAL 10 DAY, NOW(), 0),
('f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6', '周八', 'hash_zhouba', 'mac-zhouba', '平台组', NOW() - INTERVAL 5 DAY, NOW(), 0);

-- 每日汇总（30天）
INSERT INTO metrics_daily_summary (summary_date, dau, total_sessions, total_tokens, total_duration_seconds, active_projects, agent_tasks_completed, agent_tasks_failed, agent_first_pass_rate, avg_tokens_per_task, top_skill, top_agent, skill_call_count, created_stime, modified_stime, is_del)
SELECT
    d.date, FLOOR(3 + RAND()*5), FLOOR(8+RAND()*15), FLOOR(50000+RAND()*100000),
    FLOOR(3000+RAND()*5000), FLOOR(2+RAND()*4), FLOOR(5+RAND()*10), FLOOR(RAND()*3),
    ROUND(65+RAND()*25,2), FLOOR(3000+RAND()*5000),
    ELT(FLOOR(1+RAND()*3),'commit','review-pr','code-review'),
    'claude-code', FLOOR(15+RAND()*30), d.date, d.date, 0
FROM (SELECT DATE(DATE_SUB(CURDATE(),INTERVAL n DAY)) AS date
      FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
            UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
            UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29) nums) d;

-- 会话数据（7天×5条）
INSERT INTO metrics_sessions (session_id, user_id, project_name, started_at, ended_at, duration_seconds, total_tokens, tool_read_count, tool_edit_count, tool_bash_count, tool_grep_count, created_stime, modified_stime, is_del)
SELECT CONCAT('sess_',d.date,'_',n.num),
    ELT(1+FLOOR(RAND()*6),'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2','b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2','c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3','d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4','e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5','f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6'),
    ELT(1+FLOOR(RAND()*5),'funny-harness','data-pipeline','web-admin','api-gateway','ai-service'),
    TIMESTAMP(d.date,MAKETIME(FLOOR(8+RAND()*12),FLOOR(RAND()*60),0)),
    TIMESTAMP(d.date,MAKETIME(FLOOR(8+RAND()*12),FLOOR(RAND()*60),FLOOR(RAND()*60))),
    FLOOR(300+RAND()*2000), FLOOR(2000+RAND()*15000), FLOOR(5+RAND()*20), FLOOR(3+RAND()*15), FLOOR(2+RAND()*10), FLOOR(1+RAND()*8),
    d.date, d.date, 0
FROM (SELECT DATE(DATE_SUB(CURDATE(),INTERVAL n DAY)) AS date FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days) d
CROSS JOIN (SELECT 1 num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) n;

-- Agent Trace（30天×3条）
INSERT INTO metrics_agent_traces (trace_id, session_id, agent_type, task_type, task_desc, status, total_tokens, total_duration_ms, files_changed, loop_count, quality_score, revision_count, started_at, completed_at, created_stime, modified_stime, is_del)
SELECT CONCAT('T-',d.date,'-',n.num), CONCAT('sess_',d.date,'_',n.num),
    ELT(1+FLOOR(RAND()*4),'claude-code','plan-agent','code-reviewer','architect'),
    ELT(1+FLOOR(RAND()*5),'feature','bugfix','refactor','test','docs'),
    ELT(1+FLOOR(RAND()*5),'新增用户认证模块','修复登录超时问题','重构数据访问层','编写单元测试','更新API文档'),
    IF(RAND()>0.15,'completed','failed'),
    FLOOR(3000+RAND()*20000), FLOOR(5000+RAND()*60000), FLOOR(1+RAND()*8),
    IF(RAND()>0.3,1,FLOOR(2+RAND()*4)), FLOOR(70+RAND()*30), FLOOR(RAND()*3),
    TIMESTAMP(d.date,MAKETIME(FLOOR(9+RAND()*10),FLOOR(RAND()*60),0)),
    TIMESTAMP(d.date,MAKETIME(FLOOR(10+RAND()*10),FLOOR(RAND()*60),0)),
    d.date, d.date, 0
FROM (SELECT DATE(DATE_SUB(CURDATE(),INTERVAL n DAY)) AS date FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
      UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
      UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29) days) d
CROSS JOIN (SELECT 1 num UNION SELECT 2 UNION SELECT 3) n;

-- Skill 使用（7天×4条）
INSERT INTO metrics_skill_usage (session_id, user_id, project_name, capability_type, capability_name, triggered_at, tokens_consumed, duration_ms, result_status, trace_id, created_stime, modified_stime, is_del)
SELECT CONCAT('sess_',d.date,'_',n.num),
    ELT(1+FLOOR(RAND()*6),'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2','b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2','c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3','d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4','e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5','f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6'),
    ELT(1+FLOOR(RAND()*5),'funny-harness','data-pipeline','web-admin','api-gateway','ai-service'),
    ELT(1+FLOOR(RAND()*4),'skill','agent','command','slash_command'),
    ELT(1+FLOOR(RAND()*8),'commit','review-pr','code-review','test','deploy','format','search','refactor'),
    TIMESTAMP(d.date,MAKETIME(FLOOR(9+RAND()*10),FLOOR(RAND()*60),0)),
    FLOOR(500+RAND()*5000), FLOOR(1000+RAND()*30000), IF(RAND()>0.1,'success','failed'),
    CONCAT('T-',d.date,'-',n.num), d.date, d.date, 0
FROM (SELECT DATE(DATE_SUB(CURDATE(),INTERVAL n DAY)) AS date FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) days) d
CROSS JOIN (SELECT 1 num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) n;

-- 告警
INSERT INTO metrics_alerts (alert_type, severity, trace_id, details, notified, created_stime, modified_stime, is_del) VALUES
('LOOP_EXCEEDED','warning','T-2026-04-10-2','{"actual":4,"threshold":3}',1,NOW()-INTERVAL 1 DAY,NOW(),0),
('TOKEN_OVERUSE','critical','T-2026-04-09-1','{"actual":85000,"avg":12000,"ratio":7.1}',1,NOW()-INTERVAL 2 DAY,NOW(),0),
('DAU_DROP','critical',NULL,'{"today":2,"yesterday":6,"ratio":0.33}',1,NOW()-INTERVAL 3 DAY,NOW(),0),
('FAILURE_RATE','warning',NULL,'{"weekRate":0.18,"threshold":0.15}',0,NOW()-INTERVAL 2 DAY,NOW(),0),
('AGENT_STUCK','critical','T-2026-04-11-1','{"minutesElapsed":8,"threshold":5}',0,NOW(),NOW(),0),
('LOOP_EXCEEDED','warning','T-2026-04-08-3','{"actual":5,"threshold":3}',1,NOW()-INTERVAL 4 DAY,NOW(),0);

-- 报告
INSERT INTO metrics_reports (report_type, report_date, content_json, content_md, created_stime, modified_stime, is_del) VALUES
('weekly',DATE_SUB(CURDATE(),INTERVAL 1 DAY),'{"suggestions":["推广 commit skill 到后端组，使用率提升 40%","关注 Agent 一次通过率下降趋势","数据组建议增加 code-review skill 使用"]}','## 本周建议\n- 推广 commit skill\n- 关注 Agent 通过率',NOW()-INTERVAL 1 DAY,NOW(),0),
('monthly',DATE_SUB(CURDATE(),INTERVAL 1 DAY),'{"suggestions":["全团队安装覆盖率已达 83%","Token 成本月环比下降 12%","Agent loop_count>3 占比 18%"]}','## 月度建议\n- 推动安装覆盖\n- 优化 prompt 降成本',NOW()-INTERVAL 1 DAY,NOW(),0);
