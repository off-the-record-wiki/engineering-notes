# Database - Optimizing Sort, Pagination, and Grouping

#### 정렬과 페이징의 함정

* 최신순 정렬과 페이지 넘기기는 웹 서비스에서 가장 흔한 패턴이다.
  * `filesort` : 인덱스가 없으니 MySQL이 N건을 전부 읽은 뒤, 메모리(또는 디스크)에서 정렬을 수행한다. 데이터가 많을수록 이 비용이 커진다.
  * `OFFSET` : `LIMIT 20 OFFSET 20000`이면 MySQL은 정렬된 결과에서 20,020행을 읽은 뒤 앞의 20,000행을 버리고 20행만 반환한다. 20,000행을 읽고 버리는 것이다. 

| 구분 | 1페이지 (OFFSET 0) | 1000페이지 (OFFSET 20000) |
|---|---|---|
| 풀 스캔 | 500만 건 (~828ms) | 500만 건 (~884ms) |
| Top-N의 N | 20 | 20020 |
| 정렬 | 메모리에서 처리 | 메모리 초과 (뒤에서 이야기) |
| 전체 시간 | 약 1.2초 | 약 1.9초 |

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/25a73342-ce1f-4659-9fa0-fcbbf54d6da8.png)

* 1,000 페이지 쿼리가 거치는 단계를 분해하면 다음과 같다. 핵심은 2단계에서 500만 건을 전부 줄 세우는 것이 아니라, Top-N 힙으로 상위 20,020행(LIMIT + OFFSET)만 추려 정렬한다는 점이다.

| 단계 | 동작 | 비용 |
|---|---|---|
| 1단계 | 500만 건 전체 읽기 (풀 스캔) | 디스크/버퍼 풀 I/O |
| 2단계 | Top-N filesort → 상위 20020행만 출력 정렬 | 메모리 또는 디스크 정렬 비용 |
| 3단계 | 정렬된 20020행에서 앞 20000행 버리기 (OFFSET) | OFFSET만큼의 낭비 |
| 4단계 | 남은 20행 반환 | 거의 무비용 |

* 여기서 정렬 비용과 페이징 비용은 각각 별개의 문제다. 이 둘을 구분해서 공략해야 한다.

#### filesort - 메모리 vs 디스크

* MySQL은 `ORDER BY`절을 인덱스로 만족시킬 수 없을 때, filesort를 수행한다. 인덱스 자체가 이미 정렬되어 있으므로 `ORDER BY` 순서와 인덱스가 일치하면 추가 정렬이 필요 없다. 하지만 인덱스에 없거나 인덱스 순서와 `ORDER BY` 순서가 맞지 않으면 MySQL이 직접 정렬해야 한다. 이것이 filesort이다.
* filesort는 `sort_buffer_size` 내에서 정렬이 완료되면 메모리에서만 수행된다. 결과 집합이 메모리를 초과할 때만 디스크의 임시 파일을 사용한다.

**sort_buffer_size : 메모리 정렬의 한계선**
* `sort_buffer_size`는 각 정렬 작업에서 사용되는 버퍼의 최대 크기이다.
* `sort_buffer_size`를 크게 잡으면 성능상 유리해지지만 주의해야 할 부분이 있다. `sort_buffer_size`는 세션별로 할당되므로 동시 접속이 100개이고 `sort_buffer_size`이 256MB이면 정렬만으로 최대 25.6GB 메모리가 필요할 수 있다.

**sort_buffer를 초과하면 : 디스크 머지 소트**
* 결과 집합이 `sort_buffer_size`를 초과하면 MySQL은 디스크 임시 파일을 사용하여 머지 소트(merge sort)를 수행한다. 이것이 진짜 파일에 쓰는 정렬인데 이 때, 성능이 급격히 떨어진다.

**디스크 머지 소트의 동작 방법**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4e1d43fc-c724-4c38-9caf-bcdade4ca825.png)

