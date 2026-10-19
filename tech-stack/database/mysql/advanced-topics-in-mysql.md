# MySQL - Advanced Topics in MySQL

#### 데이터베이스 트랜잭션 상태 변화 흐름

**1. 트랜잭션 시작 (START TRANSACTION)**

* 데이터베이스 작업 단위인 트랜잭션이 처음 시작되는 단계이다.

**2. 활성 상태 (Active)**

* 실제로 SQL 쿼리문(INSERT, UPDATE, DELETE 등)이 실행 중인 상태이다.
* 데이터 변경은 기본적으로 메모리(Buffer Pool) 상에서 처리된다.

**3. 부분 완료 상태 (Partially Committed)**

* 모든 SQL 작업이 끝나고 COMMIT 명령어가 입력된 상태이다.
* 논리적인 연산은 모두 성공적으로 끝났지만, 그 결과가 아직 물리적인 디스크(데이터베이스)에 영구적으로 기록되지는 않은 상태이다.

**4. 디스크 반영 시도 및 결과**

* 부분 완료 상태에서 실제 디스크에 변경 사항을 저장(반영)하려는 시도를 하며, 그 결과에 따라 다음 두 가지 상태 중 하나로 전환한다.
* 성공 시 ➔ 완료 (Committed)
  * 디스크에 데이터를 기록하는 데 성공한 상태이다. (InnoDB 기준 이 때의 "디스크 기록"은 데이터 파일이 아니라 Redo Log의 기록이다.
  * 트랜잭션이 성공적으로 종료되며, 변경 사항이 영구적으로 적용된다.
* 실패 시 ➔ 실패 및 철회 (Failed ➔ Aborted)
  * Failed : 시스템 오류 등으로 인해 디스크 반영에 실패하거나 트랜잭션을 정상적으로 진행할 수 없는 상태이다.
  * Aborted : 실패(Failed) 상태가 확인되어, 트랜잭션에서 수행했던 모든 작업을 취소(ROLLBACK)하고 트랜잭션 시작 이전 상태로 되돌려진(철회된) 상태이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/056326c2-0329-471c-b4f5-c3e6e4f39abf.png)

#### 트랜잭션 활성(Active) 상태 분석

```sql
START TRANSACTION;

INSERT INTO users (name, email) VALUES ('John', 'john@example.com');
UPDATE accounts SET balance = balance - 100 WHERE user_id = 1;
```

* 활성 상태는 `START TRANSACTION` 이후 실제 SQL 쿼리들이 실행되고 있는 단계로, 데이터 변경이 기본적으로 메모리 상에서 이루어지는 것이 핵심이다.
* 이 단계에서는 트랜잭션의 안전성과 시스템 복구 능력을 보장하기 위해 데이터베이스의 주요 메모리 및 로그 영역이 다음과 같이 작동한다.
* `Buffer Pool(버퍼 풀 / 메모리)` : 실제 데이터 변경 작업이 일어나는 메인 작업 공간이다. 변경된 데이터는 우선 메모리(Buffer Pool) 상에 반영된다.
* `Undo Log(언두 로그)` : 데이터가 변경되기 전의 원본 상태를 기록한다. 작업 중 에러가 발생하거나 ROLLBACK 명령이 내려졌을 때, 데이터를 수정 이전 상태로 안전하게 되돌리기(취소하기) 위해 사용한다.
* `Redo Log Buffer(리두 로그 버퍼)` : 어떤 데이터가 어떻게 변경되었는지 그 변경 내역을 순차적으로 기록한다. 아직 디스크에 저장되지 않은 상태에서 시스템 장애(정전 등)가 발생했을 때, 잃어버린 작업을 재실행(Redo)하여 데이터를 복구하기 위해 사용한다.
* `Disk(물리적 하드 디스크)` : 일반적으로는 이 단계에서 데이터 파일에 변경이 쓰이지 않는다. 단, "커밋 전에는 디스크에 절대 안 쓰인다"는 단순화된 설명이다. 버퍼 풀 공간이 부족하면 InnoDB는 **커밋 전이라도 더티 페이지를 디스크에 플러시할 수 있다.** 즉 커밋 여부와 데이터 파일 반영 여부는 서로 독립적이며, 바로 이 때문에 크래시 복구에 "미커밋 변경을 되돌리는" Roll-backward(Undo) 단계가 반드시 필요한 것이다.
 
#### 트랜잭션 부분 완료(Partially Committed) 상태 분석 - 디스크 반영(Commit) 처리 흐름

* `COMMIT` 명령이 떨어지면 InnoDB 엔진 내부에서는 다음과 같은 작업이 순차적으로 일어난다.
  * `COMMIT` 명령 수신 : 트랜잭션을 끝내고 데이터를 저장하라는 신호를 받는다.
  * `플러시(Flush)` : 메모리(Redo Log Buffer)에 모아둔 변경 내역을 디스크의 파일(Redo Log File)로 보낸다.
  * `fsync()` 호출 : 버퍼에 들고 있지 말고 지금 당장 물리적인 하드 디스크에 확실히 저장하라는 강제 동기화 명령을 보낸다.
  * 상태 결정 : 위 과정이 성공적으로 끝나면 ➔ 완료(Committed) 상태가 된다. 단, 디스크 용량 부족이나 하드웨어 장애로 실패하면 ➔ 실패(Failed) 상태가 된다.
* 바이너리 로그가 켜져 있다면(복제 환경의 기본) 커밋은 **내부 2단계 커밋(Internal XA)** 으로 처리된다. Redo Log에 prepare 기록 → Binlog 기록 → Redo Log에 commit 기록. 이렇게 해야 크래시가 나도 Redo Log와 Binlog 사이의 일관성(복제본과 원본의 일치)이 보장된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/83a98ef2-2a6a-4cf7-9a4a-7e0324fb000d.png)

#### innodb_flush_log_at_trx_commit

* `innodb_flush_log_at_trx_commit` 옵션의 설정값에 따라 데이터의 안정성(ACID 보장)과 처리 성능이 크게 달라진다.

|설정값|동작 방식|데이터 안정성(장애 발생시)|성능|
|:---:|:---:|:---:|:---:|
|1(기본값)|매 Commit마다 Flush + `fsync()` 실행|완벽 보장 - 어떤 장애가 발생해도 커밋된 데이터가 유실되지 않음(ACID 원칙 준수)|가장 느림(디스크 I/O 성능 병목 발생)|
|0|Commit 시점에는 아무것도 하지 않고, 1초마다 주기적으로 Flush + `fsync()` 실행|최대 1초치 데이터 유실 위험(DB 프로세스만 비정상 종료돼도 유실)|가장 빠름|
|2|매 Commit마다 OS 캐시까지 write, `fsync()`는 1초마다 실행|OS 크래시 시 유실 위험(DB 프로세스가 죽어도 데이터는 OS 캐시에 있어 안전하지만, 서버 전원이나 OS 자체가 죽으면 최대 1초치 유실)|중간|

* 대부분의 중요한 서비스(결제, 회원 정보 등등)에서는 성능을 조금 희생하더라도 데이터의 절대적인 안전을 위해 기본값인 1을 사용한다.
* 반면 로그 수집이나 단순 통계처럼 약간의 데이터 유실이 치명적이지 않으면서 쓰기 성능이 극도로 중요할 때는 0이나 2를 고려할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f597137e-c65d-484f-9b95-d6b7637c2543.png)

#### 완료(Committed) 상태 분석

