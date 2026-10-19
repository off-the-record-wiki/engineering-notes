# MySQL - Data Modeling for Practical Service Development

#### Partitioning Strategies and Index Optimization for Scalable Databases

```sql
mysql> SHOW COLUMNS FROM posts;
+---------------+-------------------------------------+------+-----+-------------------+-----------------------------------------------+
| Field         | Type                                | Null | Key | Default           | Extra                                         |
+---------------+-------------------------------------+------+-----+-------------------+-----------------------------------------------+
| post_id       | bigint                              | NO   | PRI | NULL              | auto_increment                                |
| user_id       | bigint                              | NO   | MUL | NULL              |                                               |
| category_id   | int                                 | NO   | MUL | NULL              |                                               |
| title         | varchar(200)                        | NO   | MUL | NULL              |                                               |
| content       | text                                | NO   |     | NULL              |                                               |
| comment_count | int                                 | YES  |     | 0                 |                                               |
| like_count    | int                                 | YES  |     | 0                 |                                               |
| view_count    | int                                 | YES  |     | 0                 |                                               |
| status        | enum('DRAFT','PUBLISHED','DELETED') | YES  | MUL | DRAFT             |                                               |
| is_featured   | tinyint(1)                          | YES  | MUL | 0                 |                                               |
| created_at    | timestamp                           | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED                             |
| updated_at    | timestamp                           | YES  |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
+---------------+-------------------------------------+------+-----+-------------------+-----------------------------------------------+
12 rows in set (0.01 sec)
```
* `comment_count`, `like_count`, `view_count`와 같은 경우가 성능을 위해 비정규화를 적용한 대표적인 사례이다.

#### 근거 : 조인 대신 의도적 비정규화를 택하는 이유
* 목록 조회에서 댓글/좋아요/조회 수를 매번 `JOIN` + `COUNT`로 집계하면, 게시글 몇 건을 위해 수백만 건의 원본을 스캔·집계해야 한다.
* 카운터 컬럼(`comment_count` 등)을 두면 조인·집계 없이 컬럼 하나만 읽으면 되고, 커버링 인덱스까지 활용할 수 있다.
* 즉 비싼 집계 계산을 "잦은 읽기" 시점에서 "드문 쓰기" 시점으로 이전하는 것이 핵심이다.

#### 근거 : 비정규화가 정당화되는 조건

| 조건 | 근거 |
|------|------|
| 읽기 >> 쓰기 | 비싼 집계 계산을 드문 쓰기 시점으로 이전 → 잦은 읽기가 저렴해짐 |
| 실시간 정확도가 덜 중요 | 조회수·좋아요가 1~2 오차 나도 서비스에 치명적이지 않음 |
| 집계 대상이 방대 | `COUNT` 대상 행이 많을수록 매번 세는 비용이 커짐 |
| 조회가 핵심 API | 목록 조회는 초당 수천 번 → 성능이 사용자 경험에 직결 |

#### 근거 : 대가는 정합성(Consistency) 관리

* 비정규화는 읽기 성능을 위해 **쓰기 복잡도 + 정합성 리스크를 사는 거래**이다. 원본과 카운터가 어긋날 수 있으므로 유지 전략이 필요하다.

| 방법 | 정확도 | 실시간성 | 리스크 / 특징 |
|------|--------|---------|--------------|
| 동일 트랜잭션 동시 갱신 | 높음 | 즉시 | 인기 행에 락 경합(hot row) 발생 |
| 비동기/배치 flush (Redis 등) | 중간 | 지연 | 실시간성 포기, DB 부하 최소 |
| DB 트리거 | 높음 | 즉시 | 로직이 숨어 유지보수 어려움 |
| 정기 재계산 (배치 `COUNT`) | 보정 시점만 정확 | 지연 | 어긋남 감수, 정합성 복구용 |

#### 파티셔닝 전략

