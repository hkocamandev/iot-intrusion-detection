package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.ChatResponse;
import com.github.hkocamandev.iot_intrusion_detection.llm.NlQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NlQueryService nlQueryService;

    @Test
    void answersQuestion() throws Exception {
        when(nlQueryService.ask("how many DOS alerts?")).thenReturn("There are 2 DOS alerts.");

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"how many DOS alerts?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("There are 2 DOS alerts."));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returns503WhenServiceUnavailable() {
        ObjectProvider<NlQueryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        ChatController controller = new ChatController(provider);

        ResponseEntity<ChatResponse> response =
                controller.chat(new com.github.hkocamandev.iot_intrusion_detection.dto.ChatRequest("hi"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().answer()).contains("disabled");
    }
}
