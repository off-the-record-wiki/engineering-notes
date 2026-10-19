# MySQL - MySQL LockType

### MySQL - MySQL LockType

#### 락을 거는 범위(Scope)

* 락을 얼마나 넓게 걸 것인가에 대한 분류다.
* 넓게 걸수록 관리가 단순하고 데드락이 덜 생기지만 동시 처리 성능이 크게 떨어지고, 좁게 걸수록 동시성은 좋아지지만 관리가 복잡해진다. (넓다고 "안전"한 게 아니라 "동시성과의 맞바꿈"이다.)

> **테이블 락(Table Lock)**
>
> * 특정 테이블 전체에 락을 건다. 이 락이 걸리면 다른 트랜잭션은 해당 테이블을 읽거나 쓸 수 없다.
> * InnoDB는 평상시 DML(INSERT/UPDATE/DELETE)에서는 테이블 락을 쓰지 않고 행 단위 락을 쓴다. 테이블 전체가 잠기는 대표적인 상황은 DDL이다.
> * 정확히는 `ALTER TABLE` 같은 DDL은 메타데이터 락(MDL, Metadata Lock)을 잡는다.
>   * 그래서 어떤 트랜잭션이 그 테이블을 열어둔 채 커밋하지 않으면, DDL이 MDL을 얻지 못해 멈추고 뒤따르는 쿼리까지 줄줄이 대기하는 장애가 생긴다.

> **레코드 락(Row-level Lock)**
> 
> * 테이블 전체가 아니라 내가 조작하려는 대상에만 좁게 락을 건다.
> * 주의 : InnoDB의 행 락은 "행" 자체가 아니라 **인덱스 레코드**에 걸린다. 이 차이가 실무에서 중요하다.
>   * WHERE 조건이 인덱스를 타면 → 해당 인덱스 레코드만 잠긴다. (의도한 대로 좁게 잠김)
>   * WHERE 조건이 인덱스를 못 타면 → 스캔한 모든 행에 락이 걸려 사실상 테이블 전체가 잠긴 것처럼 동작한다.
>   * 즉 "행 하나만 잠근 줄 알았는데 다른 트랜잭션이 다 막히는" 문제는 대개 인덱스를 못 타서 생긴다.

> **인텐션 락(Intention Lock)**
> 
> * "행 락과 테이블 락이 어떻게 충돌 없이 공존하지?"에 대한 답이다.
> * InnoDB는 어떤 행에 락을 걸기 전에, 그 테이블 수준에 "나 이 테이블 안의 행을 잠글 거야"라는 의도(intention)를 표시하는 테이블 레벨 락(IS/IX)을 먼저 건다.
> * 덕분에 테이블 락을 요청하는 쪽이 "지금 이 테이블 안에서 행 락을 쓰는 트랜잭션이 있는지"를 행을 하나하나 확인하지 않고 빠르게 판단할 수 있다.

#### 락의 목적 - Shared vs Exclusive

* 앞의 범위(Scope)가 "얼마나 넓게"였다면, 이건 "무슨 목적으로" 거는가에 대한 분류다.
* 전제: 아래 잠금 읽기(`FOR SHARE` / `FOR UPDATE`)는 트랜잭션 안에서만 의미가 있다. autocommit 상태에서는 문장이 끝나며 바로 풀리므로, 반드시 트랜잭션으로 감싸야 COMMIT/ROLLBACK까지 유지된다.

> **공유 락(Shared Lock, S-Lock)**
> 
> * 데이터를 읽는 동안 남이 바꾸지 못하게 막고 싶을 때 건다.
> * 여러 트랜잭션이 동시에 같은 데이터에 S-Lock을 걸 수 있다. 하지만 S-Lock이 걸린 상태에서는 X-Lock을 얻을 수 없다.
```sql
-- MySQL 8.0 방식 (이전 버전은 LOCK IN SHARE MODE)
SELECT * FROM stock_trade_history WHERE stock_code = '005930' FOR SHARE;
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4b132dde-bca5-4da8-8502-54589e7664d9.png)

> **배타 락(Exclusive Lock, X-Lock)**
> 
> * 데이터를 수정하거나 삭제하기 위해 건다.
> * 이름 그대로 배타적이다. 이미 그 데이터에 S-Lock이나 X-Lock이 걸려 있으면 풀릴 때까지 대기하고, 내가 X-Lock을 쥐고 있으면 남들은 그 데이터에 잠금 접근을 할 수 없다.
```sql
-- 데이터를 조회하지만, 곧 수정할 것이므로 남들은 건드리지 못하게 X-Lock을 검
SELECT * FROM stock_trade_history WHERE stock_code = '005930' FOR UPDATE;
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c51586ff-a0ea-4f56-85df-16fb2bfee8bc.png)

> **1. 주의 : 일반 SELECT는 락을 걸지 않는다**
>
> * `FOR SHARE` / `FOR UPDATE`가 붙지 않은 평범한 SELECT는 S-Lock을 걸지 않는다.
> * InnoDB는 MVCC(다중 버전 동시성 제어)로, 잠금 대신 "그 시점의 스냅샷"을 읽기 때문이다.
> * 그래서 X-Lock이 걸린 행이라도 일반 SELECT는 대기 없이 읽을 수 있다. "읽기가 쓰기를 막지 않는다"는 게 InnoDB의 기본 동작이다.
> * 읽는 값을 확실히 고정하고 싶을 때만 명시적으로 FOR SHARE/FOR UPDATE를 쓴다.

> **2. 주의 : 잠금 순서가 엇갈리면 데드락**
>
> * 두 트랜잭션이 락을 서로 반대 순서로 잡으면 서로의 락을 기다리며 멈추는 데드락이 발생한다.
> * InnoDB는 데드락을 자동 감지해 한쪽 트랜잭션을 강제 롤백(victim)시킨다. 애플리케이션은 이 실패를 잡아 재시도하도록 설계해야 한다.
> * 예방 : 여러 행을 잠글 때 항상 같은 순서로 접근하고, 트랜잭션과 락 보유 시간을 짧게 유지한다.
> * 관측 : `SHOW ENGINE INNODB STATUS`의 `LATEST DETECTED DEADLOCK`, `performance_schema.data_locks`

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/94ead65a-1f81-4885-8566-3fc9f8b2be4b.png)
