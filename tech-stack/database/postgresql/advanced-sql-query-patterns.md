# PostgreSQL - Advanced SQL Query Patterns

#### 서브 쿼리를 활용하여 쿼리 결과에 대한 조건 및 데이터 소스 활용

```sql
-- 인라인 뷰 : FROM에 사용되는 서브쿼리

SELECT account_id 거래건수
FROM (
    SELECT account_id, COUNT(*) AS 거래건수
    FROM transactions
    GROUP BY account_id
) AS 계좌별집계
WHERE 거래건수 >= 3
ORDER BY 거래건수 DESC;
```
```sql
-- 스칼라 서브쿼리 : SELECT에 사용하는 서브쿼리

SELECT
    account_id,
    account_type,
    balance,
    (SELECT ROUND(AVG(balance), 0) FROM accounts) AS 전채평균잔액,
    balance - (SELECT AVG(balance) FROM accounts) AS 평균대비차이
FROM accounts;

SELECT account_id, account_type, balance
FROM accounts a
WHERE EXISTS (
    SELECT 1
    FROM transactions t
    WHERE t.account_id = a.account_id
)
ORDER BY account_id;
```

* 비상관 구조라면 실행 횟수는 1번, 상관 구조라면 실행 횟수를 바깥 행마다 돌게 된다.
* PostgreSQL에서 비상관은 `InitPlan`, 상관은 `SubPlan`으로 표시된다.

#### CTE(WITH절)을 활용한 서브쿼리 블록화

```sql
WITH 계좌별거래합계 AS (
    SELECT account_id, SUM(amount) AS 총거래금액
    FROM transactions
    GROUP BY account_id
),
고객별합계 AS (
    SELECT a.customer_id, SUM(c.총거래금액) AS 고객총거래금액
    FROM accounts AS a
    JOIN 계좌별거래합계 AS c ON c.account_id = a.account_id
    GROUP BY a.customer_id
),
전체평균 AS (
    SELECT AVG(고객총거래금액) AS 평균거래금액
    FROM 고객별합계
)
SELECT
    cu.name          AS 고객명,
    g.고객총거래금액,
    ROUND(p.평균거래금액, 0) AS 전체평균
FROM 고객별합계 g
         JOIN customers cu ON cu.customer_id = g.customer_id
         CROSS JOIN 전체평균 p
WHERE g.고객총거래금액 >= p.평균거래금액
ORDER BY g.고객총거래금액 DESC;
```
* PostgreSQL 12 이상과 MySQL 8에서는 한 번만 참조하는 비재귀 CTE를 옵티마이저가 인라인 뷰와 똑같이 처리하므로 실행 계획과 성능에 차이가 없다.
* 같은 CTE를 2번 이상 참조하면 결과를 한 번 계산해 저장하고 재사용하므로 계산이 무거울 때는 유리하지만 결과가 크면 인덱스와 조건 밀어넣기를 못 써서 오히려 불리할 수 있다.
* PostgreSQL 11 이하는 CTE를 항상 먼저 계산해 저장하기 때문에 바깥 조건이 안으로 전달되지 않아 인라인 뷰보다 느려질 수 있다.
* 따라서 최신 DB에서는 가독성을 위해 CTE를 써도 손해가 없고 성능이 의심될 때는 `EXPLAIN`에서 결과를 따로 저장하는 단계가 찍히는지 확인하면 된다.

#### 윈도우 함수를 사용한 집계 및 행 데이터 유지 패턴

