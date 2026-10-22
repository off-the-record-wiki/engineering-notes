# Database - Merge, MRR, New Features, Hints, and Operations

#### 인덱스 머지 - 이론

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c823d6cc-311b-4dc2-a225-627a2e49fdd5.png)

* 서로 다른 두 컬럼에 조인이 걸리고 단일 인덱스 하나로는 그중 한쪽밖에 커버하지 못할 때 두 인덱스를 동시에 활용할 수 있는 방법이 있다.
* 그 전에 이걸 `UNION` 쿼리로 합칠 수도 있을 것이라는 생각도 하게 되는데 `UNION`은 두 결과의 중복을 제거하고자 임시 테이블에 모아 정렬과 중복 제거를 한다.
* 여기에 더해, 각 갈래는 인덱스로 찾은 행을 테이블에서 통째로 읽어와 임시 테이블에 쌓는다. 조건에 맞는 행이 많으면 많을수록 이 비용은 늘어나게 된다.
* 이 "쪼개서 인덱스 타고 합치기"를 OR로 쓴 원래 쿼리만 보고 옵티마이저가 알아서 해주는 기능이 바로 인덱스 머지다.

#### 인덱스 머지 - 사용

* 기본적으로 하나의 테이블 접근에는 하나의 인덱스만 사용된다. 그런데 예외가 있다.
* 옵티마이저가 여러 인덱스를 각각 스캔한 뒤 결과를 합치거나 교차시키는 전략을 선택하는 경우가 있다. 인덱스 머지에는 3가지 알고리즘이 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/dc492dfd-ecb5-496b-a1ce-c9c40ad2e973.png)

| 알고리즘 | 적용 조건 | Extra 표시 |
|:---:|:---:|:---:|
| Intersection(교집합) | AND 조건. 각 인덱스를 동등 조건으로 사용 | Using intersect |
| Union(합집합) | OR 조건. 각 갈래가 동등 조건 | Using union |
| Sort-Union(정렬 후 합집합) | OR 조건. 범위 조건이 섞여 Union이 불가능한 경우 | Using sort_union |

* 동등 조건(=)으로 인덱스를 스캔하면 결과 row id가 PK 순서로 나오므로 그대로 합치면 된다(Union).
* 반면 범위 조건(<, >, BETWEEN)으로 스캔하면 row id가 PK 순서로 나오지 않아, 합치기 전에 먼저 정렬해야 한다(Sort-Union).
* 인덱스 머지는 옵티마이저가 비용 기반으로 선택한다.
* 그런데 `SELECT *` 로 모든 컬럼을 읽거나 한쪽 조건이 많은 행을 매칭하면, 옵티마이저는 병합 후 랜덤 접근하느니 풀 테이블 스캔이 싸다고 판단해 인덱스 머지를 버리는 경우가 많다.

#### Union: OR + 동등 조건

* 각 OR 갈래가 동등 조건이면, 각 인덱스 스캔 결과가 PK 순서로 나온다. 그래서 정렬 없이 바로 합칠 수 있다 → Union.
```sql
EXPLAIN SELECT /*+ INDEX_MERGE(product idx_product_status, idx_product_price) */ *
FROM product
WHERE product_status = 'SOLD_OUT' OR price = 5000;
```

#### Sort-Union: OR + 범위 조건

* OR 갈래에 범위 조건이 섞이면 이야기가 달라진다.
* 범위 스캔(price > 50000)은 결과 row id가 PK 순서로 나오지 않는다. 그래서 합치기 전에 먼저 정렬해야 한다 → Sort-Union.
```sql
EXPLAIN SELECT /*+ INDEX_MERGE(product idx_product_status, idx_product_price) */ *
FROM product
WHERE product_status = 'SOLD_OUT' OR price > 50000;
```

#### Intersection: AND 조건