```sql
-- 계좌별 거래 이력 테이블 (Hash 파티셔닝)
CREATE TABLE account_trade_history (
       history_id BIGINT UNSIGNED AUTO_INCREMENT,
       account_id BIGINT UNSIGNED NOT NULL,
       trade_id BIGINT UNSIGNED NOT NULL,
       trade_date DATE NOT NULL,
       symbol VARCHAR(10) NOT NULL,
       trade_type ENUM('BUY', 'SELL') NOT NULL,
       price DECIMAL(12,4) NOT NULL,
       volume INT UNSIGNED NOT NULL,
       trade_value DECIMAL(18,4) NOT NULL,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

       PRIMARY KEY (history_id, account_id)
) ENGINE=InnoDB PARTITION BY HASH(account_id) PARTITIONS 16;  -- 16개 파티션으로 분산
```

```sql
-- 시장별 거래 데이터 (List 파티셔닝)
CREATE TABLE market_specific_trades (
    trade_id BIGINT UNSIGNED AUTO_INCREMENT,
    symbol VARCHAR(10) NOT NULL,
    market_code VARCHAR(10) NOT NULL,
    trade_date DATE NOT NULL,
    trade_timestamp TIMESTAMP(6) NOT NULL,
    price DECIMAL(12,4) NOT NULL,
    volume INT UNSIGNED NOT NULL,
    trade_type ENUM('BUY', 'SELL') NOT NULL,

    PRIMARY KEY (trade_id, market_code),
    INDEX idx_symbol_timestamp (symbol, trade_timestamp),
    INDEX idx_date_volume (trade_date, volume)

) ENGINE=InnoDB
    PARTITION BY LIST COLUMNS(market_code) (
        PARTITION p_kospi VALUES IN ('KOSPI'),
        PARTITION p_kosdaq VALUES IN ('KOSDAQ'),
        PARTITION p_nasdaq VALUES IN ('NASDAQ'),
        PARTITION p_nyse VALUES IN ('NYSE'),
        PARTITION p_other VALUES IN ('OTHER', 'CRYPTO', 'FOREX')
        );
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/83afacf1-ccf4-4204-a664-a2184b314046.png)

#### 파티셔닝 개요

* 파티셔닝은 논리적으로 하나인 테이블을 여러 개의 물리 조각(파티션)으로 나눠 저장하는 기법이다. 
* 애플리케이션과 SQL 입장에서는 여전히 하나의 테이블이지만, 옵티마이저는 필요한 파티션만 골라 스캔(파티션 프루닝)하므로 대용량 테이블에서 조회·관리 비용이 크게 줄어든다.

| 얻는 것 | 설명 |
| --- | --- |
| **조회 성능** | 무관한 파티션을 스캔 대상에서 제외 (partition pruning) |
| **관리 효율** | 파티션 단위 백업·재구성·통계 갱신, 큰 테이블을 조각 단위로 다룸 |
| **보관 정책** | 오래된 파티션을 `DROP`으로 통째 삭제 → `DELETE`보다 훨씬 저렴 |
| **가용성** | 특정 파티션 유지보수가 전체 테이블 락으로 번지지 않음 |

#### RANGE - 범위 파티셔닝

* 연속된 값의 **구간(range)** 으로 분할한다. 날짜·시퀀스·금액처럼 순서가 있는 값에 적합하다.

```sql
CREATE TABLE stock_price (
    id          BIGINT       NOT NULL,
    ticker_id   BIGINT       NOT NULL,
    trade_date  DATE         NOT NULL,
    close_price DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id, trade_date)          -- 파티션 키는 PK/UK에 포함되어야 함
)
PARTITION BY RANGE (YEAR(trade_date)) (
    PARTITION p_2024 VALUES LESS THAN (2025),
    PARTITION p_2025 VALUES LESS THAN (2026),
    PARTITION p_2026 VALUES LESS THAN (2027),
    PARTITION p_max  VALUES LESS THAN MAXVALUE   -- 경계 밖 값 안전망
);
```

* 시계열 데이터의 사실상 표준. `WHERE trade_date BETWEEN ...` 조회가 특정 파티션에만 닿는다.
* 신규 기간이 오면 파티션을 **추가**하고, 만료 기간은 `ALTER TABLE ... DROP PARTITION`으로 즉시 정리한다.
* 보관 기간(retention) 정책이 명확해 오래된 데이터를 주기적으로 버려야 하는 경우나 로그·거래·시세처럼 **시간 축으로 계속 쌓이고**, 최근 데이터 위주로 조회하는 경우에 사용한다.
* 그러나 값이 한쪽 구간에 몰리면 파티션 크기가 **불균형**해진다(예: 특정 연도만 폭증).

#### LIST — 목록 파티셔닝

* 이산적인 값의 **집합(list)** 으로 분할한다. 값이 명확히 열거되는 범주형 컬럼에 적합하다.

```sql
CREATE TABLE stock_price (
    id          BIGINT      NOT NULL,
    ticker_id   BIGINT      NOT NULL,
    market      VARCHAR(10) NOT NULL,   -- KOSPI / KOSDAQ / KONEX ...
    trade_date  DATE        NOT NULL,
    PRIMARY KEY (id, market)
)
PARTITION BY LIST COLUMNS (market) (
    PARTITION p_kospi  VALUES IN ('KOSPI'),
    PARTITION p_kosdaq VALUES IN ('KOSDAQ'),
    PARTITION p_etc    VALUES IN ('KONEX', 'ETC')
);
```

* 각 파티션이 **어떤 값들을 담을지 명시적으로 열거**한다.
* 카테고리·지역·시장·상태 코드처럼 값의 종류가 유한하고 안정적일 때 자연스럽다.
* 값 → 파티션 매핑이 문서 없이도 DDL만 보면 드러나 **가독성**이 좋다.
* 시장 구분(KOSPI/KOSDAQ), 국가, 상품군처럼 **경계가 값 그 자체**인 경우거나 특정 범주만 자주 조회/관리해서 범주 단위로 분리·삭제하고 싶은 경우에 적합하다.
* 목록에 **없는 값**이 들어오면 오류에 대한 처리가 필요하며 새로운 범주가 자주 생겨나면 그 때마다 DDL 변경이 필요해 운영 부담이 될 수 있다. 범주 간 데이터량 편차가 커지면 파티션 불균형이 발생한다.

#### HASH — 해시 파티셔닝

* 파티션 키에 **해시 함수**를 적용해 파티션 수로 나눈 나머지로 분산한다. 값의 의미보다 **고른 분산**이 목적이다.

```sql
CREATE TABLE stock_price (
    id          BIGINT NOT NULL,
    ticker_id   BIGINT NOT NULL,
    trade_date  DATE   NOT NULL,
    PRIMARY KEY (id, ticker_id)
)
PARTITION BY HASH (ticker_id)
PARTITIONS 3;      -- p0, p1, p2 자동 생성 → ticker_id 를 3으로 해싱 분배
```

* 개발자가 구간/목록을 직접 정하지 않고 **엔진이 균등 분배**한다.
* 카디널리티가 높고 분포가 고른 키(예: 사용자 ID, 종목 ID)에 잘 맞는다.
* 특정 파티션에 트래픽이 쏠리는 **핫스팟**을 완화해 쓰기 부하를 분산한다.
* 자연스러운 범위/범주 경계가 없고, 그냥 **균등하게 나누고 싶은** 경우이거나 쓰기·조회 부하를 여러 파티션(나아가 여러 디스크)으로 **평탄화**하고 싶은 경우에 적합하다.
* 그러나 이 방식은 범위 조회에 약하다. `BETWEEN`·정렬은 해시로 흩어져 있어 여러 파티션을 스캔하게 된다.
* 또한, **파티션 수 변경 비용이 크다.** 개수를 바꾸면 대부분의 행이 재배치(리해싱)가 되기 떄문이다.
* 파티션 이름에 비즈니스 의미가 없어(p0, p1 …) 특정 데이터를 지목해 관리하기 어렵다.

#### 인덱스 최적화 설계 기법 - 왜 빨라지는지?

* 인덱스는 컬럼 값을 정렬해 별도로 보관하는 구조(대부분 B-Tree)다. 정렬돼 있으므로 이진 탐색으로 원하는 값을 `O(log N)`에 찾고, 리프에서 실제 행 위치(PK)로 점프한다.
* Full Table Scan : 모든 행을 읽어 조건 비교 → `O(N)`
* Index Range Scan : 정렬된 인덱스에서 필요한 구간만 → `O(log N + 결과수)`

#### 카디널리티와 선택도

* 카디널리티(cardinality) = 컬럼이 가진 서로 다른 값의 개수. 선택도(selectivity) = 고유값 수 / 전체 행 수 (0~1, 1에 가까울수록 좋다).
* 선택도가 높을수록 = 조건 하나로 결과가 잘게 걸러진다 = 인덱스 효과가 크다.

```sql
-- 선택도가 높은 컬럼이 앞쪽에 위치해야 한다.
CREATE INDEX idx_orders_user_status ON orders(user_id, status, created_at);
```
```sql
-- 카디널리티(현재 선택한 이 테이블에서 얼마나 고유한가) 확인
SELECT
    COUNT(DISTINCT email) / COUNT(*) as email_selectivity,
    COUNT(DISTINCT status) / COUNT(*) as status_selectivity
