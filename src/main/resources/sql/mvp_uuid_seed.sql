-- Simulation Service MVP UUID seed script
-- Test user UUID (all investment APIs): 33333333-3333-3333-3333-333333333333

-- 1) Ensure user_id columns are UUID type (PostgreSQL)
ALTER TABLE investment_accounts
    ALTER COLUMN user_id TYPE uuid
    USING user_id::uuid;

ALTER TABLE favorite_stocks
    ALTER COLUMN user_id TYPE uuid
    USING user_id::uuid;

-- 2) Seed tradable stock items
INSERT INTO stock_items (stock_item_id, stock_name, stock_code, asset_type, current_price, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', '삼성전자', '005930', 'STOCK', 73500.00, NOW()),
    ('22222222-2222-2222-2222-222222222222', 'KODEX 200', '069500', 'ETF', 35000.00, NOW())
ON CONFLICT (stock_code) DO UPDATE
SET stock_name = EXCLUDED.stock_name,
    asset_type = EXCLUDED.asset_type,
    current_price = EXCLUDED.current_price;

-- 3) Seed one ACTIVE investment account for the same UUID user
INSERT INTO investment_accounts (investment_account_id, user_id, status, seed_money, cash_balance, created_at)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333', 'ACTIVE', 10000000.00, 10000000.00, NOW())
ON CONFLICT (investment_account_id) DO UPDATE
SET user_id = EXCLUDED.user_id,
    status = EXCLUDED.status,
    seed_money = EXCLUDED.seed_money,
    cash_balance = EXCLUDED.cash_balance;

-- 4) Seed one favorite stock for the same UUID user
INSERT INTO favorite_stocks (favorite_stock_id, user_id, asset_type, symbol, created_at)
VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '33333333-3333-3333-3333-333333333333', 'STOCK', '005930', NOW())
ON CONFLICT ON CONSTRAINT uk_favorite_stock_user_symbol DO NOTHING;
