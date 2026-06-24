package com.github.hkocamandev.iot_intrusion_detection.api;

import com.github.hkocamandev.iot_intrusion_detection.dto.AlertView;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import com.github.hkocamandev.iot_intrusion_detection.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Son alarmları döner (en yeni önce, son 100), opsiyonel severity filtresiyle. */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository repository;

    public AlertController(AlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AlertView> recent(@RequestParam(required = false) Severity severity) {
        List<com.github.hkocamandev.iot_intrusion_detection.model.Alert> alerts =
                (severity == null)
                        ? repository.findTop100ByOrderByCreatedAtDesc()
                        : repository.findTop100BySeverityOrderByCreatedAtDesc(severity);
        return alerts.stream().map(AlertView::from).toList();
    }
}
