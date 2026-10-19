# PostgreSQL - Sorting, Aggregation & JOIN Design Patterns

#### 정렬과 페이징, 글로벌 서비스 및 금융 회계 집계를 위한 시간함수

* `DATE_TRUNC('day', created_at)` : 00:00:00.
* `DATE_TRUNC('hour', created_at)` : 정각, `year`, `week`, `quarter`, `minute`도 가능.

#### 데이터 문서화를 위한 집계 함수와 그룹화, 쿼리튜닝의 기본적인 원칙

**1. WHERE와 HAVING : 최대한 일찍 거른다.**
* `WHERE`는 그룹을 만들기 전에 행을 거른다. 집계할 행의 수 자체가 줄어든다.
* `HAVING`은 그룹을 다 만든 뒤에 결과를 거른다. 모든 행을 집계한 다음에 버리는 셈이다.
* 개별 행의 조건은 `WHERE`, 집계 결과 조건만 `HAVING`에 둔다.

```sql
-- 비효율: 전체 타입을 다 집계한 뒤 버림
SELECT account_type, SUM(balance) FROM accounts
GROUP BY account_type
HAVING account_type = '예금';

-- 효율: 예금 행만 읽어서 집계
SELECT account_type, SUM(balance) FROM accounts
WHERE account_type = '예금'
GROUP BY account_type;
```

**2. GROUP BY와 인덱스: 정렬을 피할 수 있다.**
* `GROUP BY`는 같은 값끼리 모으기 위해 내부적으로 정렬이나 해시를 한다. 행이 많으면 이 작업이 비싸다.
* `(account_type, balance)` 인덱스가 있으면 이미 정렬된 인덱스를 순서대로 읽기만 하면 된다. 테이블 대신 인덱스만 읽는 커버링 인덱스로 처리될 수 있다.

**3. COUNT(*)와 COUNT(컬럼)은 다르다.**
* `COUNT(*)` : 전체 행 수
* `COUNT(phone)` : `phone`이 `NULL`이 아닌 수

#### 테이블 관계 설계와 기본키(PK), 외래키(FK) 제약 조건에 대한 연관관계

* `RESTRICT` : 참조하는 데이터가 있다면 삭제가 불가능
* `CASCADE` : 참조하는 행을 함께 삭제
* `SET NULL` : 참조하는 컬럼을 NULL로 변경

#### JOIN 정리

```sql
-- INNER JOIN : 두 테이블에서 ON 조건을 만족하는 행

SELECT
    c.name,
    a.account_id,
    a.account_type,
    a.balance
FROM customers AS c
    INNER JOIN accounts AS a
        ON a.customer_id = c.customer_id
ORDER BY c.customer_id, a.account_id;
```

```sql
-- LEFT JOIN : 왼쪽 테이블의 모든 행은 기본적으로 포함

SELECT
    a.account_id,
    a.account_type,
    COUNT(t.transaction_id) AS 거래건수
FROM accounts a
         LEFT JOIN transactions t ON t.account_id = a.account_id
GROUP BY a.account_id, a.account_type
ORDER BY a.account_id;

SELECT a.account_id, a.account_type
FROM accounts a
         LEFT JOIN transactions t ON t.account_id = a.account_id
WHERE t.transaction_id IS NULL;
```

```sql
-- RIGHT JOIN

SELECT
    c.name,
    a.account_id,
    a.account_type
FROM accounts a
         RIGHT JOIN customers c ON a.customer_id = c.customer_id
ORDER BY c.customer_id;
```

```sql
-- FULL OUTER JOIN : 양쪽 테이블의 모든 행을 포함

SELECT
    c.customer_id,
    c.name,
    a.account_id,
    a.account_type
FROM customers c
         FULL OUTER JOIN accounts a ON a.customer_id = c.customer_id
ORDER BY c.customer_id NULLS LAST, a.account_id;

SELECT 
    c.customer_id,
    c.name,
    a.account_id
FROM customers c
         FULL OUTER JOIN accounts a ON a.customer_id = c.customer_id
WHERE c.customer_id IS NULL OR a.account_id IS NULL;
```

```sql
-- CROSS JOIN : 양쪽의 모든 조합을 만들어내는 JOIN(카테시안 곱)

SELECT
    a.account_type,
    c.name
FROM (SELECT DISTINCT account_type FROM accounts) a
         CROSS JOIN (SELECT name FROM customers LIMIT 3) c
ORDER BY a.account_type, c.name;
```
