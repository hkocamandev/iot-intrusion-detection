package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.LlmOptions;
import com.github.hkocamandev.iot_intrusion_detection.dto.ProviderOption;
import com.github.hkocamandev.iot_intrusion_detection.llm.LlmOptionsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LlmOptionsController.class)
class LlmOptionsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LlmOptionsService optionsService;

    @Test
    void returnsAvailableProvidersAndDefault() throws Exception {
        when(optionsService.options()).thenReturn(new LlmOptions(
                List.of(new ProviderOption("groq", List.of("llama-3.3-70b-versatile", "openai/gpt-oss-120b"))),
                "groq", "llama-3.3-70b-versatile"));

        mockMvc.perform(get("/api/llm/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[0].id").value("groq"))
                .andExpect(jsonPath("$.providers[0].models[1]").value("openai/gpt-oss-120b"))
                .andExpect(jsonPath("$.defaultProvider").value("groq"))
                .andExpect(jsonPath("$.defaultModel").value("llama-3.3-70b-versatile"));
    }
}
