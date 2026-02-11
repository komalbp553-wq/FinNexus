package com.finnexus.config;

import com.finnexus.domain.entity.Order;
import com.finnexus.domain.entity.Trade;
import com.finnexus.domain.entity.User;
import com.finnexus.domain.entity.Wallet;
import com.finnexus.domain.enums.OrderSide;
import com.finnexus.domain.enums.OrderStatus;
import com.finnexus.domain.enums.OrderType;
import com.finnexus.domain.enums.Role;
import com.finnexus.domain.enums.TradeStatus;
import com.finnexus.repository.OrderRepository;
import com.finnexus.repository.TradeRepository;
import com.finnexus.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, OrderRepository orderRepository,
                           TradeRepository tradeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@finnexus.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        Wallet adminWallet = new Wallet();
        adminWallet.setUser(admin);
        adminWallet.setBalance(new BigDecimal("50000.00"));
        adminWallet.setEquity(new BigDecimal("50000.00"));
        admin.setWallet(adminWallet);
        userRepository.save(admin);

        User demo = new User();
        demo.setUsername("demo");
        demo.setEmail("demo@finnexus.com");
        demo.setPassword(passwordEncoder.encode("demo123"));
        demo.setRole(Role.USER);
        demo.setEnabled(true);
        Wallet demoWallet = new Wallet();
        demoWallet.setUser(demo);
        demoWallet.setBalance(new BigDecimal("15000.00"));
        demoWallet.setEquity(new BigDecimal("15000.00"));
        demo.setWallet(demoWallet);
        userRepository.save(demo);

        Order order = new Order();
        order.setUser(demo);
        order.setSymbol("EURUSD");
        order.setSide(OrderSide.BUY);
        order.setType(OrderType.MARKET);
        order.setQuantity(new BigDecimal("1.0"));
        order.setStatus(OrderStatus.EXECUTED);
        order.setExecutedPrice(new BigDecimal("1.0820"));
        order.setExecutedAt(Instant.now());
        orderRepository.save(order);

        Trade trade = new Trade();
        trade.setUser(demo);
        trade.setOrder(order);
        trade.setSymbol("EURUSD");
        trade.setSide(OrderSide.BUY);
        trade.setEntryPrice(new BigDecimal("1.0820"));
        trade.setQuantity(new BigDecimal("1.0"));
        trade.setStatus(TradeStatus.OPEN);
        trade.setOpenedAt(Instant.now());
        tradeRepository.save(trade);
    }
}
