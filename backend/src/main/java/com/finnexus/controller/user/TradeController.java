package com.finnexus.controller.user;

import com.finnexus.domain.dto.CloseTradeRequest;
import com.finnexus.domain.dto.TradeResponse;
import com.finnexus.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TradeController {
    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    public ResponseEntity<List<TradeResponse>> list() {
        return ResponseEntity.ok(tradeService.getTrades());
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<TradeResponse> close(@PathVariable Long id, @Valid @RequestBody CloseTradeRequest request) {
        return ResponseEntity.ok(tradeService.closeTrade(id, request));
    }
}
