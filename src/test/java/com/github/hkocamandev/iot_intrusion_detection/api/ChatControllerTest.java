package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.ChatRequest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    void answersQuestionAndGeneratesSessionIdWhenMissing() throws Exception {
        when(nlQueryService.ask(anyString(), eq("how many DOS alerts?")))
                .thenReturn("There are 2 DOS alerts.");

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"how many DOS alerts?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("There are 2 DOS alerts."))
                .andExpect(jsonPath("$.sessionId").isNotEmpty());
    }

    @Test
    void echoesClientSessionId() throws Exception {
        when(nlQueryService.ask(eq("sess-123"), eq("hi"))).thenReturn("hello");

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"hi\",\"sessionId\":\"sess-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-123"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returns503WithSessionIdWhenServiceUnavailable() {
        ObjectProvider<NlQueryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        ChatController controller = new ChatController(provider);

        ResponseEntity<ChatResponse> response =
                controller.chat(new ChatRequest("hi", "sess-abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().answer()).contains("disabled");
        assertThat(response.getBody().sessionId()).isEqualTo("sess-abc");
    }

    @Test
    @SuppressWarnings("unchecked")
    void returns503WhenServiceThrows() {
        NlQueryService throwingService = mock(NlQueryService.class);
        when(throwingService.ask(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));
        ObjectProvider<NlQueryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(throwingService);
        ChatController controller = new ChatController(provider);

        ResponseEntity<ChatResponse> response =
                controller.chat(new ChatRequest("hi", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().answer()).contains("unavailable");
        assertThat(response.getBody().sessionId()).isNotBlank();
    }
}
