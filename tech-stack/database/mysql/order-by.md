# MySQL - ORDER BY

### MySQL - ORDER BY

#### ORDER BY와 인덱스의 관계

* 단순히 `ORDER BY`를 쓰는 것은 쉽지만, 데이터가 수백만 건일 때 아무렇게나 정렬을 요청하면 DB 서버에 큰 부하가 걸린다.

> **최악의 상황 : Filesort(파일소트)**
>
> * 인덱스를 정렬에 활용하지 못하면, DB는 조건에 맞는 데이터를 일단 메모리(또는 디스크)로 끌고 온 뒤 별도의 정렬 작업을 수행한다.
> * 이 때, "Filesort"라는 이름 때문에 항상 디스크(파일)를 쓴다고 오해하기 쉬운데, 실제 동작은 데이터 양에 따라 갈린다.
>   * 정렬 대상이 `sort_buffer_size` 안에 들어오면 → 메모리에서 정렬 (비교적 빠름)
>   * 버퍼를 초과하면 → 디스크 임시 파일로 나눠 정렬한 뒤 병합(merge sort) (느림) 

```sql
-- (인덱스: stock_code, trade_date)
-- 종목은 찾았지만, 인덱스에 없는 거래량(trade_volume)으로 정렬하라고 요청한 경우
SELECT * FROM stock_trade_history 
WHERE stock_code = '005930' 
ORDER BY trade_volume DESC;
```

> **최고의 상황 : Index Sort (인덱스 정렬 타기)**
>
> * 복합 인덱스 (stock_code, trade_date)는 종목코드로 모아둔 뒤, 그 안에서 거래일자 순으로 이미 정렬해 둔 상태다.
> * 이미 정렬된 순서를 그대로 읽기만 하면 되므로 정렬 작업(Filesort)을 아예 생략할 수 있다.

```sql
EXPLAIN
SELECT * FROM stock_trade_history
WHERE stock_code = '005930'
ORDER BY trade_volume DESC;
```

* `Using filesort` → 인덱스로 정렬하지 못하고 별도 정렬 수행 (나쁨)
* `Using index` 또는 정렬 관련 표기 없음 → 인덱스 순서를 그대로 활용 (좋음)
* 디스크 병합 정렬까지 발생했는지는 상태 변수로 관측할 수 있다. `Sort_merge_passes`가 크게 늘면 `sort_buffer_size`가 부족하다는 신호다.

```sql
SHOW STATUS LIKE 'Sort_merge_passes';
```

* 인덱스가 있어도 아래 상황에서는 Using filesort가 발생한다.
  * WHERE 동등 조건 컬럼과 ORDER BY 컬럼이 복합 인덱스의 컬럼 순서와 어긋날 때
  * 정렬 컬럼에 함수 · 연산을 적용할 때 → `ORDER BY DATE(created_at)``
  * 정렬 기준이 여러 테이블 컬럼에 걸쳐 있을 때
  * 혼합 방향 정렬(ORDER BY a ASC, b DESC)인데 내림차순 인덱스가 없을 때

#### ORDER BY와 LIMIT의 시너지(페이징/Top-N 쿼리)

* `ORDER BY`는 보통 가져올 개수를 제한하는 `LIMIT`와 함께 쓰인다.
* 이 때, 인덱스 정렬을 타는지 여부에 따라 성능 차이가 크게 벌어진다.

> **A. 인덱스를 타지 못하는 Filesort + LIMIT**

```sql
SELECT * FROM stock_trade_history
WHERE stock_code = '005930'
ORDER BY trade_volume DESC
LIMIT 10;
```
* 현재 위의 쿼리는 조건에 맞는 삼성전자 데이터 2,500건을 모두 읽어야 어떤 게 상위 10개인지 알 수 있다.
* 단, MySQL은 `filesort + LIMIT` 상황에서 2,500건을 전부 정렬하지는 않는다. 상위 N개만 유지하는 우선순위 큐(priority queue) 방식으로 최적화한다. 즉 "정렬 비용"은 줄지만, 행을 전부 읽어야 하는 비용은 그대로다.
* EXPLAIN을 보면 Extra에 `Using filesort`가 찍힌다.

> **B. 인덱스를 타는 Index Sort + LIMIT**

```sql
SELECT * FROM stock_trade_history
WHERE stock_code = '005930'
ORDER BY trade_date DESC
LIMIT 10;
```
* 데이터가 이미 날짜 순으로 정렬돼 있어 DB는 2,500건을 모두 볼 필요가 없다.
* 인덱스 트리 끝에서부터 딱 10개의 레코드만 순서대로 읽고 바로 응답한다 → 데이터가 아무리 많아도 빠르다.

#### 페이징의 함정 : OFFSET이 커질 때

* Top-N(첫 페이지)은 빠르지만, LIMIT 100000, 10 처럼 뒤 페이지로 갈수록 느려진다.
  * OFFSET은 건너뛸 10만 건을 일단 읽고 그냥 버린 뒤 다음 10건을 준다.
  * 인덱스 정렬을 타더라도 앞의 10만 건 읽기 비용이 그대로 남는다.

> **OFFSET 방식 (뒤 페이지일수록 느림)**

```sql
SELECT * FROM stock_trade_history
WHERE stock_code = '005930'
ORDER BY trade_date DESC
LIMIT 100000, 10;
```

> **커서(Keyset / Seek) 방식 (마지막으로 본 값 이후부터 조회)**

```sql
SELECT * FROM stock_trade_history
WHERE stock_code = '005930'
  AND trade_date < :last_seen_trade_date
ORDER BY trade_date DESC
LIMIT 10;
```

* 커서 방식은 인덱스에서 이어질 위치를 바로 찾아가므로 몇 페이지든 성능이 일정하다.
* 다만 "특정 페이지 번호로 점프"는 어려워, 무한 스크롤 / "다음 페이지" UX에 적합하다.

#### 트레이드오프

* `ORDER BY`는 데이터를 원하는 기준으로 정렬해준다.
* 하지만 실무에서는 무작정 `ORDER BY`를 걸기보다는 내가 정렬하려는 컬럼이 인덱스 순서와 일치하여 `Using filesort`를 피할 수 있는지를 꼭 확인해야한다.
