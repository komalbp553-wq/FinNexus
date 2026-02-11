package com.finnexus.repository;

import com.finnexus.domain.entity.RoboStrategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoboStrategyRepository extends JpaRepository<RoboStrategy, Long> {
    List<RoboStrategy> findByUserId(Long userId);
}