* 정렬은 메모리, 보관과 병합은 디스크의 혼합이다. 메모리 구간이 빨랐던 건 디스크를 한 번도 건드리지 않았기 때문이고, 여기서 느려진 것은 청크를 디스크에 쓰고 다시 읽어 병합하는 디스크 I/O가 더해졌기 때문이다.
* OFFSET을 0부터 10만까지 늘려가며 쿼리의 정렬 방식과 실행 시간을 측정한 그림을 보면 이해가 빠르다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/dbecc3c1-de9c-422f-aeb5-8c9f6024375b.png)

* **메모리 구간(OFFSET 0 ~ 약 2,126)** : 비용 대부분은 풀 스캔이 차지하고, 우선순위 큐는 매모리 안에서 끝나므로 OFFSET이 늘어도 시간이 거의 변하지 않는다. 풀 스캔으로 500만 건을 읽는 비용은 OFFSET과 무관하게 늘 똑같다. N(OFFSET + LIMIT)이 20에서 2,000개로 커져도 빠른 메모리 안 순위표가 조금 커질 뿐이고, 500만 건을 훑는 비용에 비하면 차이는 거의 티가 나지 않는다.
* **전환점(OFFSET + LIMIT이 약 2,146을 넘는 순간)** : 우선순위 큐가 버퍼를 초과하면서 디스크 머지 소트로 바뀌고, 시간이 폭증한다. OFFSET을 조금 늘렸을 뿐인데 정렬 비용이 그만큼 증가하게 된다.
* **디스크 구간(OFFSET 약 2,150 이상)** : 디스크 머지 소트로 고정되고, 이제부터는 OFFSET이 커질수록 시간이 완만히 상승한다. 머지로 정렬된 결과에서 앞쪽 OFFSET 건을 만들어 버려야 하기 때문이다. OFFSET이 깊어질수록 병합으로 뽑아내야 하는 행도 늘어난다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/66909be1-2b9f-4872-bc69-3e8f53e6592f.png)

#### 인덱스를 활용한 정렬 최적화

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9f7a372a-a63c-4c3c-b2bf-9a13fbf6ab23.png)

* `LIMIT`과 `OFFSET`은 행을 다 만든 다음에 잘라내는 맨 마지막 단계기 때문에 인덱스를 설계했다고 하더라도 `OFFSET 20000`과 같은 깊은 경우는 해결이 어렵다.
* 결국 정렬 비용을 줄이기 위해 인덱스를 사용해서 해결했으나 `OFFSET`은 인덱스로 완벽히 해결되지 않았다. 인덱스를 타든 안 타든 깊은 `OFFSET`은 건너뛸 행까지 모두 처리해야 하므로 깊이에 비례해 비싸진다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ae6c8df0-6e38-497f-9b0e-6b2f6f9c2b2c.png)

#### 지연 조인 최적화

* InnoDB에서 세컨더리 인덱스에는 PK 컬럼이 암묵적으로 포함된다. 따라서 `SELECT pk, key_part`처럼 PK와 인덱스 컬럼만 `SELECT`하면, 테이블을 다시 찾을 필요 없이 인덱스만으로 처리할 수 있다.
* 전체 컬럼을 비커버링 인덱스로 가져오려니 깊은 `OFFSET`에서 PK 조회가 매우 많이 필요했고 그래서 옵티마이저가 인덱스를 포기했다. 진짜로 전체 컬럼을 채워야 하는 행은 마지막 N개 뿐이다. 앞의 행들은 어차피 버리기에 그것들까지 PK 조회(재탐색)로 완성할 필요가 없다.
* 일을 2단계로 쪼갠다.
  * 싸게 위치만 찾기 : 커버링 인덱스만으로 이 페이지에 해당하는 PK를 추린다. 인덱스 안에서 끝나므로 PK 재탐색이 필요하지 않다.
  * N개 채우기 : 그 N개의 PK로만 테이블을 찾아 나머지 컬럼을 가져온다.
