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
 * 指标会话表
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName("metrics_sessions")
public class MetricsSessionDO {

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
     * 会话开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 会话结束时间
     */
    @TableField("ended_at")
    private LocalDateTime endedAt;

    /**
     * 持续时长（秒）
     */
    @TableField("duration_seconds")
    private Integer durationSeconds;

    /**
     * 总Token消耗数
     */
    @TableField("total_tokens")
    private Integer totalTokens;

    /**
     * 工具-读取次数
     */
    @TableField("tool_read_count")
    private Integer toolReadCount;

    /**
     * 工具-编辑次数
     */
    @TableField("tool_edit_count")
    private Integer toolEditCount;

    /**
     * 工具-Bash执行次数
     */
    @TableField("tool_bash_count")
    private Integer toolBashCount;

    /**
     * 工具-搜索次数
     */
    @TableField("tool_grep_count")
    private Integer toolGrepCount;

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
