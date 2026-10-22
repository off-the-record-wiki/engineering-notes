# Database - The Optimizer and Histograms

#### 비용 기반 옵티마이저

* 인덱스가 있다고 무조건 인덱스를 사용하는 것이 아니다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/03d9bc50-18a2-4d94-aa36-7f3a825f27e8.png)

| 상황 | I/O 패턴 | 비용 |
|:---:|:---:|:---:|
| 인덱스로 소량 조회 | 랜덤 I/O x 소량 | 낮음 |
| 인덱스로 대량 조회 | 랜덤 I/O x 대량 | 매우 높음|
| 풀 스캔 | 순차 I/O x 전체 | 일정 |

* 조회해야 할 행이 테이블의 상당 부분을 차지하면 랜덤 I/O 비용이 순차 I/O 비용을 넘어서는 순간이 온다. 옵티마이저는 이 비용을 비교해서 총 비용이 더 낮은 쪽을 선택하는 것이다.
* 고정 비율은 없다. 보통 테이블의 10~15% 이상 읽으면 풀 스캔으로 전환된 가능성이 높다.
* MySQL에는 고정된 비율 임계값이 없다. 옵티마이저는 I/O 비용 + CPU 비용을 합산한 후 총 비용을 비교해서 판단한다. 비용은 통계 정보(행 수, 카디널리티, 페이지 수)와 비용 상수에 의해 결정된다. 따라서 테이블의 크기, 행의 길이, 인덱스의 구조, 버퍼 풀에 캐시된 데이터 비율 등에 따라 전환 시점이 달라진다.

#### 비용이란?

* 옵티마이저는 쿼리를 실행할 수 있는 여러 가지 방법을 만들고 각 방법의 비용(Cost)을 추정한 뒤, 가장 비용이 낮은 방법을 선택한다. 이것이 비용 기반 최적화(Cost-Based Optimization)다.

| 비용 유형 | 설명 | 예시 |
|:---:|:---:|:---:|
| I/O 비용 | 디스크 또는 버퍼 풀에서 페이지를 읽는 비용 | 인덱스 페이지 읽기, 데이터 페이지 읽기 |
| CPU 비용 | 행을 비교하고 평가하는 연산 비용 | WHERE 조건 검사, 키 비교, 정렬 |

* 총 비용 = I/O 비용 + CPU 비용이다. 옵티마이저는 이 총 비용이 가장 낮은 실행 계획을 선택한다.
* 비용 계산의 기준이 되는 숫자들은 `mysql.server_cost`와 `mysql.engine_cost` 두 테이블에 저장한다.

```sql
-- 서버 비용 상수 조회
SELECT cost_name, cost_value, default_value
FROM mysql.server_cost;
```

| cost_name | cost_value | default_value |
| --- | --- | --- |
| `disk_temptable_create_cost` | NULL | 20.0 |
| `disk_temptable_row_cost` | NULL | 0.5 |
| `key_compare_cost` | NULL | 0.05 |
| `memory_temptable_create_cost` | NULL | 1.0 |
| `memory_temptable_row_cost` | NULL | 0.1 |
| `row_evaluate_cost` | NULL | 0.1 |

* `row_evaluate_cost` = 0.1 - 행 하나를 읽어 WHERE 조건에 맞는지 평가하는 비용이다. 풀 스캔처럼 많은 행을 훑을수록 이 값이 차곡차곡 쌓인다.
* `key_compare_cost` = 0.05 - 키 2개를 비교하는 비용이다. 주로 정렬(ORDER BY)이나 인덱스 키 비교에서 발생한다.
* `memory_temptable_create_cost` = 1.0 - 메모리에 임시 테이블 하나를 만드는 비용이다. GROUP BY, DISTINCT, 일부 서브쿼리처럼 중간 결과를 담을 공간이 필요할 때 발생한다.
* `disk_temtable_create_cost`= 20.0 - 디스크에 임시 테이블 하나를 만드는 비용이다. 메모리 임시 테이블이 한계를 넘어 디스크로 내려갈 때 발생한다.
* `disk_temptable_row_cost` = 0.5 - 디스크 임시 테이블에 행 하나를 처리하는 비용이다.