**1. 트랜잭션 최종 완료 흐름**

* Redo Log 디스크 기록 완료 : 변경 내역이 디스크의 안전한 영역인 Redo Log File에 쓰인다.
* 클라이언트에 "OK" 응답 : 데이터베이스가 애플리케이션(사용자)에게 작업이 성공적으로 끝났음을 알린다.
* 백그라운드 동기화 (Checkpoint) : 사용자는 이미 다음 작업을 하고 있는 동안, 데이터베이스는 내부적으로 바뀐 데이터를 실제 데이터 파일에 반영한다.

**2. 핵심 개념: 성능을 위한 지연 쓰기(Lazy Writing)**

* 매번 트랜잭션이 끝날 때마다 거대한 실제 데이터 파일을 찾아가서 수정하는 것은 매우 느리고 무거운 작업이다. (데이터 파일은 랜덤 I/O, Redo Log는 순차 I/O - WAL의 본질은 "느린 랜덤 쓰기를 빠른 순차 쓰기로 바꿔치기"하는 것이다)
* InnoDB는 다음과 같은 전략을 사용한다.
  * 보장의 근거는 Redo Log : 시스템이 재부팅될 때 이 로그를 읽어와서 데이터를 완벽히 복구(Redo)한다.
  * 더티 페이지(Dirty Page) : 메모리(Buffer Pool) 상에서는 최신 데이터로 변경되었지만, 아직 디스크의 실제 데이터 파일에는 반영되지 않은 상태의 메모리 공간
  * 체크포인트(Checkpoint) : 데이터베이스가 비교적 한가할 때(백그라운드 프로세스), 이 더티 페이지들을 모아서 한 번에 실제 디스크의 데이터 파일에 반영하는 작업이다. 이를 통해 디스크 I/O를 최소화하여 성능을 끌어올린다.

#### 실패 및 철회(Failed ➔ Aborted) 상태 분석

**1. 롤백(Rollback) 처리 흐름**

* 오류 감지 / ROLLBACK 명령 : 작업 중 문제가 생기거나 취소 명령이 떨어진다. 이 시점을 Failed 상태로 본다.
* Undo Log 역순 탐색 : 데이터베이스는 안전하게 백업해둔 Undo Log(언두 로그)를 가장 최근 기록부터 거꾸로 읽어 들인다.
* 변경 사항 되돌리기 : 메모리(Buffer Pool)에 수정해두었던 데이터들을 Undo Log에 적힌 원본 데이터로 덮어씌워 복원한다.
* 완전 취소(Aborted) : 데이터가 트랜잭션 시작 이전으로 완벽히 돌아가면, 사용했던 Undo Log를 정리하고 트랜잭션을 완전히 폐기한다.

**2. 핵심 개념: 트랜잭션의 두 가지 타임머신**

* `Redo Log(리두 로그)` : 해당 로그는 수행했던 작업을 다시 실행(Redo)하여 복구하는데 쓰인다.
* `Undo Log(언두 로그)` : 작업 중 문제가 생겼을 때, 과거 상태로 돌아가는데 쓰인다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d88ccf5a-98c7-4dfd-b628-67351c20eef1.png)

#### 자동 커밋(Autocommit = 1) 동작

* 단일 SQL 쿼리를 실행할 때마다 데이터베이스가 시작과 끝을 관리해 귀찮은 수작업 없이도 데이터를 즉시 안전하게 저장해준다.

#### 수동 커밋(Autocommit = 0) 동작 분석

* 무한 대기 및 스냅샷 유지
  * 명시적인 `COMMIT`이나 `ROLLBACK`을 하지 않으면, 트랜잭션은 영원히 끝나지 않는다.
  * 데이터베이스는 그 오래된 'Read View'를 유지하기 위해 과거 데이터(Undo Log)를 지우지 못하고 계속 쌓아두어야 하며, 이는 DB 성능 저하와 용량 부족으로 직결된다.
  * 장시간 트랜잭션은 온라인 DDL의 메타데이터 락(MDL) 대기를 유발해 그 뒤의 모든 쿼리를 연쇄적으로 멈추게 하는 원인이기도 하다.
* 커넥션 끊김 = 자동 롤백 (Auto Rollback)
  * COMMIT을 깜빡하고 데이터베이스와의 연결(Connection)을 끊어버리면, 데이터베이스는 비정상 종료로 간주하고 지금까지 했던 모든 작업을 자동으로 ROLLBACK(취소) 처리한다.

#### 명시적 트랜잭션 vs 암묵적 트랜잭션 핵심 비교

**1. 명시적 트랜잭션 (Explicit Transaction)**

* 개발자가 직접 트랜잭션의 시작과 끝을 선언(명시)하여 여러 작업을 하나의 묶음으로 통제하는 방식이다.
* 동작 흐름
  * `START TRANSACTION(또는 BEGIN)` 명령어로 시작을 알린다.
  * 첫 번째 DML(INSERT, UPDATE 등)이 실행되는 순간, 데이터베이스 내부적으로 고유한 '트랜잭션 ID'가 부여된다.(읽기만 하는 트랜잭션에는 ID를 부여하지 않는 지연 할당 최적화)
  * 이후 실행되는 모든 쿼리들은 이 트랜잭션 ID 아래 하나의 원자 단위(Atomic Unit)로 강력하게 묶인다.
  * 모든 작업이 정상적으로 끝나면 `COMMIT`으로 저장하고, 하나라도 실패하면 `ROLLBACK`으로 전체 취소하여 명시적으로 종료된다.

**2. 암묵적 트랜잭션 (Implicit Transaction)**

* 개발자가 따로 선언하지 않아도, 데이터베이스(기본적으로 autocommit = 1 상태)가 알아서 개별 SQL마다 트랜잭션을 열고 닫아주는 방식이다.
* 동작 흐름
  * `UPDATE users ...` 와 같이 단일 SQL 쿼리를 실행한다.
  * 쿼리가 시작될 때 DB가 암묵적으로 트랜잭션을 연다.
  * 쿼리 하나가 에러 없이 끝나는 즉시, DB가 자동으로 COMMIT을 실행한다.

#### DDL의 암묵적 커밋 (Implicit Commit) 분석

* DDL은 실행 **직전과 직후에** 암묵적 커밋을 발생시킨다. 진행 중이던 트랜잭션이 있었다면 DDL이 실행되는 순간 그 트랜잭션까지 조용히 커밋되어 버린다. `ROLLBACK`할 기회 자체가 사라진다.
  * 절대 섞어 쓰지 말 것 : 트랜잭션(START TRANSACTION ~ COMMIT/ROLLBACK) 내부에는 오직 데이터를 조작하는 DML(SELECT, INSERT, UPDATE, DELETE)만 넣어야 한다.
  * DDL은 트랜잭션 밖에서 : 테이블을 생성하거나 구조를 변경하는 DDL 작업은 반드시 트랜잭션과 철저히 분리하여 독립적으로 실행해야 예상치 못한 데이터 강제 저장을 막을 수 있다.
* MySQL 8.0부터 DDL 자체는 원자적(Atomic DDL)이 되었다. DDL 도중 크래시가 나도 "반쯤 만들어진 테이블"은 남지 않는다. 하지만 **암묵적 커밋은 여전히 발생**하므로 트랜잭션과 섞으면 안 된다는 원칙은 그대로다.

#### MySQL Binlog(바이너리 로그) 분석

