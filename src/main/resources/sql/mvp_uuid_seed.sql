-- Simulation Service MVP UUID seed script
-- Test user UUID (all investment APIs): 33333333-3333-3333-3333-333333333333

-- 1) Seed tradable stock items
INSERT INTO stock_items (stock_item_id, stock_name, stock_code, asset_type, current_price, current_price_updated_at, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', '삼성전자', '005930', 'STOCK', 73500, NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'KODEX 200', '069500', 'ETF', 35000, NOW(), NOW())
ON CONFLICT (stock_code) DO UPDATE
SET stock_name = EXCLUDED.stock_name,
    asset_type = EXCLUDED.asset_type,
    current_price = EXCLUDED.current_price,
    current_price_updated_at = EXCLUDED.current_price_updated_at;

-- 2) Seed one ACTIVE investment account for the same UUID user
INSERT INTO season_investment_account (
    account_id,
    investor_id,
    investor_name,
    season_id,
    season_number,
    account_status,
    initial_seed_money,
    current_cash_balance,
    total_valuation_amount,
    total_asset_amount,
    realized_profit_loss,
    unrealized_profit_loss,
    total_return_rate,
    created_at
)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '33333333-3333-3333-3333-333333333333',
    '테스트유저',
    '44444444-4444-4444-4444-444444444444',
    1,
    'ACTIVE',
    10000000,
    10000000,
    0,
    10000000,
    0,
    0,
    0.00,
    NOW()
)
ON CONFLICT (account_id) DO UPDATE
SET investor_id = EXCLUDED.investor_id,
    investor_name = EXCLUDED.investor_name,
    season_id = EXCLUDED.season_id,
    season_number = EXCLUDED.season_number,
    account_status = EXCLUDED.account_status,
    initial_seed_money = EXCLUDED.initial_seed_money,
    current_cash_balance = EXCLUDED.current_cash_balance,
    total_valuation_amount = EXCLUDED.total_valuation_amount,
    total_asset_amount = EXCLUDED.total_asset_amount,
    realized_profit_loss = EXCLUDED.realized_profit_loss,
    unrealized_profit_loss = EXCLUDED.unrealized_profit_loss,
    total_return_rate = EXCLUDED.total_return_rate;

-- 3) Seed one favorite stock for the same UUID user
INSERT INTO favorite_stocks (favorite_stock_id, user_id, asset_type, symbol, created_at)
VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '33333333-3333-3333-3333-333333333333', 'STOCK', '005930', NOW())
ON CONFLICT ON CONSTRAINT uk_favorite_stock_user_symbol DO NOTHING;
