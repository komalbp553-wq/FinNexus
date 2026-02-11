package com.finnexus.service;

import com.finnexus.domain.dto.RoboStrategyRequest;
import com.finnexus.domain.dto.RoboStrategyResponse;

import java.util.List;

public interface RoboStrategyService {
    RoboStrategyResponse create(RoboStrategyRequest request);
    RoboStrategyResponse update(Long id, RoboStrategyRequest request);
    void delete(Long id);
    List<RoboStrategyResponse> list();
    void runStrategies();
}
