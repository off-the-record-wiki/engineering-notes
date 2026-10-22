# Database - Index Internal Structures 2 - Clustered and Secondary Indexes

#### InnoDB 인덱스 종류

* 클러스터드 인덱스 : 테이블당 딱 하나. PK 기준으로 만들어지며, 리프 노드에 행 데이터 전체가 저장된다.
* 세컨더리 인덱스 : PK가 아닌 컬럼에 만드는 일반 인덱스. 리프 노드에는 인덱스 키와 PK만 저장된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/424d6ca1-1b95-4b15-beda-646b9e9219f6.png)

* 클러스터드 인덱스(Clustered Index) : 테이블당 딱 하나만 존재하는 인덱스다. 리프 노드에 실제 행 데이터가 전체가 저장되어, 인덱스 자체가 곧 테이블이다. 기본적으로 PK를 기준으로 만들어지며, 데이터가 PK 순서대로 물리적으로 정렬되어 저장된다.
* 세컨더리 인덱스(Secondary Index) : PK 외의 컬럼에 만드는 일반적인 인덱스이다. 한 테이블에 여러 개 만들 수 있다. 리프 노드에는 실제 데이터 대신 인덱스 컬럼 값과 PK 값만 저장된다. 실제 데이터가 필요한 경우라면 PK로 클러스터드 인덱스를 한 번 더 타야 한다.

#### 클러스터드 인덱스

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2fc1c9b2-f20f-4b01-978c-518bc59ea926.png)

* 리프 노드에는 실제 데이터가 저장되어 있다.
* 리프 노드끼리는 양방향으로 연결되어 있다.

#### 클러스터드 인덱스 선택 규칙

* `PRIMARY KEY`를 정의한 경우라면 그것이 클러스터드 인덱스가 된다.
* `PRIMARY KEY`를 정의하지 않은 경우라면 모든 키 컬럼이 `NOT NULL`인 첫 번째 `UNIQUE` 인덱스를 사용한다.
* 만약 둘 다 없으면 InnoDB가 `GEN_CLUST_INDEX`라는 숨겨진 클러스터드 인덱스를 자동 생성한다.

#### 클러스터드 인덱스 - 순차 PK vs 랜덤 PK

* PK 순서 = 물리 저장 순서이다. PK 선택이 테이블 전체의 물리 배치를 결정한다.
* PK를 어떻게 정하느냐에 따라서 INSERT 성능이 완전히 달라진다.
* `BIGINT AUTO_INCREMENT`: PK값이 순서대로 증가한다.
* `BIGINT` : PK값이 완전 랜덤이다. 크기/타입은 동일하므로 오직 "랜덤 삽입"의 영향만 측정된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f0d2d8f1-0279-477c-a1e0-c80bb87245f7.png)

#### 클러스터드 인덱스 - 순차 PK vs 랜덤 PK INSERT에서 성능 차이

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a1816304-144d-42a6-8ce4-7fc4b48282c5.png)

* `AUTO_INCREMENT`는 항상 현재 최대값보다 큰 값이 생성된다. 따라서 데이터 삽입시 클러스터드 인덱스의 마지막 리프 페이지에 순차적으로 추가된다. 맨 끝에 계속 append하는 셈이니 페이지 분할이 거의 일어나지 않는다. 반면 랜덤 값은 무작위다. 따라서 클러스터드 인덱스의 임의의 위치에 끼어든다. 이미 가득 찬 페이지 사이에 들어가야 하므로 빈번한 페이지 분할이 빈번하게 발생한다. 페이지 분할은 단순 CPU 작업이 아니라 새 페이지 할당 + 데이터 이동 + 부모 노드 갱신 + 디스크 쓰기까지 동반한다.
* 순차 PK는 B+Tree의 오른쪽만 건드린다. 삽입에 필요한 페이지는 사실상 루프 ⭢ 오른쪽 경로 몇 개뿐이고, 이 페이지들은 계속 재사용되어 버퍼 풀에 영구 상주한다. 따라서 디스크를 거의 읽지 않는다. 반면 랜덤 PK는 완전히 다른 위치의 리프 페이지를 건드린다. 데이터가 쌓일수록 리프 페이지 수가 늘고 버퍼 풀에 다 올라오지 못하는 페이지가 생긴다. 그 순간부터 삽입할 때마다 해당 페이지를 디스크로부터 읽어와서 수정 후 다시 쓰기라는 랜덤 디스크 I/O가 발생한다.
* 순차 PK : 페이지가 PK 순서대로 배치. 순차 쓰기 보장되어 SSD에서도 순차 쓰기가 훨씬 빠르다.
* 랜덤 PK : 페이지가 디스크 여기저기에 흩어져있다. 랜덤 쓰기로 인해 플러시해야 할 페이지 수 자체가 많아진다.
* 결국 같은 데이터를 저장하는 데 랜덤 PK 테이블은 순차 PK 테이블보다 많은 물리 공간을 쓴다. 행 크기 때문이 아니라 페이지 단위에서 생긴 빈틈이 누적된 결과이다. 테이블이 크면 버퍼 풀 히트율이 떨어진다. 풀스캔, 범위 검색, 백업, 복제가 모두 느려진다.

