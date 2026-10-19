# Database - Execution Plans 1 - Mastering EXPLAIN

#### EXPLAIN 확인

```sql
EXPLAIN
SELECT *
FROM product
WHERE category_id = 11  -- '스마트폰'
  AND price BETWEEN 10000 AND 50000
  AND product_status = 'ACTIVE'
ORDER BY created_at
LIMIT 20;
```

* 인덱스는 감으로 추가하면 실패할 가능성이 높다. 무엇보다 인덱스는 공짜가 아니다.
* INSERT/UPDATE/DELETE시에 해당 인덱스를 계속 갱신해야 한다. 따라서 갱신 성능이 떨어지고 디스크 공간도 늘어난다. 효과도 없는 인덱스를 추가하면 공간적 측면에서 손해만 보는 셈이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6ffab187-c904-4438-831d-a2ddf61340a0.png)

```
1. EXPLAIN으로 진단한다 - 옵티마이저가 이 쿼리를 어떻게 실행하는지 확인한다.
2. 병목 원인을 파악한다 - 풀스캔인지 아니면 인덱스를 잘못 타는건지, 정렬에서의 병목인지 정확히 짚는다.
3. 근거에 기반해 대응한다 - 진단 결과에 맞는 인덱스를 설계하거나 쿼리를 수정한다.
```

#### EXPLAIN이란 무엇인가?

* EXPLAIN은 MySQL이 쿼리를 어떻게 실행할지 보여주는 진단 도구이다.
* EXPLAIN을 실행하면 다음과 같은 정보를 알 수 있다.
  * **어떤 방식으로 테이블에 접근하는가** - 인덱스를 타는지, 전체를 훑는지
  * **어떤 인덱스를 사용하는가** - 여러 인덱스 중 어떤 것을 선택했는지
  * **몇 건의 데이터를 조사하는가** - 추정 행 수가 많을수록 느리다.
  * **추가 작업이 필요한가** - 정렬이 필요한지, 임시 테이블을 만드는지
* EXPLAIN은 쿼리를 실제로 실행하지 않는다. 옵티마이저가 생각한 예상 실행 계획을 보여줄 뿐이다.

#### EXPLAIN 기본 사용법 정리

* EXPLAIN은 3가지 형식으로 결과를 볼 수 있다.
  * **TRADITIONAL** : 기본 옵션으로 테이블 형식으로 조회할 수 있다.
  * **JSON** : JSON 형식은 TRADITIONAL에 없는 상세 비용 정보를 포함한다. 프로그래밍적으로 처리할 때 유용하지만 일상적인 진단에서는 TRADITIONAL로 충분하다.
  * **TREE** : 실행 계획을 트리로 보여준다. 들여쓰기로 계층을 표현한다.

#### 12개 출력 컬럼 전체 개요

* EXPLAIN의 TRADITIONAL 형식은 12개 컬럼을 출력한다.

| 컬럼 | 의미 | 중요도 |
|:----:|:----:|:----:|
| id | SELECT의 순차 번호 | 쿼리 구조 파악 |
| select_type | SELECT의 유형(SIMPLE, PRIMARY, SUBQUERY 등) | 쿼리 구조 파악|
| table | 접근하는 테이블 이름 | 기본 정보 |
| partitions | 매칭되는 파티션(파티션 미사용시 NULL) | 파티션 사용 시에만 |
| type | 접근 유형 | 가장 중요 |
| possible_keys | 사용 가능한 인덱스 후보 리스트 | 인덱스 분석 |
| key | 실제 사용된 인덱스 | 핵심 |
| key_len | 사용된 인덱스 바이트 길이 | 핵심 |
| ref | 인덱스와 비교되는 컬럼/상수 | 조인 분석 |
| rows | 조사할 추정 행 수 | 핵심 |
| filtered | 조건 필터링 비율(%) | 행 추정 정밀화 |
| Extra | 추가 정보(Using Index, Using filesort 등) | 핵심 |

#### EXPLAIN 읽는 순서 - 1단계 : 쿼리 구조 파악(`id`, `select_type`)

```sql
-- 서브쿼리가 있는 경우
EXPLAIN
SELECT *
FROM member
WHERE member_id = (
    SELECT member_id
    FROM orders
    WHERE order_id = 100
);
```
* `id`가 같으면 같은 SELECT, 다르면 별도의 SELECT(서브쿼리)이다.
* 서브쿼리가 있어도 옵티마이저가 내부적으로 조인으로 변환하면 `id`가 같게끔 나올 수 있다. 이 때는 `select_type`도 `SIMPLE`이 된다.

#### EXPLAIN 읽는 순서 - 2단계 : 접근 방식 확인(`type`)

