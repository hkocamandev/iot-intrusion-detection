package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.LlmOptions;
import com.github.hkocamandev.iot_intrusion_detection.llm.LlmOptionsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reports which chat providers/models the UI may offer. Empty when the LLM is disabled. */
@RestController
@RequestMapping("/api/llm")
public class LlmOptionsController {

    private final ObjectProvider<LlmOptionsService> optionsService;

    public LlmOptionsController(ObjectProvider<LlmOptionsService> optionsService) {
        this.optionsService = optionsService;
    }

    @GetMapping("/options")
    public LlmOptions options() {
        LlmOptionsService service = optionsService.getIfAvailable();
        if (service == null) {
            return new LlmOptions(List.of(), null, null);
        }
        return service.options();
    }
}
