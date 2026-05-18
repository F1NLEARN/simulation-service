-- Simulation Service MVP UUID seed script
-- Test user UUID (all investment APIs): 33333333-3333-3333-3333-333333333333

-- 1) Seed tradable stock items
INSERT INTO stock_items (stock_item_id, stock_name, stock_code, asset_type, current_price, current_price_updated_at, created_at)
VALUES
    -- KOSPI 대형주
    ('11111111-1111-1111-1111-111111111111', '삼성전자',        '005930', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111112', 'SK하이닉스',      '000660', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111113', 'LG에너지솔루션',  '373220', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111114', '삼성바이오로직스','207940', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111115', '현대차',          '005380', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111116', '기아',            '000270', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111117', 'POSCO홀딩스',     '005490', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111118', '삼성SDI',         '006400', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111119', 'LG화학',          '051910', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111111a', 'NAVER',           '035420', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111111b', '카카오',          '035720', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111111c', '셀트리온',        '068270', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111111d', '현대모비스',      '012330', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111111e', 'KB금융',          '105560', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111111f', '신한지주',        '055550', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111120', '하나금융지주',    '086790', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111121', '우리금융지주',    '316140', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111122', 'LG전자',          '066570', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111123', 'SK텔레콤',        '017670', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111124', 'KT',              '030200', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111125', '삼성물산',        '028260', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111126', '두산에너빌리티',  '034020', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111127', '고려아연',        '010130', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111128', '한국전력',        '015760', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-111111111129', '크래프톤',        '259960', 'STOCK', NULL, NULL, NOW()),
    -- KOSDAQ 대형주
    ('11111111-1111-1111-1111-11111111112a', '에코프로비엠',    '247540', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111112b', '에코프로',        '086520', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111112c', 'HLB',             '028300', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111112d', '알테오젠',        '196170', 'STOCK', NULL, NULL, NOW()),
    ('11111111-1111-1111-1111-11111111112e', '리가켐바이오',    '141080', 'STOCK', NULL, NULL, NOW()),
    -- ETF
    ('22222222-2222-2222-2222-222222222222', 'KODEX 200',          '069500', 'ETF', NULL, NULL, NOW()),
    ('22222222-2222-2222-2222-222222222223', 'TIGER 200',          '102110', 'ETF', NULL, NULL, NOW()),
    ('22222222-2222-2222-2222-222222222224', 'KODEX 레버리지',     '122630', 'ETF', NULL, NULL, NOW()),
    ('22222222-2222-2222-2222-222222222225', 'KODEX 인버스',       '114800', 'ETF', NULL, NULL, NOW()),
    ('22222222-2222-2222-2222-222222222226', 'TIGER 미국S&P500',   '360750', 'ETF', NULL, NULL, NOW()),
    ('22222222-2222-2222-2222-222222222227', 'KODEX 미국S&P500TR', '379800', 'ETF', NULL, NULL, NOW()),
    ('22222222-2222-2222-2222-222222222228', 'TIGER 나스닥100',    '133690', 'ETF', NULL, NULL, NOW()),
    ('22222222-2222-2222-2222-222222222229', 'KODEX 코스닥150',    '229200', 'ETF', NULL, NULL, NOW())
ON CONFLICT (stock_code) DO NOTHING;

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
