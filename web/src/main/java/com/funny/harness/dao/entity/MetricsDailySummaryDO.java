package com.funny.harness.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 每日指标汇总表
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName("metrics_daily_summary")
public class MetricsDailySummaryDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 汇总日期
     */
    @TableField("summary_date")
    private LocalDate summaryDate;

    /**
     * 日活用户数
     */
    @TableField("dau")
    private Integer dau;

    /**
     * 总会话数
     */
    @TableField("total_sessions")
    private Integer totalSessions;

    /**
     * 总Token消耗
     */
    @TableField("total_tokens")
    private Long totalTokens;

    /**
     * 总耗时(秒)
     */
    @TableField("total_duration_seconds")
    private Long totalDurationSeconds;

    /**
     * 活跃项目数
     */
    @TableField("active_projects")
    private Integer activeProjects;

    /**
     * Agent任务完成数
     */
    @TableField("agent_tasks_completed")
    private Integer agentTasksCompleted;

    /**
     * Agent任务失败数
     */
    @TableField("agent_tasks_failed")
    private Integer agentTasksFailed;

    /**
     * Agent一次通过率
     */
    @TableField("agent_first_pass_rate")
    private BigDecimal agentFirstPassRate;

    /**
     * 每任务平均Token数
     */
    @TableField("avg_tokens_per_task")
    private Integer avgTokensPerTask;

    /**
     * 最常用Skill
     */
    @TableField("top_skill")
    private String topSkill;

    /**
     * 最常用Agent
     */
    @TableField("top_agent")
    private String topAgent;

    /**
     * Skill调用次数
     */
    @TableField("skill_call_count")
    private Integer skillCallCount;

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
