import React, { useEffect, useState } from 'react';
import api from '../api/axios.js';

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [summary, setSummary] = useState({ invested: 0, pnl: 0, total: 0 });

  const load = async () => {
    const { data } = await api.get('/orders');
    setOrders(data);
    const invested = data.reduce((sum, o) => sum + (Number(o.investedAmount || 0)), 0);
    const pnl = data.reduce((sum, o) => sum + (Number(o.pnl || 0)), 0);
    setSummary({ invested, pnl, total: invested + pnl });
  };

  useEffect(() => {
    load();
  }, []);

  const processPending = async () => {
    await api.post('/orders/process');
    load();
  };

  const cancel = async (id) => {
    await api.post(`/orders/${id}/cancel`);
    load();
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Orders</h2>
          <p className="muted">Track market and limit orders.</p>
        </div>
        <div className="button-row">
          <button className="btn outline" onClick={processPending}>Process Pending</button>
          <button className="btn outline" onClick={load}>Refresh</button>
        </div>
      </div>

      <div className="grid stats">
        <div className="card stat-card">
          <div className="stat-label">Total Invested</div>
          <div className="stat-value">${summary.invested.toFixed(4)} <span className="currency">USD</span></div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Total P&L</div>
          <div className={`stat-value ${summary.pnl >= 0 ? 'pnl-up' : 'pnl-down'}`}>
            ${summary.pnl.toFixed(4)} <span className="currency">USD</span>
          </div>
        </div>
        <div className="card stat-card">
          <div className="stat-label">Total Value</div>
          <div className="stat-value">${summary.total.toFixed(4)} <span className="currency">USD</span></div>
        </div>
      </div>

      <div className="card">
        <div className="table">
          <div className="table-row header cols-7">
            <span>Symbol</span>
            <span>Side</span>
            <span>Type</span>
            <span>Status</span>
            <span>Qty</span>
            <span>Invested</span>
            <span>P&L</span>
          </div>
          {orders.map((order) => (
            <div key={order.id} className="table-row cols-7">
              <span>{order.symbol}</span>
              <span>{order.side}</span>
              <span>{order.type}</span>
              <span>{order.status}</span>
              <span>{order.quantity}</span>
              <span>{order.investedAmount ? order.investedAmount.toFixed(4) : '-'}</span>
              <span className={Number(order.pnl || 0) >= 0 ? 'pnl-up' : 'pnl-down'}>
                {order.pnl !== null && order.pnl !== undefined ? Number(order.pnl).toFixed(4) : '--'}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
