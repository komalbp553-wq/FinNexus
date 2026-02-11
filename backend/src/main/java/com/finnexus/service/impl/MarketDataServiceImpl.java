package com.finnexus.service.impl;

import com.finnexus.domain.dto.CandleDto;
import com.finnexus.domain.dto.QuoteDto;
import com.finnexus.domain.enums.Timeframe;
import com.finnexus.service.MarketDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataServiceImpl implements MarketDataService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    private final RestTemplate restTemplate;
    private final String provider;
    private final String apiKey;
    private final String baseUrl;
    private final long minTtlSeconds;
    private final long quoteTtlSeconds;
    private final double simSpeed;
    private final int simMaxSteps;
    private final int simMaxCandles;

    private final Map<String, CacheEntry<List<CandleDto>>> liveCandleCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<QuoteDto>> quoteCache = new ConcurrentHashMap<>();
    private final Map<String, List<CandleDto>> mockSeries = new ConcurrentHashMap<>();
    private final Map<String, SimState> simStates = new ConcurrentHashMap<>();

    public MarketDataServiceImpl(RestTemplate restTemplate,
                                 @Value("${market.provider:MOCK}") String provider,
                                 @Value("${twelvedata.apiKey:}") String apiKey,
                                 @Value("${twelvedata.baseUrl:https://api.twelvedata.com}") String baseUrl,
                                 @Value("${market.cache.minTtlSeconds:120}") long minTtlSeconds,
                                 @Value("${market.cache.quoteTtlSeconds:300}") long quoteTtlSeconds,
                                 @Value("${market.sim.speed:300}") double simSpeed,
                                 @Value("${market.sim.maxSteps:6}") int simMaxSteps,
                                 @Value("${market.sim.maxCandles:600}") int simMaxCandles) {
        this.restTemplate = restTemplate;
        this.provider = provider;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.minTtlSeconds = minTtlSeconds;
        this.quoteTtlSeconds = quoteTtlSeconds;
        this.simSpeed = simSpeed;
        this.simMaxSteps = simMaxSteps;
        this.simMaxCandles = simMaxCandles;
    }

    @Override
    public List<CandleDto> getCandles(String symbol, Timeframe timeframe, int limit) {
        if (!isLiveEnabled()) {
            return getMockCandles(symbol, timeframe, limit);
        }
        String key = cacheKey(symbol, timeframe);
        CacheEntry<List<CandleDto>> entry = liveCandleCache.get(key);
        long ttl = Math.max(minTtlSeconds, timeframeStepMinutes(timeframe) * 60);
        if (entry != null && !isExpired(entry.fetchedAt, ttl) && entry.data.size() >= limit) {
            return slice(entry.data, limit);
        }

        List<CandleDto> live = fetchLiveCandles(symbol, timeframe, limit);
        if (!live.isEmpty()) {
            liveCandleCache.put(key, new CacheEntry<>(live, Instant.now()));
            return slice(live, limit);
        }

        return getMockCandles(symbol, timeframe, limit);
    }

    @Override
    public QuoteDto getQuote(String symbol) {
        String key = symbol.toUpperCase(Locale.ENGLISH);
        CacheEntry<QuoteDto> cachedQuote = quoteCache.get(key);
        if (cachedQuote != null && !isExpired(cachedQuote.fetchedAt, quoteTtlSeconds)) {
            return cachedQuote.data;
        }

        if (!isLiveEnabled()) {
            QuoteDto mock = mockQuote(symbol);
            quoteCache.put(key, new CacheEntry<>(mock, Instant.now()));
            return mock;
        }

        List<CandleDto> cachedCandles = getCachedCandles(symbol, Timeframe.M1);
        if (cachedCandles != null && !cachedCandles.isEmpty()) {
            CandleDto last = cachedCandles.get(cachedCandles.size() - 1);
            QuoteDto quote = new QuoteDto(symbol.toUpperCase(Locale.ENGLISH), last.getClose(), last.getTime());
            quoteCache.put(key, new CacheEntry<>(quote, Instant.now()));
            return quote;
        }

        QuoteDto liveQuote = fetchLiveQuote(symbol);
        if (liveQuote != null) {
            quoteCache.put(key, new CacheEntry<>(liveQuote, Instant.now()));
            return liveQuote;
        }

        QuoteDto mock = mockQuote(symbol);
        quoteCache.put(key, new CacheEntry<>(mock, Instant.now()));
        return mock;
    }

    @Override
    public List<QuoteDto> getQuotes(List<String> symbols) {
        return symbols.stream().map(this::getQuote).toList();
    }

    private List<CandleDto> fetchLiveCandles(String symbol, Timeframe timeframe, int limit) {
        try {
            String interval = mapInterval(timeframe);
            String liveSymbol = toProviderSymbol(symbol);
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/time_series")
                    .queryParam("symbol", liveSymbol)
                    .queryParam("interval", interval)
                    .queryParam("outputsize", limit)
                    .queryParam("order", "asc")
                    .queryParam("apikey", apiKey)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !"ok".equalsIgnoreCase(String.valueOf(response.get("status")))) {
                return List.of();
            }
            Object valuesObj = response.get("values");
            if (!(valuesObj instanceof List<?> valuesRaw)) {
                return List.of();
            }

            List<CandleDto> candles = new ArrayList<>();
            for (Object item : valuesRaw) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                String time = String.valueOf(map.get("datetime"));
                BigDecimal open = parseDecimal(map.get("open"));
                BigDecimal high = parseDecimal(map.get("high"));
                BigDecimal low = parseDecimal(map.get("low"));
                BigDecimal close = parseDecimal(map.get("close"));
                candles.add(new CandleDto(parseInstant(time), open, high, low, close));
            }
            candles.sort(Comparator.comparing(CandleDto::getTime));
            return candles;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private QuoteDto fetchLiveQuote(String symbol) {
        try {
            String liveSymbol = toProviderSymbol(symbol);
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/quote")
                    .queryParam("symbol", liveSymbol)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.containsKey("code")) {
                return null;
            }

            BigDecimal price = firstDecimal(response, "close", "price", "bid", "ask");
            Instant time = parseInstant(String.valueOf(response.get("datetime")));
            if (price == null) {
                return null;
            }
            return new QuoteDto(symbol.toUpperCase(Locale.ENGLISH), price, time);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<CandleDto> getCachedCandles(String symbol, Timeframe timeframe) {
        String key = cacheKey(symbol, timeframe);
        CacheEntry<List<CandleDto>> entry = liveCandleCache.get(key);
        if (entry == null) {
            return null;
        }
        long ttl = Math.max(minTtlSeconds, timeframeStepMinutes(timeframe) * 60);
        if (isExpired(entry.fetchedAt, ttl)) {
            return null;
        }
        return entry.data;
    }

    private List<CandleDto> slice(List<CandleDto> data, int limit) {
        if (data.size() <= limit) {
            return data;
        }
        return data.subList(data.size() - limit, data.size());
    }

    private String cacheKey(String symbol, Timeframe timeframe) {
        return symbol.toUpperCase(Locale.ENGLISH) + "_" + timeframe.name();
    }

    private boolean isExpired(Instant fetchedAt, long ttlSeconds) {
        return fetchedAt.plusSeconds(ttlSeconds).isBefore(Instant.now());
    }

    private boolean isLiveEnabled() {
        return "TWELVEDATA".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank();
    }

    private List<CandleDto> getMockCandles(String symbol, Timeframe timeframe, int limit) {
        String key = symbol.toUpperCase(Locale.ENGLISH) + "_" + timeframe.name();
        SimState state = simStates.computeIfAbsent(key, k -> initSimState(symbol, timeframe, limit));
        List<CandleDto> candles = mockSeries.get(key);

        if (candles == null || candles.isEmpty()) {
            candles = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                candles.add(nextSimCandle(state, timeframe));
            }
        } else {
            int steps = computeSimSteps(state, timeframe);
            for (int i = 0; i < steps; i++) {
                candles.add(nextSimCandle(state, timeframe));
            }
        }

        if (candles.size() > simMaxCandles) {
            candles = new ArrayList<>(candles.subList(candles.size() - simMaxCandles, candles.size()));
        }
        if (candles.size() > limit) {
            candles = new ArrayList<>(candles.subList(candles.size() - limit, candles.size()));
        }
        candles.sort(Comparator.comparing(CandleDto::getTime));
        mockSeries.put(key, candles);
        return candles;
    }

    private QuoteDto mockQuote(String symbol) {
        List<CandleDto> candles = getMockCandles(symbol, Timeframe.M1, 30);
        CandleDto last = candles.get(candles.size() - 1);
        return new QuoteDto(symbol.toUpperCase(Locale.ENGLISH), last.getClose(), last.getTime());
    }

    private SimState initSimState(String symbol, Timeframe timeframe, int limit) {
        SimState state = new SimState();
        state.scale = priceScale(symbol);
        state.lastPrice = basePrice(symbol).setScale(state.scale, RoundingMode.HALF_UP);
        state.random = new Random((symbol + timeframe.name()).hashCode());
        state.volatility = baseVolatility(symbol, state.random);
        state.drift = baseDrift(state.random);
        state.shockProbability = 0.03;
        state.shockScale = state.volatility * 3.2;
        long step = timeframeStepMinutes(timeframe);
        Instant now = Instant.now();
        state.lastTime = now.minus(step * (long) limit, java.time.temporal.ChronoUnit.MINUTES);
        state.lastRealTime = now;
        state.lastRegimeChange = state.lastTime;
        return state;
    }

    private CandleDto nextSimCandle(SimState state, Timeframe timeframe) {
        adjustRegime(state);
        long step = timeframeStepMinutes(timeframe);
        Instant time = state.lastTime.plus(step, java.time.temporal.ChronoUnit.MINUTES);

        BigDecimal open = state.lastPrice;
        double dt = step / 1440.0;
        double shock = state.random.nextDouble() < state.shockProbability
                ? state.random.nextGaussian() * state.shockScale
                : 0.0;
        double z = state.random.nextGaussian();
        double ret = state.drift * dt + state.volatility * Math.sqrt(dt) * z + shock;

        double next = open.doubleValue() * Math.exp(ret);
        if (next <= 0) {
            next = open.doubleValue();
        }
        BigDecimal close = BigDecimal.valueOf(next).setScale(state.scale, RoundingMode.HALF_UP);

        double wickBase = open.doubleValue() * state.volatility * (0.6 + state.random.nextDouble());
        BigDecimal high = BigDecimal.valueOf(Math.max(open.doubleValue(), close.doubleValue()) + wickBase)
                .setScale(state.scale, RoundingMode.HALF_UP);
        BigDecimal low = BigDecimal.valueOf(Math.min(open.doubleValue(), close.doubleValue()) - wickBase)
                .setScale(state.scale, RoundingMode.HALF_UP);
        if (low.compareTo(BigDecimal.ZERO) <= 0) {
            low = BigDecimal.valueOf(0.0001).setScale(state.scale, RoundingMode.HALF_UP);
        }

        state.lastPrice = close;
        state.lastTime = time;
        return new CandleDto(time, open, high, low, close);
    }

    private int computeSimSteps(SimState state, Timeframe timeframe) {
        Instant now = Instant.now();
        long elapsedSeconds = java.time.Duration.between(state.lastRealTime, now).getSeconds();
        if (elapsedSeconds <= 0) {
            return 1;
        }
        double virtualSeconds = elapsedSeconds * simSpeed;
        double stepSeconds = timeframeStepMinutes(timeframe) * 60.0;
        int steps = (int) Math.floor(virtualSeconds / stepSeconds);
        if (steps < 1) {
            steps = 1;
        }
        if (steps > simMaxSteps) {
            steps = simMaxSteps;
        }
        state.lastRealTime = now;
        return steps;
    }

    private void adjustRegime(SimState state) {
        long minutesSinceChange = java.time.Duration.between(state.lastRegimeChange, state.lastTime).toMinutes();
        if (minutesSinceChange < 360) {
            return;
        }
        state.lastRegimeChange = state.lastTime;
        double driftShift = (state.random.nextDouble() - 0.5) * 0.001;
        double volShift = 0.85 + state.random.nextDouble() * 0.4;
        state.drift = clamp(state.drift + driftShift, -0.01, 0.01);
        state.volatility = clamp(state.volatility * volShift, 0.0003, 0.02);
    }

    private long timeframeStepMinutes(Timeframe timeframe) {
        return switch (timeframe) {
            case M1 -> 1;
            case M5 -> 5;
            case M15 -> 15;
            case H1 -> 60;
            case D1 -> 1440;
        };
    }

    private String mapInterval(Timeframe timeframe) {
        return switch (timeframe) {
            case M1 -> "1min";
            case M5 -> "5min";
            case M15 -> "15min";
            case H1 -> "1h";
            case D1 -> "1day";
        };
    }

    private String toProviderSymbol(String symbol) {
        String raw = symbol.toUpperCase(Locale.ENGLISH).replace("/", "");
        if (raw.length() == 6) {
            return raw.substring(0, 3) + "/" + raw.substring(3);
        }
        return symbol.toUpperCase(Locale.ENGLISH);
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal firstDecimal(Map<String, Object> response, String... keys) {
        for (String key : keys) {
            Object value = response.get(key);
            if (value != null) {
                return new BigDecimal(String.valueOf(value));
            }
        }
        return null;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return Instant.now();
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, DATE_TIME);
            return dateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
        }
        try {
            LocalDate date = LocalDate.parse(value, DATE_ONLY);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
        }
        return Instant.now();
    }

    private BigDecimal basePrice(String symbol) {
        String sym = symbol.toUpperCase(Locale.ENGLISH).replace("/", "");
        return switch (sym) {
            case "EURUSD" -> new BigDecimal("1.0850");
            case "GBPUSD" -> new BigDecimal("1.2750");
            case "USDJPY" -> new BigDecimal("148.50");
            case "AUDUSD" -> new BigDecimal("0.6550");
            case "USDCHF" -> new BigDecimal("0.8920");
            case "USDCAD" -> new BigDecimal("1.3550");
            case "NZDUSD" -> new BigDecimal("0.6150");
            case "EURJPY" -> new BigDecimal("160.20");
            default -> derivedBasePrice(sym);
        };
    }

    private BigDecimal derivedBasePrice(String symbol) {
        int hash = Math.abs(symbol.hashCode());
        if (symbol.contains("JPY")) {
            double base = 120 + (hash % 60);
            return BigDecimal.valueOf(base);
        }
        if (isStockSymbol(symbol)) {
            double base = 20 + (hash % 380);
            return BigDecimal.valueOf(base);
        }
        double base = 0.6 + ((hash % 100) / 100.0);
        return BigDecimal.valueOf(base);
    }

    private int priceScale(String symbol) {
        if (isStockSymbol(symbol)) {
            return 2;
        }
        return symbol.toUpperCase(Locale.ENGLISH).contains("JPY") ? 3 : 5;
    }

    private double baseVolatility(String symbol, Random random) {
        if (isStockSymbol(symbol)) {
            double base = 0.018;
            return base * (0.9 + random.nextDouble() * 0.7);
        }
        double base = symbol.toUpperCase(Locale.ENGLISH).contains("JPY") ? 0.006 : 0.004;
        return base * (0.8 + random.nextDouble() * 0.6);
    }

    private double baseDrift(Random random) {
        return (random.nextDouble() - 0.5) * 0.002;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isStockSymbol(String symbol) {
        String sym = symbol.toUpperCase(Locale.ENGLISH).replace("/", "");
        return sym.length() <= 5 && !sym.contains("USD") && !sym.contains("EUR") && !sym.contains("JPY");
    }

    private static class CacheEntry<T> {
        private final T data;
        private final Instant fetchedAt;

        private CacheEntry(T data, Instant fetchedAt) {
            this.data = data;
            this.fetchedAt = fetchedAt;
        }
    }

    private static class SimState {
        private BigDecimal lastPrice;
        private Instant lastTime;
        private Instant lastRegimeChange;
        private Instant lastRealTime;
        private double drift;
        private double volatility;
        private double shockProbability;
        private double shockScale;
        private int scale;
        private Random random;
    }
}