```sql
EXPLAIN SELECT /*+ INDEX_MERGE(product idx_product_category_id, idx_product_status) */ *
FROM product
WHERE category_id = 11 AND product_status = 'ACTIVE';
```
* 두 인덱스 각각을 스캔하고 결과의 교집합(intersection)을 구한다.
* `category_id = 11` 인 PK 목록과 `product_status = 'ACTIVE'`인 PK 목록의 교집합에 해당하는 행만 테이블에서 읽는다.

#### 인덱스 머지 vs 복합 인덱스

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f97c7b88-2186-4eed-8a91-901c17d83a91.png)

* 실제로 성능 차이는 각 상황마다 다르지만 대부분의 경우 복합 인덱스 1개가 인덱스 머지보다 효율적이다.

| 비교항목 | 인덱스 머지 | 복합 인덱스 |
|:---:|:---:|:---:|
| 인덱스 스캔 횟수 | 2회 이상 | 1회 |
| 결과 병합에 들어가는 비용 | 있음(정렬/교차/합집합) | 없음 |
| 테이블 접근 패턴 | 병합 후 랜덤 점근 | 단일 스캔 후 접근 |
| 옵티마이저 비용 추정 | 복잡 | 단순 |

* 인덱스 머지는 각 인덱스를 따로 스캔하고 결과를 합치는 추가 비용이 든다. 복합 인덱스는 한 번의 스캔으로 모든 조건을 처리한다.

#### 인덱스 머지의 효용 가치

* 적절한 복합 인덱스가 없는 상황에서 인덱스 머지는 풀 테이블 스캔보다 훨씬 낫다.
* 인덱스 머지 자체가 나쁜 것이 아니다. "복합 인덱스로 대체할 수 있는지"를 먼저 검토하라는 것이 핵심이다.

```sql
-- 인덱스 머지 관련 플래그 확인
-- NO_INDEX_MERGE 는 인덱스 머지를 사용하지 않는 힌트
SELECT @@optimizer_switch;
-- index_merge=on, index_merge_intersection=on,
-- index_merge_union=on, index_merge_sort_union=on
```

#### MRR(Multi-Range Read)

* Multi-Range Read는 단어 그대로 인덱스에서 매칭된 여러 범위의 행들을 한 번에 모아서 읽는다는 뜻이다.
* 세컨더리 인덱스의 리프에는 행 데이터가 없다. 대신 인덱스 키 값과 PK만 들어 있다. 그래서 `SELECT *` 처럼 행 전체가 필요한 쿼리는 인덱스에서 찾은 PK를 들고 클러스터드 인덱스를 다시 한 번 탐색해야 한다. 이것이 2단 점프다.
* 대량의 데이터를 읽을 때는 순차 I/O가 랜덤 I/O보다 훨씬 빠르다.

#### MRR의 아이디어 - 모아서, 정렬하고, 한 번에 읽는다.

* 해결책은 간단하다. PK가 나올 때마다 곧장 테이블로 뛰어가지 말고 일단 버퍼에 모아둔다.
  * 세컨더리 인덱스를 스캔해 조건에 맞는 PK들을 내부 버퍼에 모은다.
  * 모인 PK들을 PK 순서로 정렬한다.
  * 정렬된 PK 순서대로 테이블을 한 방향으로 훑으며 행을 읽는다.
* PK 순서로 정렬해두면 테이블 접근이 앞에서 뒤로 한 방향으로만 흐른다. 따라서 디스크가 앞뒤로 튀지 않으니 랜덤 I/O가 순차 I/O에 가까워진다.
* 게다가 이렇게 되면 한 페이지에 여러 데이터를 모아서 조회할 가능성도 높아진다. 

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fe4cd68c-8b68-4cdd-bce5-15d3b2a74c11.png)

* 읽는 페이지의 집합은 똑같은데 읽는 순서가 달라지는 것만으로 디스크 I/O 패턴이 완전히 바뀌는데 이것이 MRR의 전부라고 봐도 무방하다.
* 스토리지 엔진에서 PK 정렬이라는 한 수로 끌어다 쓰는 것이다.

#### EXPLAIN으로 MRR 확인