* Binlog는 데이터베이스에 발생한 모든 변경 사항(INSERT, UPDATE, DELETE, DDL 등)을 시간 순서대로 기록한다.
* Redo Log와의 구분 : `Redo Log`는 InnoDB **스토리지 엔진 계층**의 크래시 복구용 로그이고, `Binlog`는 MySQL **서버 계층**의 복제·복구용 논리 로그다. 목적과 계층이 다르며, 커밋 시 둘의 일관성을 위해 앞서 설명한 내부 2단계 커밋이 사용된다.
* 복제(Replication) : 메인 DB(Source/Master)가 자신이 한 일을 `Binlog`에 적어두면, 보조 DB(Replica/Slave)들이 이 로그를 가져가서 자기 자신에게 똑같이 따라 실행한다. 이를 통해 여러 대의 DB가 완벽히 똑같은 데이터를 유지하게 된다.
* 특정 시점 복구(PITR: Point-in-Time Recovery) : 개발자가 실수로 WHERE 절 없이 DELETE를 날렸을 때, 어제 새벽에 받아둔 '풀 백업본'을 복원한 뒤, 백업 이후부터 사고 발생 직전까지의 Binlog 기록을 쭉 재생(Replay)하여 데이터를 완벽히 복원할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f39d3e05-796d-42cd-a5de-08da04beeabe.png)

|기록 방식|동작 원리|장점|치명적 단점 및 주의사항|
|:---:|:---:|:---:|:---:|
|SBR|작성한 SQL 문장 자체를 그대로 복붙|로그에 텍스트 한 줄만 적히므로 파일 용량이 매우 작고 가볍다.|데이터 불일치 대참사 위험 - 비결정적 함수(NOW(), UUID(), LIMIT without ORDER BY 등)가 실행되는 시점에 따라 결과값이 달라져 데이터가 틀어진다.|
|RBR|변경된 실제 데이터의 Before/After 값을 기록|SQL이 아닌 결과값 자체를 복사하기에 가장 정확하고 안전한 데이터 복제가 보장된다.|로그 용량 폭발 - N건을 한 번에 UPDATE하면 로그 파일에도 N건의 변경 내역이 기록되어 디스크와 네트워크에 큰 부담을 준다.|
|MIXED|평소엔 SBR로 가볍게 적다가 위험한 비결정적 함수를 만나면 알아서 RBR로 변경해 기록한다.|데이터 안정성과 용량 절약을 타협한다.|언제 어떤 방식으로 기록되었는지 예측하기 어려워 시스템 장애 시 원인 파악하기가 매우 어렵다.|

* MySQL 5.7.7부터 **기본 포맷은 RBR(`binlog_format=ROW`)** 이다. RBR의 용량 문제는 `binlog_row_image=MINIMAL`(변경된 컬럼과 식별 컬럼만 기록)로 완화할 수 있다.
* CDC(Change Data Capture) 도구(Debezium 등)가 구독하는 것이 바로 이 RBR 형식의 Binlog다. Before/After 값이 그대로 담겨 있어 이벤트 스트림으로 변환하기에 적합하다.

#### MySQL Lock Deep Dive(Intention Lock, Gap Lock & Granularity Levels)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b25546c9-430b-42ee-8fd6-64498fa734b3.png)

**1. S-lock과 X-lock(가장 기본이 되는 '행(Row)' 잠금)**

* S-lock (Shared Lock / 공유 락) : 데이터를 안전하게 읽기 위해 사용한다.
  * `SELECT ... FOR SHARE` (MySQL 8.0 이전 문법은 `LOCK IN SHARE MODE`)
  * S-lock을 걸고 읽는 동안, 다른 트랜잭션도 S-lock을 걸고 같이 읽을 수 있다.
  * 하지만 누군가 이 데이터를 수정(X-lock)하려 한다면 읽기를 마칠 때까지 대기해야 한다.
* X-lock (Exclusive Lock / 배타 락) : 데이터를 안전하게 쓰기(수정/삭제)위해 사용한다.
  * `SELECT ... FOR UPDATE` (UPDATE/DELETE는 자동으로 X-lock을 잡는다)
  * X-lock을 쥐고 있으면 다른 사람은 S-lock, X-lock 둘 다 걸 수 없다.

**2. IS / IX Lock(테이블에 거는 '의사 표시' 잠금)**

* 특정 행에 X-lock을 걸려면 엔진은 먼저 테이블 전체에 IX-lock(의도 배타 락)을 걸고, 그 다음에 해당 행에 X-lock을 건다. (S-lock이면 IS-lock)
* 존재 이유(성능 최적화) : `LOCK TABLES`나 일부 DDL처럼 **테이블 전체 락**이 필요한 작업은 "이 테이블 어딘가에 행 락이 있는가?"를 알아야 한다. 인텐션 락이 없다면 수백만 행을 전수 검사해야 하지만, 테이블 헤더의 IS/IX 표시 하나만 보고 즉시 판단할 수 있다.
* 호환성 : IS/IX끼리는 서로 충돌하지 않는다(행 수준에서 이미 조율되므로). 테이블 수준의 S/X 락과만 충돌한다.

**3. Next-Key Lock(범위 조회 시 걸리는 '광역' 잠금)**

* Next-Key Lock = **Record Lock(행 자체) + Gap Lock(행 사이의 빈 공간)** 의 조합이다.
* `WHERE id > 10 FOR UPDATE`처럼 범위를 검색하면, 존재하는 데이터(10, 20, 30)뿐만 아니라 그 사이의 빈 공간(Gap)까지 모조리 잠가버린다.
* 장점(Phantom Read 방지) : 10~30 사이의 데이터를 작업하고 있는데, 다른 측에서 `id=15`를 새로 끼워 넣는(INSERT) 작업을 완벽히 차단한다.
* 락 범위를 결정하는 조건들
  * 유니크 인덱스의 동등 조건(`WHERE id = 10`)은 Gap 없이 **Record Lock만** 잡는다. 가장 좁고 안전한 형태.
  * 인덱스가 없는 컬럼으로 잠금 검색을 하면 풀스캔 과정에서 **스캔한 모든 행과 갭이 잠긴다.** 사실상 테이블 전체가 멈춘다.
  * Gap Lock은 REPEATABLE READ 전용이다. READ COMMITTED에서는 (외래키 검사 등을 제외하면) Gap Lock을 사용하지 않아 동시성이 높아진다.
* 오버 락킹 : 예를 들어, 데이터 하나만 잠그려고 했는데 범위로 지정하게 되면 10 이상의 모든 공간이 잠겨버려서, 다른 트랜잭션의 정상적인 INSERT들이 전부 멈춰(대기 상태)버리는 치명적인 성능 저하가 발생할 수 있다. 그렇기 때문에 락을 항상 필요한 행만 정확하게 짚어서(가급적 유니크 인덱스 동등 조건으로) 걸어야 한다.

**4. Insert Intention Lock(INSERT 전용 틈새 잠금)**

* 새로운 데이터를 끼워 넣기 전에 그 빈 공간에 거는 락을 말한다.
* 공간을 완전히 잠그는 일반적인 Gap Lock과 달리, 이 락은 위치만 다르다면 서로 방해하지 않는다.
* 즉, A가 15번을 넣고, 동시에 B가 20번을 넣어도 서로 목표 위치가 다르기 때문에 대기 없이 쾌적하게 동시에 데이터가 입력(INSERT)된다.
* 단, 다른 트랜잭션이 그 갭에 **Gap Lock을 이미 잡고 있다면 Insert Intention Lock은 대기**해야 한다. "두 트랜잭션이 같은 범위에 Gap Lock(서로 호환)을 잡은 채 서로의 갭에 INSERT를 시도"하는 것이 InnoDB 데드락의 가장 흔한 시나리오다.

