import React, { useEffect, useState } from 'react';
import api from '../api/axios.js';

export default function Analytics() {
  const [pnl, setPnl] = useState(null);
  const [trades, setTrades] = useState([]);

  useEffect(() => {
    const load = async () => {
      const [pnlRes, tradeRes] = await Promise.all([
        api.get('/analytics/pnl'),
        api.get('/trades')
      ]);
      setPnl(pnlRes.data);
      setTrades(tradeRes.data);
    };
    load();
  }, []);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Performance Analytics</h2>
          <p className="muted">Evaluate profitability and accuracy.</p>
        </div>
      </div>

      <div className="grid stats">
        <div className="card stat-card">
          <div className="stat-label">Open P&L</div>
          <div className="stat-value">{pnl ? `$${pnl.openPnl} USD` : '--'}</div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Closed P&L</div>
          <div className="stat-value">{pnl ? `$${pnl.closedPnl} USD` : '--'}</div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">ROI</div>
          <div className="stat-value">{pnl ? `${pnl.roi}%` : '--'}</div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Accuracy</div>
          <div className="stat-value">{pnl ? `${pnl.accuracy}%` : '--'}</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Closed Trades</h3>
        </div>
        <div className="table">
          <div className="table-row header cols-6">
            <span>Symbol</span>
            <span>Side</span>
            <span>Entry</span>
            <span>Exit</span>
            <span>P&L</span>
            <span>Closed</span>
          </div>
          {trades.filter(t => t.status === 'CLOSED').map((trade) => (
            <div key={trade.id} className="table-row cols-6">
              <span>{trade.symbol}</span>
              <span>{trade.side}</span>
              <span>{trade.entryPrice}</span>
              <span>{trade.exitPrice}</span>
              <span>{trade.pnl}</span>
              <span>{trade.closedAt ? new Date(trade.closedAt).toLocaleString() : '-'}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
