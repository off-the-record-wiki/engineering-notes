# PostgreSQL - PostgreSQL-Specific Features, Indexing & Performance Optimization

#### PostgreSQL의 JSONB, 역인덱싱

```sql
-- JSON : 입력된 텍스트를 그대로 저장(읽을 떄마다 파싱)
-- JSONB : 파싱된 이진 형태로 저장(저장 시 처리 비용 필요)

CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    account_id     BIGINT NOT NULL,
    type           VARCHAR(10) NOT NULL CHECK (type IN ('입금', '출금')),
    amount         NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    description    TEXT,
    created_at     TIMESTAMPTZ DEFAULT NOW(),
    metadata       JSONB
);
```

* 역인덱스란 검색엔진에서 주로 사용하는 구조를 말하는데 행 → 값이 아니라 값 → 행으로 바꾸는 것을 말한다.

```sql
-- jsonb_each : JSON 객체 하나를 (키, 값) 행들로 펼치는 함수

SELECT key, COUNT(*) AS 출현횟수
FROM transactions,
     jsonb_each(metadata) AS kv(key, value)
WHERE metadata IS NOT NULL
GROUP BY key
ORDER BY 출현횟수 DESC;
```

```sql
-- jsonb_array_elements : JSON 배열 하나를 여러 행으로 펼치는 PostgreSQL 함수

SELECT
    transaction_id,
    jsonb_array_elements_text(metadata -> 'tags') AS 태그
FROM transactions
WHERE metadata ? 'tags';
```

#### 인덱스가 없을 때: 풀 테이블 스캔

* 기본적으로 인덱스가 없으면 풀 테이블 스캔을 하게 된다.
* 비용은 `O(N)`이다. 찾는 행이 하나뿐이더라도 테이블 전체를 읽는다.

#### B-Tree 인덱스

* 컬럼 값을 정렬된 트리 형태로 따로 저장한다. 리프 노드에는 실제 행 위치를 가리키는 포인터(PostgreSQL은 TID, MySQL에는 InnoDB PK)가 들어있다.
* 루트에서 출발해 찾는 값이 기준보다 작으면 왼쪽, 크면 오른쪽으로 내려가므로 `O(log N)`의 시간복잡도를 가진다.
* 정렬되어 있으므로 `=` 뿐만 아니라 범위 조건(`BETWEEN`, `<`, `>`)과 `ORDER BY`에도 쓰인다.

#### BRIN 인덱스

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6bdc5f49-43d8-4da7-92a0-e9b537470c50.png)

* 행 하나하나가 아니라 블록 묶음마다 최솟값·최댓값만 저장한다.
* 조회할 때는 범위가 겹치지 않는 블록 묶음을 통째로 건너뛴다.
* 전제 조건은 컬럼 값이 디스크에 저장된 물리적 순서와 맞물려 있어야 한다. `created_at`처럼 시간순으로 쌓이는 `append-only` 로그·이력 테이블에서 많이 쓰인다.

```sql
-- Hash 인덱스 : 등치 조건에 사용되는 인덱스(>, <, BETWEEN) : 버킷 방식
CREATE INDEX idx_transcation_account_id ON transactions USING HASH (type);

-- Gist : 범위 조건이 필요하거나 특수한 데이터 타입
-- PostGIS : 지리데이터(위도나 경도, 다각형 영역)
CREATE INDEX idx_transcation_account_id ON transactions USING GIST (account_id);

-- BRIN : 물리적으로 연속해서 저장된 데이터에서 유리
CREATE INDEX idx_transcation_account_id ON transactions USING BRIN (account_id);
```

#### 대용량 데이터를 기반으로 하는 PostgreSQL만의 쿼리 분석 패턴

**1. Seq Scan: 테이블을 처음부터 끝까지 읽기**
```
테이블 페이지 0 → 1 → 2 → ... → 마지막
  각 페이지의 모든 행에 대해 account_id = 42인지 확인
```
* 테이블을 저장 순서대로 한 페이지(8KB)씩 전부 읽고, 행마다 조건을 검사한다.
* 디스크를 연속으로 읽으므로 페이지 하나를 읽는 비용은 가장 싸다. 대신 페이지 수가 곧 비용이라서 테이블이 클수록 비싸진다.

