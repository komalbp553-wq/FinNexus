package com.finnexus.service.impl;

import com.finnexus.domain.dto.WalletResponse;
import com.finnexus.domain.dto.WalletTxRequest;
import com.finnexus.domain.dto.WalletTxResponse;
import com.finnexus.domain.entity.User;
import com.finnexus.domain.entity.Wallet;
import com.finnexus.domain.entity.WalletTransaction;
import com.finnexus.domain.enums.WalletTxType;
import com.finnexus.exception.BadRequestException;
import com.finnexus.exception.NotFoundException;
import com.finnexus.repository.UserRepository;
import com.finnexus.repository.WalletRepository;
import com.finnexus.repository.WalletTransactionRepository;
import com.finnexus.service.WalletService;
import com.finnexus.util.SecurityUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txRepository;
    private final UserRepository userRepository;

    public WalletServiceImpl(WalletRepository walletRepository, WalletTransactionRepository txRepository,
                             UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.txRepository = txRepository;
        this.userRepository = userRepository;
    }

    @Override
    public WalletResponse getWallet() {
        Wallet wallet = getCurrentWallet();
        return new WalletResponse(wallet.getBalance(), wallet.getEquity(), wallet.getMarginUsed());
    }

    @Override
    public WalletResponse deposit(WalletTxRequest request) {
        Wallet wallet = getCurrentWallet();
        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(newBalance);
        wallet.setEquity(newBalance);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(WalletTxType.DEPOSIT);
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        txRepository.save(tx);

        walletRepository.save(wallet);
        return new WalletResponse(wallet.getBalance(), wallet.getEquity(), wallet.getMarginUsed());
    }

    @Override
    public WalletResponse withdraw(WalletTxRequest request) {
        Wallet wallet = getCurrentWallet();
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient balance");
        }
        BigDecimal newBalance = wallet.getBalance().subtract(request.getAmount());
        wallet.setBalance(newBalance);
        wallet.setEquity(newBalance);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(WalletTxType.WITHDRAW);
        tx.setAmount(request.getAmount());
        tx.setNote(request.getNote());
        txRepository.save(tx);

        walletRepository.save(wallet);
        return new WalletResponse(wallet.getBalance(), wallet.getEquity(), wallet.getMarginUsed());
    }

    @Override
    public List<WalletTxResponse> getTransactions() {
        Wallet wallet = getCurrentWallet();
        return txRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream()
                .map(tx -> new WalletTxResponse(tx.getId(), tx.getType().name(), tx.getAmount(), tx.getNote(), tx.getCreatedAt()))
                .toList();
    }

    private Wallet getCurrentWallet() {
        String username = SecurityUtil.getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
    }
}
