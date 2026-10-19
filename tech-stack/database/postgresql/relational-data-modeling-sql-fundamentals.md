# PostgreSQL - Relational Data Modeling & SQL Fundamentals

#### PostgreSQL가 제공하는 데이터 타입

* `SMALLINT` : -32,768 ~ 32,767 저장(2바이트)
* `INTEGER` : -21억 ~ 21억 저장(4바이트)
* `BIGINT` : -920경 ~ 920경 저장(8바이트)
* `NUMERIC` : 전체 자릿수와 소수점 이하 자릿수를 지정
* `FLOAT` : 컴퓨터 내부에서 숫자를 이진수로 표현하는 부동소수점 방식
* `CHAR(n)` : 고정 길이 문자열
* `VARCHAR(n)` : 최대 n자리까지 저장가능한 가변 길이 문자열
* `TEXT` : 길이 제한이 없음
* `DATE` : 날짜만 저장하고, 시간 정보는 포함이되지 않는 형태
* `TIMESTAMP` : 날짜와 시간을 함께 저장
* `TIMESTAMPTZ` : 타임존 정보까지 포함한 날짜와 시간
* `BOOLEAN` : 참 또는 거짓

#### 데이터 타입을 활용한 테이블 생성과 관계형에서의 제약 조건

* 기본 키(PK) : 행이 자동 추가 될 떄 마다 자동으로 증가(ULID, UUID, UUID(v7))
* `SERIAL` : INTEGER 범위(약 21억)
* `BIGSERIAL` : BIGINT 범위(약 920경)

```sql
CREATE TABLE  test (
   customer_id BIGSERIAL PRIMARY KEY
   -- UNIQUE : 값은 값을 허용 하지 않는 제약조건
   -- DEFAULT : INSERT 할 떄, 해당 컬럼의 값을 생략 ⭢ 생략했을 떄, 자동으로 채울 값을 지정
   -- CHECK : 컬럼에 저장되는 값이 특정 조건을 만족하는지를 확인
)
```

#### PostgreSQL에서 지원하는 INSERT 충돌 방지를 위한 UPSERT 쿼리 패턴

**1. INSERT ON CONFLICT(공통 구문)**

```sql
INSERT INTO users (id, name) VALUES (1, 'kim')
ON CONFLICT (id) DO { NOTHING | UPDATE SET ... };
```
* PostgreSQL 9.5에서 도입된 구문이며, 충돌 시 동작(`DO NOTHING` 또는 `DO UPDATE`)을 반드시 함께 지정해야 한다.
* 충돌 대상(conflict target)은 `(컬럼 목록)` 형태로 쓰거나 `ON CONSTRAINT 제약조건명`으로 지정할 수 있다.
* 충돌 판정은 `UNIQUE` 제약조건이나 고유 인덱스를 기준으로 하므로, 대상 컬럼에 해당 제약이 없으면 오류가 발생한다.
* 삽입 여부 판단과 후속 동작이 하나의 원자적 연산으로 처리되어 동시성 환경에서도 경쟁 조건 없이 안전하게 동작한다.
* 부분 고유 인덱스를 대상으로 할 때는 `ON CONFLICT (col) WHERE 조건` 형태로 인덱스 조건을 함께 명시할 수 있다.

**2. INSERT ON CONFLICT DO NOTHING**

```sql
INSERT INTO users (id, name) VALUES (1, 'kim')
ON CONFLICT (id) DO NOTHING;
```
* 충돌이 발생한 행은 '조용히' 건너뛰고, 충돌이 없는 행만 삽입한다.
* 충돌 대상을 생략할 수 있으며 생략하면 모든 고유 제약 및 배제(exclusion) 제약 위반을 무시한다.
* `RETURNING`절을 사용하면 실제로 삽입된 행만 반환되고 건너뛴 행은 결과에 나타나지 않는다.
* 같은 데이터를 여러 번 넣어도 결과가 같아야 하는 멱등성(idempotent) 삽입이나 중복 이벤트 수집에 적합하다.
* 기존 행을 변경하지 않으므로 UPDATE 권한이 필요하지 않다.

**3. INSERT ON CONFLICT DO UPDATE SET**
 
```sql
INSERT INTO users (id, name) VALUES (1, 'kim')
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
WHERE users.name IS DISTINCT FROM EXCLUDED.name;
```
* 충돌 시 기존 행을 갱신하는 전형적인 UPSERT이며, 이 경우 충돌 대상 지정이 필수이다.
* 삽입하려던 값은 특수 테이블인 `EXCLUDED`로 참조하고, 기존 행은 테이블 이름(또는 별칭)으로 참조한다.
* `WHERE`절을 붙이면 조건을 만족할 때만 갱신하므로 값이 같을 때 불필요한 쓰기를 줄일 수 있다.
* 한 명령 안에서 같은 행을 두 번 갱신하게 되면 "cannot affect row a second time" 오류가 발생하므로 다중 행 삽입 시 입력 데이터 중복을 미리 제거해야 한다.
* 갱신 대상 컬럼에 대한 UPDATE 권한이 필요하며 `RETURNING`절은 삽입된 행과 갱신된 행을 모두 반환한다.
