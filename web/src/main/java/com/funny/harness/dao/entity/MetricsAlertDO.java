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
 * 指标告警表
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName("metrics_alerts")
public class MetricsAlertDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 告警类型
     */
    @TableField("alert_type")
    private String alertType;

    /**
     * 严重程度
     */
    @TableField("severity")
    private String severity;

    /**
     * 关联追踪ID
     */
    @TableField("trace_id")
    private String traceId;

    /**
     * 告警详情
     */
    @TableField("details")
    private String details;

    /**
     * 是否已通知 0 未通知 1 已通知
     */
    @TableField("notified")
    private Integer notified;

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
