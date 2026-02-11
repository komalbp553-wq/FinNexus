import React, { useEffect, useState } from 'react';
import api from '../api/axios.js';

export default function Wallet() {
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [message, setMessage] = useState(null);

  const loadWallet = async () => {
    const [walletRes, txRes] = await Promise.all([
      api.get('/wallet'),
      api.get('/wallet/transactions')
    ]);
    setWallet(walletRes.data);
    setTransactions(txRes.data);
  };

  useEffect(() => {
    loadWallet();
  }, []);

  const doDeposit = async () => {
    setMessage(null);
    await api.post('/wallet/deposit', { amount: Number(amount), note });
    setAmount('');
    setNote('');
    setMessage('Deposit successful');
    loadWallet();
  };

  const doWithdraw = async () => {
    setMessage(null);
    await api.post('/wallet/withdraw', { amount: Number(amount), note });
    setAmount('');
    setNote('');
    setMessage('Withdraw successful');
    loadWallet();
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Wallet</h2>
          <p className="muted">Manage virtual funds and transactions.</p>
        </div>
      </div>

      <div className="grid stats">
        <div className="card stat-card">
          <div className="stat-label">Balance</div>
          <div className="stat-value">{wallet ? `$${wallet.balance} USD` : '--'}</div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Equity</div>
          <div className="stat-value">{wallet ? `$${wallet.equity} USD` : '--'}</div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Margin Used</div>
          <div className="stat-value">{wallet ? `$${wallet.marginUsed} USD` : '--'}</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Fund Management</h3>
        </div>
        {message && <div className="alert">{message}</div>}
        <div className="form-row">
          <label>Amount</label>
          <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />
        </div>
        <div className="form-row">
          <label>Note</label>
          <input value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
        <div className="button-row">
          <button className="btn primary" onClick={doDeposit}>Deposit</button>
          <button className="btn outline" onClick={doWithdraw}>Withdraw</button>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Transaction History</h3>
        </div>
        <div className="table">
          <div className="table-row header cols-4">
            <span>Type</span>
            <span>Amount</span>
            <span>Note</span>
            <span>Time</span>
          </div>
          {transactions.map((tx) => (
            <div key={tx.id} className="table-row cols-4">
              <span>{tx.type}</span>
              <span>{tx.amount}</span>
              <span>{tx.note || '-'}</span>
              <span>{new Date(tx.createdAt).toLocaleString()}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
