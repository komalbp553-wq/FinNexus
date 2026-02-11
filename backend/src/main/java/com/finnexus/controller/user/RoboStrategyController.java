package com.finnexus.controller.user;

import com.finnexus.domain.dto.RoboStrategyRequest;
import com.finnexus.domain.dto.RoboStrategyResponse;
import com.finnexus.service.RoboStrategyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
public class RoboStrategyController {
    private final RoboStrategyService strategyService;

    public RoboStrategyController(RoboStrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @PostMapping
    public ResponseEntity<RoboStrategyResponse> create(@Valid @RequestBody RoboStrategyRequest request) {
        return ResponseEntity.ok(strategyService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoboStrategyResponse> update(@PathVariable Long id, @Valid @RequestBody RoboStrategyRequest request) {
        return ResponseEntity.ok(strategyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        strategyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<RoboStrategyResponse>> list() {
        return ResponseEntity.ok(strategyService.list());
    }

    @PostMapping("/run")
    public ResponseEntity<Void> run() {
        strategyService.runStrategies();
        return ResponseEntity.ok().build();
    }
}