#### 클러스터드 인덱스 - 실무 PK 설계 방법

* PK는 `BIGINT AUTO INCREMENT` 권장 : 물리적인 순차 삽입 성능 확보를 위해 테이블 내부 식별자인 PK는 순차 증가하는 값을 사용한다.
* 회원 ID 등은 별도의 인덱스로 분리 : 사용자는 식별하는 비즈니스 키는 별도의 컬럼으로 빼고 `UNIQUE` 인덱스를 걸어서 사용한다.

> **❗INT(4바이트)를 쓰면 용량이 줄어 더 좋지 않은가?**
>
> * `INT`와 `BIGINT`의 차이는 레코드당 단 4바이트에 불과하다. 1억건을 저장하더라도 고작 400MB 차이이다.
> * 반면 서비스가 성장해 `INT`의 한계치에 도달했을 때 이를 `BIGINT`로 변경하는 대가는 상상을 초월한다. 테이블 전체에 락을 걸고 원본 데이터는 물론, PK를 참조하는 모든 세컨더리 인덱스까지 전부 재작성해야 하는 매우 위험하고 막대한 작업이 동반된다.
> * 개발 일관성 측면에서도 유리하다. 테이블마다 `INT`, `BIGINT`가 섞여 있으면 백엔드 언어와 매핑할 때 혼란이 생기고 코드 복잡도가 올라간다. 모든 PK를 `BIGINT`로 통일하면 ORM 등에서 공통 클래스를 만들어 재사용하기 훨씬 수월하다.

> **❗그럼 데이터가 적은 테이블에서는 INT를 써도 되지 않는가?**
>
> * 데이터가 많이 쌓이지 않는 테이블이어도 의미 없는 수준이다.

> **❗UUID, 분산 ID가 꼭 필요한 분산 시스템에서 ID를 미리 생성해야 하는 요구사항이 있다면 다음을 고려한다.**
>
> * `MySQL 8.0+ UUID_TO_BIN(UUID(), 1)` : MySQL이 제공하는 유일한 네이티브 순차형 UUID. `UUID()`가 만드는 시간 부분을 앞으로 재배치해 이진 저장 시 거의 순차에 가까운 정렬이 된다.
> * `UUIDv7, ULID` : 시간순 정렬이 보장되는 최신 분산 ID 표준. MySQL에는 해당 함수가 없다. 애플리케이션에서 라이브로 생성한 뒤, DB에는 `BINARY(16)`으로 저장하는 구조다. 시간순 정렬이 표준으로 보장되기에 순차 삽입의 이점을 그대로 누릴 수 있다.
> * 하이브리드 : `AUTO_INCREMENT` PK를 유지하고 `UUID`는 별도 유니크 컬럼으로 관리한다.

#### 세컨더리 인덱스

* 세컨더리 인덱스는 클러스터드 인덱스를 제외한 모든 인덱스이다. `CREATE INDEX`로 만드는 인덱스. `UNIQUE KEY`로 만드는 유니크 인덱스(PK가 아닌 경우) 모두 세컨더리 인덱스이다.
* 세컨더리 인덱스의 리프 노드에는 행 데이터가 없다. 대신 인덱스 키 컬럼 값과 PK값이 저장된다.

#### 2단 점프 - 세컨더리 인덱스 동작 구조

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7d6b6110-3cfe-4c83-8a89-0cf5e95342cd.png)

* `Index lookup` : 세컨더리 인덱스에서 조건을 찾고 PK로 클러스터드 인덱스로 2단 점프해 실제 데이터를 가져온다.
* `Limit` : `Index lookup`이 가져다주는 데이터를 받다가 N건이 채워지면 더 이상 인덱스를 탐색하지 않고 즉시 종료한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7f10d091-35da-4d27-b4e9-9179a8b7868c.png)

#### 세컨더리 인덱스 - 커버링 인덱스(Covering Index)

