package com.finnexus.service;

import com.finnexus.domain.dto.WalletResponse;
import com.finnexus.domain.dto.WalletTxRequest;
import com.finnexus.domain.dto.WalletTxResponse;

import java.util.List;

public interface WalletService {
    WalletResponse getWallet();
    WalletResponse deposit(WalletTxRequest request);
    WalletResponse withdraw(WalletTxRequest request);
    List<WalletTxResponse> getTransactions();
}
