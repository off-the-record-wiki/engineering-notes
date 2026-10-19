# MySQL - MySQL Fundamentals

### MySQL - MySQL Fundamentals

#### MySQL INSERT Guide - `INSERT ... ON DUPLICATE KEY UPDATE`

```sql
-- INSERT ... ON DUPLICATE KEY UPDATE (Upsert)
-- [동작] PK 또는 UK 충돌 시 UPDATE, 없으면 INSERT
-- [장점] INSERT/UPDATE를 단일 쿼리로 원자적으로 처리 (Upsert 패턴)
-- [주의1] VALUES(col) 함수는 MySQL 8.0.20부터 deprecated → 아래처럼 행 별칭(alias) 방식 사용
-- [주의2] AUTO_INCREMENT 컬럼이 있으면 충돌(UPDATE) 시에도 내부 카운터가 증가하므로 ID 단절(gap) 발생 가능
-- [주의3] UNIQUE 인덱스가 2개 이상인 테이블에서는 새 행이 서로 다른 기존 행들과 각각 충돌할 수 있고, 이 경우 그중 한 행만 갱신되며 어떤 행이 갱신될지 비결정적 → 공식 문서도 사용 자제 권고, statement 기반 복제에서는 unsafe 판정
-- [팁1] affected rows로 결과 구분 가능: INSERT = 1, UPDATE = 2, 값이 동일해 변경 없음 = 0
-- [팁2] updated_at은 매 쿼리마다 NOW()를 쓰는 대신 컬럼 정의에 `updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`를 걸어두는 방법도 있음
INSERT INTO users (id, name, email, age, city)
VALUES (100, '김철수', 'kim@naver.com', 13, 'Seoul') AS new
ON DUPLICATE KEY UPDATE
    name = new.name,
    age  = new.age,
    city = new.city,
    updated_at = NOW();
```

#### MySQL INSERT Guide - `INSERT IGNORE`

```sql
-- INSERT IGNORE
-- [동작] PK/UK 충돌 시 해당 행을 조용히 무시(skip), 에러 미발생
-- [장점] 중복 데이터 유입 방어에 간편하게 사용 가능
-- [주의1] 충돌 외 다른 에러(NOT NULL 위반, 타입 불일치, 데이터 잘림 등)는 행이 skip되는 것이 아니라 암묵적 기본값(0, 빈 문자열 등)이나 잘린 값으로 "보정되어 삽입"됨
-- → 에러가 묻히는 수준이 아니라 잘못된 데이터가 실제로 들어가므로 운영 환경에서 위험
-- → 실행 직후 SHOW WARNINGS로 강등된 경고를 반드시 확인
-- [주의2] 충돌로 skip된 행도 AUTO_INCREMENT 카운터는 증가 → ID 단절(gap) 발생
INSERT IGNORE INTO users (id, name, email, age, city)
VALUES
    (100, '김철수', 'kim@naver.com', 13, 'Seoul'),
    (100, '김철수', 'kim@naver.com', 13, 'Seoul');
```

#### MySQL INSERT Guide - `REPLACE INTO`

```sql
-- REPLACE INTO
-- [동작] PK/UK 충돌 시 기존 행을 DELETE 후 새 행을 INSERT → 내부적으로 DELETE + INSERT이므로 AUTO_INCREMENT 값이 항상 증가
-- [장점] 문법이 단순
-- [위험1] 운영 환경에서 사용 비권장 - 충돌한 행이 삭제되므로 연관 테이블에 CASCADE DELETE가 걸려 있으면 자식 데이터까지 삭제될 수 있음(반대로 자식 테이블이 RESTRICT면 삭제 단계에서 REPLACE 자체가 실패)
-- [위험2] 삭제-삽입이므로 binlog 기반 복제 환경에서 부하 증가
-- [위험3] created_at, 소프트딜리트 컬럼 등 기존 데이터가 전부 유실됨
-- [위험4] UNIQUE 인덱스가 여러 개면 새 행 하나가 기존 행 여러 개와 충돌해 "한 번의 REPLACE로 여러 행이 삭제"될 수 있음
-- [위험5] 내부적으로 DELETE가 실행되므로 DELETE 트리거가 실제로 발화함
REPLACE INTO users (id, name, email, age, city)
VALUES (1, 'a', 'b', 3, 's');
```

#### MySQL INSERT Guide - `INSERT INTO ... SELECT`