* `type`은 가장 중요한 컬럼이다. 테이블에 어떤 방식으로 접근하는지를 보여준다.
* `const`부터 `ALL`까지 등급이 있다.

#### EXPLAIN 읽는 순서 - 3단계 : 인덱스 선택 확인(`possible_keys`, `key`)

* `possible_keys`는 사용 가능한 인덱스 후보, `key`는 실제로 선택된 인덱스이다.
* `key`가 NULL이면 인덱스를 사용하지 않았다는 뜻이다.

#### EXPLAIN 읽는 순서 - 4단계 : 인덱스 활용 범위 확인(`key_len`)

* `key_len`은 인덱스의 바이트 길이다. 복합 인덱스에서 몇 개 컬럼까지 활용했는지 판단하는 핵심 지표다.

#### EXPLAIN 읽는 순서 - 5단계 : 행 추정 확인(`rows`, `filtered`)

* `rows`는 조사할 추정 행 수, `filtered`는 조건 필터링 비율이다. `rows`가 클수록 느리다.
* `rows`값들의 곱이 쿼리 전체가 조사해야 하는 행 수의 대략적인 지표가 된다.

#### EXPLAIN 읽는 순서 - 6단계 : 추가 정보 확인(`Extra`)

* `Extra`는 `Using index`, `Using temporary`같은 키워드가 표시된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/348a47a9-1961-48fe-80a2-7d8b92692ac4.png)

* EXPLAIN 직후에 `SHOW WARNINGS`를 실행하면 유용한 정보를 얻을 수 있다.

```sql
EXPLAIN
SELECT *
FROM member
WHERE member_id = 1;

SHOW WARNINGS;
```
* `SHOW WARNINGS`의 Message에는 옵티마이저가 재작성한 쿼리가 표시된다. 서브쿼리가 조인으로 변환되거나, 불필요한 조건이 제거되는 등의 최적화를 확인할 수 있다.

#### TYPE - 접근 유형 1

* `type`은 MySQL이 테이블에 어떤 방식으로 접근하는지를 보여준다. 최선(const)부터 최악(ALL)까지 순서가 정해져 있으며, 성능 판단의 가장 중요한 지표다.
* `const` : 상수는 변하지 않는 고정된 수라는 뜻이다. `const`는 PK 또는 유니크 인덱스의 모든 컬럼을 상수 값과 비교할 때 나타난다. 매칭되는 행이 최대 1개이므로, 옵티마이저는 이 행의 컬럼 값을 상수처럼 취급한다.
* `eq_ref` : 조인에서 PK 또는 UNIQUE NOT NULL 인덱스의 모든 컬럼이 비교에 사용될 때 나타난다. `const`는 단일 테이블 조회, `eq_ref`는 조인에서 발생한다는 점에서 다르다. 둘 다 최대 1행을 반환하지만 발생하는 맥락이 다르다.
* `ref` : 비유니크 인덱스 동등 조건. `ref`는 비유니크 인덱스의 동등 조건이나 복합 인덱스의 왼쪽 접두사만 사용할 때 나타난다. 매칭되는 행이 여러 개일 수 있지만 인덱스를 사용하므로 빠르다.

#### TYPE - 접근 유형 2

* `range` : 인덱스 범위 스캔. `range`는 인덱스를 사용하여 주어진 범위 내의 행만 검색할 때 나타난다. `>`, `>=`, `<`, `<=`, `IS NULL`, `BETWEEN`, `LIKE`, `IN` 연산자로 인덱스 컬럼을 상수와 비교할 때 주로 사용된다. `range`는 인덱스를 활용하므로 상대적으로 효율적이나 범위가 넓으면 그만큼 느려질 수 있다.
* `index` : 인덱스 트리 전체 스캔. `index`는 인덱스 트리 전체를 스캔한다. 쿼리에 필요한 모든 컬럼이 인덱스에 들어 있으면, 테이블 데이터에 접근하지 않고 인덱스만 읽는다. 인덱스는 테이블보다 훨씬 작으므로, `ALL`보다 훨씬 작다.
* `ALL` : 테이블 전체를 처음부터 끝까지 스캔한다. 대용량 테이블에서 가장 위험한 접근 유형이다. 

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7e878dc4-b4d1-410f-84f5-a38720e4e4a9.png)

* `index` : 인덱스 트리를 스캔한다. 인덱스는 보통 테이블보다 크기가 작다.
* `ALL` : 테이블을 스캔한다. 모든 컬럼을 포함하므로 보통 크기가 크다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9bbf8069-c0dc-46cf-ab0f-71623d2828c4.png)

#### 핵심 컬럼 분석 1 - `possible_keys`, `key`

