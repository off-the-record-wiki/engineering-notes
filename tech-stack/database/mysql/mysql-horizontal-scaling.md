# MySQL - MySQL Horizontal Scaling

#### Replication & Distribution

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/aa902f4a-3232-428c-ad4c-47524a5eadc0.png)

* 쓰기 경로 : Client → Master DB → Binary Log → (I/O Thread) → Relay Log → (SQL Thread) → Replica DB 
* 읽기 경로 : Client → Replica DB

***

1. 클라이언트가 쓰기(INSERT/UPDATE/DELETE)를 Master에 전송, Master가 커밋한다.
2. 변경 이벤트가 Binary Log에 기록된다.
3. Replica의 I/O 스레드가 Master의 binlog를 받아 Relay Log에 저장된다.
4. Replica의 SQL 스레드가 Relay Log를 재생하여 Replica DB에 반영

***

* 목적 = 읽기/쓰기 분리(Read/Write Splitting): 쓰기는 Master로, 읽기는 여러 Replica로 분산 → 읽기 처리량을 수평 확장하는 설계이다.
* 한계 = 쓰기는 확장 못 한다: 모든 쓰기는 여전히 단일 Master 로 몰린다. → 쓰기·용량이 한계면 샤딩으로 간다.

#### 복제 방식(읽기 확장)
| 방식 | 특징 | 트레이드오프 |
|---|---|---|
| **비동기(Async)** | Master는 Replica 반영을 기다리지 않고 커밋 | 가장 빠름, 유실·지연 위험 |
| **반동기(Semi-sync)** | 최소 1개 Replica가 **수신 ACK** 할 때까지 커밋 대기 | 유실 위험↓, 약간의 지연 |
| **그룹 복제(InnoDB Cluster)** | 다중 노드 합의 기반 자동 failover | 강한 일관성/HA, 운영 복잡 |

#### 복제 방식(읽기 확장) 설계 시 고려사항 1 - 복제 지연(Replication Lag)
* 원인 : 네트워크 지연·대역폭, 물리적 거리, Master 쓰기 폭주, 단일 SQL 스레드 재생
* 쓰기 직후 읽기 시 최신값이 안 보일 수 있다.
* 완화책
  * Read-your-writes : 방금 쓴 데이터를 즉시 읽어야 하면 그 읽기는 Master로 라우팅
  * 반동기 복제로 유실·지연 축소
  * 병렬 복제(Multi-threaded Replica) 로 재생 속도 ↑
  * Seconds_Behind_Master 등 지연 모니터링 후 임계 초과 Replica는 로드밸런싱에서 제외

#### 복제 방식(읽기 확장) 설계 시 고려사항 2 - Replica 단일 장애점(SPOF) · Master Failover
* Replica가 하나면 가용성 위험 → 다중 Replica 구성을 필요로 한다.
* Master 장애 대비 자동 Failover 필요 : GTID 기반 재배치 + Orchestrator / MHA / MySQL Router(InnoDB Cluster) 등

#### 파티셔닝 vs 샤딩 — 쓰기·용량 확장

| | **파티셔닝 (Partitioning)** | **샤딩 (Sharding)** |
|---|---|---|
| 범위 | **단일 서버 내** 테이블을 조각으로 분할 | **여러 서버**로 데이터 분산 |
| 확장 | 쓰기 확장 ❌ (한 노드) | **쓰기·용량 수평 확장 ⭕** |
| 주 목적 | 대형 테이블 관리·조회 최적화 | 노드 한계 돌파 |

#### 파티셔닝(단일 노드)
* RANGE / LIST / HASH / KEY 로 테이블을 물리적 조각으로 분할한다.
* 파티션 프루닝 : 조건에 맞는 파티션만 스캔 → 조회 성능 ↑
* 아카이빙 궁합 : 날짜 RANGE 파티션 + 오래된 파티션 DROP PARTITION → 삭제가 즉시 일어난다.
* 파티셔닝은 한 서버 안이라 여러 머신으로 쓰기를 분산하지 못한다.

#### 샤딩(다중 노드)

| 전략 | 방식 | 리샤딩 부담 |
|---|---|---|
| **레인지 (Range)** | 키 범위로 분할 (예: id 1~1M → shard0) | 낮음(범위 추가) / 핫샤드 위험 |
| **해시/모듈로 (Hash/Modulo)** | `key % N` | **높음** (N 변경 시 거의 전체 이동) |
| **일관된 해시 (Consistent Hashing)** | 해시 링에 노드 배치 | **낮음** (일부만 이동) ✅ 모듈로의 해법 |
| **디렉터리 (Directory/Lookup)** | 키→샤드 매핑 테이블 | 유연 / 매핑 조회 비용·SPOF |