```sql
-- INSERT INTO ... SELECT (테이블 간 데이터 복사)
-- [동작] 다른 테이블의 조회 결과를 그대로 삽입
-- [장점] 마이그레이션, 백업 테이블 생성, 집계 테이블 적재 등에 활용
-- [주의] REPEATABLE READ + binlog 환경에서는 소스 테이블의 읽은 행에 공유 잠금(S-Lock)이 걸릴 수 있어 대량 복사 시 소스 테이블의 쓰기 작업이 블로킹될 수 있음 → 청크 분할 또는 트래픽이 적은 시간대 실행 권장
INSERT INTO users_backup (id, name, email, age, city)
SELECT id, name, email, age, city
FROM users
WHERE created_at < '2024-01-01';
```

#### MySQL INSERT Guide - `INSERT ... WHERE NOT EXISTS`

```sql
-- INSERT ... WHERE NOT EXISTS (중복 방지 패턴)
-- [동작] 서브쿼리로 중복 확인 후 없을 때만 INSERT
-- [문제] 확인과 삽입이 원자적이지 않아 동시 요청 시 Race Condition 위험 (둘 다 "없음"으로 판단 → 중복 삽입)
-- [개선] UNIQUE 제약 + ON DUPLICATE KEY UPDATE 조합이 원자적(atomic)으로 안전
-- [참고] FROM 없는 SELECT ... WHERE 문법은 MySQL 8.0.19부터 지원, 이전 버전은 FROM DUAL 필요
INSERT INTO users (name, email, age, city)
SELECT '새 사용자', 'test@test.com', 25, 'Seoul'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'test@test.com'
);
```

#### MySQL INSERT Guide - `Batch Insert`

```sql
-- 배치 INSERT (대량 데이터 삽입)
-- [동작] 단일 INSERT로 다수 행을 한 번에 삽입
-- [장점] 단건 INSERT 반복 대비 네트워크 왕복 및 파싱 비용 절감
-- [주의] 한 번에 너무 많은 행을 넣으면 아래 문제 발생
  -- 단일 트랜잭션이 길어져 언두 로그(undo log) 폭증 → InnoDB 성능 저하
  -- max_allowed_packet 초과 시 쿼리 자체가 실패
  -- 실패 시 전체 롤백 → 재처리 범위가 커짐
  -- binlog가 커밋 시점에 한꺼번에 전송되어 복제 지연(replica lag) 유발
-- [권장] 1,000 ~ 5,000건 단위로 청크 분할 + 트랜잭션 명시
-- [JDBC/JPA 주의] rewriteBatchedStatements=true 옵션이 없으면 JDBC batch를 써도 실제로는 단건 INSERT가 반복 전송됨 (multi-value INSERT로 재작성되지 않음). 또한 IDENTITY 키 생성 전략에서는 Hibernate batch insert가 비활성화됨
INSERT INTO users (name, email, age, city) VALUES
    ('User1', 'user1@test.com', 20, 'Seoul'),
    ('User2', 'user2@test.com', 21, 'Busan'),
-- ... 많은 레코드
    ('User10000', 'user10000@test.com', 30, 'Daegu');
```

#### MySQL INSERT Guide - `LOAD DATA INFILE`

```sql
-- LOAD DATA INFILE (CSV 파일 직접 로드)
-- [동작] MySQL 서버가 직접 파일을 읽어 테이블에 삽입 (배치 INSERT보다 훨씬 빠름)
-- [장점] 대용량 데이터 로드에서 가장 빠른 방식 (bulk insert path 사용)
-- [보안/운영 제약]
  -- 서버 측 파일 로드는 secure_file_priv 설정 경로 내 파일만 허용 + FILE 권한 필요
  -- LOCAL 옵션(클라이언트 측 파일)은 local_infile이 기본 비활성
  -- (악성 서버가 클라이언트의 임의 파일을 요구할 수 있는 보안 이슈 때문)
-- [LOCAL 사용 시 동작 차이] 에러가 경고로 강등되어 INSERT IGNORE처럼 동작
-- (중복 키가 있어도 에러 없이 진행) + 클라이언트 → 서버 전송 비용으로 서버 측 로드보다 느림
-- [팁] CHARACTER SET utf8mb4 명시(한글 깨짐 방지), IGNORE 1 LINES로 CSV 헤더 스킵
LOAD DATA INFILE '/path/to/users.csv'
    INTO TABLE users
    CHARACTER SET utf8mb4
    FIELDS TERMINATED BY ','
    ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    IGNORE 1 LINES
    (name, email, age, city);
```