* 2단 점프를 아예 없애려면 쿼리가 요구하는 모든 컬럼이 세컨더리 인덱스에 포함되어 있으면 클러스터드 인덱스를 재탐색할 필요가 없다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0b2763e6-e3c6-43cf-9a9c-60e3df8fe841.png)

#### 세컨더리 인덱스 - 실무 활용

* 세컨더리 인덱스의 리프 노드에서 동일한 인덱스 키 값을 가진 레코드. 즉 타이브레이커의 경우 다시 PK값 순서로 정렬되어 저장된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bda48605-3f68-4559-9314-cf68d0a307e6.png)

#### PK의 크기

* PK가 `BIGINT`(8바이트)일 때 : 8바이트 x 1억 건 x 인덱스 5개 = 약 4GB
* PK가 `CHAR(36)`(최대 144바이트)일 때 : 144바이트 x 1억 건 x 인덱스 5개 = 약 72GB
* PK 타입을 문자열로 바꾸면 잉여 공간 낭비가 상상을 초월한다. 인덱스가 뚱뚱해지면 16KB 페이지 하나에 담을 수 있는 데이터 건수가 확연히 줄어든다. 트리는 더더욱 깊어지고 한정된 버퍼 풀은 금방 가득찬다. 결국 잦은 디스크 I/O를 유발해 시스템 전체 성능을 저하시킨다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1e31744b-6d70-4ea5-acc4-366e3774e89e.png)

* 이런 이유로 MySQL을 사용할 경우 짧고 순차적인 PK를 사용하는 것을 권장한다.

#### 랜덤 I/O vs 순차 I/O 정리

* 세컨더리 인덱스의 2단 점프 구조가 소량의 데이터를 찾을 경우에는 인덱스가 훨씬 빠르지만 조회하는 데이터의 범위가 넓어지면 상황이 완전히 달라진다.
* 특정 지점을 넘어서면 인덱스를 타는 것보다 차라리 풀 테이블 스캔이 더 빨라지는 순간이 온다. 이것은 랜덤 I/O와 순차 I/O의 근본적인 성능 차이 때문이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1dc2ff63-ea96-4507-8e76-9d067b1ccb9b.png)

| 저장 매체 | 순차 접근(Sequential) | 랜덤 접근(Random) | 성능 차이 (순차 기준) |
|---|---|---|---|
| HDD | 50,000 ns (0.05ms) | 10,000,000 ns (10ms) | 약 100 ~ 200배 빠름 |
| SSD | 약 100,000 ns | 약 100,000 ns (0.1ms) | 약 5 ~ 10배 빠름 |
| 메모리(RAM) | 1~2 ns (캐시 적중) | 약 100 ns (캐시 미스) | 수십 배 빠름 |

**1. HDD(Hard Disk Drive)**
* 순차 I/O : 디스크 헤드가 한 번 자리를 잡고 트랙을 따라 빙글빙글 돌면서 연속된 데이터를 쭉 읽으면 된다. 대량의 데이터를 매우 빠르게 읽을 수 있다.
* 랜덤 I/O : 데이터를 읽을 때마다 무거운 디스크 헤드를 이리저리 움직이고 원판을 회전시켜야 한다. 물리적인 탐색 시간이 매번 발생하기에 순차 I/O에 비해 수십 ~ 수백 배 이상 느리다.

**2. SSD(Solid State Drive)**
* 물리적인 디스크 헤드나 원판이 없다. 플래시 메모리를 사용하므로 HDD 대비 랜덤 I/O 탐색 시간이 획기적으로 줄어든다.
* SSD 컨트롤러 내부 병렬 처리, OS 수준의 미리 읽기, 블록 단위 플래시 메모리 I/O 특성 때문에 여전히 순차 I/O가 랜덤 I/O보다 수 배 이상 빠르다.

**3. 메모리(RAM)와 CPU 캐시**
* 데이터베이스 버퍼 풀에 데이터가 올라와 있다면 디스크가 아니라 메모리에서 데이터를 읽는다. 메모리는 이름부터 RAM이니 랜덤과 순차 접근 속도가 거의 없을 것 같으나 여기에는 CPU 캐시(L1, L2, L3)라는 변수가 숨어있다.
* 순차 읽기 : 데이터가 메모리 상에 연속해서 배치되어 있으면 CPU 하드웨어는 다음에 연속된 데이터를 찾을 것이라는 예측을 하고 데이터를 CPU 캐시로 미리 끌어온다. 덕분에 캐시 적중률이 극대화되어 메모리 접근조차 최소화된다.
* 랜덤 읽기 : 이리저리 흩어진 메모리 주소를 무작위로 찌르면 CPU 예측이 불가능하다. CPU 캐시 미스가 빈번히 발생하고 CPU는 매번 상대적으로 느린 메인 메모리(RAM)까지 다녀와야 한다.

