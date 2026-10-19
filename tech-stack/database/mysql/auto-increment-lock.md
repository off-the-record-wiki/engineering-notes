# MySQL - AUTO_INCREMENT_LOCK

### MySQL - AUTO_INCREMENT_LOCK

#### AUTO_INCREMENT_LOCK이란?

* 테이블을 설계할 때 기본키(PK)로 `AUTO_INCREMENT`를 많이 사용한다.
* 데이터베이스는 각 트랜잭션에게 절대 중복되지 않는 순차적인 ID를 부여해야 하는데 A와 B가 동시에 접근했을 때 둘 다 동일한 ID를 가져가면 PK 중복 에러가 발생한다.
* 이를 방지하기 위해 MySQL(InnoDB)은 새로운 ID 번호를 발급하는 아주 짧은 시간 동안 특수한 형태의 락을 거는데 이것이 바로 `AUTO_INCREMENT_LOCK`이다.
* 이 락을 얼마나 빡빡하게/느슨하게 걸지 결정하는 설정이 `innodb_autoinc_lock_mode`다.

#### INSERT의 3가지 종류 (모드 이해의 전제)

* 락 모드는 "INSERT가 어떤 종류냐"에 따라 다르게 동작한다.
  * `Simple Insert` : 삽입할 행 수를 실행 전에 알 수 있는 INSERT
  * `Bulk Insert` : 몇 건이 들어갈지 실행해봐야 아는 INSERT
  * `Mixed-mode Insert` : 일부 행은 ID를 명시하고 일부는 자동 할당에 맡기는 INSERT

#### 락 모드(Lock Mode)의 3가지 유형

> **Mode 0: Traditional**

* `INSERT` 쿼리가 시작해서 끝날 때까지 테이블 전체에 락을 유지한다.
* 트랜잭션 A가 100건을 넣는 동안 트랜잭션 B는 단 1건도 넣지 못하고 무조건 대기해야 한다.
* 성능이 매우 나쁘기 때문에 현재는 거의 쓰이지 않는다. 대신 하나의 문장이 만든 ID는 항상 연속임이 보장된다.

> **Mode 1: Consecutive(MySQL 5.7 이하의 기본값은 1(Consecutive)이었다.)**

* Simple Insert처럼 행 개수를 미리 알면, 무거운 테이블 락 대신 가벼운 뮤텍스로 필요한 개수만큼 번호만 빼가고 즉시 락을 푼다 → 빠르다.
* Bulk Insert처럼 개수를 모르면 테이블 AUTO-INC 락을 건다.
* 한 문장이 받은 ID 묶음은 서로 연속임을 보장한다.

> **Mode 2: Interleaved(MySQL 8.0부터 기본값이 2(Interleaved)로 바뀌었다.)**

* 개수를 알든 모르든 테이블 락을 걸지 않고, 항상 가벼운 뮤텍스로 ID만 할당하고 즉시 해제한다.
* 동시성 성능이 가장 뛰어나 수많은 스레드가 동시에 INSERT해도 병목이 거의 없다.
* 대신 여러 트랜잭션의 ID가 섞이고(interleaved) 한 문장 안에서도 ID가 연속이 아닐 수 있다.

```sql
-- 현재 AUTO_INCREMENT 락 모드 확인
SHOW VARIABLES LIKE 'innodb_autoinc_lock_mode';
```