> 정리 : 중복 처리가 필요한 운영 환경에서는 UNIQUE 제약 + ON DUPLICATE KEY UPDATE 조합이 가장 안전하고 원자적인 선택이다. 단, UNIQUE 인덱스가 여러 개인 테이블에서는 갱신 대상이 비결정적이 될 수 있으므로 이 경우에는 사용을 재검토해야 한다.

#### MySQL UPDATE Guide - 범위 조건 UPDATE와 인덱스

```sql
-- [전제] age에 인덱스가 있을 때 O(log n + m) (n : 전체 레코드, m : 범위 내 레코드)
--        인덱스가 없으면 풀스캔 O(n)
UPDATE users
SET city = 'Sejong'
WHERE age BETWEEN 25 AND 35;
```

#### MySQL UPDATE Guide - IN 조건 UPDATE

```sql
UPDATE users
SET city = 'Busan'
WHERE city IN ('Incheon', 'Gyeonggi'); -- WHERE city = 'Incheon' OR city ='Gyeonggi'
```

#### MySQL UPDATE Guide - 상관 서브쿼리 UPDATE(동작 원리)

```sql
/*
====================================================================
[쿼리 설명] 
user_preferences 테이블에 존재하는 유저에 한하여, users 테이블의 city 정보를 업데이트합니다.
(동작 원리 설명용 예제 - 실무 권장안은 아래 UPDATE ... JOIN 참고)

[시간 복잡도 및 성능 분석]
이 쿼리의 성능은 'user_preferences' 테이블의 인덱스 여부에 크게 좌우됩니다.
(n = users 레코드 수, m = user_preferences 레코드 수)

1. O(n log m) : 정상적인/권장되는 상황 (인덱스 O)
- user_preferences.user_id 에 인덱스가 걸려있는 경우입니다.
- users 테이블을 순회(n)하며, 각 레코드마다 인덱스를 타고 user_preferences를 빠르게 탐색(log m)하므로 이 방식에서는 가장 이상적인 성능을 냅니다.
- 단, 이 쿼리는 행마다 user_preferences를 두 번 탐색합니다.(SET 절의 스칼라 서브쿼리 1회 + WHERE EXISTS 1회)

2. O(n * m) : 최악의 상황 (인덱스 X)
- user_preferences.user_id 에 인덱스가 없는 경우입니다.
- users를 순회(n)할 때마다 user_preferences 전체를 Full Scan(m) 해야 하므로, 데이터가 많아질수록 쿼리 성능이 급격히 저하됩니다.

3. O(n + m)을 원한다면 : UPDATE ... JOIN으로 재작성
- MySQL 8.0의 해시 조인(Hash Join)은 조인 구문에만 적용되며, SET 절의 상관 스칼라 서브쿼리를 해시 조인으로 변환해 주지는 않습니다.
- 따라서 옵티마이저에 기대지 말고 아래의 UPDATE ... JOIN 문법으로 직접 재작성해야 합니다.
====================================================================
*/

UPDATE users AS u 
SET city = (
    -- u.id와 일치하는 데이터를 인덱스를 통해 탐색하여 city 값을 가져옵니다.
    SELECT city 
    FROM user_preferences AS up 
    WHERE up.user_id = u.id
)
WHERE EXISTS(
    -- 이 구문이 없으면 user_preferences에 없는 유저의 city가 NULL로 덮어씌워집니다.
    -- 서브쿼리 내 'SELECT 1'은 데이터 존재 여부(True/False)만 빠르게 판별합니다.
    SELECT 1 
    FROM user_preferences AS up 
    WHERE up.user_id = u.id
);
```

#### MySQL UPDATE Guide - `UPDATE ... JOIN`(권장안)

```sql
-- UPDATE ... JOIN (권장안)
-- [동작] 조인에 성공한 행만 갱신 (INNER JOIN이므로 EXISTS 없이도 NULL 덮어쓰기 문제가 원천적으로 없음)
-- [장점] 위 서브쿼리 방식과 달리 user_preferences를 한 번의 조인으로만 탐색, 해시 조인 등 옵티마이저의 조인 최적화도 이 문법에서만 가능
-- [주의] user_preferences에 같은 user_id가 여러 행(1:N)이면
--        - 스칼라 서브쿼리 방식 : "Subquery returns more than 1 row" 에러로 실패
--        - JOIN 방식 : 에러 없이 여러 행 중 하나가 비결정적으로 적용
--        → 양쪽 다 함정이므로 1:N 가능성이 있다면 어느 행을 쓸지 명시적으로 결정해야 함
-- [참고] 갱신 대상 테이블을 서브쿼리의 FROM에서 다시 참조하는 것은 MySQL에서 에러(1093) 발생(예: UPDATE users ... WHERE id IN (SELECT id FROM users ...))
UPDATE users u
JOIN user_preferences up ON up.user_id = u.id
SET u.city = up.city;
```

