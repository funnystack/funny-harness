-- ============================================================
-- 度量体系 DDL（8 张表）
-- 对应设计文档：design.md D4.0 ~ D4.7
-- ============================================================

-- D4.0 原始事件表（上报入口，所有数据先进这张表）
CREATE TABLE IF NOT EXISTS metrics_raw_events (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_id VARCHAR(64) DEFAULT NULL COMMENT '上报批次 ID（来自客户端 X-Batch-Id）',
    client_id VARCHAR(64) NOT NULL COMMENT '客户端标识（SHA256($USER@hostname)）',
    client_ip VARCHAR(45) DEFAULT NULL COMMENT '客户端 IP（服务端从 HTTP 请求获取，支持 IPv6）',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型：session_start/tool_use/capability_use/session_stop',
    raw_payload JSON NOT NULL COMMENT '原始事件 JSON 报文，完整保留',
    processed TINYINT(1) DEFAULT 0 COMMENT '处理状态：0=未处理 1=已处理 2=处理失败',
    process_error VARCHAR(512) DEFAULT NULL COMMENT '处理失败原因',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
    created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_processed (processed, received_at),
    KEY idx_client (client_id, received_at),
    KEY idx_type (event_type, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原始事件表';

-- D4.1 用户表
CREATE TABLE IF NOT EXISTS metrics_users (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id VARCHAR(64) NOT NULL COMMENT '用户唯一标识（SHA256($USER@hostname)）',
    username VARCHAR(128) NOT NULL COMMENT '系统用户名（$USER）',
    username_hash VARCHAR(64) NOT NULL COMMENT '用户名 SHA256，脱敏查询用',
    hostname VARCHAR(128) NOT NULL COMMENT '机器主机名',
    team VARCHAR(64) DEFAULT NULL COMMENT '所属团队',
    created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_team (team),
    KEY idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='度量用户表';

-- D4.2 会话表（第一阶段核心）
CREATE TABLE IF NOT EXISTS metrics_sessions (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id VARCHAR(128) NOT NULL COMMENT '会话唯一标识',
    user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
    project_name VARCHAR(128) NOT NULL COMMENT '项目名称（仓库名）',
    started_at DATETIME NOT NULL COMMENT '会话开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '会话结束时间',
    duration_seconds INT DEFAULT 0 COMMENT '会话时长（秒）',
    total_tokens INT DEFAULT 0 COMMENT 'Token 总消耗',
    tool_read_count INT DEFAULT 0 COMMENT 'Read 工具调用次数',
    tool_edit_count INT DEFAULT 0 COMMENT 'Edit 工具调用次数',
    tool_bash_count INT DEFAULT 0 COMMENT 'Bash 工具调用次数',
    tool_grep_count INT DEFAULT 0 COMMENT 'Grep 工具调用次数',
    created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_started (user_id, started_at),
    KEY idx_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='度量会话表';

-- D4.3 能力调用记录表（核心创新）
CREATE TABLE IF NOT EXISTS metrics_skill_usage (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id VARCHAR(128) NOT NULL COMMENT '关联会话',
    user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
    project_name VARCHAR(128) NOT NULL COMMENT '项目名称',
    capability_type VARCHAR(32) NOT NULL COMMENT '能力类型：skill/agent/command/slash_command',
    capability_name VARCHAR(128) NOT NULL COMMENT '能力名称',
    triggered_at DATETIME NOT NULL COMMENT '触发时间',
    tokens_consumed INT DEFAULT 0 COMMENT '消耗 Token 数',
    duration_ms INT DEFAULT 0 COMMENT '执行耗时（毫秒）',
    result_status VARCHAR(16) DEFAULT 'success' COMMENT '结果状态：success/failed/timeout',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '关联 agent_traces（可选）',
    created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_capability (capability_type, capability_name),
    KEY idx_user_project (user_id, project_name),
    KEY idx_triggered (triggered_at),
    KEY idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能力调用记录表';

-- D4.4 Agent Trace 表（第二阶段核心）
CREATE TABLE IF NOT EXISTS metrics_agent_traces (
    id BIGINT(20) NOT NULL AUTO_INCREMENT AUTO_INCREMENT,
    trace_id VARCHAR(128) NOT NULL COMMENT 'Trace 唯一标识',
    session_id VARCHAR(128) NOT NULL COMMENT '关联会话',
    agent_type VARCHAR(32) NOT NULL COMMENT 'Agent 类型：研发/质量/安全/测试',
    task_type VARCHAR(32) NOT NULL COMMENT '任务类型：编码/评审/测试/文档',
    task_desc TEXT COMMENT '任务描述摘要',
    status VARCHAR(16) NOT NULL DEFAULT 'running' COMMENT '状态：running/completed/failed/timeout',
    total_tokens INT DEFAULT 0 COMMENT '总 Token 消耗',
    total_duration_ms INT DEFAULT 0 COMMENT '总耗时（毫秒）',
    files_changed INT DEFAULT 0 COMMENT '变更文件数',
    loop_count INT DEFAULT 0 COMMENT '修正循环次数',
    quality_score INT DEFAULT NULL COMMENT '质量评分（人工，1-100）',
    revision_count INT DEFAULT 0 COMMENT '人工修正次数',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trace_id (trace_id),
    KEY idx_agent_type (agent_type),
    KEY idx_task_type (task_type),
    KEY idx_started (started_at),
    KEY idx_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 执行追踪表';

-- D4.5 每日汇总表（看板直接读）
CREATE TABLE IF NOT EXISTS metrics_daily_summary (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    summary_date DATE NOT NULL COMMENT '汇总日期',
    dau INT NOT NULL DEFAULT 0 COMMENT '日活跃用户数',
    total_sessions INT DEFAULT 0 COMMENT '总会话数',
    total_tokens BIGINT DEFAULT 0 COMMENT '总 Token 消耗',
    total_duration_seconds BIGINT DEFAULT 0 COMMENT '总时长（秒）',
    active_projects INT DEFAULT 0 COMMENT '活跃项目数',
    agent_tasks_completed INT DEFAULT 0 COMMENT 'Agent 任务完成数',
    agent_tasks_failed INT DEFAULT 0 COMMENT 'Agent 任务失败数',
    agent_first_pass_rate DECIMAL(5,2) DEFAULT NULL COMMENT 'Agent 一次通过率',
    avg_tokens_per_task INT DEFAULT NULL COMMENT '平均单任务 Token',
    top_skill VARCHAR(64) DEFAULT NULL COMMENT '当日最热 Skill',
    top_agent VARCHAR(64) DEFAULT NULL COMMENT '当日最热 Agent',
    skill_call_count INT DEFAULT 0 COMMENT 'Skill 总调用次数',
    created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_date (summary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日汇总表';

-- D4.6 告警表
CREATE TABLE IF NOT EXISTS metrics_alerts (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    alert_type VARCHAR(32) NOT NULL COMMENT '告警类型：TOKEN_OVERUSE/LOOP_EXCEEDED/DAU_DROP/FAILURE_RATE/AGENT_STUCK',
    severity VARCHAR(16) NOT NULL DEFAULT 'warning' COMMENT '严重级别：info/warning/critical',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '关联 Trace（可选）',
    details JSON DEFAULT NULL COMMENT '告警详情（JSON）',
    notified TINYINT(1) DEFAULT 0 COMMENT '是否已通知：0=未通知 1=已通知',
    created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_type_time (alert_type, created_stime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='度量告警表';

-- D4.7 报告表
CREATE TABLE IF NOT EXISTS metrics_reports (
    id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    report_type VARCHAR(16) NOT NULL COMMENT '报告类型：daily/weekly/monthly',
    report_date DATE NOT NULL COMMENT '报告日期',
    content_json JSON NOT NULL COMMENT '结构化报告内容（JSON）',
    content_md TEXT COMMENT 'Markdown 文本版本（IM 推送用）',
    created_stime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_stime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_del TINYINT(1) DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    KEY idx_type_date (report_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='度量报告表';
