CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL UNIQUE,
  email VARCHAR(120) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE wallets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  balance DECIMAL(19,4) NOT NULL,
  equity DECIMAL(19,4) NOT NULL,
  margin_used DECIMAL(19,4) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE wallet_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  wallet_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  amount DECIMAL(19,4) NOT NULL,
  note VARCHAR(200),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  symbol VARCHAR(15) NOT NULL,
  side VARCHAR(10) NOT NULL,
  type VARCHAR(10) NOT NULL,
  status VARCHAR(15) NOT NULL,
  price DECIMAL(19,5),
  executed_price DECIMAL(19,5),
  quantity DECIMAL(19,4) NOT NULL,
  executed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE trades (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  symbol VARCHAR(15) NOT NULL,
  side VARCHAR(10) NOT NULL,
  entry_price DECIMAL(19,5) NOT NULL,
  exit_price DECIMAL(19,5),
  quantity DECIMAL(19,4) NOT NULL,
  status VARCHAR(10) NOT NULL,
  pnl DECIMAL(19,4),
  opened_at TIMESTAMP NULL,
  closed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_trade_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_trade_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE robo_strategies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  enabled BOOLEAN NOT NULL,
  symbol VARCHAR(15) NOT NULL,
  timeframe VARCHAR(5) NOT NULL,
  fast_period INT,
  slow_period INT,
  rsi_period INT,
  rsi_overbought INT,
  rsi_oversold INT,
  last_signal VARCHAR(10),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_strategy_user FOREIGN KEY (user_id) REFERENCES users(id)
);