#### 인덱스 스캔 = 랜덤 I/O, 풀 스캔 = 순차 I/O

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3074b965-55dd-4319-bf42-429f551ecd09.png)

* 인덱스 스캔(세컨더리 인덱스를 통한 접근) = 랜덤 I/O : 세컨더리 인덱스에서 PK를 찾고 그 PK로 클러스터드 인덱스를 재탐색한다. 각 행이 서로 다른 페이지에 흩어져 있으면 매번 다른 위치의 페이지를 읽어야 한다.
* 풀 테이블 스캔 = 순차 I/O : 클러스터드 인덱스의 리프 노드를 처음부터 끝까지 연결 리스트를 따라 읽는다. 연속된 페이지를 순서대로 읽으므로 순차 I/O 패턴을 보인다. 디스크의 순차 읽기는 랜덤 읽기보다 훨씬 빠르다.

#### 미리 읽기(Read-Ahead) : 미리 읽어두는 순차 읽기 최적화 메커니즘

* 풀 스캔이 빠른 이유 중 하나로 InnoDB의 미리 읽기 기능 덕분이 있다. 이 기능은 이름 그대로 쿼리가 페이지를 순차적으로 읽고 있다는 것을 InnoDB가 눈치채면, 다음에 필요할 페이지를 미리 디스크에서 메모리로 가져다 놓는다. 쿼리는 그 페이지가 실제로 필요해졌을 때 디스크를 기다릴 필요 없이 바로 메모리에서 꺼내 쓴다.
* InnoDB는 데이터를 익스텐트(Extent) 덩어리로 관리한다. 한 익스텐트는 64페이지로 구성되어 있다. 이 중 56페이지 이상이 순차적으로 읽히면 풀 스캔이라고 판단하고 다음 익스텐트를 미리 읽어온다. 이런 기능 덕분에 디스크에서 페이지를 기다리는 시간이 크게 줄어든다.
* `Innodb_buffer_pool_read_ahead` : 미리 읽기. 미리 가져다 놓은 페이지 수. 풀 스캔이 많이 돌았을수록 이 값이 쌓인다.
* `Innodb_buffer_pool_read_ahead_evicted` : 미리 가져가 놓긴 했는데 쿼리가 실제로는 쓰지 않고 버려진 페이지 수. InnoDB 예측이 빗나간 횟수. 이 값이 너무 크면 미리 읽기가 비효율적으로 돌고 있다는 신호이다.

#### 버퍼 풀 히트율 : I/O 패턴 차이

```sql
-- 버퍼 풀 히트율 확인
SELECT
    (1 - (
        (SELECT VARIABLE_VALUE FROM performance_schema.global_status
          WHERE VARIABLE_NAME = 'Innodb_buffer_pool_reads')
        /
        NULLIF((SELECT VARIABLE_VALUE FROM performance_schema.global_status
                WHERE VARIABLE_NAME = 'Innodb_buffer_pool_read_requests'), 0)
    )) * 100 AS hit_rate_pct;
```

* 히트율이 99% 이상이면 대부분의 읽기가 메모리에서 처리되어 I/O 패턴 차이가 줄어든다. 히트율이 낮으면 디스크 I/O가 발생하므로 랜덤 I/O와 순차 I/O 차이가 명확해진다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8aadf607-8c61-4b9a-b793-662a7f00904c.png)

* 인덱스를 설계했는데 타지 않았다고 해서 강제로 인덱스 사용을 부여하게 되면 역효과가 발생한다.