#### MySQL InnoDB Internals(Crash Recovery, Redo Log & MVCC)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/65f670c5-1751-4303-b1cb-8e5160176e81.png)

**1. Undo Log (언두 로그): 롤백과 동시성 제어의 핵심**

* 데이터가 변경되기 전의 원본 상태(과거 데이터)를 저장한다.
* ROLLBACK(작업 취소) : 트랜잭션 실행 중 오류가 발생하거나 명시적으로 롤백할 때, 언두 로그에 기록된 이전 값을 가져와 덮어씌움으로써 원래 상태로 복구한다.
* MVCC(다중 버전 동시성 제어) : 다른 트랜잭션이 아직 커밋되지 않은 데이터를 수정하고 있을 때, 또 다른 트랜잭션이 조회를 요청하면 언두 로그에 있는 수정 전 원본 데이터를 보여준다. 이를 통해 락(Lock)을 걸지 않고도 대기 없이 안전한 읽기가 가능하다.
* `SHOW ENGINE INNODB STATUS`를 통해 확인할 수 있는 HLL(History List Length)은 **아직 Purge되지 않은 언두 로그의 개수(길이)** 를 말한다. 이 수치가 계속 높아지면(주범은 대부분 장시간 미종료 트랜잭션) 성능 문제가 발생할 수 있으며, 불필요해진 로그는 백그라운드 Purge 쓰레드가 알아서 정리한다.

**2. Redo Log & Crash Recovery: 데이터 영구 보존의 마법**

* 성능을 위해 메모리(버퍼 풀)에서만 수정된 데이터가 갑작스러운 정전 등으로 날아가는 것을 막기 위해, 변경 내역을 디스크에 빠르게 순차 기록하는 WAL(Write-Ahead Logging) 공간이다.
* Roll-forward(전진) : 트랜잭션이 성공적으로 COMMIT 되었으나 실제 데이터 파일에는 미처 반영되지 못하고 죽은 경우, 리두 로그를 보고 그대로 다시 실행하여 복원한다.
* Roll-backward(후진) : COMMIT 되지 않은 상태로 죽은 트랜잭션의 찌꺼기는 언두 로그를 활용해 깔끔하게 취소(롤백)한다. (커밋 전에도 더티 페이지가 디스크에 플러시될 수 있기 때문에 이 단계가 반드시 필요하다)
* 복구 순서 : 크래시 후 재시작하면 항상 **Roll-forward 먼저, Roll-backward 나중** 이다 - Redo로 크래시 직전 상태를 완전히 재현한 뒤, 그 위에서 미커밋 트랜잭션들을 골라 되돌린다.

**3. MVCC와 Read View: 과거 데이터를 찾아가는 원리**

* 하나의 데이터 행(Row)에 대해 여러 버전을 관리하여, 읽기 작업과 쓰기 작업이 서로 간섭하지 않게 만드는 원리이다.
* 숨겨진 메타 데이터 : InnoDB의 모든 테이블 행에는 사용자 눈에 보이지 않는 중요한 식별자가 붙어 있다.
  * DB_TRX_ID : 이 행을 마지막으로 수정한 트랜잭션의 고유 번호
  * DB_ROLL_PTR : 수정되기 전 과거 데이터가 저장된 언두 로그 주소
* Read View : 특정 트랜잭션이 조회를 시작할 때, 현재 실행 중인 트랜잭션들의 ID 목록을 바탕으로 기준점을 만든다.
* 가시성 판정 : 행의 DB_TRX_ID가 "내 Read View 생성 당시 아직 활성이던 트랜잭션"이거나 "그 이후에 시작된 트랜잭션"이라면 그 버전은 보이지 않는다 → DB_ROLL_PTR을 따라 언두 로그의 과거 버전으로 거슬러 올라가며, 보이는 버전을 찾을 때까지 반복한다.

#### Transaction Isolation Levels(Read Phenomena, MVCC & Snapshot)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f16aefb4-8e3a-4df7-8bef-c34eee5f165b.png)

* MySQL(InnoDB)의 기본 격리 수준은 **REPEATABLE READ**다.

**Lv 1. READ UNCOMMITTED(커밋되지 않은 읽기)**

* MVCC(다중 버전 제어)를 무시하고, 메모리(Buffer Pool)에 있는 최신 데이터를 무작정 읽는다.
* 성능은 가장 빠르지만, Dirty Read가 발생하는 치명적인 문제가 있다. 남이 취소(Rollback)할지도 모르는 가짜 데이터를 기반으로 비즈니스 로직이 돌아가면 시스템이 망가질 수 있어 실무에서는 절대 쓰지 않는다.

**Lv 2. READ COMMITTED(커밋된 읽기)**

* 쿼리(SELECT)를 실행할 때마다 새로운 Read View(스냅샷)를 생성한다.
* 남이 커밋하기 전에는 Undo Log의 과거 데이터를 읽는다.
* Dirty Read는 막아준다. 하지만 트랜잭션 도중에 남이 커밋을 해버리면 다음 SELECT 때 그 변경된 값을 읽어버리므로 Non-Repeatable Read와 Phantom Read가 발생한다.
* 대신 Gap Lock을 거의 사용하지 않으므로 동시 쓰기 처리량이 유리하다. 대량 쓰기 워크로드에서 의도적으로 RC로 낮추는 경우도 있다.

**Lv 3. REPEATABLE READ(반복 가능한 읽기)**

* 트랜잭션이 시작되는 순간(정확히는 첫 SELECT)에 Read View를 딱 한 번만 생성하고, 트랜잭션이 끝날 때까지 그 기준만 잡고 있다.
* 남이 중간에 커밋을 하든 말든 나는 내 Undo Log를 통해 과거 버전만 일관되게 읽는다.
* 자신이 두 번 읽는 동안 값이 변하지 않기 때문에 Non-Repeatable Read가 방지된다.
* 표준 SQL의 REPEATABLE READ는 Phantom Read를 허용하지만, **InnoDB의 RR은 대부분의 팬텀도 함께 방지한다.** 일반 SELECT는 고정된 스냅샷(MVCC)으로, 잠금 읽기(`FOR UPDATE` 등)는 Next-Key Lock으로 막는다. "MySQL RR ≠ 표준 RR"은 격리 수준 관련 단골 면접 주제다.
  * 예외 : 스냅샷 읽기와 현재 읽기(current read)를 한 트랜잭션에서 섞으면 팬텀 유사 현상이 나타날 수 있다. 예를 들어 SELECT(스냅샷)로 안 보이던 행이, 내가 UPDATE를 실행한 뒤(현재 읽기 발생) 다시 SELECT하면 보이는 식이다.

**Lv 4. SERIALIZABLE(직렬화)**

* 단순한 조회(SELECT)조차도 내부적으로 읽기 잠금(`FOR SHARE`, S-lock)을 걸어버린다.
* 데이터를 읽고 있으면(S-lock 사용) 절대 수정(X-lock)을 할 수 없다. 모든 이상 현상을 완벽하게 방지하지만, 동시 처리 능력이 바닥으로 떨어져 실무에서 거의 사용하지 않는다.

#### MySQL Query Optimizer(Query Rewriting, Cost Model & Histogram)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2b0e0ed7-f23b-4229-a1c1-b849302d79a1.png)