* 이 둘을 한 쿼리로 합친 것이 바로 지연 조인(Deferred Join)이다. 무거운 본문 읽기를 마지막 N개까지 미뤄두는 방식이다.
```sql
SELECT o.order_id, o.member_id, o.order_status, o.total_price, o.ordered_at
FROM orders o
JOIN (
    -- 1단계: 커버링 인덱스로 페이지의 PK만 추린다 (PK 조회 없음)
    SELECT order_id
    FROM orders
    ORDER BY ordered_at DESC
    LIMIT 20 OFFSET 20000
) AS k ON o.order_id = k.order_id   -- 2단계: 그 20개만 PK로 본문 조회
ORDER BY o.ordered_at DESC;
```
```
-> Sort: o.ordered_at DESC (actual time=4.24..4.24 rows=20 loops=1)
   -> Nested loop inner join (actual time=4.19..4.22 rows=20 loops=1)
       -> Table scan on k (rows=20)
           -> Materialize
               -> Limit/Offset: 20/20000 row(s) (rows=20)
                   -> Covering index scan on orders using idx_orders_ordered_at (reverse)
                      (actual time=0.166..3.73 rows=20020 loops=1)
       -> Single-row index lookup on o using PRIMARY (order_id=k.order_id)
          (rows=1) (loops=20)
```
* `Covering index scan`은 `rows=20020`이다. 인덱스 엔트리는 여전히 20,020개를 훑는다. 하지만 PK 재탐색은 없다.
* `Single-row index lookup`에서 PK 조회가 20번 발생했다.

| 방식 | PK 조회(2단 점프) | 실행 시간(워밍업) |
| --- | --- | --- |
| 풀 스캔 + filesort (옵티마이저 기본 선택) | 0 (500만 풀 스캔) | 약 1.80초 |
| 인덱스 강제 (비커버링) | 20020번 | 약 0.08초 |
| **지연 조인** | **20번** | **약 0.01초** |

* 지연 조인이 가장 빠르고, 콜드 캐시에서도 안전하다.
* 그러나 지연 조인이 만능은 아니다. 1단게 커버링 스캔은 여전히 OFFSET + LIMIT개의 인덱스 엔트리를 훑는다. PK 조회 비용은 없앴지만, OFFSET에 비례해 인덱스를 훑는 비용은 그대로 남는다. 그래서 페이지가 더 깊어지면 지연 조인도 조금씩 느려진다. 이것을 해결하기 위한 방법으로 커서 기반 페이징이 있다.

> **MySQL에서 지연 조인을 자동으로 해주지 않는 이유**
>
> * 첫째, 행은 잘리기 전에 이미 만들어진다. 비커버링 인덱스 스캔은 인덱스를 읽으면서 곧바로 PK 본문까지 찾아 행을 완성한다. `LIMIT`, `OFFSET`은 그렇게 완성된 행을 맨 마지막에 잘라낼 뿐이다. PK만 먼저 추려 줄인 뒤, 그것만 으로 본문은 채우자는 우리들이 인지하는 순서를 MySQL이 스스로 끼워 넣지는 못한다.
> * 둘째, 항상 이득인 것은 아니다. 지연 조인이 빠른 상황은 `OFFSET`이 깊을 때뿐이다. 얕은 OFFSET에서는 서브쿼리를 따로 만들어 조인하는 단계가 필요없다. 상황에 따라 이득과 손해가 갈기에 옵티마이저가 모든 쿼리에 일률적으로 적용하기 어렵다.
> * 정리한다면 지연 조인은 결국 개발자가 깊은 OFFSET 페이징이라는 패턴을 알기 때문에 직접 선택하는 최적화다.

* `filesort`를 피하는 가장 확실한 방법은 `ORDER BY`가 인덱스를 타도록 인덱스를 설계하는 것이다.

#### OFFSET 페이징 함정

* 지연 조인을 사용하더라도 커버링 스캔은 여전히 `OFFSET + LIMIT`개의 인덱스 엔트리를 훑어야 한다.
* 결국 OFFSET 자체는 남아있는 것이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d3a88806-cd2f-4b5c-8f99-136d97049371.png)

* 인덱스를 타고 있음에도 불구하고 페이지가 깊어질수록 실행 시간이 읽고 버리는 행 수에 정비례해서 증가한다.
* OFFSET의 내부 동작은 읽고 버리는 것이 핵심이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fd518d5c-1055-4e4a-ad3e-145a7d9de096.png)

* OFFSET 크기에 비례하여 낭비가 증가한다.
* 인덱스는 정렬 비용(filesort)을 제거하지만, 건너뛰기 비용 자체는 제거하지 못한다. 따라서 페이지 깊이(OFFSET 크기)에 비례하여 성능이 선형적으로 저하된다. 이것이 OFFSET 페이징의 구조적 한계다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/bc7f59ec-4627-47b5-b65c-6120a96d8302.png)