#### MySQL UPDATE Guide - 대량 UPDATE 청크 분할

```sql
-- 대량 UPDATE 청크 분할
-- [동작] LIMIT으로 한 번에 갱신할 행 수를 제한하고, affected rows가 0이 될 때까지 반복 실행
-- [장점] 장시간 트랜잭션 방지 → 락 유지 시간 단축, 언두 로그 증가 및 복제 지연(replica lag) 완화
UPDATE users
SET city = 'Sejong'
WHERE age BETWEEN 25 AND 35
  AND city <> 'Sejong'  -- 이미 갱신된 행을 제외해 반복 실행 시 같은 행을 다시 잡지 않도록 함
LIMIT 1000;
```

#### MySQL UPDATE Guide - 인덱스 부재와 X-Lock 동시성 문제

* 인덱스가 없으면 성능 문제가 아니라 동시성 문제가 된다.
  * REPEATABLE READ에서 WHERE 컬럼에 인덱스가 없으면 UPDATE가 풀스캔하면서 조건에 맞지 않는 행까지 포함해 스캔한 모든 행에 X-Lock을 잡는다.
  * 즉 인덱스 유무는 "느려진다" 수준이 아니라 테이블 전체 쓰기가 블로킹되는 동시성 사고로 이어질 수 있다.

#### MySQL DELETE Guide - 외래키 참조 옵션(RESTRICT/CASCADE/SET NULL/NO ACTION)

* `RESTRICT` : 참조하는 데이터가 있으면 삭제를 거부하고 에러가 발생한다.(기본 동작)
* `CASCADE` : 참조하는 데이터도 함께 삭제된다.
* `SET NULL` : 참조하는 데이터의 외래키를 NULL로 설정한다.(해당 FK 컬럼이 NULL 허용이어야 함)
* `NO ACTION` : 표준 SQL에서는 참조 무결성 검사를 지연할 수 있다는 의미지만, MySQL(InnoDB)은 지연 검사를 지원하지 않으므로 RESTRICT와 완전히 동일하게 동작한다.

#### MySQL DELETE Guide - 대량 DELETE 청크 분할

```sql
-- 한 번에 1000개씩 삭제(MySQL CPU/Memory 사용량 최소화)
-- affected rows가 0이 될 때까지 반복 실행하고, 매 반복마다 커밋해 락 유지 시간과 언두 로그를 짧게 유지
-- [주의] ORDER BY 없는 DELETE ... LIMIT은 삭제 대상이 비결정적이라 statement 기반 복제에서 unsafe 경고 발생 → PK 기준 ORDER BY를 함께 쓰는 것이 안전
DELETE FROM users
WHERE status = 'deleted'
ORDER BY id
LIMIT 1000;
```

#### MySQL DELETE Guide - 인덱스를 타는 WHERE 조건(컬럼 가공 금지)

```sql
-- 비효율적: 인덱스 사용 불가(인덱스 컬럼을 가공하게 되면 인덱스를 사용할 수 없다)
DELETE FROM users
WHERE DATE(created_at) < '2024-01-01';

-- 효율적: 인덱스 사용 가능
DELETE FROM users
WHERE created_at < '2024-01-01';
```

#### MySQL DELETE Guide - IN / NOT EXISTS 조건 DELETE

```sql
-- 특정 이메일들을 가진 사용자들 삭제(IN 쿼리)
DELETE FROM users
WHERE email IN ('user1@test.com', 'user2@test.com', 'user3@test.com');

-- NOT EXISTS 사용 (NOT IN보다 안전)
-- [이유] NOT IN (SELECT ...)은 서브쿼리 결과에 NULL이 하나라도 있으면 전체 조건이 UNKNOWN이 되어 아무 행도 삭제되지 않는 함정이 있음 → NOT EXISTS 권장
-- [주의] orders.user_id에 인덱스가 있어야 행마다 빠른 존재 확인 가능
DELETE FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM orders WHERE user_id = users.id
);
```

