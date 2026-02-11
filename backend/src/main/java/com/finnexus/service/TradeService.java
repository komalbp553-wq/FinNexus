package com.finnexus.service;

import com.finnexus.domain.dto.CloseTradeRequest;
import com.finnexus.domain.dto.TradeResponse;

import java.util.List;

public interface TradeService {
    List<TradeResponse> getTrades();
    TradeResponse closeTrade(Long tradeId, CloseTradeRequest request);
}