* OFFSET 페이징에는 성능 말고도 한 가지 더 주의할 점이 있다.
* `ORDER BY` 컬럼 값이 동일한 행이 여러 개 있으면 MySQL은 실행 계획에 따라 다른 순서로 반환할 수 있다. 이것은 페이지 간 행이 중복되거나 누락되는 문제를 일으킨다.
* 예를 들어, `ordered_at` 값이 같은 주문이 N건 있다면 1페이지에 나왔던 행이 2페이지에 또 나오거나 아예 빠질 수 있다. 이럴 경우 해결 방법은 `ORDER BY`절에 유니크한 컬럼을 추가하면 된다.

```sql
-- 결정적 정렬: ordered_at이 같으면 order_id로 구분
SELECT order_id, member_id, ordered_at
FROM orders
ORDER BY ordered_at DESC, order_id DESC
LIMIT 20 OFFSET 0;
```
* `order_id`는 PK로 유니크하다. 이렇게 하면 정렬 순서가 항상 동일하게 결정되어 페이지 간 중복/누락이 발생하지 않는다.
* `ordered_at`만으로 정렬할 경우 값이 같은 행끼리는 우열이 정해지지 않아 실행마다 배치 순서가 달라질 수 있는데 여기에 `order_id`를 더하면 모든 행의 정렬 키가 유일함을 보장할 수 있기 때문에 가능한 순서가 단 하나로 확정된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f397fc36-67be-484c-9df8-c2131fd69130.png)

* OFFSET 페이징은 구현이 간단하고 SQL 표준에 가까운 방식이다. 그러나 이 OFFSET 페이징은 구조적으로 뒷페이지가 느려질 수밖에 없다.

| 핵심 | 설명 |
|---|---|
| OFFSET의 동작 | M+N행을 읽고, M행을 버리고, N행만 반환 |
| 성능 특성 | OFFSET 크기에 비례하여 선형적으로 느려짐 |
| 인덱스 효과 | filesort 제거는 가능하지만, 건너뛰기 비용은 제거 불가 |
| 비결정적 순서 | ORDER BY에 유니크 컬럼을 추가하여 해결 |

#### 커서 기반 페이징