```sql
EXPLAIN ANALYZE SELECT * FROM product FORCE INDEX (idx_product_category_id)
WHERE category_id BETWEEN 11 AND 19;
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1afb334f-5994-4cf3-87cf-0b0dfc70baf3.png)

* 옵티마이저가 인덱스를 무시하고 풀 스캔을 선택한 것은 바보가 아니라 원리를 알고 있고 비효율적이라는 것을 알기 때문이다.

#### 명시적 비율 임계값이란 없다.

* 옵티마이저는 여러 비용적 요소를 종합해 인덱스를 사용할지 안할지를 결정한다. 거의 대부분의 손익분기점은 5 ~ 25% 정도이다.
  * 테이블 크기와 행 길이
  * 인덱스 선택도
  * 버퍼 풀 크기와 히트율
  * 스토리지 유형(SSD vs HDD)

#### MySQL, Oracle, PostgreSQL 설계 철학 정리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/06e5875a-cd03-40b7-a443-a378b7da3fa9.png)

* 힙 구조란, PK 순서와 상관없이 데이터가 삽입되는 순서대로 무작위로 쌓이는 방식이다.
* 클러스터드 구조란, 비슷한 PK 값을 가진 행들이 디스크 위에서 서로 가까이 뭉쳐있다는 의미이다.

> **❗힙 블록 = InnoDB의 페이지와 동일한 개념**
>
> * 스토리지 엔진이 데이터를 담아 두는 I/O 최소 단위이다.

* 힙(Heap, Oracle/PostgreSQL) : 인덱스 리프 노드가 데이터의 물리적 주소를 직접 가리킨다.
* InnoDB(MySQL) : 인덱스 리프 노드가 데이터의 논리적 식별자인 기본 키 값을 가리킨다.

> **❗어떤 구조가 인덱스 읽기에 더 유리한 구조일까?**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/57699090-489d-4ba5-9c12-e97331a85678.png)

> * 힙(Heap) : 세컨더리 인덱스를 탐색한 뒤, 리프 노드에 있는 물리적 주소로 디스크 블록에 직접 1회 접근해 데이터를 가져온다.
> * 클러스터드 : 세컨더리 인덱스를 탐색한 뒤, 리프 노드의 PK를 얻고 그 PK로 클러스터드 인덱스를 처음부터 재탐색한다. 

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/94f23377-f0e5-403b-bafc-5f1627f96790.png)

> * 힙(Heap) : 세컨더리 조회와 비슷한 비용이 발생한다.
> * 클러스터드 : 추가 블록 읽기 없이 즉시 반환 가능해 압도적으로 빠르다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b9fb0bc6-3b27-4de9-a457-ce1dc1677bc9.png)

> * 힙(Heap) : PK 인덱스는 PK 순서대로 정렬되어 있지만, 실제 힙 데이터는 PK 순서로 저장되어 있지 않다. 인덱스를 따라가며 각 행을 힙에서 꺼내면 여기저기 흩어진 블록을 찌르는 대량의 랜덤 I/O가 발생한다.
> * 클러스터드 : 데이터 자체가 PK 순서대로 물리적으로 정렬되어 있다. PK 범위 검색은 디스크 페이지가 연속적으로 읽히는 순차 I/O가 발생해 압도적으로 빠르다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fe8f3c5a-0eea-4395-bad3-fee34dd4e156.png)

> * 힙(Heap) : 세컨더리 인덱스를 순서대로 훑으면서 각 행마다 ROWID/CTID로 힙 블록에 직접 1회 접근한다. 행마다 1회의 랜덤 I/O가 발생한다.
> * 클러스터드 : 행마다 랜덤 I/O 1회가 발생하는 것은 맞다. 그런데 여기서 리프 노드의 PK 값을 가지고 클러스터드 인덱스를 재탐색하는 과정이 반복된다.
> * **❗결론 : PK 범위 검색과 정반대로 세컨더리 인덱스 범위 검색은 InnoDB가 구조적으로 불리하다는 점을 인지하자.**

#### 커버링 인덱스의 구조적 안정성

* 커버링 인덱스 : InnoDB 세컨더리 인덱스에는 PK가 리프 노드에 포함되어 있다. 만약 쿼리가 세컨더리 인덱스 컬럼과 PK만 조회한다면 재탐색없이 바로 응답하는 구조가 되면서 단점이 상쇄된다.
* UPDATE 비용 감수 : Heap 구조에서는 데이터가 갱신되어 물리적 위치가 바뀌면 해당 테이블에 걸린 모든 인덱스들의 물리 주소가 갱신되어 쓰기 오버헤드가 원리상 존재한다. 반면 InnoDB는 물리적 위치가 이리저리 바뀌어도 논리적 식별자인 PK는 변하지 않으므로 세컨더리 인덱스를 전혀 갱신할 필요는 없다.

#### 우열 관계가 아닌 설계 철학의 차이

* PostgreSQL/Oracle : 어떤 인덱스로 접근하든 균일하게 빠른 읽기 성능을 보장한다.
* MySQL(InnoDB) : 세컨더리 인덱스에서 약간의 손해를 감수하더라도 PK 기반 접근의 성능을 극대화하고 데이터의 물리적 정렬을 통해 I/O를 최적화하겠다.
