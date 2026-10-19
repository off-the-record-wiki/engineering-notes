# MySQL - Covering Index & RDB vs ElasticSearch Diff

### MySQL - Covering Index

#### 커버링 인덱스란?

* 데이터베이스가 인덱스를 통해 데이터를 찾는 과정은 크게 두 단계로 나뉜다.
  * 인덱스 탐색 : 인덱스 트리를 타서 원하는 조건의 데이터를 찾고 그 데이터가 저장된 원본 테이블의 실제 주소를 알아낸다.
  * 테이블 접근 : 알아낸 주소를 가지고 원본 테이블로 찾아가서 나머지 필요한 컬럼의 데이터를 읽어온다.
* 커버링 인덱스는 "테이블 접근"을 아예 생략하도록 만드는 인덱스이다.
* 즉, 쿼리의 `SELECT`, `WHERE`, `ORDER BY`, `GROUP BY` 등에 사용되는 모든 컬럼이 이미 하나의 인덱스 안에 다 포함(Cover)되어 있는 상태를 말한다.
* 원본 테이블에 갈 필요 없이 인덱스만 쓱 읽고 바로 결과를 반환하므로 디스크 I/O가 획기적으로 줄어들어 속도가 무척 빠르다.

#### 근거 : 왜 빠른가(랜덤 I/O 제거)

* "테이블 접근" 단계는 인덱스가 알려준 주소로 원본 데이터를 찾아가는 과정인데, 이 접근이 랜덤 I/O를 유발한다.
* 인덱스는 정렬되어 순차적으로 읽히지만, 그 인덱스가 가리키는 실제 행들은 디스크 여기저기에 흩어져 있기 때문이다.
* 조회 대상이 많을수록 이 "인덱스 → 원본 테이블" 왕복이 행 수만큼 반복되어 성능을 크게 떨어뜨린다. 커버링 인덱스는 이 왕복 자체를 없애므로, 조회 건수가 많은 쿼리일수록 효과가 극적으로 커진다.

#### 근거 : InnoDB 보조 인덱스에는 이미 PK가 들어있다

* InnoDB 보조 인덱스는 이미 리프 노드에 인덱스 컬럼과 기본 키(PK) 값을 함께 저장한다. 원본 행을 찾아갈 때 이 PK로 클러스터형 인덱스를 다시 타기 때문이다.
* `INDEX (stock_code, trade_date)`는 사실상 `(stock_code, trade_date, id)`를 담고 있는 셈이다. PK 컬럼은 인덱스에 명시하지 않아도 이미 커버된다.

#### 근거 검증 : 커버링 인덱스인지 확인하는 법 (`Using index`)

```sql
-- (O) 커버링: SELECT/WHERE 컬럼이 모두 인덱스 안에 있음 → Extra: Using index
EXPLAIN SELECT stock_code, trade_date
FROM stock_trade_history
WHERE stock_code = '005930';

-- (X) 비커버링: closing_price는 인덱스에 없어 원본 테이블 접근 필요 → Extra: NULL (또는 Using where)
EXPLAIN SELECT stock_code, trade_date, closing_price
FROM stock_trade_history
WHERE stock_code = '005930';
```

* `Using index` : 커버링 인덱스(원본 테이블 접근 없음)
* `Using index condition` : ICP(Index Condition Pushdown). 인덱스로 조건을 미리 걸러주긴 하지만 최종적으로는 원본 테이블에 접근함을 의미한다.

```sql
CREATE TABLE stock_trade_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    closing_price DECIMAL(10, 2),
    trade_volume BIGINT,
    
    INDEX idx_stock_trade (stock_code, trade_date)
);
```

#### 트레이드오프

* 커버링 인덱스를 만들겠다고 SELECT에 필요한 모든 컬럼을 인덱스에 다 때려 넣으면 안 된다.
* 그렇게 되면 인덱스 자체가 거대해져서 메모리를 과도하게 차지하고, `INSERT`, `UPDATE`시 데이터를 써야 할 곳이 많아져 쓰기 성능이 심각하게 떨어진다.
* 조회 빈도가 압도적으로 높고, 성능에 크리티컬한 핵심 API에 대해서만 전략적으로 사용하는 것이 좋다.
* `SELECT *` 는 커버링 인덱스를 깨뜨린다. 인덱스에 없는 컬럼이 하나라도 끼면 결국 원본 테이블에 접근해야 하므로, 커버링을 노린다면 **필요한 컬럼만 명시적으로 SELECT** 해야 한다.
