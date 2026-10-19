# MySQL - Basic CRUD in MySQL

#### CREATE 쿼리문 최적화

* 한 문장에 여러 행을 넣으면 행 하나씩 넣는 것보다 훨씬 빠르다.

```sql
INSERT INTO posts (user_id, category_id, title, content, status) VALUES
     (1, 1, '제목1', '내용1', 'PUBLISHED'),
     (1, 2, '제목2', '내용2', 'PUBLISHED'),
     (2, 1, '제목3', '내용3', 'DRAFT');

-- Prepared Statement로 반복 INSERT 최적화
PREPARE stmt_insert_comment FROM
    'INSERT INTO comments (post_id, user_id, content) VALUES (?, ?, ?)';
```

* 빨라지는 이유 — 단건 INSERT를 N번 하면 아래 비용이 N배로 든다:

| 비용 | 단건 × N | 다중 행 1회 |
| --- | --- | --- |
| 네트워크 왕복(round-trip) | N회 | **1회** |
| SQL 파싱 | N회 | **1회** |
| 커밋/디스크 플러시(autocommit 시) | N회 | **1회** |

> * 주의 : 무한정 키우면 안 된다. 한 INSERT의 크기는 `max_allowed_packet`(기본 64MB) 제한을 받는다. 수십만 건을 한 방에 넣지 말고 500~1000행 단위 배치로 쪼개는 게 안정적이다.

#### 트랜잭션으로 묶기

* `autocommit` 상태에서 `INSERT`를 한 건씩 날리면 매번 커밋 = 매번 디스크 `fsync`가 발생한다. 여러 건을 한 트랜잭션으로 묶으면 커밋(플러시)이 1회로 줄어 대량 삽입 성능이 급상승한다.
```sql
START TRANSACTION;

-- 1. 게시글 작성
INSERT INTO posts (user_id, category_id, title, content, status)
VALUES (1, 1, '새 게시글', '내용', 'PUBLISHED');

SET @new_post_id = LAST_INSERT_ID();

-- 2. 첫 댓글 자동 생성 (같은 트랜잭션 → 원자적으로 함께 커밋/롤백)
INSERT INTO comments (post_id, user_id, content, status)
VALUES (@new_post_id, 1, '게시글을 작성했습니다.', 'APPROVED');

COMMIT;
```
* 다중 테이블에 걸친 삽입은 원자성(둘 다 성공 or 둘 다 롤백)이 필요하므로 트랜잭션이 필수다.
* Spring에서는 `@Transactional`(AOP 기반 선언적 트랜잭션)로 같은 효과를 얻는다.
* 트랜잭션은 짧게. 오래 열어두면 락 보유 시간과 언두 로그가 늘어 동시성이 떨어진다.

> * JDBC 필수 옵션 — `rewriteBatchedStatements=true` 이 옵션이 없으면 `PreparedStatement.executeBatch()`가 내부적으로 한 건씩 전송된다. `true`로 켜야 드라이버가 다중 행 `INSERT`로 재작성해 실제 배치 이득이 난다. MyBatis \<foreach> 배치를 쓰더라도 이 옵션이 꺼져 있으면 효과가 반감된다.

#### Prepared Statement

* 같은 형태의 INSERT를 값만 바꿔 반복할 때, 문장을 미리 준비(prepare)하고 파라미터만 바인딩한다.

```sql
PREPARE stmt_insert_comment FROM
    'INSERT INTO comments (post_id, user_id, content) VALUES (?, ?, ?)';
```

* SQL 처리 4단계와 Prepared Statement가 절감하는 부분:

| 단계 | 내용 | Prepared Statement |
| --- | --- | --- |
| 1. Parse(파싱) | SQL 문자열 → 토큰 → 문법 검증 → AST | **재사용됨 ✅** |
| 2. Validate(유효성) | 테이블/컬럼 존재·타입·권한을 카탈로그로 확인 | 재사용됨 ✅ |
| 3. Optimize/Plan | 옵티마이저가 실행 경로 결정(인덱스·조인 순서 등) | **⚠️ DBMS·버전·파라미터에 따라 매번 재수행될 수 있음** |
| 4. Execute(실행) | 실제 데이터 읽기/쓰기 | 매번 수행 |