* 라우팅 방식은 다음과 같이 크게 3가지로 나뉜다.
  * 애플리케이션 : 앱이 직접 샤딩 키 계산 → DataSource 선택
  * ProxySQL : SQL을 파싱해 쿼리 라우팅(읽기/쓰기 분리 겸용)
  * Vitess : MySQL 클러스터 전체를 추상화, 자동 리샤딩 지원

#### 샤딩 설계 시 고려사항 1 - 모듈로 샤딩의 리샤딩 위험
* 서버 추가 시 key % N의 N이 바뀌어 거의 전 데이터 이동 → 매우 위험하다.
* `consistent hashing` 또는 디렉터리 매핑 으로 이동량 최소화하면 된다.

#### 샤딩 설계 시 고려사항 2 - 크로스 샤드 쿼리 한계
* 여러 샤드에 걸친 조회(예: WHERE user_id IN (123, 124))는 scatter-gather 필요해 오히려 느려지고 복잡하다.
* 집계(JOIN/GROUP BY) 어렵다.
* 자주 함께 읽는 데이터를 같은 샤드로(샤딩 키 정렬), 글로벌/레퍼런스 테이블 복제, 비정규화 방법 등이 있다.

#### 샤딩 설계 시 고려사항 3 - 샤딩 키 설계가 성패를 가른다
* 핫샤드(특정 샤드에 데이터·트래픽 집중) 방지 : 카디널리티 높고 접근이 고르게 분산되는 키를 선택한다.

#### 샤딩 설계 시 고려사항 4 - 트랜잭션 범위 제한
* 크로스 샤드 분산 트랜잭션은 2PC 또는 SAGA 필요 → 설계 복잡도가 급증하게된다.
* 되도록 트랜잭션을 단일 샤드 내로 유지하도록 키를 설계한다.

#### 데이터 생명주기 — 아카이브 & 압축(노드 경량화)

**1. 아카이브(오래된/콜드 데이터 분리)**
* 파티션 DROP : 날짜 RANGE 파티션에서 오래된 파티션을 DROP PARTITION → 대량 DELETE 없이 즉시 제거한다.
* pt-archiver(Percona Toolkit) : 조건에 맞는 행을 별도 테이블/파일/아카이브 DB로 점진 이관한다.
* 콜드 스토리지 이관 : 조회 빈도 낮은 데이터를 저렴한 저장소/별도 DB로 이관한다.
* TTL/Purge 배치 : 보존 기간 지난 로그·이벤트 주기 삭제한다.

**2. 압축(Compression)**
```sql
CREATE TABLE logs_compressed (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    message TEXT,
    created_at TIMESTAMP
)
ROW_FORMAT=COMPRESSED
KEY_BLOCK_SIZE=8;
```
* `ROW_FORMAT=COMPRESSED`: zlib 기반 페이지 압축
* 페이지 압축(Transparent Page Compression, MySQL 8): 파일시스템 hole-punching 기반의 대안(단, 해당 방식은 파일시스템 지원을 필요로 한다.)

#### 아카이브 & 압축 설계 시 고려사항 1 - CPU vs Disk 트레이드오프
* 압축은 Disk I/O를 줄이지만 CPU 사용이 증가한다.
* I/O 바운드·로그성 테이블에 유리 / CPU 바운드·고빈도 쓰기에는 불리하다.

#### 아카이브 & 압축 설계 시 고려사항 2 - 버퍼 풀 효율 저하
* 압축 페이지와 압축 해제 페이지가 둘 다 메모리 점유 → 버퍼 풀 소비 최대 2배, 메모리 부족 환경에선 히트율이 저하된다.

#### 아카이브 & 압축 설계 시 고려사항 3 - 압축률은 예측 불가하다.
* 데이터 특성에 크게 의존 : Text/JSON은 높고, 이미 압축된 BLOB·숫자 위주 테이블은 이득 적고 CPU만 낭비된다.

#### 아카이브 & 압축 설계 시 고려사항 4 - KEY_BLOCK_SIZE가 중요하다.
* 너무 작으면 압축 실패 → 페이지 분할 → 오히려 용량 증가하는 문제가 발생한다.

| 상황 | 처방 |
|---|---|
| 읽기가 병목 | 캐시 → **읽기 복제본(Replication)** |
| 쓰기 직후 최신값 필요 | 그 읽기만 **Master 라우팅** / semi-sync |
| 쓰기·용량이 단일 노드 한계 | **샤딩** (consistent hashing, 샤딩 키 신중히) |
| 대형 테이블 조회 최적화 | **파티셔닝**(단일 노드) |
| 노드가 무거워짐 | **아카이브(파티션 DROP/pt-archiver) + 압축** |
