INSERT INTO users (username, email, password, role, enabled, created_at, updated_at)
VALUES
('admin', 'admin@finnexus.com', '$2y$05$BpjXZSPJVd.TxcjjfXhYtuxF.cJcRJp2VddhjV77wnfxTddfbfEEi', 'ADMIN', true, NOW(), NOW()),
('demo', 'demo@finnexus.com', '$2y$05$cgVT.NJlEwnC1uUKy77.De98Wca.Hcct/3mjA6IC4OX0CJCDw6qAm', 'USER', true, NOW(), NOW());

INSERT INTO wallets (user_id, balance, equity, margin_used, created_at, updated_at)
VALUES
(1, 50000.0000, 50000.0000, 0.0000, NOW(), NOW()),
(2, 15000.0000, 15000.0000, 0.0000, NOW(), NOW());

INSERT INTO orders (user_id, symbol, side, type, status, price, executed_price, quantity, executed_at, created_at, updated_at)
VALUES
(2, 'EURUSD', 'BUY', 'MARKET', 'EXECUTED', NULL, 1.08200, 1.0000, NOW(), NOW(), NOW());

INSERT INTO trades (user_id, order_id, symbol, side, entry_price, exit_price, quantity, status, pnl, opened_at, closed_at, created_at, updated_at)
VALUES
(2, 1, 'EURUSD', 'BUY', 1.08200, NULL, 1.0000, 'OPEN', NULL, NOW(), NULL, NOW(), NOW());
