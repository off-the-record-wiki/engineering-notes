# MySQL ‐ MySQL Internal Architecture and Storage Engines

#### Internal Architecture and Storage Engines

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e5c2ad3f-b02f-4e7b-a041-9fe03c8d25c0.png)

#### 각 계층별 역할

* MySQL은 크게 연결 계층 → SQL 계층 → 스토리지 엔진 계층의 3단으로 나뉜다.

**1. 클라이언트 (Application · JDBC)**
* 애플리케이션이 커넥션을 열고 SQL 문을 서버로 전송힌다.
* 커넥션 하나가 서버의 쓰레드(세션) 하나와 연결된다.

**2. 연결 계층 (Connection Layer)**
* 인증/권한 : "이 사용자가 맞는지, 이 작업을 할 권한이 있는지" 확인한다.
* 커넥션 풀 : 커넥션을 매번 새로 만들면 비싸므로 재사용한다.
* 스레드(세션) 관리 : 각 커넥션을 독립된 세션으로 관리한다.

**3. SQL 계층 (MySQL 서버)**
* SQL을 실제로 이해하고, 계획을 세우고, 실행하는 부분이다.

| 구성 요소 | 역할 |
|---|---|
| **파서 (Parser)** | SQL 문법·구문 분석 (문법 오류를 여기서 잡음) |
| **옵티마이저 (Optimizer)** | **어떤 인덱스로, 어떤 순서로 읽을지 실행 계획 수립** |
| **실행기 (Executor)** | 계획대로 스토리지 엔진을 호출하고 결과를 취합 |

**4. 스토리지 엔진 계층(InnoDB)**
* 실제 데이터를 읽고 쓰는 곳이다.
* MySQL은 엔진을 갈아끼울 수 있고(MyISAM 등), MySQL 8의 기본은 InnoDB이다.
* SQL 계층은 "무엇을 할지"만 정하고, "어떻게 저장/조회할지"는 엔진에 맡긴다.

**5. 디스크(파일 시스템)**
* `.ibd` 테이블스페이스(데이터·인덱스), redo/undo 로그, binlog(복제·복구용) 등이 저장된다.
* 서버가 죽어도 남는 영구 저장소이다.

#### InnoDB 핵심 3가지 정리

**1. Buffer Pool — 읽기가 빠른 이유**
* 자주 쓰는 데이터·인덱스를 메모리에 캐시하는 공간이다.
* 대부분의 읽기가 디스크까지 안 가고 메모리에서 처리되어 빠르다.
* InnoDB 성능 튜닝 1순위가 버퍼 풀 크기(innodb_buffer_pool_size)이다. 메모리에 데이터가 많이 올라가 있을수록 디스크 I/O가 줄어든다.

**2. Redo Log — 안 죽는 이유(내구성)**
* 변경 사항을 데이터 파일에 바로 쓰지 않고, 먼저 순차적으로 로그에 기록한다.
* 커밋 시점에 redo 로그만 안전하게 기록되면, 서버가 갑자기 죽어도 재시작 때 복구할 수 있다. (Durability / 내구성)
* 랜덤 쓰기(데이터 파일) 대신 순차 쓰기(로그)를 먼저 하므로 쓰기 성능도 좋아진다.

**3. Undo Log — 되돌리고, 동시에 읽는 이유**
* 변경 전(old) 값을 따로 보관한다.
* 롤백(Rollback) : 트랜잭션을 취소하면 undo로 원래 값을 복원한다.
* MVCC : 다른 트랜잭션이 커밋 전 값을 보지 않도록 이전 버전을 제공 → 트랜잭션 격리 수준의 기반을 제공한다.

#### MySQL Core Memory, Transaction Logs, and Buffers

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bc574055-731b-498b-9e93-be37e3083ef1.png)

