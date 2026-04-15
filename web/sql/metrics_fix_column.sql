-- 修复 metrics 表的 created_stime 列名（creates_stime → created_stime）
-- 原因：DDL 初版误写为 creates_stime，Entity 映射为 created_stime

ALTER TABLE metrics_raw_events CHANGE creates_stime created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE metrics_users CHANGE creates_stime created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE metrics_sessions CHANGE creates_stime created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE metrics_skill_usage CHANGE creates_stime created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE metrics_agent_traces CHANGE creates_stime created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE metrics_daily_summary CHANGE creates_stime created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE metrics_alerts CHANGE creates_stime created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE metrics_reports CHANGE creates_stime created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