* `EXPLAIN`은 옵티마이저가 세운 계획이다. "MRR을 쓸 작정이다"라는 의도가 Using MRR 로 표시된다.
* `EXPLAIN ANALYZE`는 실행 엔진의 연산자(iterator) 트리를 보여준다. 그런데 MRR은 별도의 연산자가 아니라, `Index range scan`이 행을 읽어 올 때, 스토리지 엔진 내부에서 PK를 모아 정렬하는 읽기 방식일 뿐이다. 트리의 노드 단위로 드러나는 동작이 아니라서 별도로 찍히지 않는다.
* `Using MRR`은 `EXPLAIN`의 `Extra`에서 확인하는 신호고, `EXPLAIN ANALYZE` 트리에 안 보인다고 MRR이 동작하지 않은 것은 아니다.

#### MRR은 비용 기반이다 - 항상 켜지지 않는다

* MRR은 무조건 켜지는 게 아니다. 옵티마이저가 비용 기반(cost-based)으로 "MRR이 이득인가"를 따져서 쓸지 말지를 결정한다.
* MRR의 이득은 랜덤 I/O를 순차 I/O로 바꾸는 것이다. 그런데 데이터가 이미 메모리(버퍼 풀)에 올라와 있다면 애초에 디스크를 안 읽으니 랜덤 I/O의 패널티가 거의 없다. 이런 상황에서 MRR을 PK를 모아 정렬하는 비용이 더 들기 때문에 옵티마이저가 MRR을 버린다.
* MRR이 빛을 발하는 순간은 데이터가 버퍼 풀에 다 안 들어가서 디스크에서 읽어야 하는, 대량의 범위 스캔이다.

#### MRR 제어

* MRR 동작은 `optimizer_switch`로 제어한다.
```sql
SELECT @@optimizer_switch;
-- ... mrr=on, mrr_cost_based=on ...
```
* `mrr` : MRR 기능 자체를 켜고 끈다.(기본값 on)
* `mrr_cost_based` : MRR을 비용 기반으로 판단할지 여부다.(기본값 on) `on`이면 위에서 본 대로 이득일 때만 쓴다.

```sql
-- 비용 판단을 끄고 MRR을 적극적으로 사용 (관찰/디버깅용)
SET optimizer_switch = 'mrr=on,mrr_cost_based=off';
```

* PK를 모아 정렬하는 버퍼의 크기는 `read_rnd_buffer_size`(기본값 256KB)로 정한다.  이 버퍼가 가득 차면 모인 만큼 정렬해서 읽고, 다시 채우기를 반복한다.
* 버퍼가 클수록 한 번에 더 많은 PK를 정렬해 순차성이 좋아지지만, 세션마다 잡히는 메모리이므로 동시 접속이 많은 환경에서 무작정 키우면 메모리 사용량도 불어난다.
* `mrr_cost_based=off`로 강제하더라도 커버링 인덱스라서 테이블 접근이 없거나 결과가 극소수라면 MRR이 의미가 없어 나타나지 않을 수 있다. MRR은 어디까지나 세컨더리 인덱스로 테이블 행을 다량 읽어야 할 때 쓰는 도구이다.

```sql
-- 원상 복귀
SET optimizer_switch = 'mrr=on,mrr_cost_based=on';
```

#### 함수 기반 인덱스

```sql
EXPLAIN SELECT *
FROM member
WHERE LOWER(email) = 'user100@shop.com';
```
* `member` 테이블에 `uk_member_email` UNIQUE 인덱스가 있음에도 불구하고 풀 테이블 스캔이 된다.
* 그 이유는 `LOWER(email)`이라는 함수를 적용했기 때문이다. 인덱스에는 `email`이라는 원본 값이 저장되어 있는데, 쿼리는 `LOWER(email)` 결과를 비교한다. 따라서 인덱스의 정렬 구조를 활용할 수 없다.
* WHERE절에서 컬럼에 함수를 적용하면 인덱스를 탈 수 없다. 이럴 경우 MySQL 8.0.13부터 제공하는 함수 기반 인덱스(Function Index)를 사용하면 컬럼을 가공해도 인덱스를 태울 수 있다.

