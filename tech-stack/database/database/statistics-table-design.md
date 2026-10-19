# Database - Statistics Table Design

### Database - Statistics Table Design

#### 통계 데이터와 성능 문제 정리

* 원본 테이블에 대해서 직접 집계를 하는 구조는 서비스 규모가 커지게 된다면 문제가 되는 구조가 된다.
  * 성능 저하 : 기본적으로 인덱스를 타기 어렵거나 타더라도 범위가 매우 넓다.
  * 데이터베이스 부하 : 통계 쿼리는 보통 처리해야 하는 양이 많으므로 CPU와 메모리를 많이 사용한다. 이 무거운 쿼리가 도는 동안 데이터베이스 자원을 아주 많이 사용한다.
  * 서비스 장애 전파 : DB가 통계 쿼리를 처리하느라 바빠지면 정작 중요한 비즈니스 로직에 대한 쿼리가 느려지거나 최악의 경우 타임아웃이 발생한다.

#### 통계 테이블 설계

```sql
CREATE TABLE daily_sales_stats (
    stat_date DATE NOT NULL,
    total_order_count INT NOT NULL DEFAULT 0,
    total_sales_amount BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (stat_date)
);
```
* 하루에 딱 한 행만 만들어지기 때문에 1년이 지나도 최대 365행, 10년이 지나도 3,650행이다.
* 원본 테이블에서 데이터를 읽어와서 통계 테이블에 넣어주어야 하는데 보통 이런 작업은 하루가 지난 다음날 새벽(트래픽 거의 없는 시간대)에 배치 프로그램으로 실행한다.
* 통계 테이블을 이렇게 분리하면 장점과 단점이 있다.
* 장점
  * 조회 속도 : 1억건을 뒤지는게 아니라 단 1건 조회만 하면 된다.
  * 부하 분리 : 무거운 집계 쿼리는 새벽에 한 번만 실행되기 때문에 실제 업무 시간에는 서비스 DB에 영향을 주지 않는다.
* 단점
  * 실시간성 부족 : 어제까지의 데이터만 볼 수 잇다.
  * 관리 포인트 증가 : 배치 프로그램을 별도로 만들고 관리해야 한다. 배치가 실패하면 통계가 갱신되지 않는다.

#### 주간/월간 통계 효율적인 처리

* 결론은 월간/연간까지는 필요가 없고 일별 데이터만 잘 관리하면 된다.
* 일별 통계 테이블 하나만 잘 설계해두면 주간, 월간, 연간, 분기 등 시간과 관련된 모든 통계를 자유자재로 아주 빠르게 뽑아낼 수 있다.

#### 실시간 통계와 하이브리드 설계

* 실시간 통계는 조회하는 방법으로 과거 데이터를 일별 통계 테이블에서 가져오고 오늘 데이터를 원본 테이블에서 가져와 UNION ALL로 결합하면 실시간 통계를 볼 수 있다.
* 그러나 원본 테이블은 데이터가 많을 수 있기 때문에 별다른 전략이 없다면 해당 구조가 의미가 없을 수 있다.
* 해당 전략이 유효하려면 원본 테이블의 날짜 컬럼에 인덱스가 있어야 한다. 그렇지 않게 되면 기본적으로 풀 테이블 스캔(Full Table Scan) 방식으로 동작하기 때문에 서비스에 점점 심각한 문제가 발생하게 된다.

```sql
SELECT
    DATE_FORMAT(stat_date, '%Y-%m') as stat_month,
    SUM(total_order_count) AS monthly_order_count,
    SUM(total_sales_amount) AS monthly_sales_amount
FROM (
    -- 1. 과거 데이터 (통계 테이블)
    SELECT
        stat_date,
        total_order_count,
        total_sales_amount
    FROM daily_sales_stats
    WHERE stat_date >= '2026-01-01' AND stat_date < '2026-01-04'

    UNION ALL

    -- 2. 오늘 실시간 데이터 (원본 테이블)
    SELECT
        DATE(order_date) as stat_date,
        COUNT(*) as total_order_count,
        SUM(total_amount) as total_sales_amount
    FROM orders
    WHERE order_date >= '2026-01-04 00:00:00'
      AND order_status = 'COMPLETED'
    GROUP BY DATE(order_date)
) AS hybrid_table
GROUP BY DATE_FORMAT(stat_date, '%Y-%m');
```

#### 멱등성 설계

* 멱등성(Idempotency)이란, 연산을 한 번 실행하든 N번 실행하든 그 결과가 항상 같아야 한다는 성질이다.
* 통계 배치 시스템에서 멱등성을 지키는 가장 단순하고 확실한 방법은 지우고 다시 쓰기(DELETE & INSERT) 전략이다.
* 기존에 값이 있든 없든, 틀렸든 맞든, 그냥 싹 지우고 원본 데이터를 기반으로 다시 계산해서 넣으면 된다.
> 그러나 지우고 다시 쓰는 작업 방식은 다음과 같은 비효율성이 있다.
>
> * 불필요한 리소스 낭비 : 기존 데이터를 DELETE하면 DB는 복구를 위해 Undo Log를 생성한다. 다시 INSERT가 되면 Redo Log를 기록하고 인덱스를 다시 구성한다. 결국 멀쩡한 데이터를 지우고 다시 만드는 과정마다 DB 엔진이 불필요한 일을 많이 하게 된다.<br>
> * ID 고갈 및 인덱스 파편화 : 잦은 삭제와 삽입은 데이터 페이지의 파편화를 유발할 수 있다.<br>

