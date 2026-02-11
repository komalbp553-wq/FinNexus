import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../api/axios.js';
import CandleChart from '../components/CandleChart.jsx';
import { getSimCandles, getSimQuote, getSimQuotes, getSimWallet } from '../sim/marketSim.js';

const USE_SIM_MARKET = true;

export default function Trade() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [symbol, setSymbol] = useState('EURUSD');
  const [timeframe, setTimeframe] = useState('M15');
  const [candles, setCandles] = useState([]);
  const [quote, setQuote] = useState(null);
  const [ordersMessage, setOrdersMessage] = useState(null);
  const [trades, setTrades] = useState([]);
  const [wallet, setWallet] = useState(null);
  const [quoteMap, setQuoteMap] = useState({});
  const [form, setForm] = useState({ side: 'BUY', type: 'MARKET', quantity: 1, price: '' });

  const loadMarket = async () => {
    if (USE_SIM_MARKET) {
      const simCandles = getSimCandles(symbol, timeframe, 60);
      setCandles(simCandles);
      const simWallet = getSimWallet();
      setWallet(simWallet);
      const symbols = ['EURUSD', 'GBPUSD', 'USDJPY', 'AUDUSD', 'USDCHF', 'USDCAD', 'NZDUSD', 'EURJPY', symbol];
      const simQuotes = getSimQuotes(symbols);
      const map = {};
      simQuotes.forEach((q) => {
        map[q.symbol] = q;
      });
      setQuoteMap(map);
      setQuote(getSimQuote(symbol));
      setTrades([]);
      return;
    }

    try {
      const [candleRes, tradeRes, walletRes] = await Promise.all([
        api.get(`/market/candles?symbol=${symbol}&timeframe=${timeframe}&limit=60`),
        api.get('/trades'),
        api.get('/wallet')
      ]);
      if (!candleRes.data || candleRes.data.length === 0) {
        throw new Error('No candles');
      }
      setCandles(candleRes.data);
      setTrades(tradeRes.data);
      setWallet(walletRes.data);

      const symbols = new Set(['EURUSD', 'GBPUSD', 'USDJPY', 'AUDUSD', 'USDCHF', 'USDCAD', 'NZDUSD', 'EURJPY', symbol]);
      tradeRes.data.forEach((t) => symbols.add(t.symbol));
      const { data: quotes } = await api.get(`/market/quotes?symbols=${Array.from(symbols).join(',')}`);
      const map = {};
      quotes.forEach((q) => {
        map[q.symbol] = q;
      });
      setQuoteMap(map);
      setQuote(map[symbol] || quotes.find((q) => q.symbol === symbol));
    } catch (e) {
      const simCandles = getSimCandles(symbol, timeframe, 60);
      setCandles(simCandles);
      setTrades([]);
      const simWallet = getSimWallet();
      setWallet(simWallet);
      const symbols = ['EURUSD', 'GBPUSD', 'USDJPY', 'AUDUSD', 'USDCHF', 'USDCAD', 'NZDUSD', 'EURJPY', symbol];
      const simQuotes = getSimQuotes(symbols);
      const map = {};
      simQuotes.forEach((q) => {
        map[q.symbol] = q;
      });
      setQuoteMap(map);
      setQuote(getSimQuote(symbol));
    }
  };

  useEffect(() => {
    loadMarket();
    const interval = setInterval(loadMarket, 3000);
    return () => clearInterval(interval);
  }, [symbol, timeframe]);

  useEffect(() => {
    const param = searchParams.get('symbol');
    const stored = localStorage.getItem('activeSymbol');
    if (param) {
      setSymbol(param.toUpperCase());
    } else if (stored) {
      setSymbol(stored.toUpperCase());
    }
  }, []);

  useEffect(() => {
    if (!symbol) return;
    localStorage.setItem('activeSymbol', symbol);
    setSearchParams({ symbol }, { replace: true });
  }, [symbol, setSearchParams]);

  const placeOrder = async (e) => {
    e.preventDefault();
    await submitOrder(form.side);
  };

  const submitOrder = async (sideOverride) => {
    setOrdersMessage(null);
    try {
      const payload = {
        symbol,
        side: sideOverride || form.side,
        type: form.type,
        quantity: Number(form.quantity),
        price: form.type === 'LIMIT' ? Number(form.price) : null
      };
      await api.post('/orders', payload);
      setOrdersMessage('Order placed successfully');
      loadMarket();
    } catch (err) {
      setOrdersMessage(err.response?.data?.message || 'Order failed');
    }
  };

  const closeTrade = async (tradeId) => {
    if (!quote) return;
    await api.post(`/trades/${tradeId}/close`, { exitPrice: quote.price });
    loadMarket();
  };

  const watchlist = useMemo(
    () => ['EURUSD', 'GBPUSD', 'USDJPY', 'AUDUSD', 'USDCHF', 'USDCAD', 'NZDUSD', 'EURJPY'],
    []
  );

  const calculatePnlValue = (trade) => {
    const current = quoteMap[trade.symbol]?.price;
    if (!current) return null;
    const diff = trade.side === 'BUY' ? current - trade.entryPrice : trade.entryPrice - current;
    return diff * trade.quantity;
  };

  const calculatePnl = (trade) => {
    const value = calculatePnlValue(trade);
    if (value === null || Number.isNaN(value)) return null;
    return value.toFixed(4);
  };

  const openTrades = trades.filter((t) => t.status === 'OPEN');
  const totalOpenPnl = openTrades.reduce((sum, t) => {
    const val = calculatePnlValue(t);
    return val === null ? sum : sum + val;
  }, 0);
  const liveEquity = wallet ? Number(wallet.balance) + totalOpenPnl : null;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Manual Trading</h2>
          <p className="muted">Place market or limit orders with margin checks.</p>
        </div>
        {quote && <div className="pill">{quote.symbol} {quote.price}</div>}
      </div>

      <div className="trade-layout">
        <div className="trade-sidebar">
          <div className="card">
            <div className="card-header">
              <h3>Watchlist</h3>
            </div>
            <div className="watchlist">
              {watchlist.map((item) => {
                const q = quoteMap[item];
                return (
                  <button key={item} className={`watchlist-item ${item === symbol ? 'active' : ''}`} onClick={() => setSymbol(item)}>
                    <span>{item}</span>
                    <span className="price">{q ? q.price : '--'}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        <div className="trade-center">
          <div className="card">
            <div className="card-header">
              <div>
                <h3>{symbol} • {timeframe}</h3>
                <span className="muted">Simulated live chart</span>
              </div>
              <div className="timeframe-row">
                <label>Timeframe</label>
                <select value={timeframe} onChange={(e) => setTimeframe(e.target.value)}>
                  <option>M1</option>
                  <option>M5</option>
                  <option>M15</option>
                  <option>H1</option>
                  <option>D1</option>
                </select>
              </div>
            </div>
            <div className="form-row">
              <label>Symbol</label>
              <input
                list="symbols-list"
                value={symbol}
                onChange={(e) => setSymbol(e.target.value.toUpperCase())}
                placeholder="e.g. AAPL / EURUSD / TSLA"
              />
              <datalist id="symbols-list">
                <option value="EURUSD" />
                <option value="GBPUSD" />
                <option value="USDJPY" />
                <option value="AUDUSD" />
                <option value="USDCHF" />
                <option value="USDCAD" />
                <option value="NZDUSD" />
                <option value="EURJPY" />
                <option value="AAPL" />
                <option value="TSLA" />
                <option value="GOOGL" />
              </datalist>
            </div>
            <CandleChart data={candles} />
          </div>
        </div>

        <div className="trade-right">
          <div className="card">
            <div className="card-header">
              <h3>Order Ticket</h3>
            </div>
            {ordersMessage && <div className="alert">{ordersMessage}</div>}
            <form onSubmit={placeOrder}>
              <label>Side</label>
              <select value={form.side} onChange={(e) => setForm({ ...form, side: e.target.value })}>
                <option>BUY</option>
                <option>SELL</option>
              </select>
              <label>Type</label>
              <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                <option>MARKET</option>
                <option>LIMIT</option>
              </select>
              <label>Quantity (lots)</label>
              <input type="number" step="0.1" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} />
              {form.type === 'LIMIT' && (
                <>
                  <label>Limit Price</label>
                  <input type="number" step="0.00001" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
                </>
              )}
              <div className="button-row">
                <button className="btn primary" type="submit">Place Order</button>
                <button
                  className="btn buy"
                  type="button"
                  onClick={() => submitOrder('BUY')}
                >
                  Quick Buy
                </button>
                <button
                  className="btn sell"
                  type="button"
                  onClick={() => submitOrder('SELL')}
                >
                  Quick Sell
                </button>
              </div>
            </form>
          </div>

          <div className="card">
            <div className="card-header">
              <h3>Your Wallet</h3>
            </div>
            <div className="wallet-mini">
              <div>
                <span className="muted">Balance</span>
                <div className="wallet-value">{wallet ? `$${wallet.balance}` : '--'} <span className="currency">USD</span></div>
              </div>
              <div>
                <span className="muted">Equity</span>
                <div className="wallet-value">{wallet ? `$${wallet.equity}` : '--'} <span className="currency">USD</span></div>
              </div>
              <div>
                <span className="muted">Margin Used</span>
                <div className="wallet-value">{wallet ? `$${wallet.marginUsed}` : '--'} <span className="currency">USD</span></div>
              </div>
              <div>
                <span className="muted">Unrealized P&L</span>
                <div className={totalOpenPnl >= 0 ? 'wallet-value pnl-up' : 'wallet-value pnl-down'}>
                  {wallet ? `$${totalOpenPnl.toFixed(4)}` : '--'} <span className="currency">USD</span>
                </div>
              </div>
              <div>
                <span className="muted">Live Equity</span>
                <div className="wallet-value">
                  {wallet && liveEquity !== null ? `$${liveEquity.toFixed(4)}` : '--'} <span className="currency">USD</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <h3>Open Trades</h3>
        </div>
        <div className="table">
          <div className="table-row header cols-7">
            <span>Symbol</span>
            <span>Side</span>
            <span>Entry</span>
            <span>Qty</span>
            <span>Status</span>
            <span>P&L</span>
            <span>Action</span>
          </div>
          {openTrades.map((trade) => (
            <div key={trade.id} className="table-row cols-7">
              <span>{trade.symbol}</span>
              <span>{trade.side}</span>
              <span>{trade.entryPrice}</span>
              <span>{trade.quantity}</span>
              <span>{trade.status}</span>
              <span className={Number(calculatePnl(trade)) >= 0 ? 'pnl-up' : 'pnl-down'}>
                {calculatePnl(trade) ?? '--'}
              </span>
              <button className="btn outline" onClick={() => closeTrade(trade.id)}>Close</button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
