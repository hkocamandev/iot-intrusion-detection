package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.model.*;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AlertRepository repository;

    private Alert alert() {
        return new Alert(Instant.parse("2026-06-20T10:00:00Z"), "src-1", DetectionSource.RULE,
                AttackType.DOS, Severity.HIGH, 5.0, "dos-high-packet-rate", "tcp", "http", "x");
    }

    @Test
    void returnsRecentAlerts() throws Exception {
        when(repository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(alert()));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceId").value("src-1"))
                .andExpect(jsonPath("$[0].attackType").value("DOS"))
                .andExpect(jsonPath("$[0].detectionSource").value("RULE"));
    }

    @Test
    void severityParamUsesFilteredQuery() throws Exception {
        when(repository.findTop100BySeverityOrderByCreatedAtDesc(eq(Severity.HIGH)))
                .thenReturn(List.of(alert()));

        mockMvc.perform(get("/api/alerts").param("severity", "HIGH"))
                .andExpect(status().isOk());

        verify(repository).findTop100BySeverityOrderByCreatedAtDesc(Severity.HIGH);
    }
}