#### MySQL DELETE Guide - `DELETE ... JOIN`

```sql
-- 다른 테이블과 조인하여 삭제
-- [주의1] 조인 조건 실수 시 의도보다 훨씬 많은 행이 삭제될 수 있음 → 같은 조건의 SELECT로 삭제 대상을 먼저 검증한 뒤 실행 권장
-- [주의2] 다중 테이블 DELETE는 ORDER BY / LIMIT을 지원하지 않아 청크 분할이 불가능
DELETE u FROM users u
      JOIN inactive_list il ON u.email = il.email
WHERE il.inactive_date < DATE_SUB(NOW(), INTERVAL 6 MONTH);
```

#### MySQL DELETE Guide - `Soft Delete`

```sql
-- 완전 삭제 대신 삭제 표시
-- [주의1] 모든 조회 쿼리에 WHERE deleted_at IS NULL 조건이 필요 (누락 시 삭제된 데이터가 노출됨)
-- [주의2] UNIQUE(email) 제약이 있으면 탈퇴 후 동일 이메일 재가입 시 충돌 → 유니크 키에 deleted_at 포함, 삭제 시 이메일 값 치환 등의 설계가 함께 필요
UPDATE users
SET deleted_at = NOW()
WHERE email = 'withdraw@example.com';
```

#### MySQL DELETE Guide - `TRUNCATE vs DELETE`

* TRUNCATE TABLE은 DML이 아닌 DDL로 동작한다 - 테이블을 드롭 후 재생성하므로 행 수와 무관하게 빠르다.
* DELETE와의 차이 : 암묵적 커밋 발생 → 롤백 불가, AUTO_INCREMENT 카운터가 초기화됨, DELETE 트리거가 발화하지 않음, 다른 테이블이 FK로 참조 중이면 실행 불가
* 전체 데이터를 비울 때만 사용하고, 조건부 삭제나 트랜잭션 내 삭제는 DELETE를 사용한다.

#### MySQL SELECT Guide - 트랜잭션 격리 수준과 SELECT

* SELECT → 읽기 작업 (트랜잭션 격리 수준)
* SELECT는 데이터를 읽기만 하므로, 어떤 격리 수준을 쓰느냐에 따라 동작이 달라진다.
* MySQL의 InnoDB 기본 격리 수준은 REPEATABLE READ이다.

|격리 수준|동작|
|:---:|:---:|
|READ UNCOMMITTED|커밋 안 된 데이터도 읽는다.|
|READ COMMITTED|커밋된 데이터만 읽는다.|
|REPEATABLE READ|트랜잭션 내에서 같은 SELECT는 항상 같은 결과를 가진다.|
|SERIALIZABLE|완전한 직렬화, 가장 엄격|

#### MySQL SELECT Guide - S-Lock / X-Lock과 읽기·쓰기 차단

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ec4761ff-3017-4003-805a-0f9af91ddb44.png)

* X-Lock은 다른 트랜잭션의 쓰기와 잠금 읽기(FOR SHARE / FOR UPDATE)를 차단한다. 이 때, 주의할 점은 일반 SELECT는 MVCC 스냅숏을 읽으므로 X-Lock에 블로킹되지 않는다.
* 일반 SELECT는 락을 잡지 않는 일관된 비잠금 읽기(consistent nonlocking read)다.
* S-Lock은 `SELECT ... FOR SHARE(8.0 이전 LOCK IN SHARE MODE)`를 명시하거나 `SERIALIZABLE` 격리 수준일 때 잡힌다.
* `SELECT ... FOR UPDATE`는 읽으면서 X-Lock을 잡는다. 갱신할 행을 미리 선점할 때 사용한다.
* UPDATE/DELETE는 반드시 X-Lock을 가진다.

#### MySQL SELECT Guide - LIKE 패턴과 인덱스 사용 여부

|패턴|인덱스|이유|
|:---:|:---:|:---:|
|`LIKE 'abc%'`|✅ 사용|앞이 고정 → B-Tree 범위 탐색 가능|
|`LIKE '%abc'`|❌ 미사용|앞이 불확정 → 풀스캔|
|`LIKE '%abc%'`|❌ 미사용|양쪽 불확정 → 풀스캔|

