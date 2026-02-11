package com.finnexus.controller.user;

import com.finnexus.domain.dto.PnlResponse;
import com.finnexus.service.PnlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final PnlService pnlService;

    public AnalyticsController(PnlService pnlService) {
        this.pnlService = pnlService;
    }

    @GetMapping("/pnl")
    public ResponseEntity<PnlResponse> pnl() {
        return ResponseEntity.ok(pnlService.getPnlSummary());
    }
}
