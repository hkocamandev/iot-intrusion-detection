package com.github.hkocamandev.iot_intrusion_detection.dto;

import java.util.List;

/** One selectable provider and its curated model list. */
public record ProviderOption(String id, List<String> models) { }