* `possible_keys` : MySQL이 이 테이블에서 사용할 수 있는 인덱스 후보 목록이다. `key`는 그 중 옵티마이저가 실제로 선택한 인덱스이다.
  * 그런데 실제로 `possible_keys`에 인덱스가 있어도 `key`가 NULL인 특수한 경우가 있다. 옵티마이저가 생각할 때 선택 가능한 인덱스가 있지만, 선택하지 않는 것이 더 좋다고 생각하는 특수한 경우이다.
* 이 경우 풀 스캔이 더 빠른데 인덱스로 데이터를 읽으면 다음과 같은 과정을 거친다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f736b6e2-cc51-485e-af3a-2c09601147d6.png)

* 읽어야 할 행이 전체의 일부(일반적으로 20 ~ 30% 미만)일 때는 이 방식이 빠르다. 하지만 거의 모든 행을 읽어야 하는 상황이라면 N번의 랜덤 I/O가 발생하고 같은 데이터 페이지를 여러 번 오가며 읽어야 할 수도 있다.
* 반면 풀 테이블 스캔은 데이터 페이지를 처음부터 끝까지 순차적으로 읽는다. 디스크는 이런 순차 I/O에 훨씬 강하다.

#### 후보에 없지만 인덱스 사용

* `possible_keys`는 NULL인데 `key`에는 실제 인덱스 이름이 찍히는 경우가 있다.
* 이 현상은 쿼리가 오직 인덱스만 읽고 처리를 마치는 커버링 인덱스(Covering Index)로 동작할 때 발생한다. `WHERE` 조건절에 인덱스 컬럼이 아예 없어서 옵티마이저 후보에는 오르지 못했지만 SELECT하는 컬럼들이 인덱스에 전부 포함되어 있어 옵티마이저가 테이블 데이터 페이지를 읽지 말고 인덱스만 훑자는 똑똑한 판단을 하는 것이다.
* 테이블에서 데이터를 읽으려면 모든 컬럼이 담긴 무거운 데이터 페이지를 디스크에서 읽어와야 하고 인덱스 리프 노드에서 알아낸 PK로 테이블을 일일이 매핑해 찾아가는 무거운 랜덤 I/O 비용까지 치러야 한다. 하지만 인덱스만 읽고 쿼리를 끝내면 테이블로의 이동 자체가 완전히 생략된다. 테이블 접근을 원천 차단해서 디스크 I/O 부하가 극적으로 줄어들기 때문에 더 빠른 성능을 보장받는다.

#### 커버링 인덱스인지 판단하는 방법

* `Using index` : 인덱스만 읽고 끝. 데이터 페이지를 전혀 건드리지 않는다.
* `Using where` : 테이블 데이터를 읽고 WHERE 조건으로 한 번 더 걸러낸다.
* 비어있음 : 인덱스로 위치를 찾고 데이터 페이지를 읽는다.(일반적인 인덱스 접근)

#### 핵심 컬럼 분석 2 - `key_len`, `ref`

* `key_len` : 사용된 인덱스 키의 바이트 길이다. 이 값이 중요한 이유는 복합 인덱스에서 몇 개의 컬럼까지 실제로 활용되었는가를 역산할 수 있다.

| 데이터 타입 | 바이트 크기(기본) | 비고 |
|:--:|:--:|:--:|
| TINYINT | 1바이트 | |
| SMALLINT | 2바이트 | |
| INT | 4바이트 | |
| BIGINT | 8바이트 | |
| DATE | 3바이트 | |
| TIME | 3바이트 | |
| DATETIME | 5바이트 | |
| TIMESTAMP | 4바이트 | |
| CHAR(n) | n x (문자셋 바이트) | |
| VARCHAR(n) | n x (문자셋 바이트) + 길이 접두사 | |

#### 복합 인덱스 `key_len` 연산

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ad8668dd-c8d3-4923-a229-48bbde364740.png)

* 운영 중인 쿼리의 EXPLAIN을 볼 때 `key_len`이 기대보다 짧으면 복합 인덱스의 일부 컬럼만 활용되고 있다는 뜻이다. 인덱스를 다 만들어 놓고 제대로 활용하지 못하는 상황이다.

#### `ref` - 인덱스 비교 대상

* `ref`는 참조의 약자로, 인덱스를 통해 데이터를 검색할 때 비교 대상(매칭 값)으로 어떤 것이 사용되었는가를 보여주는 컬럼이다. 인덱스 조건에 매칭되는 값을 제공하기 위해 사용한 값의 정체를 밝히는 중요한 단서가 된다.
* `const` : 비교 대상이 고정된 상수인 경우
* `데이터베이스.테이블.컬럼` : 다른 테이블과 조인하여 상대 테이블의 컬럼 값을 참조한 경우
* `func` : 비교 대상이 어떤 함수의 연산 결과이거나, 데이터 타입이 달라 내부적인 형변환을 거친 값의 경우다.