**2. Index Scan: 인덱스에서 하나 찾을 때마다 테이블로 가기**
```
B-Tree를 타고 리프까지 내려감
  → 첫 번째 42 항목: 행 위치 (페이지 812, 3번째 행) → 테이블 페이지 812 읽기
  → 두 번째 42 항목: 행 위치 (페이지 57, 11번째 행)  → 테이블 페이지 57 읽기
  → 세 번째 42 항목: 행 위치 (페이지 812, 9번째 행)  → 테이블 페이지 812 또 읽기
  ...
```
* 인덱스 리프에서 조건에 맞는 항목을 순서대로 훑는다. 항목 하나마다 즉시 테이블의 그 행을 읽으러 간다.
* 인덱스는 값 순서로 정렬되어 있지만 행은 테이블 곳곳에 흩어져 있다. 그래서 디스크를 이리저리 오가는 랜덤 I/O가 되고, 같은 페이지를 여러 번 읽을수도 있다.
* 대신 결과가 인덱스 순서대로 나오므로 `ORDER BY` 정렬 비용을 아낄 수 있다.

**3. Bitmap Heap Scan: 위치를 먼저 다 모은 뒤 페이지 순서대로 읽기**
* 2단계로 나뉘고 계획에도 노드 2개가 짝으로 나온다.
```
① Bitmap Index Scan
   인덱스에서 42인 항목의 행 위치를 전부 모아 메모리의 비트맵에 표시
   → 페이지 57, 203, 812, ... (페이지 번호 순서로 정렬된 상태)

② Bitmap Heap Scan
   비트맵을 따라 테이블을 페이지 번호 순서대로 한 번씩만 읽기
   57 → 203 → 812 → ...
```
* Index Scan의 2가지 약점. 같은 페이지를 반복해서 읽는 것과 랜덤 I/O를 해결한다.
* 대가로 결과가 인덱스 순서가 아니게 되어 `ORDER BY` 정렬이 따로 필요하다.
* `Recheck Cond` : 찾은 행이 너무 많아 비트맵이 메모리를 넘으면 행 단위 표시를 포기하고 "이 페이지 어딘가에 있음"이라고 페이지 단위로만 표시한다. 그러면 그 페이지의 행 조건으로 다시 걸러야 하는데, 그 조건이 `Recheck Cond`이다.

#### Database Performance를 위한 최적화 패턴 및 전략

**1. 컬럼을 가공하면 인덱스를 못 쓴다**
```sql
WHERE EXTRACT(YEAR FROM created_at) = 2024                       -- ✗ Seq Scan
WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01'   -- ✓ 인덱스 사용 가능
WHERE account_id::text = '42'                                    -- ✗ Seq Scan
```
* 인덱스에는 `created_at`의 원래 값이 정렬되어 있을 뿐이다. 함수를 적용한 결과나 `::text`로 바꾼 값이 인덱스에 없다.
* 그래서 행마다 직접 계산해야하고 결국 풀 스캔이 된다.
* 해결 방법은 조건을 원래 컬럼에 대한 범위로 다시 쓰는 것이다. 가공한 형태를 꼭 써야만 한다면 식 자체에 인덱스를 거는 함수 인덱스를 만들어야 한다.

**2. LIKE는 앞부분이 고정되어야 하고, 정렬 규칙도 맞아야 한다**
* `LIKE 'a1b2%'`는 "a1b2로 시작하는 값"이므로 B-Tree에서 `a1b2` 이상이고 `a1b3` 미만인 범위로 바꿔 찾을 수 있다.
* 그런데 기본 인덱스는 DB의 collation(언어별 정렬 규칙)을 따른다. 예를 들어 `ä`가 `a` 바로 다음에 오는지 `z` 다음에 오는지는 언어마다 다르다. 그래서 PostgreSQL은 "a1b2로 시작하는 값이 한 구간에 모여 있다"고 단언하지 못하고 인덱스를 쓰지 않는다.
* `text_pattern_ops`로 만든 인덱스는 문자열을 바이트 순서로 정렬한다. 그래서 앞부분이 고정된 LIKE에 인덱스를 쓸 수 있다.

**3. OFFSET 페이지네이션은 뒤로 갈수록 느려진다**
* OFFSET 방식은 건너뛸 행을 실제로 전부 읽기 때문에 페이지가 깊어질수록 비용이 선형으로 늘어난다.
* 반면 커서 방식은 "마지막으로 받은 값 다음부터 달라"고 요청하므로 몇 번째 페이지든 비용이 거의 같다. 이 방식을 키셋 또는 커서 페이지네이션이라고 한다.
* `created_at`는 값이 겹칠 수 있다. 경계에 같은 시각을 가진 행이 여럿 있으면 타이 브레이커 문제가 발생하기에 고유한 컬럼을 붙이고 인덱스도 복합 인덱스로 만들어야 한다.

