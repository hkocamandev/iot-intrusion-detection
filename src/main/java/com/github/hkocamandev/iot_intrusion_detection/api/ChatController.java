package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.ChatRequest;
import com.github.hkocamandev.iot_intrusion_detection.dto.ChatResponse;
import com.github.hkocamandev.iot_intrusion_detection.llm.NlQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Natural-language chat over alerts. Returns 503 when the LLM is disabled or unreachable. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ObjectProvider<NlQueryService> nlQueryService;

    public ChatController(ObjectProvider<NlQueryService> nlQueryService) {
        this.nlQueryService = nlQueryService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? UUID.randomUUID().toString()
                : request.sessionId();
        NlQueryService service = nlQueryService.getIfAvailable();
        if (service == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatResponse(
                            "LLM chat is disabled. Set app.llm.enabled=true and the selected provider's API key.",
                            sessionId, null, null));
        }
        try {
            var answer = service.ask(sessionId, request.question(), request.provider(), request.model());
            return ResponseEntity.ok(
                    new ChatResponse(answer.answer(), sessionId, answer.provider(), answer.model()));
        } catch (Exception e) {
            log.warn("LLM chat failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ChatResponse("LLM chat temporarily unavailable.",
                            sessionId, request.provider(), request.model()));
        }
    }
}
