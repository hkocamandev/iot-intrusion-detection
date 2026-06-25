package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.ChatRequest;
import com.github.hkocamandev.iot_intrusion_detection.dto.ChatResponse;
import com.github.hkocamandev.iot_intrusion_detection.llm.NlQueryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Natural-language chat over alerts. Returns 503 when the LLM is disabled or unreachable. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ObjectProvider<NlQueryService> nlQueryService;

    public ChatController(ObjectProvider<NlQueryService> nlQueryService) {
        this.nlQueryService = nlQueryService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        NlQueryService service = nlQueryService.getIfAvailable();
        if (service == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatResponse(
                            "LLM chat is disabled. Set app.llm.enabled=true and ANTHROPIC_API_KEY."));
        }
        try {
            return ResponseEntity.ok(new ChatResponse(service.ask(request.question())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatResponse("LLM chat failed: " + e.getMessage()));
        }
    }
}
