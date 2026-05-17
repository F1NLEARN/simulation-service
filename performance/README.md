# Investment Order Load Test

This directory contains local JMeter data for buy/sell load tests.

## Prepare Local Data

Run `simulation-service` with local schema update mode:

```bash
export KIS_ENABLED=false
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
sh gradlew bootRun
```

Seed 20 independent accounts and holdings:

```bash
docker cp /Users/seongjun/Desktop/spartacoding/finlearn/simulation-service/src/main/resources/sql/investment_order_load_test_seed.sql finlearn-postgres:/tmp/investment_order_load_test_seed.sql
docker exec finlearn-postgres psql -U postgres -d simulationdb -f /tmp/investment_order_load_test_seed.sql
```

Check seed data:

```bash
docker exec finlearn-postgres psql -U postgres -d simulationdb -c "select count(*) from season_investment_account;"
docker exec finlearn-postgres psql -U postgres -d simulationdb -c "select count(*) from holding;"
docker exec finlearn-postgres psql -U postgres -d simulationdb -c "select stock_code, current_price from stock_items where stock_code='005930';"
```

## JMeter CSV Data Set Config

Add a CSV Data Set Config:

```text
Filename: /Users/seongjun/Desktop/spartacoding/finlearn/simulation-service/performance/order-users.csv
Variable Names: userId,stockCode,quantity
Ignore first line: True
Recycle on EOF: True
Stop thread on EOF: False
Sharing mode: All threads
```

## Buy Request

```text
Method: POST
Path: /api/v1/investments/orders/buy
Header:
  Content-Type: application/json
  Accept: application/json
  X-User-Id: ${userId}
Body:
{
  "stockCode": "${stockCode}",
  "quantity": ${quantity}
}
```

Assertions:

```text
Response Code = 200
Response Text contains "success":true
Response Text contains "BUY"
```

## Sell Request

```text
Method: POST
Path: /api/v1/investments/orders/sell
Header:
  Content-Type: application/json
  Accept: application/json
  X-User-Id: ${userId}
Body:
{
  "stockCode": "${stockCode}",
  "quantity": ${quantity}
}
```

Assertions:

```text
Response Code = 200
Response Text contains "success":true
Response Text contains "SELL"
```

## Suggested Scenarios

Start small:

```text
Threads: 5
Ramp-up: 10
Loop Count: 2
```

Then increase gradually:

```text
Threads: 10 -> 20
Ramp-up: 10 -> 30
Loop Count: 5
```

Use separate runs for buy and sell. Running buy and sell at the same time against
the same account set is useful for concurrency testing, but failures may be
business failures rather than pure performance failures.

## Post-Test Checks

```bash
docker exec finlearn-postgres psql -U postgres -d simulationdb -c "select min(current_cash_balance), max(current_cash_balance) from season_investment_account;"
docker exec finlearn-postgres psql -U postgres -d simulationdb -c "select min(quantity), max(quantity) from holding where instrument_code='005930';"
docker exec finlearn-postgres psql -U postgres -d simulationdb -c "select trade_type, count(*) from trade_history group by trade_type;"
```