#### UPSERT 최적화 - 조회 후 등록 수정(Select-Insert-Update) 패턴

* 무작정 지우고 다시 쓰는 방식은 데이터베이스 입장에서 효율성이 떨어지기 때문에 "데이터가 있는지 먼저 확인해보고 없으면 넣고 있으면 수정"하는 것을 많이 사용한다.
* 이 방식은 불필요한 삭제를 막아주지만 쿼리를 2번(SELECT → INSERT/UPDATE) 실행해야하는 단점이 있다. 네트워크 통신 비용이 2배로 드는 셈이다.

#### UPSERT 최적화 - ON DUPLICATE KEY UPDATE 활용

* MySQL에서는 DELETE 후 INSERT하는 과정의 비효율을 해결해주는 기능을 제공한다.
* 바로 `INSERT ... ON DUPLICATE KEY UPDATE` 구문으로 이를 흔히 UPSERT라고 부른다.
* `ON DUPLICATE KEY UPDATE` 문법 구조는 INSERT 뒤에 ON DUPLICATE KEY UPDATE 절을 붙이는 형태이다.
```sql
INSERT INTO 테이블명 (컬럼1, 컬럼2, ...)
VALUES (값1, 값2, ...)
ON DUPLICATE KEY UPDATE
컬럼1 = 새로운_값,
컬럼2 = 새로운_값;
```
* `INSERT INTO ...` : 데이터를 삽입하는 일반적인 쿼리와 같다. 데이터베이스는 가장 먼저 이 데이터를 입력을 시도한다.
* `ON DUPLICATE KEY` : 만약 삽입하려는 데이터의 PK가 이미 테이블에 존재해서 충돌이 발생한다면?
* `UPDATE ...` : 오류를 발생시키는 대신 뒤에 작성된 UPDATE 로직을 실행하여 기존 행의 값을 수정한다.
* 이 구문을 사용하면 "일단 넣어보고, 안 되면 수정해라"라는 로직을 SQL 한 문장으로 깔끔하게 처리할 수 있다.
> UPSERT의 원리
> 
> * UPSERT는 이름 그대로 없으면 넣고, 있으면 고친다는 뜻이다.<br>
> * INSERT 시도 : 데이터베이스는 먼저 INSERT하려고 시도한다.<br>
> * 충돌 확인 : 중복이 없다면 정상적으로 INSERT가 실행되고 중복이 있다면 INSERT를 포기하고 UPDATE문을 실행한다.<br>
> * 이 방식은 기존 행을 삭제하지 않고 필요한 컬럼의 값만 쏙 바꾸기 때문에 DELETE + INSERT 방식보다 훨씬 효율적이고 가볍다.<br>

#### DELETE & INSERT 방식과 UPSERT 방식의 성능 비교 정리 → 비용 발생과 근본 원인 : 인덱스와 로그

* DELETE + INSERT 방식
  * 인덱스 파괴 및 재구축 : 테이블에 인덱스가 걸려 있다면 DELETE시에 인덱스에서 모두 데이터를 찾아 제거해야 한다.
  * INSERT시 다시 N개의 인덱스에 데이터를 끼워 넣어야 한다. 인덱스 정렬을 위한 오버헤드가 2배로 발생한다.
  * 방대한 로그 생성 : 데이터베이스는 복구를 위해 변경 사항을 모두 기록한다. 데이터를 삭제했다는 기록과 새로 넣었다는 기록을 모두 남기므로 로그 사용량이 급증한다.
  * 페이지 단편화 : 데이터를 지우고 다시 넣다 보면 데이터가 저장된 물리적 공간에 구멍이 뚫리는 단편화 현상이 발생한다. 이는 나중에 데이터를 조회할 때 성능 저하의 주범이 된다.
* UPSERT 방식
  * 인덱스 보존 : PK나 인덱스 컬럼을 건드리지 않고 일반 컬럼만 수정한다면 인덱스 트리 구조를 변경할 필요가 전혀 없고 단순 값만 덮어쓰면 된다.
  * 최소한의 로그 : 변경된 값에 대한 로그만 남기면 되므로 디스크 I/O가 훨씬 적다.
  * 공간 재사용 : 이미 할당된 공간을 그대로 사용하므로 단편화가 발생하지 않는다.
  * 네트워크 호출 최적화 : 네트워크 호출 횟수가 줄어든다.
> 결론적으로 통계 데이터를 자주 갱신하거나 기존 정보를 자주 업데이트할 때는 UPSERT 방식을 사용하여 데이터베이스 부하를 줄일 수 있다.