**1. 쿼리 재작성(Query Rewriting): 옵티마이저의 마법**

* 개발자가 작성한 SQL을 그대로 실행하지 않고, 데이터베이스가 스스로 더 빠르게 처리할 수 있는 형태로 내부적으로 쿼리를 뜯어고치는 과정이다.
* 서브쿼리 최적화 : 무거운 서브쿼리(IN)를 효율적인 세미 조인(Semi Join) 형태로 변환하여 성능을 끌어올린다.
* 뷰 병합(View Merging) : 뷰(View)를 조회할 때, 뷰의 정의를 메인 쿼리에 그대로 병합하여 한 번에 처리한다.(단, GROUP BY, DISTINCT, LIMIT, 집계 함수 등이 있으면 병합하지 못하고 임시 테이블(TEMPTABLE)을 만든다.)
* 상수 전파(Constant Propagation) : A = B이고 A = 100이면, 옵티마이저가 알아서 B = 100이라는 조건을 추가해 인덱스를 더 잘 타게 만들어준다.
* 조건 단순화 : a > 5 AND a > 3 같은 조건은 굳이 두 번 검사할 필요 없이 a > 5로 줄여버린다.
* 외부 조인 제거 : LEFT JOIN을 썼더라도 WHERE 절에서 조인된 테이블의 특정 값을 강제로 필터링한다면(NULL이 통과할 수 없는 조건), 굳이 외부 조인을 할 필요가 없으므로 더 빠른 INNER JOIN으로 바꿔버린다.

```sql
-- 1. 기본 실행 계획 확인 (가장 많이 씀)
EXPLAIN 
SELECT * FROM orders WHERE amount > 100000;

-- 2. 💡 옵티마이저가 '재작성(Rewriting)'한 실제 쿼리 확인
-- EXPLAIN을 실행한 직후에 아래 쿼리를 실행하면, DB가 내부적으로 뜯어고친 최종 SQL 문장을 보여준다.
SHOW WARNINGS;

-- 3. 비용(Cost) 정보가 포함된 상세 실행 계획 보기 (MySQL 5.6+)
-- 옵티마이저가 이 경로를 선택했을 때 계산한 정확한 비용(cost)을 JSON 형태로 볼 수 있다.
EXPLAIN FORMAT=JSON 
SELECT o.*, c.name 
FROM orders o JOIN customers c ON o.customer_id = c.id;

-- 4. 쿼리 실행 흐름을 트리(Tree) 형태로 보기 (MySQL 8.0.16+)
-- 조인 순서나 실행 단계를 시각적으로 가장 직관적으로 보여준다.
EXPLAIN FORMAT=TREE 
SELECT * FROM orders WHERE customer_id IN (SELECT id FROM customers WHERE country='KR');
```

**2. 비용 기반 최적화 (CBO: Cost-Based Optimization)**

* 판단의 기준(통계 정보)
  * 옵티마이저는 테이블의 전체 행 수, 인덱스의 카디널리티(고유한 값의 개수), 디스크 페이지 수 등의 통계(innodb_table_stats, innodb_index_stats)를 기반으로 비용을 계산한다.
  * 이 통계는 전체를 다 세어보는 것이 아니라 일부(기본 20개 페이지, `innodb_stats_persistent_sample_pages`)만 샘플링하여 추정하는 것이다.
* 비용 계산 공식(Cost Model)
  * 단순히 데이터 개수만 보는 것이 아니라 CPU 연산 비용(server_cost)과 디스크/메모리 I/O 비용(engine_cost)을 상수로 정해두고, 이를 곱하여 최종 실행 비용을 산출한다.
* 수동 갱신(ANALYZE TABLE)
  * 대량의 데이터가 갑자기 추가되거나 삭제되면 DB의 통계가 옛날 정보에 머물러 있어 옵티마이저가 잘못된 선택을 할 수 있다.
  * 이 때, `ANALYZE TABLE`을 실행해 최신 상태로 갱신해 주어야 한다.

```sql
-- 1. 테이블 통계 정보 최신화 (가장 중요)
-- 대량의 INSERT/UPDATE/DELETE 이후에는 옵티마이저가 잘못된 계획을 세우지 않도록 반드시 실행한다.
ANALYZE TABLE orders;

-- 2. 현재 테이블의 통계 정보 확인 (행 개수, 클러스터링 인덱스 크기 등)
SELECT * FROM mysql.innodb_table_stats 
WHERE database_name = 'mydb' AND table_name = 'orders';

-- 3. 현재 인덱스의 통계 정보 확인 (카디널리티, 페이지 수 등)
SELECT * FROM mysql.innodb_index_stats 
WHERE database_name = 'mydb' AND table_name = 'orders';

-- 4. MySQL이 내부적으로 사용하는 '작업 단가표(비용 모델)' 확인
SELECT * FROM mysql.server_cost;  -- CPU 연산, 메모리 관련 비용
SELECT * FROM mysql.engine_cost;  -- 스토리지 엔진(디스크 I/O) 관련 비용
```

**3. 히스토그램 (Histogram): 데이터 분포의 해상도 높이기 (MySQL 8.0+)**

* 인덱스가 없는 컬럼이거나, 데이터가 한쪽으로 극단적으로 쏠려 있을 때(예: 구매 금액 10만 원 이상 데이터 조회) 행 개수를 매우 정확하게 예측하기 위해 사용하는 고급 통계 기법이다.
* 기존의 단순 샘플링 통계만으로는 `amount > 100000`인 데이터가 50만 건인지 5만 건인지 정확히 알기 어렵다. 그래서 히스토그램을 생성(`UPDATE HISTOGRAM ON ...`)하면 이 오차를 획기적으로 줄여준다.
* 인덱스와 달리 쓰기 비용이 전혀 없다(쿼리마다 갱신되지 않으므로). 대신 **자동으로 갱신되지 않으므로** 데이터 분포가 크게 바뀌면 다시 생성해 주어야 한다. (MySQL 8.4부터는 `AUTO UPDATE` 옵션으로 자동 갱신 지정 가능)

```sql
-- 1. 단일 컬럼에 히스토그램 생성 (예: 구매 금액 분포)
ANALYZE TABLE orders UPDATE HISTOGRAM ON amount;

-- 2. 여러 컬럼에 한 번에 히스토그램 생성
ANALYZE TABLE orders UPDATE HISTOGRAM ON amount, status;

-- 3. 생성된 히스토그램 정보 확인 (JSON 형태로 저장되어 있음)
SELECT TABLE_NAME, COLUMN_NAME, HISTOGRAM 
FROM information_schema.COLUMN_STATISTICS 
WHERE TABLE_NAME = 'orders';

-- 4. 히스토그램 삭제 (더 이상 필요 없거나 통계가 꼬였을 때)
ANALYZE TABLE orders DROP HISTOGRAM ON amount;
```

#### MySQL Performance(Filesort, Temporary Tables & Partitioning)

**1. 정렬의 비용: Filesort (파일소트)**

