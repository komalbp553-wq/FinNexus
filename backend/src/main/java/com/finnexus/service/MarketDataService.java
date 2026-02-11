package com.finnexus.service;

import com.finnexus.domain.dto.CandleDto;
import com.finnexus.domain.dto.QuoteDto;
import com.finnexus.domain.enums.Timeframe;

import java.util.List;

public interface MarketDataService {
    List<CandleDto> getCandles(String symbol, Timeframe timeframe, int limit);
    QuoteDto getQuote(String symbol);
    List<QuoteDto> getQuotes(List<String> symbols);
}
