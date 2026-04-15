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
 * 指标用户表
 * </p>
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
@TableName("metrics_users")
public class MetricsUserDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 用户名哈希
     */
    @TableField("username_hash")
    private String usernameHash;

    /**
     * 主机名
     */
    @TableField("hostname")
    private String hostname;

    /**
     * 团队
     */
    @TableField("team")
    private String team;

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
