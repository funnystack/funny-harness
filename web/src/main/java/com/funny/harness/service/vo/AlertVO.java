package com.funny.harness.service.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 告警记录 VO
 *
 * @author funny2048
 * @since 2026-04-11
 */
@Getter
@Setter
public class AlertVO {

    private Long id;
    private String alertType;
    private String severity;
    private String traceId;
    private String details;
    private Integer notified;
    private LocalDateTime createdStime;
}