```sql
-- 함수 기반 인덱스 생성
CREATE INDEX idx_member_lower_email ON member ((LOWER(email)));
```
* 괄호가 이중으로 감싸져 있는데 바깥쪽 괄호는 인덱스 컬럼의 목록, 안쪽 괄호는 표현식을 나타낸다.
* 함수 기반 인덱스는 내부적으로 숨겨진 가상 생성 컬럼(hidden virtual generated column)으로 구현된다.
* MySQL의 `LOWER(email)` 결과를 계산해 별도의 가상 컬럼에 저장하고, 그 컬럼에 인덱스를 건다. 쿼리에서 `LOWER(email)`을 사용하면 옵티마이저가 이 가상 컬럼의 인덱스를 자동으로 매칭한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4f33f447-bb23-4abb-a0bc-6b579f728c27.png)

* 함수의 계산 결과를 인덱스가 들고 있으니, 그 결과를 조회하는 쿼리는 인덱스만으로 끝낼 수 있다.
* PK에는 함수 기반 키 파트를 포함할 수 없다.(가상 컬럼은 PK가 될 수 없다)
* 생성 컬럼에 허용되는 함수만 사용할 수 있다.(서브쿼리, 저장 프로시저 등은 불가하다)
* 각 함수 기반 키 파트는 테이블의 전체 컬럼 수 제한에 포함된다.
* 함수 기반 인덱스가 유용한 대표적인 경우는 대소문자 무시 검색, 날짜에서 연/월 추출 검색, JSON 값 추출 검색 등이 있다.

#### 인덱스 숨기기

* 운영 환경에서 인덱스를 잘못 삭제해서 중요한 쿼리가 느려지면 대참사가 발생한다. 다시 인덱스를 만들려면 수십 분이 걸릴 수도 있다.
* 그 사이 사용자 요청은 계속 느려져서 인덱스를 다시 만들 때까지 장애가 발생한다.
* 그렇다고 아무것도 하지 않으면 사용하지 않는 인덱스가 계속 남아 쓰기 성능과 디스크 공간을 잡아먹는다. 운영에서는 삭제 전 영향도를 안전하게 검증하는 방법이 필요하다.

**Invisible Index**
* Invisible Index는 인덱스를 물리적으로 삭제하지 않고, 옵티마이저에서만 안 보이게 만드는 기능이다.
```sql
-- idx_product_category_id 인덱스를 INVISIBLE로 변경
ALTER TABLE product ALTER INDEX idx_product_category_id INVISIBLE;
```
* 해당 명령은 빠른 인플레이스 작업이라 테이블을 재구성하지 않는다. 인덱스는 물리적으로 그대로 존재하고 INSERT/UPDATE/DELETE시 계속 유지된다. 다만 옵티마이저가 실행 계획을 세울 때 이 인덱스를 무시한다.
* PK는 Invisible로 만들 수 없다. PK가 없는 테이블에서 암묵적 PK 역할을 하는 UNIQUE 인덱스도 마찬가지다.
* Invisible Index도 INSERT/UPDATE/DELETE시 유지 비용이 발생한다. 쓰기 성능에 미치는 영향은 Visible과 동일하다.
* `optimizer_switch`의 `use_invisible_indexes` 플래그를 `on`으로 설정하면 Invisible 인덱스도 옵티마이저가 사용한다. 기본값은 `off`이다.

> **운영 절차 : 인덱스를 제거할 때는 보통 다음 순서로 진행하는 것을 권장한다.**
> * 삭제 후보 인덱스를 바로 `DROP` 하지 말고 먼저 `INVISIBLE`로 바꾼다.
> * 대표 쿼리 실행 계획과 응답 시간을 확인한다.
> * 운영 트래픽을 일정 시간 모니터링한다.
> * 문제가 생기면 즉시 `INVISIBLE`로 원복한다.
> * 문제가 없으면 그 때, `DROP INDEX`로 실제 삭제한다.

#### 인덱스 정렬