FROM users;

-- email(높은 카디널리티) > status(낮은 카디널리티)
CREATE INDEX idx_users_email_status ON users(email, status);
```

#### 인덱스가 안 타는 대표 패턴

* 인덱스가 있더라도 아래처럼 사용하면 옵티마이저가 인덱스를 포기하고 풀스캔을 한다.

```sql
-- ❌ 컬럼에 함수/연산 → 인덱스 무력화
WHERE YEAR(created_at) = 2026
-- ✅ 컬럼은 그대로, 값 쪽을 가공
WHERE created_at >= '2026-01-01' AND created_at < '2027-01-01'

-- ❌ 앞부분 와일드카드 → 정렬 이점 소멸
WHERE name LIKE '%kim'
-- ✅ 뒤쪽 와일드카드는 인덱스 사용 가능
WHERE name LIKE 'kim%'

-- ❌ 암시적 형변환 (컬럼이 문자열인데 숫자로 비교)
WHERE phone = 01012345678       -- phone 이 VARCHAR면 형변환으로 인덱스 회피
-- ✅ 타입을 맞춘다
WHERE phone = '01012345678'

-- ❌ OR 로 서로 다른 컬럼을 묶으면 인덱스 결합이 어렵다
WHERE user_id = ? OR email = ?
-- ✅ 각각 인덱스가 있다면 UNION 으로 분리 (또는 index_merge에 의존)
```

#### 주의 : 비용이 공짜가 아니다.

* 인덱스는 조회를 빠르게 하는 대신 다음과 같은 비용을 지불한다.

| 비용 | 설명 |
| --- | --- |
| **쓰기 저하** | `INSERT`/`UPDATE`/`DELETE` 때마다 인덱스도 함께 갱신 → 쓰기 많은 테이블은 인덱스 최소화 |
| **저장 공간** | 인덱스도 디스크를 차지. 넓은 복합 인덱스는 테이블만큼 커지기도 |
| **옵티마이저 혼란** | 비슷한 인덱스가 여럿이면 잘못된 인덱스를 고를 수 있음 |
| **유지보수** | 페이지 분할·단편화 → 주기적 재구성/통계 갱신 필요 |

> **원칙: 인덱스는 "필요해서 만드는" 것이지 "혹시 몰라서" 만드는 게 아니다.**
> 느린 쿼리를 `EXPLAIN`으로 확인 → 병목 지점에만 최소한으로 추가하는 게 정석.