* `ORDER BY`를 실행할 때 인덱스를 타지 못하면, MySQL은 데이터를 메모리(Sort Buffer)나 디스크로 가져와서 강제로 정렬한다.
* 이를 FileSort라고 부르며, 쿼리 성능 저하의 가장 큰 원인 중 하나이다.
* 동작 방식(Modified/Single-pass Filesort) : MySQL은 정렬 기준이 되는 키뿐만 아니라 SELECT로 조회하는 모든 컬럼을 통째로 메모리 버퍼에 올려서 한 번에 정렬한다. 메모리는 많이 쓰지만 랜덤 I/O를 줄여서 훨씬 빠르다.
* 디스크 병합 정렬 : 정렬해야 할 데이터가 메모리(Sort Buffer) 크기를 초과하면, 데이터를 쪼개서 임시 파일(디스크)에 썼다가 다시 병합(Multi-way merge)하므로 속도가 급격히 느려진다.
* 해결책 : `ORDER BY`에 사용되는 컬럼에 인덱스를 생성하는 것이 가장 확실한 방법이다.
* ⚠️ 단, 인덱스가 있다고 항상 인덱스 정렬을 쓰는 것은 아니다 - `SELECT *`로 **테이블 전체**를 정렬 조회하면, 보조 인덱스 순회는 행마다 클러스터형 인덱스를 다시 찾아가는 랜덤 I/O가 되므로 옵티마이저가 오히려 풀스캔 + filesort를 선택할 수 있다. 인덱스 정렬의 진가는 `LIMIT`과 결합해 "앞의 몇 건만" 읽고 끝낼 때 드러난다.

```sql
-- ❌ [나쁜 예] 인덱스 없는 정렬 (Extra: Using filesort)
EXPLAIN SELECT * FROM orders ORDER BY amount DESC LIMIT 10;

-- ✅ [좋은 예] 인덱스 생성 후 LIMIT과 결합 (Extra에 filesort 없음 = 인덱스 순서로 10건만 읽고 종료)
CREATE INDEX idx_amount ON orders(amount);
EXPLAIN SELECT * FROM orders ORDER BY amount DESC LIMIT 10;

-- 🔍 디스크를 사용한 느린 정렬이 몇 번 발생했는지 모니터링
SHOW STATUS LIKE 'Sort_merge_passes';
```

**2. 숨겨진 무거움: 임시 테이블 (Temporary Table)**

* `GROUP BY`, `DISTINCT`, `UNION` 등 복잡한 연산을 할 때, 메모리나 디스크에 중간 결과를 잠시 저장해두는 공간이다.
* 문제점 : 메모리에서 처리되면 다행이지만, 데이터가 너무 크면 결국 디스크에 써야 하므로 매우 무거워진다.
* 해결책 : UNION 대신 가급적 중복 제거 연산이 없는 UNION ALL을 사용하고, GROUP BY 대상 컬럼에 인덱스를 생성하여 임시 테이블 생성을 막아야 한다.

```sql
-- ❌ [나쁜 예] 임시 테이블 발생 (Extra: Using temporary)
EXPLAIN SELECT customer_id, COUNT(*) FROM orders GROUP BY customer_id;

-- ✅ [좋은 예] 인덱스를 통한 그룹화 (Extra: Using index)
CREATE INDEX idx_customer_id ON orders(customer_id);
EXPLAIN SELECT customer_id, COUNT(*) FROM orders GROUP BY customer_id;

-- 🔍 디스크에 생성된 무거운 임시 테이블 카운트 확인
SHOW STATUS LIKE 'Created_tmp_disk_tables';
```

**3. 인덱스 효율성: Loose vs Tight Index Scan**

* GROUP BY나 MIN/MAX를 사용할 때, 인덱스를 어떻게 읽느냐에 따라 성능 차이가 크게 난다.
* Tight Index Scan : 인덱스를 처음부터 끝까지 다 훑으면서 조건을 찾는다.(일반적인 GROUP BY, SUM, AVG 처리 시)
* Loose Index Scan(최강의 성능) : 인덱스가 이미 정렬되어 있다는 점을 이용해 각 그룹의 첫 번째 행(또는 마지막 행)만 찍어서 건너뛰며 읽는다.
* 발동 조건 : GROUP BY 컬럼이 인덱스의 선두 컬럼이고, 집계가 `MIN()`/`MAX()`(그것도 인덱스의 바로 다음 컬럼에 대한)이거나 DISTINCT일 때 주로 발동한다.

```sql
-- 🚀 [Loose Scan] 각 제품 그룹별 최소 금액 찾기 (매우 빠름)
-- ⚠️ 복합 인덱스 (product_id, amount)가 필요하다
--    - product_id만 인덱싱하면 amount 값이 인덱스에 없어 Loose Scan이 불가능하다
CREATE INDEX idx_product_amount ON sales(product_id, amount);
EXPLAIN SELECT product_id, MIN(amount) FROM sales GROUP BY product_id;
-- 결과: Extra 필드에 'Using index for group-by' 표시됨

-- 🐢 [Tight Scan] 평균은 건너뛸 수 없고 전부 다 더해봐야 알 수 있음
EXPLAIN SELECT product_id, AVG(amount) FROM sales GROUP BY product_id;
-- 결과: Extra 필드에 'Using index' (커버링이지만 처음부터 끝까지 다 읽음)
```

**4. 대용량 데이터 관리: 파티셔닝(Partitioning)과 프루닝(Pruning)**

* 수억 건의 데이터를 가진 거대한 테이블을 물리적으로 여러 개의 방(파티션)으로 쪼개어 관리하는 기술이다.
* 파티션 프루닝(Partition Pruning) : 쿼리를 호출할 때, 모든 데이터를 뒤지지 않고 데이터가 들어있는 특정 파티션만 정확히 찾아서 읽는 방식이다.
* 주의사항1 : WHERE절에서 파티션 키 컬럼을 함수로 가공하면 프루닝이 불가능해져 **모든 파티션을 스캔**하는 비효율적인 결과를 초래한다. 반드시 날짜 범위를 직접 지정해야 한다.
* 주의사항2 : 파티션 키는 테이블의 모든 유니크 키(PK 포함)에 포함되어야 하고, **파티션 테이블은 외래키를 가질 수도, 참조될 수도 없다.** 파티셔닝 도입 시점에 FK 전략을 함께 재검토해야 한다.
* 파티셔닝 전략 3가지
  * RANGE(범위) : 시계열 데이터(날짜/연도별 분리)에 최고. 오래된 데이터를 지울 때(DROP PARTITION) 단 1초면 된다.
  * HASH(해시) : 특정 컬럼 값(ex: 유저 ID)을 기준으로 데이터를 N개의 방에 아주 고르게 분산시킬 때 쓴다.
  * LIST(리스트) : 국가, 카테고리 등 명확하게 정해진 값들의 묶음 단위로 쪼갤 때 쓴다.

```sql
-- [RANGE 파티셔닝 테이블 생성 예시]
CREATE TABLE orders_partitioned (
    id INT NOT NULL AUTO_INCREMENT,
    order_date DATE NOT NULL,
    PRIMARY KEY (id, order_date) -- 파티션 키가 반드시 PK에 포함되어야 함
) PARTITION BY RANGE COLUMNS(order_date) (
    PARTITION p_2023 VALUES LESS THAN ('2024-01-01'),
    PARTITION p_2024 VALUES LESS THAN ('2025-01-01')
);

-- ❌ [프루닝 실패] 함수를 쓰면 모든 파티션을 다 뒤짐
EXPLAIN SELECT * FROM orders_partitioned WHERE YEAR(order_date) = 2024;

-- ✅ [프루닝 성공] 범위를 주면 p_2024 파티션 1개만 깔끔하게 뒤짐
EXPLAIN SELECT * FROM orders_partitioned 
WHERE order_date >= '2024-01-01' AND order_date < '2025-01-01';

-- 🗑️ 2023년 데이터 전체 삭제 (DELETE 쿼리보다 수백 배 빠름)
ALTER TABLE orders_partitioned DROP PARTITION p_2023;
```