```sql
-- 엔진 비용 상수 조회
SELECT engine_name, cost_name, cost_value, default_value
FROM mysql.engine_cost;
```
* `io_block_read_cost` = 1.0 - 디스크에서 페이지를 읽는 비용
* `memory_block_read_cost` = 0.25 - 버퍼 풀(메모리)에서 페이지를 읽는 비용
* 디스크 읽기가 메모리 읽기보다 4배 비싸다. 따라서 이 비율이 옵티마이저의 판단에 큰 영향을 미친다. 버퍼 풀에 데이터가 많이 캐시되어 있으면 I/O 비용이 낮아져서 옵티마이저가 더 적극적으로 인덱스를 사용할 수 있다.
* 이런 내용을 토대로 결국 옵티마이저는 비용이 가장 낮은 계획을 고른다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ec04c9b9-aa04-4270-b66c-5badb4d0edf5.png)

#### 테이블 통계 정보

* InnoDB의 통계 정보는 기본적으로 디스크에 영구 저장된다. 이것을 영구 통계(Persistent Statistics)라고 한다.
```sql
-- InnoDB 통계 관련 설정 확인
SHOW VARIABLES LIKE 'innodb_stats%';
```
| Variable_name | Value | 의미 |
|---|---|---|
| `innodb_stats_persistent` | ON | 통계를 디스크에 영구 저장한다 |
| `innodb_stats_auto_recalc` | ON | 행의 10% 이상 변경 시 통계를 자동 재계산한다 |
| `innodb_stats_persistent_sample_pages` | 20 | 통계 계산 시 인덱스당 20페이지를 샘플링한다 |

#### 테이블 통계 : mysql.innodb_table_stats

```sql
-- 테이블 단위 통계 조회
SELECT database_name, table_name, n_rows,
clustered_index_size, sum_of_other_index_sizes, last_update
FROM mysql.innodb_table_stats
WHERE database_name = 'shop';
```

| database_name | table_name | n_rows | clustered_index_size | sum_of_other_index_sizes | last_update |
|---|---|---:|---:|---:|---|
| shop | category | 47 | 1 | 1 | 2026-05-19 12:05:03 |
| shop | member | 2,911,814 | 19,897 | 19,507 | 2026-06-04 12:42:34 |
| shop | order_item | 8,784,119 | 30,144 | 31,157 | 2026-05-19 12:17:38 |
| shop | orders | 4,858,780 | 18,998 | 25,997 | 2026-06-08 15:04:05 |
| shop | product | 4,863,628 | 24,958 | 43,913 | 2026-06-05 11:19:45 |
| shop | review | 2,995,457 | 11,952 | 13,538 | 2026-05-19 12:18:52 |

* `n_rows` : 테이블의 추정 행 수다. 옵티마이저가 "이 테이블을 전부 읽으면 몇 행을 만나는가"를 계산할 때 출발점으로 삼는 값이다.
* `clustered_index_size` : 클러스터형 인덱스(Clustered Index). 즉 실제 행 데이터가 저장된 본체의 크기다. 단위는 페이지(Page)이고, InnoDB의 한 페이지는 기본 16KB다.
* `sum_of_other_index_sizes` : 클러스터형 인덱스를 뺀 나머지 세컨더리 인덱스(Secondary Index)들의 크기를 모두 더한 값이다. 이 역시 페이지 단위다.
* `last_update` : 이 통계가 마지막으로 갱신된 시각이다. 이 값이 오래됐다면 통계가 현실과 어긋나 있을 수 있다는 신호다.
* 그런데 이 값은 샘플링 기반 추정 값이기 때문에 정확한 행 수는 아니다. 따라서 실제 `COUNT(*)`와 비교하면 차이가 날 수 있다.

#### 인덱스 통계 : mysql.innodb_index_stats

