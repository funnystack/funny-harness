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
 * 指标技能使用记录表
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName("metrics_skill_usage")
public class MetricsSkillUsageDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 项目名称
     */
    @TableField("project_name")
    private String projectName;

    /**
     * 能力类型
     */
    @TableField("capability_type")
    private String capabilityType;

    /**
     * 能力名称
     */
    @TableField("capability_name")
    private String capabilityName;

    /**
     * 触发时间
     */
    @TableField("triggered_at")
    private LocalDateTime triggeredAt;

    /**
     * Token消耗数
     */
    @TableField("tokens_consumed")
    private Integer tokensConsumed;

    /**
     * 执行耗时（毫秒）
     */
    @TableField("duration_ms")
    private Integer durationMs;

    /**
     * 执行结果状态
     */
    @TableField("result_status")
    private String resultStatus;

    /**
     * 追踪ID
     */
    @TableField("trace_id")
    private String traceId;

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