* MySQL 8.0부터 인덱스 정의에서 `DESC`를 명시적으로 지정할 수 있다.
* `Descending Index`는 혼합 정렬 요구사항에서 빛을 발한다.
```sql
-- 카테고리별로 가격이 높은 상품부터 정렬하는 기능이 필요하다는 요구사항 가정
EXPLAIN SELECT product_id, category_id, price
FROM product
ORDER BY category_id ASC, price DESC
LIMIT 20;
```

#### 옵티마이저 힌트

* 인덱스를 잘 설계했음에도 불구하고 옵티마이저가 원하는 실행 계획을 선택하지 않을 때가 있다.
* 예를 들어, 특정 인덱스가 명백히 유리한데 다른 인덱스를 선택한다거나, 반대로 특정 인덱스를 사용하면 성능이 나빠지는 경우가 있다.
* 이럴 때, 실행 계획에 개입하는 도구가 바로 힌트다. 과거에는 `USE INDEX`, `FORCE INDEX`, `IGNORE INDEX`와 같은 인덱스 힌트를 많이 사용했다. 하지만 MySQL의 앞으로의 방향은 `/*+ ... */` 형태의 옵티마이저 힌트다.
* 공식 문서 기준으로는 `USE INDEX`, `FORCE INDEX`, `IGNORE INDEX`가 향후 MySQL에서 deprecated 되고, 이후 제거될 수 있다고 나와있다. 반대로 MySQL 8.0.20부터 지원되는 `INDEX()`, `NO_INDEX()`와 같은 인덱스 레벨 옵티마이저 힌트는 이들을 대체하기 위한 문법으로 제공된다.

#### 옵티마이저 힌트 기본 구문

* 옵티마이저 힌트는 `/*+ ... */` 형태의 특수 주석으로 작성한다. `SELECT`, `UPDATE`, `DELETE` 등의 키워드 바로 다음 뒤에 위치한다.

```sql
--
기본 구문
SELECT /*+ 힌트이름(대상) */ 컬럼 FROM 테이블 WHERE 조건;

-- 여러 힌트를 하나의 주석에 작성
SELECT /*+ INDEX(t1 idx1) JOIN_ORDER(t1, t2) */ ...
```
* 이 때, 주의할 점이 있다. 힌트 주석은 하나만 쓸 수 있다. 2개의 힌트 주석을 쓰면 두 번째는 무시된다. 여러 힌트는 모두 하나 안에 넣어야 한다.

**INDEX() : 특정 인덱스 사용 강제**
* INDEX() 힌트는 지정한 인덱스를 사용하도록 강하게 유도한다. 기존 `FORCE INDEX`와 동등한 의미다.
```sql
EXPLAIN SELECT /*+ INDEX(product idx_productcatestatus___price) */ *
FROM product
WHERE category_id = 11;
```
* 힌트는 `SELECT` 바로 뒤의 `/*+ ... */` 주석 안에 작성한다.

**NO_INDEX() : 특정 인덱스 배제**
* `NO_INDEX()`는 지정한 인덱스를 사용하지 못하게 한다. 기존 `IGNORE INDEX`와 동등한 의미다.
```sql
EXPLAIN SELECT /*+ NO_INDEX(product idx_product_cate_status_price) */ *
FROM product
WHERE category_id = 11
  AND product_status = 'ACTIVE'
  AND price = 10000;
```

#### JOIN_ORDER - 조인 순서 제어

* 조인 쿼리에서 옵티마이저가 선택한 테이블 순서가 비효율적인 경우 사용한다.
```sql
-- JOIN_ORDER: 조인 순서 강제
EXPLAIN SELECT /*+ JOIN_ORDER(p, oi, o) */ o.order_id, p.product_name
FROM orders o
JOIN order_item oi ON o.order_id = oi.order_id
JOIN product p ON oi.product_id = p.product_id
WHERE o.member_id = 2800000;
```
* `JOIN_ORDER`로 조인 순서를 강제할 수 있다.
* 잘못된 순서를 강제하면 오히려 성능이 악화된다.
* 조인 순서 힌트에는 다음과 같은 다른 변형도 있다.

