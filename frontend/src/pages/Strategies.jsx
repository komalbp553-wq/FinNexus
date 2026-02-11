import React, { useEffect, useState } from 'react';
import api from '../api/axios.js';

const emptyForm = {
  type: 'MOVING_AVERAGE_CROSSOVER',
  symbol: 'EURUSD',
  timeframe: 'M15',
  enabled: true,
  fastPeriod: 5,
  slowPeriod: 20,
  rsiPeriod: 14,
  rsiOverbought: 70,
  rsiOversold: 30
};

export default function Strategies() {
  const [strategies, setStrategies] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [message, setMessage] = useState(null);

  const load = async () => {
    const { data } = await api.get('/strategies');
    setStrategies(data);
  };

  useEffect(() => {
    load();
  }, []);

  const save = async (e) => {
    e.preventDefault();
    setMessage(null);
    if (editingId) {
      await api.put(`/strategies/${editingId}`, form);
      setMessage('Strategy updated');
    } else {
      await api.post('/strategies', form);
      setMessage('Strategy created');
    }
    setForm(emptyForm);
    setEditingId(null);
    load();
  };

  const edit = (strategy) => {
    setForm({
      type: strategy.type,
      symbol: strategy.symbol,
      timeframe: strategy.timeframe,
      enabled: strategy.enabled,
      fastPeriod: strategy.fastPeriod || 5,
      slowPeriod: strategy.slowPeriod || 20,
      rsiPeriod: strategy.rsiPeriod || 14,
      rsiOverbought: strategy.rsiOverbought || 70,
      rsiOversold: strategy.rsiOversold || 30
    });
    setEditingId(strategy.id);
  };

  const remove = async (id) => {
    await api.delete(`/strategies/${id}`);
    load();
  };

  const run = async () => {
    await api.post('/strategies/run');
    setMessage('Strategies executed. Check orders/trades.');
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2>Robo Strategies</h2>
          <p className="muted">Configure algorithmic trading and auto-execution.</p>
        </div>
        <button className="btn outline" onClick={run}>Run Now</button>
      </div>

      {message && <div className="alert">{message}</div>}

      <div className="grid trade-grid">
        <div className="card">
          <div className="card-header">
            <h3>{editingId ? 'Edit Strategy' : 'Create Strategy'}</h3>
          </div>
          <form className="strategy-form" onSubmit={save}>
            <div className="form-row">
              <label>Type</label>
              <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                <option>MOVING_AVERAGE_CROSSOVER</option>
                <option>RSI</option>
              </select>
            </div>
            <div className="form-row">
              <label>Symbol</label>
              <input
                list="strategy-symbols"
                value={form.symbol}
                onChange={(e) => setForm({ ...form, symbol: e.target.value.toUpperCase() })}
                placeholder="e.g. EURUSD"
              />
              <datalist id="strategy-symbols">
                <option value="EURUSD" />
                <option value="GBPUSD" />
                <option value="USDJPY" />
                <option value="AUDUSD" />
                <option value="USDCHF" />
                <option value="USDCAD" />
                <option value="NZDUSD" />
                <option value="EURJPY" />
              </datalist>
            </div>
            <div className="form-row">
              <label>Timeframe</label>
              <select value={form.timeframe} onChange={(e) => setForm({ ...form, timeframe: e.target.value })}>
                <option>M1</option>
                <option>M5</option>
                <option>M15</option>
                <option>H1</option>
                <option>D1</option>
              </select>
            </div>
            <div className="form-row">
              <label>Enabled</label>
              <select value={form.enabled ? 'true' : 'false'} onChange={(e) => setForm({ ...form, enabled: e.target.value === 'true' })}>
                <option value="true">true</option>
                <option value="false">false</option>
              </select>
            </div>

            {form.type === 'MOVING_AVERAGE_CROSSOVER' && (
              <>
                <div className="form-row">
                  <label>Fast Period</label>
                  <input type="number" value={form.fastPeriod} onChange={(e) => setForm({ ...form, fastPeriod: Number(e.target.value) })} />
                </div>
                <div className="form-row">
                  <label>Slow Period</label>
                  <input type="number" value={form.slowPeriod} onChange={(e) => setForm({ ...form, slowPeriod: Number(e.target.value) })} />
                </div>
              </>
            )}

            {form.type === 'RSI' && (
              <>
                <div className="form-row">
                  <label>RSI Period</label>
                  <input type="number" value={form.rsiPeriod} onChange={(e) => setForm({ ...form, rsiPeriod: Number(e.target.value) })} />
                </div>
                <div className="form-row">
                  <label>Overbought</label>
                  <input type="number" value={form.rsiOverbought} onChange={(e) => setForm({ ...form, rsiOverbought: Number(e.target.value) })} />
                </div>
                <div className="form-row">
                  <label>Oversold</label>
                  <input type="number" value={form.rsiOversold} onChange={(e) => setForm({ ...form, rsiOversold: Number(e.target.value) })} />
                </div>
              </>
            )}

            <div className="form-actions">
              <button className="btn primary" type="submit">{editingId ? 'Update' : 'Create'}</button>
            </div>
          </form>
        </div>

        <div className="card">
          <div className="card-header">
            <h3>Strategies List</h3>
          </div>
          <div className="table">
            <div className="table-row header cols-6">
              <span>Type</span>
              <span>Symbol</span>
              <span>TF</span>
              <span>Enabled</span>
              <span>Last Signal</span>
              <span>Action</span>
            </div>
            {strategies.map((s) => (
              <div key={s.id} className="table-row cols-6">
                <span>{s.type}</span>
                <span>{s.symbol}</span>
                <span>{s.timeframe}</span>
                <span>{String(s.enabled)}</span>
                <span>{s.lastSignal || '-'}</span>
                <div className="button-row">
                  <button className="btn outline" onClick={() => edit(s)}>Edit</button>
                  <button className="btn outline" onClick={() => remove(s.id)}>Delete</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
