
package com.funny.harness.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 */
@RestController
@Slf4j
public class HealthController {
    @GetMapping("/heartbeat")
    public String heartbeat() {
        return "ok";
    }

    @GetMapping("/log")
    public String log() {
        log.info("log info");
        log.warn("log warn");
        log.error("log error");
        return "ok";
    }
}