**4. 옵티마이저는 통계를 보고 판단한다**
* `pg_stats` : `pg_statistic`을 사람이 읽기 쉽게 보여주는 뷰.
* `n_distinct` : 서로 다른 값의 개수.
* `most_common_vals` : 자주 나오는 값 목록과 각 비율.
* `correlation` : 값의 순서와 디스크 저장 순서가 얼마나 일치하는지(-1 ~ 1).
* `ANALYZE`는 이 통계를 새로 수집한다. autovacuum도 변경된 행이 일정 비율(기본 약 10%)을 넘으면 자동으로 돌린다. 만 대량으로 적재한 직후에는 아직 돌지 않았을 수 있다. 통계가 낡으면 `rows` 추정이 틀어지고 계획도 틀어지므로 대량 INSERT 직후에는 직접 실행하는 것이 좋다.

**5. Sort 노드 제거: 인덱스는 정렬 순서도 제공한다**
```sql
ORDER BY created_at LIMIT 20                  -- Index Scan → 20행 읽고 끝
ORDER BY created_at + INTERVAL '0' LIMIT 20   -- Seq Scan → Sort → Limit
```
* B-Tree는 이미 정렬되어 있다. 그래서 `ORDER BY created_at` 인덱스를 앞에서부터 N개만 읽으면 끝나고 Sort 노드가 계획에서 사라진다.

**6. Range 파티셔닝: 필요 없는 테이블은 아예 열지 않는다**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0620b1df-6c04-4629-87dc-fd2fb82dfad9.png)

```
transactions_part (부모: 데이터 없음, 행을 자식 테이블로 보내는 역할만 함)
 ├─ _2023   [2023-01-01, 2024-01-01)
 ├─ _2024   [2024-01-01, 2025-01-01)
 └─ _2025   [2025-01-01, 2026-01-01)
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c3f6deab-8b83-4a25-83e4-edc52c98560a.png)

* 파티션 프루닝 : `WHERE created_at >= '2024-01-01' AND created_at < '2024-04-01'`으로 조회하면 `_2024` 하나만 나온다. 나머지 파티션들이 계획에서 빠지는데 이를 파티션 프루닝이라고 한다.
* BRIN을 테이블 단위로 적용한 것과 같은 원리이다. BRIN이 범위가 겹치지 않는 블록 묶음을 건너뛰듯이 프루닝은 범위가 겹치지 않는 파티션 전체를 건너뛴다.
* 프루닝이 되려면 `WHERE` 조건이 파티션 키에 직접 걸려있어야 한다. `EXTRACT(YEAR FROM created_at) = 2024`처럼 함수를 사용해 가공하면 인덱스를 못 쓰는 것과 같이 프루닝도 되지 않고 모든 파티션을 탐색한다.

> **[23514] no partition ... found for row**
* 어느 범위에도 속하지 않는 행이 하나라도 있으면 `INSERT` 전체가 실패하고 롤백된다.
* 그래서 미래 파티션을 미리 만들어두고 매월·매년 파티션을 자동으로 만드는 배치나 `pg_partman`과 같은 확장을 사용한다.
* `DEFAULT` 파티션을 둔다. 범위 밖의 값을 받아 주는 파티션이다. 다만 나중에 새 범위 파티션을 추가할 때, `DEFAULT` 파티션 안에 그 범위 행이 이미 들어 있으면 추가가 실패한다.

**7. List 파티셔닝: 값 목록으로 나눈다**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e3945cfd-ca58-45dd-863a-081bb25e6867.png)

* 범위가 아니라 'deposit', 'withdrawal'처럼 정해진 값으로 나뉜다. 지역, 테넌트, 상태처럼 값의 종류가 정해진 컬럼에 적합하다.
* `Range`와 같은 함정이 있는데 목록에 없는 값이나 `NULL`은 들어갈 곳이 없어 오류가 발생한다. 그래서 `NOT NULL`을 붙이고 새로운 값이 생길 수 있다면 `DEFAULT` 파티션이 필요하다.
