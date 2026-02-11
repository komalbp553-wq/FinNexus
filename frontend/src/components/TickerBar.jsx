import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios.js';
import { getSimQuotes } from '../sim/marketSim.js';

const USE_SIM_MARKET = true;

const SYMBOLS = ['EURUSD', 'GBPUSD', 'USDJPY', 'AUDUSD', 'USDCHF', 'USDCAD', 'NZDUSD', 'EURJPY'];

function formatPrice(symbol, price) {
  if (price === null || price === undefined) return '--';
  const precision = symbol.includes('JPY') ? 3 : 5;
  return Number(price).toFixed(precision);
}

export default function TickerBar() {
  const navigate = useNavigate();
  const [quotes, setQuotes] = useState([]);
  const [lastUpdated, setLastUpdated] = useState(null);

  const load = async () => {
    if (USE_SIM_MARKET) {
      const data = getSimQuotes(SYMBOLS);
      setQuotes((prev) => {
        const prevMap = new Map(prev.map((q) => [q.symbol, q.price]));
        return data.map((q) => {
          const prevPrice = prevMap.get(q.symbol);
          const direction = prevPrice ? Math.sign(q.price - prevPrice) : 0;
          return { ...q, direction };
        });
      });
      setLastUpdated(new Date());
      return;
    }

    try {
      const { data } = await api.get(`/market/quotes?symbols=${SYMBOLS.join(',')}`);
      setQuotes((prev) => {
        const prevMap = new Map(prev.map((q) => [q.symbol, q.price]));
        return data.map((q) => {
          const prevPrice = prevMap.get(q.symbol);
          const direction = prevPrice ? Math.sign(q.price - prevPrice) : 0;
          return { ...q, direction };
        });
      });
      setLastUpdated(new Date());
    } catch (e) {
      const data = getSimQuotes(SYMBOLS);
      setQuotes((prev) => {
        const prevMap = new Map(prev.map((q) => [q.symbol, q.price]));
        return data.map((q) => {
          const prevPrice = prevMap.get(q.symbol);
          const direction = prevPrice ? Math.sign(q.price - prevPrice) : 0;
          return { ...q, direction };
        });
      });
      setLastUpdated(new Date());
    }
  };

  useEffect(() => {
    load();
    const interval = setInterval(load, 30000);
    return () => clearInterval(interval);
  }, []);

  const items = useMemo(() => {
    if (quotes.length === 0) {
      return SYMBOLS.map((symbol) => ({ symbol, price: null, direction: 0 }));
    }
    return quotes;
  }, [quotes]);

  return (
    <div className="ticker">
      <div className="ticker-inner">
        <div className="live-dot" />
        <span className="ticker-title">Live Forex</span>
        {items.map((q) => (
          <button
            key={q.symbol}
            className={`ticker-item ${q.direction > 0 ? 'up' : q.direction < 0 ? 'down' : ''}`}
            onClick={() => {
              localStorage.setItem('activeSymbol', q.symbol);
              navigate(`/trade?symbol=${q.symbol}`);
            }}
          >
            <span className="ticker-symbol">{q.symbol}</span>
            <span className="ticker-price">{formatPrice(q.symbol, q.price)}</span>
          </button>
        ))}
      </div>
      <div className="ticker-time">
        {lastUpdated ? `Updated ${lastUpdated.toLocaleTimeString()}` : 'Fetching prices...'}
      </div>
    </div>
  );
}
