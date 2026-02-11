const states = new Map();
const accountState = {
  balance: 10000,
  equity: 10000,
  marginUsed: 0,
  lastTime: Date.now(),
  drift: 0.0002,
  vol: 0.004
};

function hashString(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

function createRng(seed) {
  let s = seed % 2147483647;
  if (s <= 0) s += 2147483646;
  return () => (s = (s * 16807) % 2147483647) / 2147483647;
}

function timeframeMinutes(tf) {
  switch (tf) {
    case 'M1':
      return 1;
    case 'M5':
      return 5;
    case 'M15':
      return 15;
    case 'H1':
      return 60;
    case 'D1':
      return 1440;
    default:
      return 15;
  }
}

function isStockSymbol(symbol) {
  const sym = symbol.toUpperCase().replace('/', '');
  return sym.length <= 5 && !sym.includes('USD') && !sym.includes('EUR') && !sym.includes('JPY');
}

function basePrice(symbol) {
  const sym = symbol.toUpperCase().replace('/', '');
  if (sym === 'EURUSD') return 1.085;
  if (sym === 'GBPUSD') return 1.275;
  if (sym === 'USDJPY') return 148.5;
  if (sym === 'AUDUSD') return 0.655;
  if (sym === 'USDCHF') return 0.892;
  if (sym === 'USDCAD') return 1.355;
  if (sym === 'NZDUSD') return 0.615;
  if (sym === 'EURJPY') return 160.2;
  const h = hashString(sym);
  if (sym.includes('JPY')) return 120 + (h % 60);
  if (isStockSymbol(sym)) return 20 + (h % 380);
  return 0.6 + ((h % 100) / 100);
}

function priceScale(symbol) {
  if (isStockSymbol(symbol)) return 2;
  return symbol.toUpperCase().includes('JPY') ? 3 : 5;
}

function initState(symbol, timeframe, limit) {
  const seed = hashString(symbol + timeframe);
  const rng = createRng(seed);
  const scale = priceScale(symbol);
  const vol = (symbol.toUpperCase().includes('JPY') ? 0.006 : 0.004) * (0.8 + rng() * 0.6);
  const drift = (rng() - 0.5) * 0.002;
  const now = Date.now();
  return {
    symbol,
    timeframe,
    rng,
    scale,
    vol,
    drift,
    shockProb: 0.03,
    shockScale: vol * 3.2,
    lastPrice: Number(basePrice(symbol).toFixed(scale)),
    lastTime: now - timeframeMinutes(timeframe) * 60 * 1000 * limit,
    lastReal: now,
    lastRegime: now
  };
}

function adjustRegime(state) {
  const minutes = (Date.now() - state.lastRegime) / 60000;
  if (minutes < 360) return;
  state.lastRegime = Date.now();
  state.drift = clamp(state.drift + (state.rng() - 0.5) * 0.001, -0.01, 0.01);
  state.vol = clamp(state.vol * (0.85 + state.rng() * 0.4), 0.0003, 0.02);
}

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function nextCandle(state) {
  adjustRegime(state);
  const stepMinutes = timeframeMinutes(state.timeframe);
  const nextTime = state.lastTime + stepMinutes * 60 * 1000;
  const open = state.lastPrice;

  const dt = stepMinutes / 1440;
  const shock = state.rng() < state.shockProb ? (state.rng() - 0.5) * 2 * state.shockScale : 0;
  const z = (state.rng() - 0.5) * 2;
  const ret = state.drift * dt + state.vol * Math.sqrt(dt) * z + shock;

  let next = open * Math.exp(ret);
  if (next <= 0) next = open;

  const wick = open * state.vol * (0.6 + state.rng());
  const high = Math.max(open, next) + wick;
  const low = Math.max(0.0001, Math.min(open, next) - wick);

  state.lastPrice = Number(next.toFixed(state.scale));
  state.lastTime = nextTime;

  return {
    time: new Date(nextTime).toISOString(),
    open: Number(open.toFixed(state.scale)),
    high: Number(high.toFixed(state.scale)),
    low: Number(low.toFixed(state.scale)),
    close: Number(state.lastPrice.toFixed(state.scale))
  };
}

function computeSteps(state, speed = 240, maxSteps = 8) {
  const now = Date.now();
  const elapsed = Math.max(1, (now - state.lastReal) / 1000);
  const virtualSeconds = elapsed * speed;
  const stepSeconds = timeframeMinutes(state.timeframe) * 60;
  let steps = Math.floor(virtualSeconds / stepSeconds);
  if (steps < 1) steps = 1;
  if (steps > maxSteps) steps = maxSteps;
  state.lastReal = now;
  return steps;
}

export function getSimCandles(symbol, timeframe, limit = 60) {
  const key = `${symbol}_${timeframe}`;
  let state = states.get(key);
  if (!state) {
    state = initState(symbol, timeframe, limit);
    states.set(key, state);
  }

  let series = state.series;
  if (!series) {
    series = [];
    for (let i = 0; i < limit; i++) {
      series.push(nextCandle(state));
    }
  } else {
    const steps = computeSteps(state);
    for (let i = 0; i < steps; i++) {
      series.push(nextCandle(state));
    }
  }

  if (series.length > 600) {
    series = series.slice(series.length - 600);
  }
  state.series = series;
  return series.slice(-limit);
}

export function getSimQuote(symbol) {
  const candles = getSimCandles(symbol, 'M1', 30);
  const last = candles[candles.length - 1];
  return { symbol: symbol.toUpperCase(), price: last.close, time: last.time };
}

export function getSimQuotes(symbols) {
  return symbols.map((s) => getSimQuote(s));
}

export function getSimWallet() {
  const now = Date.now();
  const dt = Math.max(1, (now - accountState.lastTime) / 1000);
  const noise = (Math.random() - 0.5) * accountState.vol * Math.sqrt(dt);
  const drift = accountState.drift * dt;
  const change = accountState.equity * (drift + noise);
  accountState.equity = Math.max(100, accountState.equity + change);
  accountState.marginUsed = Math.max(0, Math.min(accountState.equity * 0.2, accountState.marginUsed + change * 0.2));
  accountState.lastTime = now;
  return {
    balance: Number(accountState.balance.toFixed(2)),
    equity: Number(accountState.equity.toFixed(2)),
    marginUsed: Number(accountState.marginUsed.toFixed(2))
  };
}

export function getSimPnl(wallet) {
  const openPnl = wallet.equity - wallet.balance;
  const closedPnl = openPnl * 0.4;
  const dailyPnl = openPnl * 0.2;
  const weeklyPnl = openPnl * 0.3;
  const roi = (openPnl / wallet.balance) * 100;
  const accuracy = 40 + Math.random() * 40;
  return {
    openPnl: Number(openPnl.toFixed(2)),
    closedPnl: Number(closedPnl.toFixed(2)),
    dailyPnl: Number(dailyPnl.toFixed(2)),
    weeklyPnl: Number(weeklyPnl.toFixed(2)),
    roi: Number(roi.toFixed(2)),
    accuracy: Number(accuracy.toFixed(2))
  };
}
