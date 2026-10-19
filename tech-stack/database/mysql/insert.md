# MySQL - INSERT

### MySQL - INSERT

#### 다중 행 삽입 (Bulk Insert) - 네트워크 및 I/O 최적화

* 여러 건의 데이터를 하나의 쿼리로 묶어서 보내는 것을 말한다.
* DB 서버와 통신할 때 가장 큰 비용을 차지하는 것은 데이터 자체가 아니라 네트워크 왕복(Round-Trip)과 DB 내부의 쿼리 파싱 및 트랜잭션 커밋 비용이다. 
* 1,000건의 데이터를 1,000번의 쿼리로 쪼개서 보내면 이 비용이 1,000배로 발생하지만, Bulk Insert를 사용하면 단 1번으로 줄일 수 있다.

```sql
-- 100건의 데이터를 1번의 쿼리로 묶어서 실행 (속도 수십~수백 배 향상)
INSERT INTO stock_trade_history (stock_code, trade_date, closing_price) 
VALUES 
    ('005930', '2026-04-12', 80000),
    ('000660', '2026-04-12', 150000),
    ('035420', '2026-04-12', 200000);
```
* 인덱스 재정렬(B-Tree 갱신)도 데이터를 한꺼번에 모아서 처리하므로 디스크 쓰기 속도가 훨씬 빠르다.

> **주의 1 : 배치가 무조건 클수록 좋은 건 아니다**
>
> * 한 쿼리가 너무 커지면 오히려 문제가 생긴다.
> * `max_allowed_packet` 크기를 넘으면 쿼리 자체가 실패한다.
> * 한 트랜잭션이 길어져 락 유지 시간·언두 로그가 늘고, 복제 지연(replication lag)을 유발할 수 있다.

> **주의 2 : 부분 실패 = 전체 롤백**
> 
> * Bulk Insert는 하나의 문장이라 원자적으로 처리된다. 100건 중 1건이라도 제약 조건에 걸리면 100건 전부 롤백된다.
> * 중복 키를 무시하거나 갱신하고 싶다면 상황에 맞는 문법을 쓴다.

```sql
-- 중복 키면 무시하고 넘어감
INSERT IGNORE INTO stock_trade_history ...;

-- 중복 키면 특정 컬럼을 갱신 (Upsert)
INSERT INTO stock_trade_history (stock_code, trade_date, closing_price)
VALUES ('005930', '2026-04-12', 80000)
ON DUPLICATE KEY UPDATE closing_price = VALUES(closing_price);
```

> **주의 3 (Java/Spring) : JDBC 배치는 그냥 두면 묶이지 않는다**
>
> * JPA `saveAll()`이나 `JdbcTemplate.batchUpdate()`를 써도, MySQL 드라이버 옵션이 없으면 실제로는 INSERT가 한 건씩 개별 전송된다.
> * 접속 URL에 아래 옵션을 켜야 드라이버가 여러 INSERT를 위 예시처럼 하나의 멀티 VALUES 쿼리로 다시 써서(rewrite) 보낸다.

```yml
jdbc:mysql://.../db?rewriteBatchedStatements=true
```

> * JPA를 쓴다면 `hibernate.jdbc.batch_size` 설정과 함께, IDENTITY 전략(auto_increment)은 배치를 막으므로 주의한다.

#### 병합 삽입 (Upsert) - 비즈니스 로직 최적화

> **전제 : UNIQUE(또는 PK) 제약이 반드시 있어야 한다**

* 수집한 데이터가 이미 DB에 있으면 수정(Update), 없으면 삽입(Insert)하는 로직이 자주 필요하다.
* 이를 애플리케이션에서 "SELECT로 확인 → INSERT 또는 UPDATE"로 처리하면 두 가지 문제가 생긴다.
  * 통신이 최소 2배로 늘어난다.
  * SELECT와 INSERT 사이에 다른 트랜잭션이 끼어들 수 있어 동시성 문제(Race Condition)가 발생한다.
* Upsert는 이 판정과 삽입/수정을 DB에서 원자적(Atomic)으로 한 번에 처리한다.
* Upsert는 "중복"을 UNIQUE / PRIMARY KEY 충돌로 판단한다. 해당 제약이 없으면 항상 INSERT만 되고 UPDATE로 넘어가지 않는다.

> **1. ON DUPLICATE KEY UPDATE (진짜 Upsert)**
> 
> * 참고 : 예전에 쓰던 `VALUES(closing_price)` 함수 표기는 MySQL 8.0.20부터 deprecated 되었다. 위처럼 AS new 별칭 방식으로 쓰는 것이 좋다.
> * 반환되는 영향받은 행 수로 무슨 일이 일어났는지 구분할 수 있다.
>   * 1 = 새로 INSERT됨
>   * 2 = 기존 행이 UPDATE됨
>   * 0 = 값이 같아 변화 없음

```sql
-- 테이블에 (stock_code, trade_date) 조합으로 UNIQUE 인덱스가 걸려 있다고 가정

-- MySQL 8.0.20+ 권장 문법: 새 행에 별칭을 주고 참조
INSERT INTO stock_trade_history (stock_code, trade_date, closing_price, trade_volume)
VALUES ('005930', '2026-04-12', 81000, 5000000) AS new
ON DUPLICATE KEY UPDATE
    closing_price = new.closing_price,              -- 새로 들어온 값으로 덮어씀
    trade_volume = trade_volume + new.trade_volume; -- 기존 누적값에 방금 값을 더함
```

> **2. INSERT IGNORE (충돌 시 무시)**
> 
> * 주의 : IGNORE는 중복 키뿐 아니라 다른 오류까지 경고로 바꿔 삼킨다.
> * 예 : NOT NULL 위반, 타입 변환 실패, 값 길이 초과 등이 에러가 아니라 조용한 데이터 손실/왜곡으로 이어질 수 있다.
> * "중복만 무시" 의도라면 IGNORE의 부작용을 인지하고, 데이터 정합성이 중요하면 ON DUPLICATE KEY UPDATE를 쓰는 편이 안전하다.

```sql
-- (stock_code, trade_date)가 이미 있으면 그냥 조용히 건너뜀
INSERT IGNORE INTO stock_trade_history (stock_code, trade_date, closing_price)
VALUES ('005930', '2026-04-12', 80000);
```

> **주의할 점**
>
> * **AUTO_INCREMENT 갭** : 실제로 UPDATE로 처리되거나 IGNORE로 무시돼도, InnoDB 기본 설정에서는 auto_increment 값이 미리 소모되어 PK에 구멍(gap)이 생길 수 있다. (순번이 연속임을 가정하면 안 됨)
> * **데드락** : 동시성이 높고 UNIQUE 키가 여러 개인 테이블에서 ON DUPLICATE KEY UPDATE는 갭 락으로 인해 데드락이 발생할 수 있다.
> * **REPLACE INTO와 혼동 금지** : REPLACE는 내부적으로 DELETE 후 INSERT라서, 지정하지 않은 컬럼이 기본값으로 초기화되고 PK가 바뀌며 트리거도 다르게 동작한다. "기존 행을 유지하며 일부만 수정"하려면 REPLACE가 아니라 ON DUPLICATE KEY UPDATE다.