> * 파싱(1단계)은 확실히 절감되지만, 플랜(3단계)은 MySQL에서 EXECUTE마다 재최적화되는 경우가 많다. PostgreSQL은 5회 실행 후 generic plan으로 전환하는 조건부 캐싱이다.
> * 캐시된 generic plan이 데이터 편중(parameter sniffing) 때문에 오히려 더 느릴 수도 있다.
> * Prepared Statement의 1순위 이점은 성능이 아니라 **SQL Injection 방어**에 있다. 반복 실행 시 파싱·네트워크 절감은 부차적 보너스로 보는 게 정확하다.

#### Upsert & 중복 처리

1. `INSERT IGNORE` — 오류를 삼켜 건너뛰기

```sql
INSERT IGNORE INTO users (username, email, password_hash, display_name)
VALUES
    ('john_doe', 'john@example.com',  SHA2('pass1', 256), 'John'),
    ('jane_doe', 'jane@example.com',  SHA2('pass2', 256), 'Jane'),
    ('john_doe', 'john2@example.com', SHA2('pass3', 256), 'John2');  -- 중복 → 조용히 스킵
```

> * 주의 : "중복만" 건너뛰는 게 아니다. `IGNORE`는 중복 키뿐 아니라 타입 변환 오류·NOT NULL 위반·데이터 절삭 등 모든 오류를 경고로 낮춰 삼킨다. 데이터 문제가 조용히 묻힐 수 있으므로, "중복 스킵"이 목적이면 의도를 명확히 하고 유니크 제약을 진짜 방어막으로 둔다. 
> * 이식성 : PostgreSQL은 `INSERT ... ON CONFLICT (col) DO NOTHING` — 대상이 중복 키로 한정돼 더 안전하다.

2. `ON DUPLICATE KEY UPDATE` — 진짜 Upsert(권장)

* 유니크/PK 충돌 시 지정한 컬럼만 갱신한다. 삭제가 없어 안전하다.

```sql
-- 중복 시 특정 컬럼 업데이트
INSERT INTO users (username, email, password_hash, display_name, login_count)
VALUES ('john_doe', 'john@example.com', SHA2('pass', 256), 'John Doe', 1)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    login_count  = login_count + 1,           -- 기존 값 기반 증감
    updated_at   = CURRENT_TIMESTAMP;

-- 복잡한 업데이트 로직 (기존 내용에 이어붙이기 + 카운터 누적)
INSERT INTO posts (user_id, category_id, title, content, view_count)
VALUES (1, 1, '제목', '내용', 1)
ON DUPLICATE KEY UPDATE
    content    = CONCAT(content, '\n--- 업데이트됨 ---\n', VALUES(content)),
    view_count = view_count + VALUES(view_count),
    updated_at = CURRENT_TIMESTAMP;
```