#### 핵심 컬럼 분석 3 - `rows`, `filtered`

* `rows` : 조사할 추정 행 수. `rows`는 옵티마이저가 쿼리를 실행하기 위해 조사해야 한다고 추정하는 행 수이다.
* `filtered` : 테이블 조건에 의해 필터링되어 남는 행의 추정 비율(%)이다.

![스크린샷 2026-08-03 22 07 38](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c623a19c-0b5d-4ce3-b8f7-31a926104fcf.png)

#### Extra 컬럼 해석

* `Using index` : 긍정 신호. 커버링 인덱스가 적용되었다는 뜻이다. 쿼리에 필요한 모든 컬럼이 인덱스에 포함되어 있어서 테이블 데이터에 접근하지 않고 인덱스만으로 결과를 반환한다.
* `Using where` : WHERE 조건이 적용되어 읽은 행 중 일부를 필터링한다는 뜻이다.
* 실행 엔진과 스토리지 엔진이 주고받는 순서 : 실행 엔진(Server Layer)은 쿼리 파싱, 조인/정렬/WHERE 평가 등을 담당하고 스토리지 엔진(Storage Engine)은 실제 데이터와 인덱스를 읽고 쓰는 역할을 담당한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/dafd15e1-30c1-40e9-b6ef-ef8cc931f4bb.png)

* `Using where` 자체가 나쁜 것은 아니다. 하지만 서버 레벨 필터링이 많다면(=filterd 값이 낮다면) 인덱스 설게를 재검토할 필요가 있다.

#### Using filesort - 최악의 신호

* 조회한 결과를 정렬해서 가져오고 싶을 때, 보통 ORDER BY를 사용한다.
* 데이터베이스 서버가 인덱스로 정렬을 해결하지 못하고 직접 CPU를 써서 정렬 연산을 수행했다는 경고 신호이기 때문이다.
* 여기서 말하는 파일 정렬(filesort)은 쿼리의 정렬 요구사항을 인덱스로 해결하지 못해 MySQL이 자체적으로 수행하는 정렬 작업을 뜻한다. 
* MySQL은 정렬할 데이터를 메모리 공간인 정렬 버퍼(Sort Buffer)에 올려두고 정렬을 시도한다.
  * 메모리 정렬 : 정렬해야 할 데이터 크기가 시스템 설정값인 정렬 버퍼 크기(sort_buffer_size)보다 작으면 메모리에서 모든 정렬 작업을 완료한다.
  * 디스크 정렬 : 데이터 크기가 정렬 버퍼를 초과하면 데이터를 여러 조각으로 나누어 디스크에 임시 파일로 저장한 뒤, 이를 병합하는 정렬을 수행한다. 디스크 I/O가 발생하므로 속도는 느리다.
* 디스크를 사용했다는 것이 핵심이 아니다. 메모리 안에서 정렬을 끝냈다 하더라도 수십만 건에 달하는 대량의 데이터를 매번 CPU 자원을 소모해가며 `O(log N)`의 정렬 연산을 직접 실행하는 것 자체가 시스템 전체에 심각한 병목을 일으킨다는 점이다. 인덱스를 사용했다면 `O(log N)`의 정렬 연산 없이 거져 얻을 수 있었을 정렬 순서를, 서버가 직접 땀을 흘려 가며 연산하고 있는 셈이다.

#### Using temporary - 위험 신호

* 옵티마이저는 "가져온 데이터를 가공해서 결과를 만들어야 하는데 이 중간 데이터를 잠시 보관해 둘 장소가 없는지?" 생각해서 MySQL 뒤에서 만드는 작업 공간을 임시 테이블을 사용한다.
* 임시 테이블은 기본적으로 메모리에 먼저 만들어진다. 메모리 안에서 가볍게 처리될 때 그나마 성능이 버틸 만하지만 가공해야 할 데이터가 너무 많아서 설정된 임시 테이블 크기(tmp_table_size)나 최대 힙 테이블 크기(max_heap_table_size)를 초과해 버리면 메모리에 있던 임시 테이블을 던져 버리고 디스크 기반 임시 테이블로 강제 전환한다.
* 디스크 I/O가 발생하는 순간 쿼리 성능은 매우 크게 떨어진다. 주로 인덱스가 없는 상태에서 ORDER BY와 GROUP BY를 섞어 쓰는 상황에 자주 발생한다.

#### Using index condition - 긍정 신호

* `Using index condition`은 ICP(Index Condition Pushdown) 최적화가 적용되었다는 뜻이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/05b5e530-934b-401c-8425-be0ba27809b9.png)