```sql
-- 인덱스 단위 통계 조회
SELECT index_name, stat_name, stat_value, sample_size, stat_description, last_update
FROM mysql.innodb_index_stats
WHERE database_name = 'shop'
AND table_name = 'orders'
AND index_name = 'idx_orders_member_id';
```

| index_name | stat_name | stat_value | sample_size | stat_description | last_update |
|---|---|---:|---:|---|---|
| idx_orders_member_id | n_diff_pfx01 | 2,032,515 | 20 | member_id | 2026-... |
| idx_orders_member_id | n_diff_pfx02 | 5,133,739 | 20 | member_id, order_id | 2026-... |
| idx_orders_member_id | n_leaf_pages | 8,742 | NULL | Number of leaf pages in the index | 2026-... |
| idx_orders_member_id | size | 10,035 | NULL | Number of pages in the index | 2026-... |

* `index_name` : 통계 대상 인덱스 이름이다. 여기서는 `idx_orders_member_id` 하나만 조회했다.
* `stat_name` : 이 행이 어떤 통계인지를 가리키는 이름표이다. 인덱스 하나의 통계가 여러 행으로 나뉘어 저장되는데, 그 종류를 구분하는 키다.
  * `n_diff_pfx01` : 인덱스 선두 1개 컬럼(member_id)의 고유값 수, 즉 카디널리티 추정치다.
  * `n_diff_pfx02` : 선두 2개 컬럼(member_id, order_id)을 묶었을 때의 고유값 수다. 여기서 pfx는 프리픽스(prefix, 앞쪽 컬럼)를 뜻하고, 뒤의 숫자는 선두에서 몇 번째 컬럼까지 묶었는지를 가리킨다. `order_id`가 PK라 거의 모든 행이 유일하므로 값이 전체 행 수에 가깝게 나온다.
  * `n_leaf_pages` : 인덱스의 리프 페이지(Leaf Page, 실제 키가 저장된 최하단 페이지) 수다.
  * `size` : 리프와 내부 노드를 모두 합한 인덱스 전체 페이지 수다.
* `stat_value` : 해당 통계의 값이다. 
* `sample_size` : 그 값을 구하려고 샘플링한 페이지 수다.  전수 조사가 아님을 보여준다. 샘플링이 필요 없는 실측 메타정보는 이 값이 `NULL`이다.
* `stat_description` : 사람이 읽으라고 붙은 설명이다. 어떤 컬럼 조합인지, 또는 무엇을 센 값인지 알려준다.

#### ANALYZE TABLE : 통계를 수동으로 갱신하기

* 통계가 오래되거나 부정확하다면 `ANALYZE TABLE`을 실행하면 된다.
* 이렇게 실행하면 InnoDB가 `innodb_stats_persistent_sample_pages`(기본 20) 페이지를 다시 샘플링하여 통계를 갱신한다.
* 따라서 `mysql.innodb_table_stats`와 `mysql.innodb_index_stats`가 즉시 업데이트된다.
* Mysql 8.4 InnoDB에서 `ANALYZE TABLE`은 통계 계산 중에도 INSERT, UPDATE, DELETE가 가능하고, 다른 쿼리를 막지도 않으므로 운영 환경에서 부담 없이 실행할 수 있다.
* 다만 통계가 갱신되면 옵티마이저 실행 계획이 바뀔 수 있어 실행 후에는 주요 쿼리 실행 계획이 의도대로인지 확인하는 것이 안전하다.

#### 자동 재계산의 트리거 : 시간이 아니라 변경량

* `innodb_stats_auto_recalc` = ON 이면 통계가 자동으로 재계산된다.
* 테이블 행의 10% 이상이 변경되면 자동 재계산이 트리거된다.
* 그리고 이 재계산은 비동기(백그라운드)로 실행된다. 즉, 트리거 직후에도 수 초 동안 오래된 통계가 남아 있을 수 있다. 즉시 갱신이 필요하면 `ANALYZE TABLE`을 수동으로 실행해야 한다.