```sql
SELECT
    transaction_id,
    account_id,
    amount,
    SUM(amount) OVER (PARTITION BY account_id) AS 계좌별총거래금액
FROM transactions
ORDER BY account_id, transaction_id;

SELECT
    account_id,
    amount,
    SUM(amount) OVER () AS 계좌별총거래금액
FROM transactions
LIMIT 5;

SELECT
    transaction_id,
    account_id,
    amount,
    created_at,
    row_number() over (ORDER BY amount DESC) AS 전체순번
FROM transactions
ORDER BY amount DESC
LIMIT 5;


SELECT
    transaction_id,
    account_id,
    amount,
    created_at,
    row_number() over (PARTITION BY account_id ORDER BY amount DESC) AS 전체순번
FROM transactions
ORDER BY amount DESC
LIMIT 5;

SELECT
    transaction_id,
    account_id,
    amount,
    RANK()       OVER (ORDER BY amount DESC) AS rank순위,
    DENSE_RANK() OVER (ORDER BY amount DESC) AS dense_rank순위
FROM transactions
ORDER BY amount DESC
LIMIT 8;
```
* `PARTITION BY`는 윈도 함수가 아니라, `SUM()`이나 `RANK()`와 같은 윈도 함수의 `OVER()` 안에서 어떤 그룹 단위로 계산할지를 나누는 절이다.
* `GROUP BY`는 그룹마다 한 행으로 결과를 줄이지만, `PARTITION BY`는 원래 행을 그대로 두고 각 행 옆에 그룹 단위 계산 결과를 붙인다.
* `CREATE TABLE`에 사용하는 `PARTITION BY`는 테이블을 물리적으로 나눠서 저장하는 파티셔닝 기법으로 이름만 같고 윈도 함수와는 관계가 없다. 

#### 누적 및 데이터 분위 나누기와 프레임

```
transaction_id  account_id  created_at  amount
1               1           09:00       500,000
2               1           09:05       150,000
3               1           09:10        80,000
4               1           09:15       200,000
5               1           09:20       120,000
```

**1. 누적: 처음부터 지금까지 쌓인 값**
* `ORDER BY`로 순서를 정하고 프레임을 "첫 행 ~ 현재 행"으로 두면 각 행에 그때까지의 합계가 붙는다.
* 분모 측의 `SUM(...) OVER (PARTITION BY account_id)`는 `ORDER BY`가 없어서 파티션 전체 합계가 된다.
* `ORDER BY`에 `transaction_id`를 붙이고 `ROWS`를 명시한 이유는 같은 시각의 거래가 한꺼번에 묶이는 기본값(RANGE) 문제를 막기 위해서이다.
```sql
SELECT transaction_id, amount,
       SUM(amount) OVER (
           PARTITION BY account_id
           ORDER BY created_at, transaction_id
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS 누적금액,
       ROUND(100.0 * SUM(amount) OVER (
           PARTITION BY account_id
           ORDER BY created_at, transaction_id
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) / SUM(amount) OVER (PARTITION BY account_id), 1) AS 누적비율
FROM transactions;
```
**2. 데이터 분위 나누기: 전체 중 어디쯤인가**
* 행을 크기순으로 줄 세운 뒤 몇 등분 중 어디에 속하는지(NTILE) 또는 상위 몇 %인지(PERCENT_RANK, CUME_DIST)를 붙인다.
```sql
SELECT transaction_id, amount,
       NTILE(4)       OVER (ORDER BY amount DESC) AS 사분위,
       PERCENT_RANK() OVER (ORDER BY amount DESC) AS 백분위순위,
       CUME_DIST()    OVER (ORDER BY amount DESC) AS 누적분포
FROM transactions
WHERE account_id = 1;
```
**3. 프레임: 행마다 움직이는 계산 범위**
* 현재 행 기준으로 앞뒤 몇 행(또는 얼마의 기간)을 계산에 넣을지를 정한다. 누적은 프레임의 한 가지 경우이고, 범위를 앞뒤로 좁히게 되면 이동 평균이 된다.
```sql
SELECT transaction_id, amount,
       ROUND(AVG(amount) OVER (
           PARTITION BY account_id
           ORDER BY created_at, transaction_id
           ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
       )) AS 이동평균3,
       SUM(amount) OVER (
           PARTITION BY account_id
           ORDER BY created_at, transaction_id
           ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
       ) AS 남은합계
FROM transactions;
```
