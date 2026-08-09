package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.ChatRequest;
import com.github.hkocamandev.iot_intrusion_detection.dto.ChatResponse;
import com.github.hkocamandev.iot_intrusion_detection.llm.ChatAnswer;
import com.github.hkocamandev.iot_intrusion_detection.llm.NlQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The Boot 4.1 @WebMvcTest slice builds no security filter chain, so this does not test
// authorization — it only populates SecurityContextHolder. Access control is covered by
// SecurityAuthorizationIT.
@WebMvcTest(ChatController.class)
@WithMockUser(roles = "ADMIN")
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NlQueryService nlQueryService;

    @Test
    void answersAndEchoesResolvedProviderModel() throws Exception {
        when(nlQueryService.ask(anyString(), eq("how many DOS alerts?"), any(), any()))
                .thenReturn(new ChatAnswer("There are 2 DOS alerts.", "groq", "openai/gpt-oss-120b"));

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"how many DOS alerts?\",\"provider\":\"groq\",\"model\":\"openai/gpt-oss-120b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("There are 2 DOS alerts."))
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.provider").value("groq"))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returns503WithSessionIdWhenServiceUnavailable() {
        ObjectProvider<NlQueryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        ChatController controller = new ChatController(provider);

        ResponseEntity<ChatResponse> response =
                controller.chat(new ChatRequest("hi", "sess-abc", "groq", "m1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().answer()).contains("disabled");
        assertThat(response.getBody().sessionId()).isEqualTo("sess-abc");
        assertThat(response.getBody().provider()).isNull();
        assertThat(response.getBody().model()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void returns503WhenServiceThrows() {
        NlQueryService throwing = mock(NlQueryService.class);
        when(throwing.ask(anyString(), anyString(), any(), any())).thenThrow(new RuntimeException("timeout"));
        ObjectProvider<NlQueryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(throwing);
        ChatController controller = new ChatController(provider);

        ResponseEntity<ChatResponse> response =
                controller.chat(new ChatRequest("hi", null, null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().answer()).contains("unavailable");
        assertThat(response.getBody().sessionId()).isNotBlank();
    }
}
