package com.github.hkocamandev.iot_intrusion_detection.repository;

import com.github.hkocamandev.iot_intrusion_detection.model.NetworkFlow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkFlowRepository extends JpaRepository<NetworkFlow, Long> { }
