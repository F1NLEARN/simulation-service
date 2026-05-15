-- Local-only seed for investment order load tests.
-- It creates 20 independent users/accounts and preloads holdings so buy and sell
-- JMeter scenarios can run without sharing one account.

WITH load_users AS (
    SELECT
        i,
        ('33333333-3333-3333-3333-' || lpad(i::text, 12, '0'))::uuid AS user_id,
        ('aaaaaaaa-aaaa-aaaa-aaaa-' || lpad(i::text, 12, '0'))::uuid AS account_id,
        ('bbbbbbbb-bbbb-bbbb-bbbb-' || lpad(i::text, 12, '0'))::uuid AS holding_id,
        ('cccccccc-cccc-cccc-cccc-' || lpad(i::text, 12, '0'))::uuid AS holding_stock_id
    FROM generate_series(1, 20) AS i
),
target_accounts AS (
    SELECT account_id
    FROM season_investment_account
    WHERE investor_id IN (SELECT user_id FROM load_users)
)
DELETE FROM trade_history
WHERE account_id IN (SELECT account_id FROM target_accounts);

WITH load_users AS (
    SELECT
        i,
        ('33333333-3333-3333-3333-' || lpad(i::text, 12, '0'))::uuid AS user_id,
        ('aaaaaaaa-aaaa-aaaa-aaaa-' || lpad(i::text, 12, '0'))::uuid AS account_id
    FROM generate_series(1, 20) AS i
),
target_accounts AS (
    SELECT account_id
    FROM season_investment_account
    WHERE investor_id IN (SELECT user_id FROM load_users)
)
DELETE FROM trade_histories
WHERE account_id IN (SELECT account_id FROM target_accounts);

WITH load_users AS (
    SELECT
        i,
        ('33333333-3333-3333-3333-' || lpad(i::text, 12, '0'))::uuid AS user_id,
        ('aaaaaaaa-aaaa-aaaa-aaaa-' || lpad(i::text, 12, '0'))::uuid AS account_id
    FROM generate_series(1, 20) AS i
),
target_accounts AS (
    SELECT account_id
    FROM season_investment_account
    WHERE investor_id IN (SELECT user_id FROM load_users)
)
DELETE FROM holding
WHERE account_id IN (SELECT account_id FROM target_accounts);

WITH load_users AS (
    SELECT
        i,
        ('33333333-3333-3333-3333-' || lpad(i::text, 12, '0'))::uuid AS user_id,
        ('aaaaaaaa-aaaa-aaaa-aaaa-' || lpad(i::text, 12, '0'))::uuid AS account_id
    FROM generate_series(1, 20) AS i
),
target_accounts AS (
    SELECT account_id
    FROM season_investment_account
    WHERE investor_id IN (SELECT user_id FROM load_users)
)
DELETE FROM holding_stocks
WHERE account_id IN (SELECT account_id FROM target_accounts);

DELETE FROM season_investment_account
WHERE investor_id IN (
    SELECT ('33333333-3333-3333-3333-' || lpad(i::text, 12, '0'))::uuid
    FROM generate_series(1, 20) AS i
);

INSERT INTO stock_items (stock_item_id, stock_name, stock_code, asset_type, current_price, current_price_updated_at, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', '삼성전자', '005930', 'STOCK', 279000, NOW(), NOW())
ON CONFLICT (stock_code) DO UPDATE
SET stock_name = EXCLUDED.stock_name,
    asset_type = EXCLUDED.asset_type,
    current_price = EXCLUDED.current_price,
    current_price_updated_at = EXCLUDED.current_price_updated_at;

WITH load_users AS (
    SELECT
        i,
        ('33333333-3333-3333-3333-' || lpad(i::text, 12, '0'))::uuid AS user_id,
        ('aaaaaaaa-aaaa-aaaa-aaaa-' || lpad(i::text, 12, '0'))::uuid AS account_id
    FROM generate_series(1, 20) AS i
)
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
SELECT
    account_id,
    user_id,
    '부하테스트',
    '11111111-1111-1111-1111-111111111111',
    1,
    'ACTIVE',
    1000000000,
    1000000000,
    279000000,
    1279000000,
    0,
    0,
    0.00,
    NOW()
FROM load_users;

WITH load_users AS (
    SELECT
        i,
        ('aaaaaaaa-aaaa-aaaa-aaaa-' || lpad(i::text, 12, '0'))::uuid AS account_id,
        ('bbbbbbbb-bbbb-bbbb-bbbb-' || lpad(i::text, 12, '0'))::uuid AS holding_id
    FROM generate_series(1, 20) AS i
)
INSERT INTO holding (
    holding_id,
    account_id,
    holding_name,
    season_id,
    season_number,
    instrument_code,
    quantity,
    average_buy_price,
    current_price,
    total_buy_amount,
    valuation_amount,
    unrealized_profit_loss,
    return_rate,
    created_at
)
SELECT
    holding_id,
    account_id,
    '삼성전자',
    '11111111-1111-1111-1111-111111111111',
    1,
    '005930',
    1000,
    279000,
    279000,
    279000000,
    279000000,
    0,
    0.00,
    NOW()
FROM load_users;

WITH load_users AS (
    SELECT
        i,
        ('aaaaaaaa-aaaa-aaaa-aaaa-' || lpad(i::text, 12, '0'))::uuid AS account_id,
        ('cccccccc-cccc-cccc-cccc-' || lpad(i::text, 12, '0'))::uuid AS holding_stock_id
    FROM generate_series(1, 20) AS i
)
INSERT INTO holding_stocks (
    holding_id,
    account_id,
    holding_name,
    season_id,
    season_number,
    instrument_code,
    quantity,
    average_buy_price,
    current_price,
    total_buy_amount,
    valuation_amount,
    unrealized_profit_loss,
    return_rate,
    created_at
)
SELECT
    holding_stock_id,
    account_id,
    '삼성전자',
    '11111111-1111-1111-1111-111111111111',
    1,
    '005930',
    1000,
    279000,
    279000,
    279000000,
    279000000,
    0,
    0.00,
    NOW()
FROM load_users;
