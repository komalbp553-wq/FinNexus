package com.finnexus.controller.user;

import com.finnexus.domain.dto.CandleDto;
import com.finnexus.domain.dto.QuoteDto;
import com.finnexus.domain.enums.Timeframe;
import com.finnexus.service.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final MarketDataService marketDataService;

    public MarketController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/candles")
    public ResponseEntity<List<CandleDto>> candles(@RequestParam String symbol,
                                                   @RequestParam(defaultValue = "M15") String timeframe,
                                                   @RequestParam(defaultValue = "60") int limit) {
        return ResponseEntity.ok(marketDataService.getCandles(symbol, Timeframe.valueOf(timeframe.toUpperCase()), limit));
    }

    @GetMapping("/quote")
    public ResponseEntity<QuoteDto> quote(@RequestParam String symbol) {
        return ResponseEntity.ok(marketDataService.getQuote(symbol));
    }

    @GetMapping("/quotes")
    public ResponseEntity<List<QuoteDto>> quotes(@RequestParam String symbols) {
        List<String> list = Stream.of(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return ResponseEntity.ok(marketDataService.getQuotes(list));
    }
}
