package com.finnexus.controller.user;

import com.finnexus.domain.dto.WalletResponse;
import com.finnexus.domain.dto.WalletTxRequest;
import com.finnexus.domain.dto.WalletTxResponse;
import com.finnexus.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> wallet() {
        return ResponseEntity.ok(walletService.getWallet());
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(@Valid @RequestBody WalletTxRequest request) {
        return ResponseEntity.ok(walletService.deposit(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(@Valid @RequestBody WalletTxRequest request) {
        return ResponseEntity.ok(walletService.withdraw(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTxResponse>> transactions() {
        return ResponseEntity.ok(walletService.getTransactions());
    }
}