#### MySQL B-Tree Index(Clustered vs Secondary, Page Format)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/28122f55-8372-4574-a27e-92dbe131caa7.png)

**1. B+ 트리 구조**

* InnoDB의 클러스터형 인덱스는 데이터를 효율적으로 검색하고 저장하기 위해 B+ 트리(B+ Tree) 자료구조를 사용한다.
* 루트 노드(Root Node) : 트리의 최상위 노드로, 검색의 시작점이다.
* 내부 노드(Internal Nodes) : 실제 데이터가 아닌, 기본키(PK)값과 하위 페이지를 가리키는 포인터만을 포함하여 탐색 경로를 제공한다.
* 리프 노드(Leaf Nodes) : 트리의 가장 하위 계층으로, 전체 행 데이터를 직접 포함하고 있다. 노드 간에 연결 리스트 형태로 연결되어 있어 순차(범위) 탐색에 유리하다.

**2. 리프 노드 내 실제 행의 구조**

* 레코드 헤더(Record Header) : 필드 개수(n_fields), 힙 번호(heap_no), 삭제 여부를 나타내는 플래그(delete_flag), 다음 레코드를 가리키는 포인터(next_ptr) 등 메타 데이터를 저장한다.
* 숨겨진 시스템 컬럼(Hidden System Columns)
  * DB_TRX_ID : 마지막으로 행을 삽입하거나 업데이트한 트랜잭션의 ID
  * DB_ROLL_PTR : 롤백 포인터로, Undo 로그에 있는 이전 버전의 데이터 레코드를 가리킨다.
  * DB_ROW_ID : 테이블에 명시적인 기본키(와 NOT NULL UNIQUE 키)가 없을 때만 생성되는 숨겨진 고유 ID
* 기본키 컬럼(Primary Key Columns) : 테이블의 기본키 데이터가 저장된다.
* 보조 컬럼 사용자 데이터(Secondary Columns User Data) : 기본키를 제외한 나머지 일반 컬럼들의 실제 데이터가 저장된다.

**3. 클러스터형 인덱스의 장점**

* 빠른 기본키 검색 : 기본키를 통한 검색 속도가 빠르며(O(log N)), 인덱스 탐색 후 리프 노드에서 행 데이터를 즉시 사용할 수 있다.
* 효율적인 범위 검색 : 데이터가 기본키 순서대로 물리적으로 정렬되어 저장되므로, 특정 범위의 데이터를 읽을 때 매우 효율적이다.
* 저장 공간 효율 : 인덱스와 데이터가 하나의 구조로 통합되어 있으므로, 별도의 데이터 저장 구조를 이중으로 유지할 필요가 없다.

**4. 클러스터형 인덱스의 단점**

* 단편화 문제 : 무작위 값이나 UUID를 기본키로 사용할 경우 데이터가 순차적으로 삽입되지 않아 잦은 페이지 분할(Page Splits)이 발생하고, 이로 인해 데이터 단편화(Fragmentation)가 일어난다. (AUTO_INCREMENT PK가 권장되는 이유)
* 보조 인덱스 크기 증가 : 보조 인덱스(Secondary Indexes)는 데이터의 물리적 주소 대신 기본키 값을 포인터로 저장한다. 따라서 기본키 크기가 크면 **모든** 보조 인덱스 크기도 함께 커진다.
* 행 이동 발생 : 기본키 값을 수정(UPDATE)하거나 중간에 삽입(INSERT)할 경우, 물리적인 정렬을 유지하기 위해 데이터 행의 물리적인 이동이 발생할 수 있어 오버헤드가 생긴다. (PK 수정은 내부적으로 DELETE + INSERT와 같다)

```sql
CREATE TABLE user (
    id INT PRIMARY KEY,               -- 클러스터형 인덱스 (기본키)
    username VARCHAR(50),             -- 보조 인덱스 생성 대상
    email VARCHAR(100),               -- 일반 컬럼
    created_at DATETIME,              -- 일반 컬럼
    INDEX idx_username (username)     -- 보조 인덱스 생성
);
```

**1. 보조 인덱스(Secondary Index)의 구조**

* B-Tree 구조 : 클러스터형 인덱스와 마찬가지로 루트, 내부 노드, 리프 노드로 구성된다.
* 리프 노드의 특징 : 보조 인덱스의 리프 노드에는 실제 행 데이터가 없고 인덱스로 지정된 키 값과 그 데이터가 실제 위치한 기본키(PK)값 쌍으로 저장된다.

**2. 일반적인 보조 인덱스 조회 흐름: 2단계 조회**

* 보조 인덱스 탐색 : 인덱스 트리에서 검색해 도달한 리프 노드에서 해당 데이터의 기본키 값을 획득한다.
* 클러스터형 인덱스 탐색 : 획득한 기본키를 가지고 다시 클러스터형 인덱스 트리를 탐색해서 리프 노드에 도달해서야 비로소 나머지 모든 컬럼이 포함된 실제 행 데이터를 조회할 수 있다.
* 이 2단계 조회 비용 때문에, 조건에 맞는 행이 너무 많으면(대략 전체의 20~25% 이상) 옵티마이저는 보조 인덱스를 버리고 풀스캔을 선택하기도 한다.

**3. 커버링 인덱스(Covering Index) 발생**

* 쿼리 결과를 만들기 위한 모든 데이터가 보조 인덱스 안에 커버링되어 있으면 굳이 무거운 클러스터형 인덱스까지 찾아갈 필요가 없다.
* 보조 인덱스만 읽고 조회를 끝내기 때문에 성능이 비약적으로 향상된다. (EXPLAIN의 Extra에 `Using index`로 표시)
* 아래 쿼리에서 보조 인덱스 리프에는 (username, id) 쌍이 이미 들어있으므로, SELECT 대상이 id와 username뿐이라면 클러스터형 인덱스 탐색이 생략된다.

```sql
SELECT id, username 
FROM user 
WHERE username = 'alice';
```

#### MySQL Buffer Pool(Page Management, LRU & Eviction Strategy)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/db06d185-0807-44ee-972b-731538480743.png)

**1. InnoDB Buffer Pool(메모리 캐시)**

* 데이터베이스 시스템의 성능을 좌우하는 가장 핵심적인 메모리 영역이다.
* 디스크에 있는 데이터 파일이나 인덱스 정보를 메모리에 캐싱하여 무거운 작업인 물리적 디스크 I/O를 최소화하는 역할을 한다.

**2. Buffer Pool Size**

* 버퍼 풀에 할당되는 메모리 크기이다.
* 데이터베이스 전용 서버의 경우 일반적으로 시스템 전체 RAM의 70~80% 수준으로 크게 설정해 캐시 히트율을 극대화한다.

**3. Chunk(innodb_buffer_pool_chunk_size)**

* 버퍼 풀 메모리를 관리하는 단위이다.
* Chunk 단위로 메모리를 동적 할당 및 해제할 수 있어 무중단으로 버퍼 풀 크기를 리사이징할 수 있다.

**4. Buffer Pool Instances(innodb_buffer_pool_instances)**