| 힌트 | 동작 |
|:--:|:--:|
| JOIN_FIXED_ORDER | FROM절에 나열한 순서대로 조인(STRAIGHT_JOIN과 동일) |
| JOIN_ORDER(t1, t2, t3) | 지정한 순서로 조인, 나머지는 옵티마이저가 결정 |
| JOIN_PREFIX(t1) | 지정한 테이블을 조인 순서 앞쪽에 배치 |
| JOIN_SUFFIX(t1) | 지정한 테이블을 조인 순서 뒤쪽에 배치 |

#### 용도별 인덱스 힌트

* `INDEX()`와 `NO_INDEX()`는 조인 정렬, 그룹화 전체 범위에 적용된다. 더 좁은 범위만을 제어하고 싶다면 용도별 힌트를 사용하면 된다.

| 옵티마이저 힌트 | 의미 | 기존 인덱스 힌트 대응 |
| --- | --- | --- |
| `JOIN_INDEX(t i)` | 행을 찾거나 조인할 때 인덱스 `i` 사용 강제 | `FORCE INDEX FOR JOIN` |
| `ORDER_INDEX(t i)` | 정렬에 인덱스 `i` 사용 강제 | `FORCE INDEX FOR ORDER BY` |
| `GROUP_INDEX(t i)` | 그룹화에 인덱스 `i` 사용 강제 | `FORCE INDEX FOR GROUP BY` |
| `NO_JOIN_INDEX(t i)` | 행 조회/조인에서 인덱스 `i` 배제 | `IGNORE INDEX FOR JOIN` |
| `NO_ORDER_INDEX(t i)` | 정렬에서 인덱스 `i` 배제 | `IGNORE INDEX FOR ORDER BY` |
| `NO_GROUP_INDEX(t i)` | 그룹화에서 인덱스 `i` 배제 | `IGNORE INDEX FOR GROUP BY` |

> **대량 데이터를 읽을 때는 순차 I/O가 랜덤 I/O보다 빠를 수 있다. `INDEX()`로 판단을 무시해버리면 오히려 성능이 나빠진다.**
* 정리하자면, 새로 작성하는 쿼리에서는 옵티마이저 힌트를 우선 사용한다. 다만 기존 코드에서 `USE INDEX`를 발견했다면 단순히 `INDEX()`로 바꾸면 안 된다. `USE INDEX`는 풀 스캔을 허용하는 약한 힌트이고, `INDEX()`는 `FORCE INDEX`와 동등한 강한 힌트이기 때문이다.

#### 인덱스 비용

* 인덱스를 얼마나 만들어야 하는가가 상당히 중요한 포인트이다.
* 인덱스가 많으면 SELECT가 빨라진다는 것은 이미 알고 있다. 다만 세상에 공짜는 없다.
* 인덱스는 SELECT를 빠르게 만들어주는 대신, INSERT/UPDATE/DELETE를 느리게 한다. 인덱스는 별도의 B+Tree 구조를 가진다. 인덱스를 하나 추가하면 B+Tree가 하나 더 생긴다. 데이터를 삽입할 때, 테이블(클러스터드 인덱스)뿐 아니라, 모든 세컨더리 인덱스의 B+Tree도 갱신해야 한다. 인덱스가 N개면 B+Tree 갱신이 N번 발생한다.
* 게다가 각 B+Tree에서 페이지 분할이 발생할 수 있다. 인덱스가 많을수록 페이지 분할 확률도 높아진다.

| 비용 | 설명 |
|:---:|:---:|
| 쓰기 성능 저하 | INSERT/UPDATE/DELETE마다 인덱스 B+Tree 갱신 |
| 디스크 공간 증가 | 각 인덱스가 별도의 B+Tree로 저장 |
| 페이지 분할 빈도 증가 | 인덱스별로 독립적으로 페이지 분할 발생 |

* 왜 인덱스가 많아지면 INSERT가 느려지는지 그 과정을 정리하면 다음과 같다.