* B-Tree는 정렬된 값의 앞부분으로 탐색하기 때문에 접두어가 고정이어야 인덱스를 탈 수 있다.
* %abc% 같은 중간 일치 검색이 자주 필요하다면 FULLTEXT 인덱스(전문 검색)를 고려한다.

#### MySQL SELECT Guide - FileSort 메커니즘

**1. WHERE — 대상 row 필터링**
```sql
SELECT * FROM orders WHERE status = 'PAID' ORDER BY created_at DESC;
```
* 먼저 WHERE 조건으로 정렬 대상 row를 추린다.
* 이 단계에서 인덱스를 잘 타면 `FileSort` 부하가 줄어든다.
* `WHERE` 결과가 많을수록 정렬 비용이 증가한다.

**2. 정렬할 데이터를 메모리로 로드**
* 추려진 row를 `sort_buffer`에 올린다.
* 이 때, `sort_buffer_size` = 256KB (기본값)이다.
* 두 가지 방식이 있다.
  * `Single-pass` : SELECT 컬럼 전체를 버퍼에 로드 → 정렬 후 바로 반환
  * `Two-pass` : row pointer + 정렬 키만 로드 → 정렬 후 다시 원본 읽기
* 컬럼이 많고 row가 크면 Two-pass 방식을 사용하고 이 선택 기준은 MySQL 옵티마이저가 자동 선택한다.

**3. sort_buffer_size — 메모리 vs 디스크 분기점**
* `데이터 크기 ≤ sort_buffer_size` →  메모리 정렬
* `데이터 크기 >  sort_buffer_size`  →  External Sort
* 이 때, External Sort가 발생하면 디스크 I/O가 발생하므로 성능이 크게 떨어진다.

**4. 정렬된 결과 반환**
* 메모리 정렬이면 버퍼에서 바로 반환한다.
* `External Sort`면 병합된 임시 파일에서 읽어서 반환한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9c8375ec-4f03-4c75-b426-39fb1515f9d5.png)

#### MySQL SELECT Guide - ORDER BY 컬럼 순서와 성능

* ORDER BY A, B의 의미는 다음과 같다.
  * A로 먼저 정렬
  * A가 같은 값끼리 묶인 그룹 안에서 B로 정렬
* 즉, A의 카디널리티가 높을수록 2번에서 다시 정렬할 그룹이 작아진다.
* 그러나 MySQL FileSort는 두 컬럼을 하나의 복합 키로 묶어서 한 번에 정렬한다.
* 그래서 실제로 `ORDER BY A, B`나 `ORDER BY B, A`든 퀵소트 비교 횟수 자체가 거의 동일하다.
* 진짜 차이가 나는 순간은 인덱스가 있을 때 발생하게 된다.
  * 복합 인덱스 `(A, B)`가 있으면 `ORDER BY A, B`는 인덱스 순서를 그대로 읽어 filesort가 생략되지만, `ORDER BY B, A`는 인덱스 순서와 달라 filesort가 발생한다.
  * `ORDER BY A ASC, B DESC`처럼 방향이 섞이면 일반 복합 인덱스로는 filesort를 피할 수 없고, MySQL 8.0의 내림차순 인덱스 `(A ASC, B DESC)`로 해결할 수 있다.

#### MySQL SELECT Guide - GROUP BY 최적화

* MySQL 5.7까지는 GROUP BY가 암묵적으로 정렬(정렬 → 집계)까지 수행했지만, 8.0부터는 암묵적 정렬이 제거되었다. 따라서 정렬된 결과가 필요하면 ORDER BY를 반드시 명시해야 한다. (8.0 업그레이드 시 결과 순서가 바뀌는 대표적 원인)
* MySQL 8.0의 GROUP BY 처리 방식은 다음과 같다.
  * GROUP BY 컬럼 순서와 일치하는 인덱스가 있는 경우 - 인덱스를 순회해 정렬 없이 그룹별 집계(Loose/Tight Index Scan)
  * 인덱스를 사용할 수 없는 경우 - GROUP BY 컬럼을 키로 하는 임시 테이블을 만들어 집계(filesort 발생 가능)
* 커버링 인덱스를 사용하면 대상 row 읽기와 그룹핑을 인덱스만으로 해결할 수 있기에 커버링 인덱스를 많이 사용한다.
* 꼭 커버링 인덱스가 아니더라도 WHERE로 대상 row를 줄이거나 인덱스 컬럼 순서 맞추기, 집계 함수 단순화 등을 통해 GROUP BY절 최적화도 도모할 수 있다.
