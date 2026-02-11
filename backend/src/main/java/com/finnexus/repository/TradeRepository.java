package com.finnexus.repository;

import com.finnexus.domain.entity.Trade;
import com.finnexus.domain.enums.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserIdOrderByOpenedAtDesc(Long userId);
    List<Trade> findByUserIdAndStatus(Long userId, TradeStatus status);
}