| 참여자 | 역할 |
|---|---|
| 트랜잭션1 (쓰기) | `id=1`을 UPDATE → **행에 락을 거는 쪽** |
| 트랜잭션2 (읽기) | 같은 데이터를 SELECT → **MVCC로 읽는 쪽** |
| 트랜잭션3 (쓰기) | 같은 행/범위를 UPDATE·INSERT → **대기하는 쪽** |
| MySQL InnoDB | 락 관리자 |
| MVCC 시스템 | 스냅샷(수정 전 버전) 제공자 |

**1. 행 락과 MVCC — 읽기는 안 막고, 쓰기끼리만 막는다.**

***

1. T1 : `UPDATE ... id=1` → InnoDB가 id=1 행에 배타적 락(X Lock)을 설정(커밋 전)
2. T2 : 같은 id=1을 SELECT → 락을 기다리지 않고 MVCC 스냅샷(수정 전 버전)을 즉시 반환한다.
3. T3 : 같은 id=1을 UPDATE → 쓰기라서 대기 (T1이 X락 보유 중)
4. T1 : COMMIT → 락 해제 → T3이 락 획득 후 UPDATE 실행 → 커밋

***

* 일반 SELECT는 락을 안 건다 — 잠긴 행이어도 MVCC로 논블로킹 읽기가 가능하다.
* 쓰기 vs 쓰기는 막는다 — 같은 행을 동시에 UPDATE하면 뒤엣놈은 앞엣놈의 커밋/롤백까지 대기한다.
* 스냅샷의 출처는 Undo Log — T2가 본 "수정 전 데이터"는 undo log에 남아있던 이전 버전이다.

**2. 갭 락 — 존재하지 않는 값의 삽입까지 막는다.**

***

1. T2 : `SELECT ... WHERE id BETWEEN 10 AND 20 FOR UPDATE` → `FOR UPDATE`는 락을 거는 읽기. InnoDB가 id 10~20 범위(갭)에 갭 락 설정한다.
2. T3 : `INSERT ... VALUES(15, ...)` → 15는 아직 없는 값인데도 갭이 잠겨 INSERT 대기한다.
3. T2 : COMMIT → 갭 락 해제 → T3의 INSERT 실행 → 커밋한다.

***

* 갭 락은 "행"이 아니라 "행과 행 사이 빈 공간(범위)" 을 잠근다. → 즉, 아직 없는 15번 삽입도 막힌다.
* 목적은 팬텀 리드(Phantom Read) 방지. T2가 10 ~ 20 범위로 작업 중인데 그 사이 15가 끼어들면, 다시 조회 시 없던 행이 나타난ㄷ.
* 갭 락이 이를 원천 차단한다.

| 개념 | 무엇을 잠그나 | 언제 | 효과 |
|---|---|---|---|
| **배타적 락 (X Lock)** | 해당 **행** | UPDATE/DELETE, `FOR UPDATE` | 다른 트랜잭션의 **쓰기 대기** |
| **MVCC 스냅샷 읽기** | (잠그지 않음) | 일반 SELECT | 락 없이 **수정 전 버전** 읽기 |
| **갭 락 (Gap Lock)** | 행 사이 **범위** | 범위 조건 락 읽기 (REPEATABLE READ) | 범위 내 **INSERT 차단** = 팬텀 방지 |

> * 주의 1 : 일반 `SELECT` ≠ `SELECT ... FOR UPDATE` → 일반 SELECT는 MVCC. 즉, undo log를 통한 이전 버전이지만 `FOR UPDATE / LOCK IN SHARE MODE`은 락을 통한 읽기이다.
> * 주의 2 : 갭 락은 격리 수준에 의존한다. → InnoDB 기본 REPEATABLE READ에서 갭 락이 동작한다. 그러나 READ COMMITTED로 낮추면 갭 락이 사라져 INSERT가 막히지 않지만 대신 PHANTOM READ가 발생한다.
> * 주의 3 : 락 = 대기, 대기 = 데드락·성능 이슈의 씨앗이다. FOR UPDATE 범위를 넓게 잡거나 트랜잭션을 길게 열면 다른 트랜잭션이 줄줄이 대기하면서 처리량이 저하되는 결과로 이어진다. 즉, 트랜잭션은 짧게, 락 범위는 좁게 가져가야 한다. 
