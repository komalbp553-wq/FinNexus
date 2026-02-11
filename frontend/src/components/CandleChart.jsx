import React, { useEffect, useMemo, useRef, useState } from 'react';

export default function CandleChart({ data }) {
  const containerRef = useRef(null);
  const [width, setWidth] = useState(800);
  const fallbackRef = useRef(null);
  const height = 380;

  useEffect(() => {
    if (!containerRef.current) return;
    const updateSize = () => {
      const nextWidth = containerRef.current?.clientWidth || 800;
      setWidth(nextWidth);
    };
    updateSize();
    if (typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver(updateSize);
      observer.observe(containerRef.current);
      return () => observer.disconnect();
    }
    window.addEventListener('resize', updateSize);
    return () => window.removeEventListener('resize', updateSize);
  }, []);

  const sourceData = useMemo(() => {
    if (data && data.length > 0) {
      return data;
    }
    if (!fallbackRef.current) {
      const now = Date.now();
      let price = 1 + Math.random() * 0.5;
      fallbackRef.current = Array.from({ length: 60 }).map((_, i) => {
        const time = new Date(now - (60 - i) * 60 * 1000).toISOString();
        const open = price;
        const change = (Math.random() - 0.5) * 0.01;
        price = Math.max(0.1, open * (1 + change));
        const high = Math.max(open, price) * (1 + Math.random() * 0.002);
        const low = Math.min(open, price) * (1 - Math.random() * 0.002);
        return { time, open, high, low, close: price };
      });
    }
    return fallbackRef.current;
  }, [data]);

  const chart = useMemo(() => {
    if (!sourceData || sourceData.length === 0) return null;

    const candles = sourceData.map((d) => ({
      time: new Date(d.time),
      open: Number(d.open),
      high: Number(d.high),
      low: Number(d.low),
      close: Number(d.close)
    }));

    const lows = candles.map((c) => c.low);
    const highs = candles.map((c) => c.high);
    let min = Math.min(...lows);
    let max = Math.max(...highs);
    const range = max - min || 1;
    const padding = range * 0.05;
    min -= padding;
    max += padding;

    const plot = {
      left: 54,
      right: 20,
      top: 20,
      bottom: 30
    };

    const plotWidth = width - plot.left - plot.right;
    const plotHeight = height - plot.top - plot.bottom;
    const step = plotWidth / candles.length;
    const candleWidth = Math.max(4, step * 0.6);

    const yScale = (price) => plot.top + (max - price) / (max - min) * plotHeight;

    const yTicks = Array.from({ length: 5 }).map((_, i) => {
      const value = max - (range + padding * 2) * (i / 4);
      const y = plot.top + plotHeight * (i / 4);
      return { value, y };
    });

    const xTicks = Array.from({ length: 6 }).map((_, i) => {
      const index = Math.round((candles.length - 1) * (i / 5));
      const x = plot.left + index * step + step / 2;
      return { index, x, time: candles[index]?.time };
    });

    return { candles, plot, step, candleWidth, yScale, yTicks, xTicks };
  }, [sourceData, width]);

  return (
    <div className="tv-chart" ref={containerRef}>
      {chart && (
        <svg className="chart-svg" width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
          <rect x="0" y="0" width={width} height={height} fill="transparent" />

          {chart.yTicks.map((tick, idx) => (
            <g key={`y-${idx}`}>
              <line
                x1={chart.plot.left}
                x2={width - chart.plot.right}
                y1={tick.y}
                y2={tick.y}
                stroke="rgba(36,48,65,0.6)"
                strokeDasharray="4 4"
              />
              <text x={6} y={tick.y + 4} fill="#94a3b8" fontSize="11">
                {tick.value.toFixed(5)}
              </text>
            </g>
          ))}

          {chart.xTicks.map((tick, idx) => (
            <g key={`x-${idx}`}>
              <line
                x1={tick.x}
                x2={tick.x}
                y1={chart.plot.top}
                y2={height - chart.plot.bottom}
                stroke="rgba(36,48,65,0.4)"
              />
              {tick.time && (
                <text x={tick.x - 20} y={height - 8} fill="#94a3b8" fontSize="11">
                  {tick.time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </text>
              )}
            </g>
          ))}

          {chart.candles.map((candle, idx) => {
            const x = chart.plot.left + idx * chart.step + chart.step / 2;
            const openY = chart.yScale(candle.open);
            const closeY = chart.yScale(candle.close);
            const highY = chart.yScale(candle.high);
            const lowY = chart.yScale(candle.low);
            const isUp = candle.close >= candle.open;
            const color = isUp ? '#33d17a' : '#ff6b6b';
            const bodyY = Math.min(openY, closeY);
            const bodyH = Math.max(2, Math.abs(closeY - openY));

            return (
              <g key={`c-${idx}`}>
                <line x1={x} x2={x} y1={highY} y2={lowY} stroke={color} strokeWidth="1" />
                <rect
                  x={x - chart.candleWidth / 2}
                  y={bodyY}
                  width={chart.candleWidth}
                  height={bodyH}
                  fill={color}
                />
              </g>
            );
          })}
        </svg>
      )}
    </div>
  );
}