| 단계 | 인덱스 0개 | 인덱스 5개 |
|:---:|:---:|:---:|
| 클러스터드 인덱스(PK) B+Tree 갱신 | 1회 | 1회 |
| 세컨더리 인덱스 B+Tree 갱신 | 0회 | 5회 |
| 잠재적 페이지 분할 | PK만 | PK + 5개 인덱스 모두 |

* 결과적으로 인덱스가 많을수록 쓰기 성능은 그에 비례해 저하된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cf42c78d-8f1a-4c43-b13a-1f31ba031c3e.png)

> **❗읽기 vs 쓰기 비율에 따른 전략 선택이 필요 : 인덱스를 얼마나 만들지는 서비스의 읽기/쓰기 비율에 따라 달라진다**

| 서비스 유형 | 읽기/쓰기 비율 | 인덱스 전략 |
|:---:|:---:|:---:|
| 조회 위주(쇼핑몰 상품 검색, 게시판 목록) | 읽기 90%, 쓰기 10% | 인덱스를 적극적으로 추가해도 좋다. 읽기 성능 이익이 쓰기 비용을 압도한다. |
| 균형(일반적인 CRUD 서비스) | 읽기 : 60%, 쓰기 40% | 자주 사용되는 쿼리 패턴에 집중해 인덱스를 설계한다. |
| 쓰기 위주(로그 수집, 실시간 센서 데이터, 대량 배치) | 읽기 20%, 쓰기 80% | 인덱스를 최소화한다. PK와 꼭 필요한 인덱스만 유지한다. |

#### 인덱스 설계 의사결정 체크리스트

**추가 케이스**

1. 이 쿼리가 자주 실행되는가? - 하루에 1번 실행되는 쿼리를 위해 인덱스를 추가하는 것은 비효율적이다.
2. 기존 인덱스로 커버할 수 없는가? - 기존 복합 인덱스 좌측 접두사로 해결되지 않는지 먼저 확인한다.
3. 쓰기 비용을 감수할 만큼 읽기 이익이 큰가? - 인덱스 추가로 INSERT가 느려져도 괜찮은가?
4. 인덱스 컬럼 순서가 최적인가? - 동등 ⭢ 범위 ⭢ 정렬 원칙을 따르는가?

**제거 케이스**

1. 이 인덱스를 사용하는 쿼리가 있는가? - 실제 쿼리 사용 여부를 확인한다.
2. 다른 인덱스로 대체 가능한가? - 복합 인덱스가 단일 인덱스의 역할을 포함하는지 확인한다.
3. 안전하게 검증했는가? - Invisible Index로 먼저 비활성화하고 영향을 확인한다.

**중복 인덱스 케이스**

* 복합 인덱스 `(a, b, c)`가 있으면 단일 인덱스 `(a)`는 중복이다. 복합 인덱스 좌측 접두사로 `(a)` 검색이 커버되기 때문이다. 이런 중복 인덱스는 쓰기 비용만을 늘리고 읽기 이익은 없다.

#### Sorted Index Build

* InnoDB는 세컨더리 인덱스를 만들 때 기존 데이터를 한 건씩 B+Tree에 끼워 넣는 방식만 사용하지 않는다. 일반적인 인덱스 생성 과정은 다음 흐름으로 진행된다.

1. 클러스터드 인덱스, 즉 테이블 데이터를 스캔한다.
2. 새 인덱스에 필요한 키 값을 임시 정렬 파일에 기록한다.
3. 인덱스 키 순서로 정렬한다.
4. 정렬된 데이터를 새 세컨더리 인덱스에 적재한다.

* 이러한 과정을 Sorted Index Build라고 한다.
* 이 방식의 장점은 기존의 한 건씩 삽입하는 방식보다 B+Tree에서 삽입 위치를 매번 찾는 비용이 줄고, 페이지가 가득 찰 때마다 반복적으로 쪼개고 합치는 비용도 줄어든다는 것이다. 그래서 대량 데이터가 이미 들어있는 테이블에 인덱스를 추가할 때 더 효율적이다.