* OFFSET은 매번 처음부터 세기 시작한다. 그러면 처음부터 세지 않고 이전 페이지의 마지막 값 이후부터 바로 읽기 시작하는 방법이 있을텐데 이것이 바로 커서 기반 페이징(Keyset Pagniation)이라고 한다.
```sql
-- 커서 기반 방식
SELECT ... WHERE id < :last_id ORDER BY id DESC LIMIT 20;
```
* 커서 기반 페이징은 `WHERE + ORDER BY + LIMIT`이 하나의 인덱스를 타는 원리를 그대로 응용한 패턴이다.
```sql
-- 1페이지
SELECT order_id, member_id, ordered_at
FROM orders
ORDER BY order_id DESC
LIMIT 20;
```
```sql
-- 2페이지
SELECT order_id, member_id, ordered_at
FROM orders
WHERE order_id < 4999981
ORDER BY order_id DESC
LIMIT 20;
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d9b2dc56-cbf6-4ed9-9928-93d34314f922.png)

* 이전 타이 브레이커 문제의 경우 `order_id`라는 유니크한 키를 추가했다. 그러나 실제 주문 화면 목록에서 원하는 정렬은 최신순이다.
* `ordered_at`으로 커서를 잡으면 해결이 되지 않는다. 왜냐하면 `ordered_at`은 같은 '초'에 여러 주문이 들어올 수 있기 때문이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2a450f62-8fd4-4283-a645-68786d9c966c.png)

* 여기서도 해법은 간단하다. 정렬 기준이 유니크하지 않으면, 마지막으로 본 위치를 시각 하나로 콕 집을 수 없다. 유니크한 값을 덧붙이면 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6c5fa52c-e030-4155-af15-1173d2eeb2f7.png)

* 차이는 더 깊이 들어갈 때 드러난다.

| 방식 | 위치 | 읽고 버리는(스캔하는) 행 | 실행 시간 |
|---|---|---|---|
| OFFSET | 1000페이지 (OFFSET 19,980) | 20,000 | 약 0.006 sec |
| OFFSET | 깊은 위치 (OFFSET 2,999,980) | 3,000,000 | 약 0.27 sec |
| 커서 기반 | 어느 위치든 | 20 | 약 0.002 sec |

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/00a3083b-c5a2-496b-be85-fda0a63edfbf.png)

> **커서 기반 페이징의 제약사항 정리**
>
> * 커서 기반 페이징이 만능 아니다. 트레이드오프가 있다.
> * 페이지 점프 불가 : 특정 페이지 번호로 바로 이동할 수 없다. 이전 페이지의 마지막 값을 알아야 다음 페이지를 가져올 수 있어 순차 탐색이 가능하다.
> * 정렬 기준 유니크 필요 : 정렬 기준이 유니크하지 않으면 중복/누락이 발생한다. 유니크하지 않은 컬럼으로 정렬할 때는 PK 등을 추가해야 한다.
> * 정렬 기준 변경 어려움 : 정렬 기준을 바꾸면 커서 의미가 달라진다. 이 경우 인덱스도 새로 설계해야 한다.

* 결론부터 말하자면 대용량 서비스에서 페이징은 커서 기반이 정답에 가깝다. 특히 무한 스크롤이 대세인 모바일 환경에서 다음 페이지만 필요하기에 커서 기반이 잘 맞다. 페이지 번호가 꼭 필요하면 OFFSET + 최대 페이지 제한이 현실적인 타협이다.

| 항목 | **OFFSET** 페이징 | 커서 기반 페이징 |
|---|---|---|
| 원리 | M행 읽고 버리기 | 마지막 값 이후부터 읽기 |
| 성능 | O(offset + limit) | O(limit) |
| 뒷페이지 | 선형적으로 느려짐 | 일정한 성능 |
| 페이지 점프 | 가능 | 불가 |
| 구현 복잡도 | 낮음 | 중간 |
| 적합한 UI | 페이지 번호 클릭 | 무한 스크롤, 더 보기 |

#### GROUP BY 최적화

* `GROUP BY`를 사용하면 MySQL은 내부적으로 그룹을 만들어야 하는데, 인덱스를 활용하지 않으면 임시 테이블(Using temporary)을 생성한다. 그런데 애용량 데이터에서 임시 테이블은 심각한 성능 저하를 유발한다. 이전 버전(MySQL 5.7 등)에서는 `GROUP BY`가 결과를 자동으로 정렬했다. MySQL 8.4에서 `GROUP BY`는 더 이상 암묵적 정렬을 수행하지 않는다. 따라서 정렬된 결과가 필요하면 반드시 명시적 `ORDER BY`절을 추가해야 한다.
* 정렬을 인덱스로 없앴듯이 그룹핑도 결국 인덱스가 좌우한다. MySQL이 `GROUP BY`를 처리하는 길은 인덱스를 어떻게 쓰느냐로 갈리는데, 크게 보면 세 갈래다.
  * 루스 인덱스 스캔(Loose Index Scan) : 인덱스로 각 그룹의 대표값 한 줄만 읽고 끝내는 방법
  * 타이트 인덱스 스캔(Tight Index Scan) : 인덱스 전체를 정렬된 순서 그대로 읽으며 세는 방법
  * 임시 테이블 : 인덱스를 못 써서 임시 테이블을 따로 만드는 방법

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fcffb957-cc00-4d0d-989f-ac27240dcec3.png)

* 각 그룹의 첫(또는 마지막) 키만 집어 읽고 나머지는 통째로 건너뛰는 방식이 바로 루스 인덱스 스캔이다.
* 루스 인덱스 스캔은 아무 때나 걸리지 않는다. 다음과 조건이 충족되어야만 가능하다.

| # | 조건 |
|---|---|
| 1 | **단일 테이블** 쿼리여야 한다 |
| 2 | GROUP BY 컬럼이 인덱스의 **맨 앞 컬럼**(leftmost prefix)부터 차례로 구성해야 한다 |
| 3 | 집계 함수는 **MIN()과 MAX()만** 허용된다 |
| 4 | MIN/MAX 대상 컬럼은 인덱스에서 GROUP BY 컬럼 바로 다음에 위치해야 한다 |
| 5 | GROUP BY 컬럼 이후의 인덱스 부분은 상수 동등 조건이어야 한다 |
| 6 | 접두사 인덱스는 불가 (전체 컬럼을 인덱싱해야 함) |

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/edd29987-24c8-4cab-8ce9-6ed2626b67f4.png)

* `GROUP BY`는 같은 그룹끼리 모아 집계값 하나를 내는 동작이다. 그런데 행이 그룹과 무관한 순서로 뒤섞여 들어오면 MySQL은 한 그룹이 언제 끝났는지 알 수 없다. 그래서 모든 그룹의 중간 집계 상태를 동시에 들고 있어야 하는데, 이 상태를 담아둘 공간이 바로 임시 테이블이다.
* 그룹 키로 임시 테이블을 만들어 두고, 행을 한 줄 읽을 때마다 해당 그룹 칸을 찾아 값을 갱신하는 방식이다. 이것이 `GROUP BY`절에 `Using temporary`가 뜨는 기본 동작이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/89590486-1494-4d61-9148-9e68ed10a46f.png)

* 이렇게 인덱스 전체를 정렬된 순서 그대로 읽되, 그 정렬을 활용해 추가 정렬, 임시 테이블 없이 그룹핑하는 방식이 타이트 인덱스 스캔 방식이다.
* 루스 인덱스 스캔이 각 분류의 첫 번째만 읽었다면 타이트 인덱스 스캔은 순서대로 나열된 것들을 처음부터 끝까지 읽되 같은 분류가 연속이라 세기 쉬운 방식이다.
* `Using index for group-by`가 뜨지 않고, `Using temporary`가 없다는 점이 타이트 인덱스가 걸렸다는 신호로 볼 수 있다.

#### Using temporary : 임시 테이블이 만들어지면 문제다

* 루스 인덱스 스캔도, 타이트 인덱스 스캔도 공통 전제는 `GROUP BY`절 컬럼을 인덱스가 정렬해 준다는 것이다. 그래야 같은 그룹이 인덱스 안에 연속으로 모여 첫 줄만 잡거나 경계만 감지하면 됐다. 그 전제가 무너지면 즉, `GROUP BY`절 컬럼이 인덱스를 활용하지 못하면 MySQL은 임시 테이블을 만들어 그룹핑한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0761e450-8460-4abf-bf7b-37e53ed8b602.png)

* 인덱스를 사용해서 `Using temporary`를 회피한다. 결국 임시 테이블을 피하는 핵심은 `GROUP BY`절 컬럼을 인덱스가 정렬하게 만드는 것이다.

#### GROUP BY + ORDER BY 조합의 함정

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9e91c38a-905f-4dd3-8ce5-b956ad960408.png)

* `filesort`가 정렬하는 대상은 원본이 아니라 그룹 결과다. 따라서 비용은 그룹 수에 달려 있다.
  * 그룹이 적으면(수십 ~ 수백) : 그냥 둔다.
  * 그룹이 많으면(수십만 이상) : 정렬 대상이 그만큼 커지고, 스트리밍으로도 감당이 안되면 진짜 임시 테이블이 디스크로 떨어진다.
* 정렬 기준을 그룹 컬럼으로 바꿀 수 있는지 본다. 집계값(count)이 아니라 그룹 컬럼(category_id)으로 정렬하면 인덱스가 그룹핑과 정렬을 한꺼번에 만족시킨다.
* 그러나 집계값으로 정렬해야 한다면 인덱스로는 그 정렬을 없앨 수 없다. 이 때는 `WHERE`절로 대상을 좁혀 그룹 수 자체를 줄이거나 같은 집계를 자주 돌리는 상황이라면 미리 계산해 둔 요약 테이블을 두고 거기서 정렬하는 것이 정공법이다. 통계, 대시보드성 쿼리를 매번 원본 수백만 건에서 집계하지 않고, 일, 시간 단위로 미리 집계해 둔 작은 테이블에서 조회하는 것이다.
