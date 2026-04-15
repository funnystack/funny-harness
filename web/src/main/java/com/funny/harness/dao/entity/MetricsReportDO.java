package com.funny.harness.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 指标报告表
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName("metrics_reports")
public class MetricsReportDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 报告类型
     */
    @TableField("report_type")
    private String reportType;

    /**
     * 报告日期
     */
    @TableField("report_date")
    private LocalDate reportDate;

    /**
     * 报告内容(JSON格式)
     */
    @TableField("content_json")
    private String contentJson;

    /**
     * 报告内容(Markdown格式)
     */
    @TableField("content_md")
    private String contentMd;

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
