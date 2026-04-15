package com.funny.harness.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Agent执行轨迹表
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName("metrics_agent_traces")
public class MetricsAgentTraceDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 追踪ID
     */
    @TableField("trace_id")
    private String traceId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * Agent类型
     */
    @TableField("agent_type")
    private String agentType;

    /**
     * 任务类型
     */
    @TableField("task_type")
    private String taskType;

    /**
     * 任务描述
     */
    @TableField("task_desc")
    private String taskDesc;

    /**
     * 执行状态
     */
    @TableField("status")
    private String status;

    /**
     * 消耗Token总数
     */
    @TableField("total_tokens")
    private Integer totalTokens;

    /**
     * 总耗时(毫秒)
     */
    @TableField("total_duration_ms")
    private Integer totalDurationMs;

    /**
     * 变更文件数
     */
    @TableField("files_changed")
    private Integer filesChanged;

    /**
     * 循环次数
     */
    @TableField("loop_count")
    private Integer loopCount;

    /**
     * 质量评分
     */
    @TableField("quality_score")
    private Integer qualityScore;

    /**
     * 修订次数
     */
    @TableField("revision_count")
    private Integer revisionCount;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    @TableField("created_stime")
    private LocalDateTime createdStime;

    /**
     * 修改时间
     */
    @TableField("modified_stime")
    private LocalDateTime modifiedStime;

    /**
     * 是否删除 0 正常 1 删除
     */
    @TableField("is_del")
    @TableLogic
    private Boolean isDel;
}
