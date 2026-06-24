package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.StatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aggregate alarm istatistikleri. */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final AlertStatsService statsService;

    public StatsController(AlertStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsResponse stats() {
        return statsService.buildStats();
    }
}