> * 이식성 : [PostgreSQL은 `ON CONFLICT (col) DO UPDATE SET ... = EXCLUDED.col.`](https://www.postgresql.org/docs/current/sql-insert.html)
> * 이식성 : VALUES(col)은 "INSERT하려던 값"을 가리킨다. [그런데 공식 문서상 VALUES()는 MySQL 8.0.20부터 deprecated(향후 제거 예정)이므로, 행 별칭 문법(8.0.19+)으로 대체하는 것을 권장한다.](https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html)

3. `REPLACE INTO` — 되도록 피하라

```sql
REPLACE INTO users (user_id, username, email, password_hash, display_name)
VALUES (1, 'john_updated', 'john_new@example.com', SHA2('newpass', 256), 'John Updated');
```

> * 주의 : REPLACE는 UPDATE가 아니라 DELETE + INSERT다.
> * 삭제 후 재삽입이라 AUTO_INCREMENT id가 새로 발급 → 그 id를 참조하던 FK가 깨지고, `ON DELETE CASCADE`가 걸려 있으면 자식 행이 함께 삭제된다.
> * `INSERT`에 명시하지 않은 컬럼은 `DEFAULT`로 초기화 → 조용한 데이터 유실.
> * `DELETE·INSERT` 트리거가 둘 다 발동한다.

#### INSERT ... SELECT

* 다음과 같이 다른 테이블에서 복사한다.
* 애플리케이션으로 데이터를 꺼냈다 다시 넣지 않고 DB 안에서 한 번에 처리 → 네트워크·메모리 절약할 수 있다.

```sql
INSERT INTO posts (user_id, category_id, title, content, status)
SELECT
    user_id,
    1 AS category_id,
    CONCAT('임시 제목 - ', user_id) AS title,
    '자동 생성된 내용' AS content,
    'DRAFT' AS status
FROM users
WHERE status = 'ACTIVE'
  AND login_count > 10;
```

#### EXISTS를 이용한 조건부 삽입

* 원자적이지 않다.
* `EXISTS` 검사와 실제 `INSERT` 사이에 다른 트랜잭션이 끼어들면 두 세션이 모두 조건을 통과하게 되어 중복이 생길 수 있다.
* 진짜 중복 방지는 유니크 제약으로 처리해야 하고, `EXISTS`는 불필요한 삽입을 줄이는 보조 수단으로 가져가야 한다.

```sql
INSERT INTO comments (post_id, user_id, content)
SELECT 1, 2, '댓글 내용'
WHERE EXISTS (SELECT 1 FROM posts WHERE post_id = 1 AND status = 'PUBLISHED')
  AND NOT EXISTS (
        SELECT 1 FROM comments
        WHERE post_id = 1 AND user_id = 2
          AND created_at > DATE_SUB(NOW(), INTERVAL 1 MINUTE)   -- 도배 방지
      );
```

#### 대량 적재(bulk load)

* 수만~수백만 건을 넣을 때는 INSERT 반복보다 전용 경로가 빠르다.
* `LOAD DATA INFILE` — 파일에서 직접 적재. 다중 행 INSERT보다도 훨씬 빠른 대량 적재 표준이다.
* 적재 중 임시로 검사를 끄면 더 빨라진다(적재 후 반드시 복구·검증)

```sql
SET unique_checks = 0;
SET foreign_key_checks = 0;
-- ... 대량 INSERT / LOAD DATA ...
SET unique_checks = 1;
SET foreign_key_checks = 1;
```

* PK 순서로 정렬해 삽입하면 B-Tree 페이지 분할이 줄어 InnoDB 적재가 빨라진다.

#### 생성 키 받기

```sql
INSERT INTO posts (...) VALUES (...);
SELECT LAST_INSERT_ID();          -- 방금 이 커넥션에서 생성된 AUTO_INCREMENT 값
```
* 커넥션(세션) 단위로 안전하다. 다른 세션의 삽입에 영향받지 않는다.
* 다중 행 INSERT 에서는 첫 번째 행의 id를 반환한다(나머지는 연속값 가정).
* JDBC에서는 `Statement.RETURN_GENERATED_KEYS` + `getGeneratedKeys()`로 받는다.

> * 이식성 : [PostgreSQL·MariaDB는 `INSERT ... RETURNING id`로 별도 왕복 없이 생성 키를 바로 받는다.](https://mariadb.com/docs/server/reference/sql-statements/data-manipulation/inserting-loading-data/insertreturning)
> * 이식성 : [MySQL은 RETURNING 미지원 → LAST_INSERT_ID() 사용](https://dev.mysql.com/doc/refman/8.0/en/insert.html)

#### SELECT 쿼리 최적화 - EXPLAIN으로 측정한다.

| 항목 | 나쁜 신호 | 좋은 신호 |
| --- | --- | --- |
| `type` | `ALL`(풀스캔) | `const` / `eq_ref` / `ref` / `range` |
| `rows` | 실제 결과보다 훨씬 큰 값(많이 읽고 버림) | 결과 수에 근접 |
| `key` | `NULL`(인덱스 미사용) | 의도한 인덱스 이름 |
| `Extra` | `Using filesort`, `Using temporary` | `Using index`(커버링) |

* `Using filesort`, `Using temporary`가 보이면 정렬/그룹핑을 인덱스로 흡수할 여지가 있다는 신호다.

#### LIKE 검색 - 접두사 vs 앞 와일드카드

```sql
-- ✅ 접두사 검색: 인덱스 range scan 가능 ('john' 으로 시작하는 구간만)
SELECT user_id, username
FROM users
WHERE username LIKE 'john%' AND status = 'ACTIVE';

-- ❌ 앞 와일드카드: 인덱스 무력화 → 풀 테이블 스캔
SELECT post_id, title
FROM posts
WHERE status = 'PUBLISHED'
  AND (
        title   LIKE '%데이터베이스%'
     OR content LIKE '%성능%'
      )
ORDER BY created_at DESC;
```

* `LIKE 'john%'` — 시작점이 고정 → 인덱스에서 해당 구간만 스캔. 빠르다.
* `LIKE '%...%'` - 시작점이 열려 있어 정렬 이점이 사라진다. → 결국 모든 행을 훑는다.
* 위의 두 번째 쿼리는 OR + 앞 와일드카드 + %한글% 이 겹친 전형적인 안티패턴이다. 본문(text)에서 부분 문자열을 찾는 이런 요구가 바로 아래 전문 검색이 필요한 이유다.

#### 전문 검색(Full-Text)

```sql
-- 전문 검색 인덱스 활용 (긴 본문에서 LIKE '%...%' 보다 훨씬 효율적)
SELECT post_id, title, content
FROM posts
WHERE MATCH(title, content) AGAINST('MySQL 최적화' IN NATURAL LANGUAGE MODE)
  AND status = 'PUBLISHED';
```
* `title`, `content`처럼 긴 텍스트에서 단어를 찾을 때는 `LIKE '%...%'` 대신 `FULLTEXT` 인덱스 + `MATCH ... AGAINST` 를 쓴다.
* 긴 텍스트를 빠르게 검색하기 위한 기능. InnoDB·MyISAM만 지원하고, `CHAR/VARCHAR/TEXT` 컬럼만 인덱싱한다.
* 하나의 `FULLTEXT` 인덱스에 여러 컬럼을 함께 지정할 수 있다 : `FULLTEXT(title, content)`.

#### 필요한 컬럼만 SELECT + 커버링

* `SELECT *` 지양. 실제 쓰는 컬럼만 나열하면 I/O가 줄고, 커버링 인덱스가 성립할 여지가 생긴다.
* 조회에 필요한 컬럼이 모두 인덱스에 있으면 테이블 접근 없이 인덱스만 읽어 끝난다 → Extra : `Using index`.

```sql
-- (user_id, status, created_at) 인덱스가 세 컬럼을 덮으면 테이블에 안 감 → 커버링
SELECT status, created_at FROM orders WHERE user_id = ?;
```

#### 정렬과 페이지네이션

```sql
-- (status, created_at) 인덱스 → 필터 + 정렬을 한 인덱스로 해결
SELECT post_id, title FROM posts
WHERE status = 'PUBLISHED'
ORDER BY created_at DESC
LIMIT 20;
```
* `ORDER BY` 컬럼이 인덱스 순서와 맞으면 별도 정렬(`Using filesort`)을 생략한다. `WHERE`의 등치 조건 + `ORDER BY` 컬럼을 한 인덱스로 묶는 게 이상적이다.

```sql
-- ❌ OFFSET 방식: 앞의 100,000행을 읽고 버림 → 뒤로 갈수록 급격히 느려짐
SELECT * FROM posts ORDER BY id DESC LIMIT 20 OFFSET 100000;

-- ✅ keyset 방식: 마지막으로 본 id 기준으로 이어서 → 항상 일정하게 빠름
SELECT * FROM posts
WHERE id < :last_seen_id
ORDER BY id DESC
LIMIT 20;
```
* `OFFSET N`은 건너뛸 `N`행을 실제로 읽고 버린다. 뒤 페이지로 갈수록 비용이 선형 증가하므로, 정렬 키(보통 PK나 created_at)를 커서로 삼는 keyset 페이지네이션이 대용량에서 훨씬 유리하다.

#### 조회 고급화 - 윈도우 함수(`RANK` vs `DENSE_RANK` vs `ROW_NUMBER`)

| 함수 | 동점 처리 | 예: 100, 90, 90, 80 |
| --- | --- | --- |
| `ROW_NUMBER()` | 무조건 유일 번호 | 1, 2, 3, 4 |
| `RANK()` | 동점은 같은 순위, 이후 **건너뜀** | 1, 2, 2, 4 |
| `DENSE_RANK()` | 동점은 같은 순위, 이후 **안 건너뜀** | 1, 2, 2, 3 |

* 윈도우 함수는 `WHERE`절에서 거를 수 없다.
* 윈도우 함수는 `WHERE`/`GROUP BY` **이후**에 평가된다. 따라서 순위로 필터하려면 **서브쿼리/CTE로 감싼 뒤** 바깥에서 걸러야 한다.

```sql
-- ❌ 불가: WHERE 절에서 윈도우 함수를 참조할 수 없음
-- SELECT ... , ROW_NUMBER() OVER (...) rn FROM posts WHERE rn <= 3;

-- ✅ 서브쿼리로 감싸 바깥에서 필터
SELECT *
FROM (
    SELECT post_id, category_id, view_count,
           ROW_NUMBER() OVER (PARTITION BY category_id ORDER BY view_count DESC) AS rn
    FROM posts
) t
WHERE rn <= 3;   -- 카테고리별 조회수 상위 3개
```

> * **집계(GROUP BY)와의 차이** : 윈도우 함수는 행을 **줄이지 않고** 각 행에 값을 덧붙인다.
> * `PARTITION BY`는 "그룹 경계"일 뿐 행이 합쳐지지 않는다.

#### CTE(`WITH`)

* MySQL 8.0의 비재귀 CTE는 옵티마이저가 상황에 따라 **머지(merge)** 하거나 **머티리얼라이즈(실체화)** 한다. PostgreSQL(12 미만)처럼 항상 실체화되는 게 아니다.
* **CTE는 가독성 도구이지, 서브쿼리보다 본질적으로 빠른 게 아니다.**
* 실체화되면 그 중간 결과에는 **인덱스가 없어** 오히려 느려질 수 있다. 거대한 중간 집합을 여러 번 조인한다면, 인덱스를 붙일 수 있는 **임시 테이블**이 더 나을 때도 있다.
* 다음과 같이 카테고리 트리·댓글 스레드·조직도 같은 계층 구조는 `WITH RECURSIVE`로 푼다.

```sql
WITH RECURSIVE category_tree AS (
    SELECT id, name, parent_id, 1 AS depth
    FROM categories
    WHERE parent_id IS NULL          -- 최상위(anchor)

    UNION ALL

    SELECT c.id, c.name, c.parent_id, ct.depth + 1
    FROM categories c
    JOIN category_tree ct ON c.parent_id = ct.id   -- 재귀
)
SELECT * FROM category_tree ORDER BY depth;
```
> * 무한 재귀 방지를 위해 `cte_max_recursion_depth`(기본 1000) 제한이 있다. 깊은 트리는 이 값을 조정한다.

#### GROUP BY + JOIN

* 부모에 **1:N 자식**을 조인한 뒤 부모 컬럼을 `SUM`/`COUNT`하면 자식 개수만큼 **중복 집계**된다.

```sql
-- ❌ 버그: order_items 행 수만큼 orders.total 이 중복 합산됨
SELECT o.user_id, SUM(o.total_amount)
FROM orders o
JOIN order_items i ON i.order_id = o.id
GROUP BY o.user_id;

-- ✅ 조인 전에 먼저 집계하거나, 서브쿼리로 분리
SELECT o.user_id, SUM(o.total_amount)
FROM orders o
WHERE EXISTS (SELECT 1 FROM order_items i WHERE i.order_id = o.id)
GROUP BY o.user_id;
```

#### `LEFT JOIN` + `COUNT` 함정

```sql
-- ❌ COUNT(*)는 매칭 안 된 행도 1로 셈 → 댓글 0개 글도 1로 나옴
SELECT p.id, COUNT(*)          FROM posts p LEFT JOIN comments c ON c.post_id = p.id GROUP BY p.id;
-- ✅ 자식 키를 세면 NULL(미매칭)은 제외됨 → 실제 댓글 수
SELECT p.id, COUNT(c.id)       FROM posts p LEFT JOIN comments c ON c.post_id = p.id GROUP BY p.id;
```

#### `WHERE` vs `HAVING`

* 집계 전 필터는 `WHERE`로 내려야 스캔량이 준다. 
* `HAVING`은 집계 결과에만 쓴다.

```sql
-- status 필터는 집계 전이므로 WHERE (HAVING에 두면 불필요하게 다 집계 후 버림)
SELECT user_id, COUNT(*) AS cnt
FROM posts
WHERE status = 'PUBLISHED'          -- 집계 전 필터
GROUP BY user_id
HAVING cnt >= 10;                   -- 집계 후 필터
```

#### EXISTS / NOT EXISTS

* 서브쿼리 결과에 `NULL`이 **하나라도** 있으면 `NOT IN`은 전체가 **빈 결과**를 낸다(3치 논리)
* `NOT IN` + `NULL` 함정 → `NOT EXISTS` 권장한다.

```sql
-- ❌ blocked_user_id 에 NULL이 섞이면 결과가 통째로 사라짐
SELECT * FROM users
WHERE id NOT IN (SELECT blocked_user_id FROM blocks);

-- ✅ NOT EXISTS 는 NULL-safe
SELECT * FROM users u
WHERE NOT EXISTS (SELECT 1 FROM blocks b WHERE b.blocked_user_id = u.id);
```

> * 조건부 삽입/조회에서 `EXISTS` 검사와 실제 동작은 **원자적이지 않다**.
> * 동시성 환경의 중복 방지는 반드시 **유니크 제약**으로 보장한다.

#### 페이지네이션 (keyset)

* `created_at`처럼 **중복 가능한 컬럼**만 커서로 쓰면, 같은 값 경계에서 행이 **누락되거나 중복**된다. → `(created_at, id)` **복합 커서**로 타이브레이커를 둔다.

```sql
-- ✅ (created_at, id) 튜플 비교로 안전하게 이어감
SELECT id, title, created_at
FROM posts
WHERE (created_at, id) < (:last_created_at, :last_id)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

> * 주의 : keyset은 **임의의 N페이지로 점프할 수 없다**(다음/이전 이동에 최적). "10페이지로 바로 가기" UI가 필요하면 offset과의 트레이드오프를 감안한다.

#### WHERE와 안전장치

* `WHERE` 없는 `UPDATE/DELETE` = 테이블 전체 변경/삭제. 반드시 조건을 건다.
* `WHERE` 컬럼에 인덱스가 없으면 풀스캔 + 대량 행에 락이 걸려 동시성이 무너진다. `UPDATE/DELETE`의 성능은 곧 그 `WHERE`의 조회 성능이다.
* 가드레일 — `SQL_SAFE_UPDATES` : 키 없는 `WHERE`로 `UPDATE/DELETE`를 시도하면 막아준다. 규칙이 아니라 설정으로 실수를 강제 차단한다.

```sql
SET SQL_SAFE_UPDATES = 1;   -- 세션에서 키 없는 UPDATE/DELETE 차단
```

#### 조건부 일괄 갱신 (CASE WHEN)

* `CASE WHEN`으로 한 번의 `UPDATE`에 여러 분기를 담을 수 있다.

```sql
UPDATE posts
SET status = CASE
        WHEN view_count < 10    THEN 'DRAFT'       -- 조회수 낮으면 임시저장
        WHEN view_count >= 1000 THEN 'FEATURED'    -- 조회수 높으면 추천글
        ELSE 'PUBLISHED'                           -- 나머지는 일반 발행
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

> **고민 지점 : 어디에 둘 것인가 — 앱 vs DB**
> * 판정 로직을 SQL에 넣을지 말지는 트레이드오프다.
> * 평상시 요청 경로 : 비즈니스 판정은 애플리케이션 계층에 두는 게 좋다(유지보수·테스트·가시성).
> * 대량 일괄 갱신/백필 : 이 경우는 다르다. N행을 앱으로 꺼내 계산 후 되쓰면 왕복·락이 N배다. 이런 set-based bulk update는 CASE WHEN 한 문장이 압도적으로 효율적이다.

#### 대량 변경은 청크로 나눈다 → 작은 배치로 쪼개 실행하고, 배치 사이에 커밋(+짧은 휴지)을 둔다.

* 수십만 ~ 수백만 행을 한 문장으로 `UPDATE/DELETE` 하면 다음과 같은 상황이 연출된다.
* 긴 락 보유 → 다른 트랜잭션 대기한다.
* 거대 트랜잭션 → 언두 로그 팽창, 롤백 비용 폭증한다.
* 복제 지연(replication lag) — 세컨더리가 그 큰 문장을 재생하는 동안 밀린다.

```sql
-- 대량 데이터를 나눠서 업데이트 — 단, "1회"가 아니라 반복 실행해야 완성된다
UPDATE users
SET status = 'INACTIVE'
WHERE last_login_at < DATE_SUB(NOW(), INTERVAL 90 DAY)
  AND status = 'ACTIVE'     -- ✅ 이미 바꾼 행을 자동 제외 → 같은 쿼리 반복이 안전(루프-세이프)
LIMIT 1000;
```

> * 주의 : 위 문장은 1회 실행이 아니라 affected rows가 0이 될 때까지 반복해야 한다. `status = 'ACTIVE'` 조건이 이미 처리된 행을 걸러주므로 같은 쿼리를 반복해도 중복 처리되지 않는다.
> * LIMIT에 ORDER BY가 없으면 어떤 N행인지 비결정적이고, statement 기반 복제에서 unsafe로 경고된다. 더 견고한 방법은 LIMIT보다 PK 범위 청크다. 매 배치가 이미 처리한 구간을 다시 스캔하지 않아 뒤로 갈수록 느려지지 않는다.(PK는 기본적으로 클러스터링 인덱스기 때문에)

#### DELETE — 물리 삭제 전에 따질 것

```sql
DELETE FROM comments
WHERE comment_id = 456   -- WHERE는 필수: 없으면 테이블 전체 삭제
  AND user_id    = 123;
```
* 실무에서는 물리 `DELETE` 대신, 플래그(UPDATE)로 지우는 경우가 많다.
```sql
UPDATE comments SET deleted_at = NOW() WHERE comment_id = 456 AND user_id = 123;
```
* 장점 : 감사(audit)·복구 가능, FK 참조 안전, 실수 되돌리기 쉽다.
* 단점 : 조회마다 `WHERE deleted_at IS NULL` 필요, 데이터 누적 → 주기적 아카이빙/파티션 만료가 이루어져야 한다.

#### 청크 루프 템플릿 (UPDATE/DELETE 공용)

* `LIMIT`의 비결정성을 피하고 뒤로 갈수록 느려지지 않는 PK 범위 청크 방식. 애플리케이션(또는 스토어드 프로시저)에서 아래를 반복한다.

```sql
-- 커서: 직전 배치에서 처리한 마지막 id (@last_id, 시작은 0)
-- 1) 이번 배치의 상한 id 를 구한다 (PK 순서로 1000개 앞)
SELECT id INTO @batch_max
FROM users
WHERE id > @last_id
ORDER BY id
LIMIT 1 OFFSET 999;        -- 1000번째 행의 id (없으면 NULL → 마지막 배치)

-- 2) 그 구간만 변경 (인덱스 범위 → 락 발자국 최소)
UPDATE users
SET status = 'INACTIVE'
WHERE id > @last_id
  AND (@batch_max IS NULL OR id <= @batch_max)
  AND last_login_at < DATE_SUB(NOW(), INTERVAL 90 DAY)
  AND status = 'ACTIVE';

-- 3) 커서 전진 후 커밋. @batch_max 가 NULL 이면 종료.
SET @last_id = @batch_max;
COMMIT;
-- affected rows / @batch_max 를 보고 루프 지속 여부 결정. 배치 사이 짧은 휴지 권장.
```
* PK 범위(`id > @last_id AND id <= @batch_max`)로 자르므로 매 배치가 이전 구간을 다시 스캔하지 않는다.
* DELETE도 `UPDATE ...`를 `DELETE FROM users WHERE ...`로 바꾸면 그대로 적용된다.
* 배치 크기(1000)는 락 시간·복제 지연을 보며 조정한다.
