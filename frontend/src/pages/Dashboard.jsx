import React, { useEffect, useState } from 'react';
import api from '../api/axios.js';
import CandleChart from '../components/CandleChart.jsx';
import StatCard from '../components/StatCard.jsx';
import { getSimCandles, getSimPnl, getSimWallet } from '../sim/marketSim.js';

const USE_SIM_MARKET = true;

export default function Dashboard() {
  const [profile, setProfile] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [pnl, setPnl] = useState(null);
  const [candles, setCandles] = useState([]);
  const [symbol] = useState('EURUSD');
  const [timeframe] = useState('M15');

  useEffect(() => {
    const load = async () => {
      if (USE_SIM_MARKET) {
        const simWallet = getSimWallet();
        setProfile({ username: 'SimUser', role: 'SIM' });
        setWallet(simWallet);
        setPnl(getSimPnl(simWallet));
        setCandles(getSimCandles(symbol, timeframe, 60));
        return;
      }
      try {
        const [profileRes, walletRes, pnlRes, candleRes] = await Promise.all([
          api.get('/user/profile'),
          api.get('/wallet'),
          api.get('/analytics/pnl'),
          api.get(`/market/candles?symbol=${symbol}&timeframe=${timeframe}&limit=60`)
        ]);
        if (!candleRes.data || candleRes.data.length === 0) {
          throw new Error('No candles');
        }
        setProfile(profileRes.data);
        setWallet(walletRes.data);
        setPnl(pnlRes.data);
        setCandles(candleRes.data);
      } catch (e) {
        const simWallet = getSimWallet();
        setProfile({ username: 'SimUser', role: 'SIM' });
        setWallet(simWallet);
        setPnl(getSimPnl(simWallet));
        setCandles(getSimCandles(symbol, timeframe, 60));
      }
    };
    load();
    const interval = setInterval(load, 6000);
    return () => clearInterval(interval);
  }, [symbol, timeframe]);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Trading Overview</h2>
          <p className="muted">Live-like market feed with portfolio snapshot.</p>
        </div>
        {profile && (
          <div className="pill">{profile.username} • {profile.role}</div>
        )}
      </div>

      <div className="grid stats">
        <StatCard label="Balance" value={wallet ? `$${wallet.balance} USD` : '--'} />
        <StatCard label="Equity" value={wallet ? `$${wallet.equity} USD` : '--'} />
        <StatCard label="Margin Used" value={wallet ? `$${wallet.marginUsed} USD` : '--'} />
        <StatCard label="Open P&L" value={pnl ? `$${pnl.openPnl} USD` : '--'} />
      </div>

      <div className="card">
        <div className="card-header">
          <div>
            <h3>{symbol} • {timeframe}</h3>
            <span className="muted">Candlestick chart</span>
          </div>
        </div>
        <CandleChart data={candles} />
      </div>

      <div className="grid stats">
        <StatCard label="Closed P&L" value={pnl ? `$${pnl.closedPnl} USD` : '--'} />
        <StatCard label="Daily P&L" value={pnl ? `$${pnl.dailyPnl} USD` : '--'} />
        <StatCard label="Weekly P&L" value={pnl ? `$${pnl.weeklyPnl} USD` : '--'} />
        <StatCard label="ROI" value={pnl ? `${pnl.roi}%` : '--'} />
      </div>
    </div>
  );
}