* 버퍼 풀을 여러 개의 독립적인 메모리 영역(인스턴스)으로 쪼갠 것이다.
* 멀티 쓰레드 환경에서 여러 커넥션이 단일 버퍼 풀의 자원을 동시에 수정하려고 하면 락 경합이 발생해 성능 저하로 이어질 수 있다.
* 이를 여러 인스턴스로 분리하여 동시성 처리 성능을 높이고 락 경합을 줄이는 역할을 한다.

**5. 내부 관리 리스트(Instance 하위 구조)**

* Free List : 데이터가 채워지지 않은, 즉시 사용할 수 있는 비어있는 페이지들의 목록이다. 쿼리가 새로운 데이터를 디스크에서 읽어와야 할 때 이 리스트에서 공간을 가져온다.
* LRU List : 현재 메모리에 캐싱되어 사용 중인 페이지들의 목록이다. 메모리 공간이 부족해질 경우, 가장 오랫동안 사용되지 않은 페이지를 식별해 메모리에서 밀어내는 역할을 한다.
* Flush List : 메모리에서 데이터가 변경되었지만 아직 물리적 디스크에는 동기화되지 않은 페이지들의 목록이다. 백그라운드 쓰레드가 이 리스트를 보고 주기적으로 디스크에 변경 사항을 기록한다.

**6. 변형 LRU : Midpoint Insertion 전략(풀스캔으로부터 캐시 지키기)**

* InnoDB의 LRU는 교과서적인 "맨 앞에 넣고 맨 뒤에서 밀어내는" LRU가 아니라, 리스트를 두 구역으로 나눈 **변형 LRU**다.
  * New(Young) Sublist : 자주 쓰이는 뜨거운 페이지 구역(기본 5/8)
  * Old Sublist : 새로 읽혀 들어온 페이지가 처음 놓이는 구역(기본 3/8, `innodb_old_blocks_pct` = 37%)
* 새 페이지는 리스트의 맨 앞이 아니라 **중간(Old Sublist의 머리)** 에 삽입되고, 일정 시간(`innodb_old_blocks_time`, 기본 1초)이 지난 후 다시 접근되어야만 Young 구역으로 승격된다.
* 존재 이유 : 대형 테이블 풀스캔이나 mysqldump 같은 일회성 대량 읽기가 발생했을 때, 그 페이지들이 자주 쓰이는 페이지들을 전부 밀어내고 캐시를 갈아치우는 것(Buffer Pool 오염)을 방지하기 위해서다 - 한 번 읽히고 말 페이지는 Old 구역에서 승격되지 못한 채 그대로 밀려난다.

**7. Page Structure(Page 16KB & Control Block)**

* Page 16KB : 디스크와 메모리 간에 데이터를 읽고 쓰는 기본 I/O 블록 단위이다. InnoDB는 기본적으로 데이터를 16KB 크기의 페이지 단위로 쪼개어 관리한다.
* Control Block : 각 16KB 페이지의 상태나 메타 데이터, 페이지 번호는 무엇인지를 관리하는 영역이다. 이 제어 블록 자체가 메모리를 차지하므로, 실제 버퍼 풀 크기 설정 시 약 5% 정도의 추가적인 메모리 오버헤드를 발생시킨다.

#### MySQL Durability(Doublewrite Buffer, WAL & FSync Performance)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a1091741-d40e-4456-a5b5-61a6bc8dce89.png)

**1. 부분 쓰기 문제(Partial Write)와 Torn Page**

* 16KB 페이지 vs 512B/4KB 섹터 : InnoDB는 데이터를 16KB 단위로 기록하려고 하지만 운영체제나 물리적 디스크 하드웨어는 보통 512B(바이트)나 4KB 단위로 데이터를 기록한다.
* Torn Page(찢어진 페이지) : 16KB 데이터를 디스크에 쓰려면 물리적으로 여러 번 나누어 기록해야 한다. 만약 이 기록 과정 중간에 정전이나 커널 패닉 등으로 크래시가 발생하면 16KB 페이지 중 일부만 디스크에 기록되는 '부분 쓰기' 상태가 된다. 이를 데이터가 찢어졌다고 하여 Torn Page라고 부른다.
* Redo Log로 복구 불가 : Redo Log는 데이터의 변경 사항만을 기록한다. 원본 페이지 자체가 Torn Page 상태로 손상되어 버리면, 변경 사항을 덧붙일 기준점이 망가진 것으로 Redo Log만으로 복구할 수 없다.
* 참고 : Redo Log 자체에는 Doublewrite가 필요 없다 - Redo Log는 처음부터 디스크 섹터 크기에 맞는 512B 블록 단위로 기록되도록 설계되어 Torn Page 문제가 발생하지 않는다.

**2. 복구 과정(Crash Recovery)**

* 체크섬(Checksum) 검증 : 재시작 시, InnoDB는 데이터 페이지를 읽어오면서 체크섬을 통해 페이지가 정상인지 검사한다.
* 손상 페이지 Doublewrite에서 복구 : 만약 특정 페이지에서 체크섬 오류(Torn Page 발견)가 발생하면, 실제 위치의 데이터는 무시하고 Doublewrite Buffer(디스크 영역)에 안전하게 백업되어 있던 온전한 페이지를 가져와서 덮어씌워 복원한다.
* Redo Log 재생 : 원본 페이지가 정상적으로 복원되었으므로, 이제 Redo Log를 순차적으로 적용하여 크래시 직전의 최신 상태로 데이터를 복구(Roll-forward)할 수 있게 된다.

**3. Doublewrite 과정(데이터 기록 과정)**

* Torn Page 문제를 예방하기 위해, 데이터를 실제 위치에 쓰기 전에 안전한 곳에 먼저 한 번 더 기록(Doublewrite)하는 과정이다.
* 더티 페이지 플러시 : 버퍼 풀의 Flush List에 있던 변경된 데이터(Dirty Page)를 디스크로 내보낸다.
* Doublewrite Buffer 순차 쓰기 : 실제 데이터 파일 위치에 바로 쓰지 않고, 디스크에 연속된 공간으로 할당된 Doublewrite Buffer 영역에 먼저 모아서 한 번에 기록한다. 순차 쓰기이므로 디스크 I/O 비용이 상대적으로 적다.
* fsync 강제 기록 : 운영체제 캐시에만 머물지 않고 물리적 디스크에 확실히 쓰여지도록 `fsync()` 시스템 콜을 호출해 보장한다.
* 실제 위치 랜덤 쓰기 : Doublewrite 영역에 안전하게 기록된 것이 확인되면 그 때 백그라운드 쓰레드가 원래 목적지인 개별 테이블의 데이터 파일 위치에 데이터를 기록(Random Write)한다.

**4. Doublewrite 구조(MySQL 8.0+)**

* 메모리 버퍼 → 디스크 영역 : 버퍼 풀에서 변경된 페이지들을 메모리 상의 Doublewrite 버퍼 공간에 먼저 모은 뒤, 디스크의 Doublewrite 영역으로 플러시한다.
* MySQL 8.0.20부터 Doublewrite 영역이 시스템 테이블스페이스(ibdata)에서 분리되어 **전용 파일(`.dblwr`)** 에 기록된다 - 시스템 테이블스페이스의 경합이 줄어 쓰기 성능이 개선되었다.
* 모든 쓰기가 두 번 일어나지만 순차 쓰기 + 배치 기록 덕분에 실측 오버헤드는 2배가 아니라 수 % 수준이다. 원자적 쓰기(atomic write)를 보장하는 스토리지에서는 `innodb_doublewrite=OFF`로 끌 수도 있다.
