# MySQL - NoOffset For Query Tuning

### MySQL - NoOffset For Query Tuning

#### 기존 페이징의 치명적인 문제 : OFFSET의 읽고 버리기

```sql
-- 최신 주식 거래 내역을 1,000,000번째부터 20개만 가져와라
SELECT * FROM stock_trade_history 
ORDER BY id DESC 
LIMIT 1000000, 20;
```
* 해당 쿼리를 보면 1번부터 2,000,020번째 데이터까지 총 1,000,020건을 모두 디스크에서 읽어온 뒤, 앞의 1,000,000건은 모두 버리고 마지막 20건만 반환하게 된다.
* 페이지 번호(Offset)가 뒤로 갈수록 읽고 버리는 데이터가 기하급수적으로 늘어나며, 결국 쿼리 응답 시간이 수 초에서 수십 초까지 늘어나는 슬로우 쿼리(Slow Query)의 주범이 된다.

#### NoOffset (커서 기반) 페이징의 해결책

* 이전 페이지를 조회할 때 마지막으로 본 데이터의 식별값을 기억해 두었다가 다음 쿼리에서 그 값부터 시작해 필요한 개수만큼만 읽어오는 방식이다.

```sql
-- 첫 번째 페이지 요청 (가장 최신 데이터 20건)
SELECT * FROM stock_trade_history 
ORDER BY id DESC 
LIMIT 20;
```
* 클라이언트는 응답받은 20개 데이터 중 가장 마지막 id값을 기억한다.
* 이후 다음 페이지를 요청할 때는 기억해 둔 `id`값을 조건으로 넘겨준다.

```sql
-- 두 번째 페이지 요청 (NoOffset 적용)
SELECT * FROM stock_trade_history 
WHERE id < 999980   -- 이전 페이지의 마지막 id보다 작은 것부터 찾아라!
ORDER BY id DESC 
LIMIT 20;
```
* `id`는 PK이기 때문에 B-Tree 구조로 완벽히 정렬되어 있다. MySQL은 `id=?`를 타고 이동한다.
* NoOffset 페이징은 무적의 성능을 자랑하지만, UI/UX 기획에 따라 적용하지 못할 수도 있다.
