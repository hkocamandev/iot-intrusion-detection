package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.StatsResponse;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AlertStatsService statsService;
    @MockitoBean AlertRepository alertRepository;

    @Test
    void returnsStats() throws Exception {
        when(statsService.buildStats()).thenReturn(new StatsResponse(
                3L, Map.of("DOS", 2L, "RECON", 1L), Map.of("HIGH", 2L, "MEDIUM", 1L),
                Map.of("RULE", 2L, "ML", 1L)));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.byAttackType.DOS").value(2))
                .andExpect(jsonPath("$.byDetectionSource.ML").value(1));
    }
}