> **운영 중 통계 관련으로 주의해야 할 상황들 정리**
> * 배치 작업 후 : 야간 배치로 대량 INSERT/DELETE를 수행한 직후에는 통계가 부정확할 수 있다. 배치 작업 스크립트 마지막에 `ANALYZE TABLE`을 추가하는게 필요할 수 있다.
> * 배포 직후 :  새 인덱스를 추가하면 해당 인덱스의 통계가 자동으로 생성된다. 하지만 대량 데이터가 이미 있는 테이블에 인덱스를 추가하면, 즉시 `ANALYZE TABLE`로 정확한 통계를 확보하는 것이 좋다.
> * 실행 계획이 갑자기 바뀌었을 때 : 어제까지 빨랐는데 갑자기 느려졌다는 보고를 받으면 통계를 먼저 의심한다. `ANALYZE TABLE`을 실행하고 실행 계획이 달라지는지 확인한다.

#### 히스토그램

* 인덱스가 있는 컬럼은 옵티마이저가 분포를 들여다볼 수단이 있다.
* 반면 인덱스가 없는 컬럼은 그 수단이 없다. 기본 통계만으로는 추정할 방법이 없다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8adeac75-c7d7-4507-959b-33f3dd9e18d8.png)

* MySQL 8.0에서 도입된 히스토그램(Histogram)은 바로 이 문제를 해결한다. 히스토그램은 컬럼의 값 분포 통계를 옵티마이저에게 제공하여 편향된 데이터에서도 어느 정도 정확한 행 수 추정을 가능케 한다.
```sql
-- orders.order_status 컬럼에 히스토그램 생성
ANALYZE TABLE orders UPDATE HISTOGRAM ON order_status;
```
```sql
-- 히스토그램 정보 조회
SELECT COLUMN_NAME,
JSON_EXTRACT(HISTOGRAM, '$.\"histogram-type\"') AS histogram_type,
JSON_EXTRACT(HISTOGRAM, '$.\"number-of-buckets-specified\"') AS buckets,
JSON_EXTRACT(HISTOGRAM, '$.\"sampling-rate\"') AS sampling_rate
FROM information_schema.COLUMN_STATISTICS
WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'order_status';
```

| COLUMN_NAME | histogram_type | buckets | sampling_rate |
|---|---|---:|---:|
| order_status | "singleton" | 100 | 0.0148... |

* `COLUMN_NAME` : 히스토그램이 생성된 대상 컬럼이다.
* `histogram_type` : 히스토그램의 유형이다. 값마다 한 칸을 쓰는 `singleton`과 여러 값을 범위로 묶는 `equi-height` 두 가지가 있다.
* `buckets` : 히스토그램을 만들 때 쓸 버킷(칸) 수의 상한이다. `WITH n BUCKETS`로 지정하지 않으면 기본값 100이 들어간다. 주의할 점은, 이 값은 "최대 몇 칸까지 쓸 수 있는가"라는 상한일 뿐 실제로 만들어진 칸 수가 아니다.
* `sampling_rate`: 히스토그램을 만들 때 테이블에서 얼마나 표본을 뽑았는지를 나타내는 비율(0~1)이다. `0.0148...` 은 전체의 약 1.48%만 읽어 분포를 추정했다는 뜻이 된다. 1.0이면 전수 조사다. MySQL은 `histogram_generation_max_mem_size`(기본 20MB) 메모리 한도 안에서 가능한 만큼만 샘플링이므로 테이블이 클수록 이 비율은 낮아진다. 즉 히스토그램도 인덱스 통계와 마찬가지로 샘플링 기반 추정이며, 그래서 빈도 값에 약간의 오차가 있을 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3873e37e-8378-46e7-bf6e-aa373ea64881.png)

* Singleton 히스토그램의 버킷은 [값, 누적 빈도] 쌍으로 저장된다.
  * 누적 빈도는 줄자에서 각 칸이 끝나는 위치다. 첫 칸부터 지금 칸까지를 모두 더한 값이라 갈수록 커지기만 하고, 마지막 칸은 반드시 1.0이 된다.
  * 자체 빈도는 그 칸 하나의 너비, 곧 그 값이 실제로 차지하는 비율이다. (현재 칸이 끝난 위치 − 직전 칸이 끝난 위치)로 구한다.

#### 히스토그램의 두 가지 유형

| 유형 | 조건 | 버킷 구조 | 정밀도 |
|---|---|---|---|
| **Singleton** | 고유 값 수 <= 버킷 수 | `[값, 누적 빈도]` | 높음 (각 값의 정확한 빈도) |
| **Equi-Height** | 고유 값 > 버킷 수 | `[하한, 상한, 누적 빈도, 고유 값 수]` | 중간 (범위로 근사) |

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/af91a17a-3ac7-467b-8ce5-7cd034577c0b.png)

#### 히스토그램과 인덱스의 관계

* 히스토그램은 인덱스가 아니다. 인덱스는 데이터 접근 경로를 제공하고 히스토그램은 옵티마이저의 행 수 추정을 돕는 통계일 뿐이다.
* 쿼리 실행시 실시간으로 참조되는 구조가 아니라, 실행 계획을 선택하는 단계에서만 사용된다.

| 비교 항목 | 인덱스 | 히스토그램 |
|---|---|---|
| 역할 | 데이터 접근 경로 | 행 수 추정 보조 |
| DML 오버헤드 | INSERT/UPDATE/DELETE 시 유지 비용 발생 | **없음** (행이 변경될 때마다 유지하지 않음) |
| 저장 위치 | B+Tree (디스크) | information_schema (딕셔너리) |
| 갱신 방식 | DML 발생 시 자동 반영 | 기본은 수동, MySQL 8.4부터 자동 갱신 설정 가능 |

* 히스토그램은 DML 오버헤드가 없다는 것이 큰 장점이다. 인덱스를 추가하면 INSERT/UPDATE/DELETE가 느려지지만, 히스토그램은 그런 부작용이 없다.

#### 히스토그램 갱신 방식 : MANUAL UPDATE와 AUTO UPDATE

* 히스토그램을 만들 때 수동 갱신(MANUAL UPDATE)과 자동 갱신(AUTO UPDATE) 중 하나를 선택할 수 있다.

```sql
-- 갱신 옵션을 생략하면 MANUAL UPDATE가 적용된다.(수동 갱신)
ANALYZE TABLE orders UPDATE HISTOGRAM ON order_status;
```
```sql
-- (자동 갱신)
ANALYZE TABLE orders UPDATE HISTOGRAM ON order_status AUTO UPDATE;
```
* 단, `AUTO UPDATE`도 INSERT나 UPDATE가 발생할 때마다 히스토그램을 즉시 고치는 기능이 아니다. InnoDB 통계 자동 재계산은 백그라운드에서 비동기로 실행되므로 갱신이 조금 늦을 수 있다.

#### 히스토그램을 언제 만들어야 하는가?

* 인덱스가 있는 컬럼에서는 히스토그램 효과가 제한적이다. 인덱스가 있으면 옵티마이저가 인덱스 다이브를 통해 더 정확한 행 수를 추정할 수 있기 때문이다. 히스토그램은 인덱스가 없는 컬럼에서 선택도 추정의 주된 소스 역할을 한다.

| 상황 | 히스토그램 효과 |
|---|---|
| 값 분포가 심하게 편향된 컬럼 (인덱스 없음) | **높음** |
| WHERE 절에 자주 등장하는 컬럼 (인덱스 없음) | **높음** |
| 조인 조건의 필터링에 사용되는 컬럼 | 중간 |
| 이미 인덱스가 있는 컬럼 | **낮음 (인덱스 다이브가 우선)** |
| 고유 값이 1~2개인 컬럼 | **낮음 (분포가 단순)** |

* 히스토그램을 만들 수 없는 경우도 있다. 암호화된 테이블, TEMPORARY 테이블, 공간 데이터 컬럼, JSON 컬럼, 그리고 단일 컬럼 유니크 인덱스가 있는 컬럼에는 히스토그램을 생성할 수 없다.
