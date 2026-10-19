# MySQL - Why You Should Use MySQL: JOIN

#### MySQL JOIN - Nested Loop Join과 Match Function

* Nested Loop Join(중첩 루프 조인)은 이름 그대로 "이중 FOR문"과 동일한 방식으로 작동하는 가장 기본적인 JOIN 알고리즘이다.
* `Driving Table(드라이빙 테이블, Outer)` : 외부 루프를 도는 테이블 - 어느 테이블이 드라이빙이 될지는 옵티마이저가 결정한다.
* `Driven Table(드리븐 테이블, Inner)` : 내부 루프를 도는 테이블
* 시간 복잡도와 비효율성 : 두 테이블의 행이 각각 `N`, `M`개라면 기본적으로 `O(N × M)`을 가진다. 데이터가 많아질수록 급격히 느려지기 때문에, 실전에서는 드리븐 테이블의 조인 컬럼 인덱스를 타는 `Index Nested Loop Join(O(N log M))`이 기본이 되고, 인덱스가 없을 때는 조인 버퍼를 활용하는 `Block Nested Loop Join`이 된다. MySQL 8.0.18부터는 `Hash Join`으로 대체되었다. 

```sql
-- Nested Loop Join (LEFT JOIN 기준 의사코드)
FOR EACH row_left IN left_table:
    matched = false

    FOR EACH row_right IN right_table:
        IF match_function(row_left, row_right) IS TRUE:
            OUTPUT (row_left + row_right)
            matched = true
    IF matched = false:
        OUTPUT (row_left + NULL)   -- NULL 패딩: OUTER JOIN 전용, INNER JOIN이면 이 분기가 없다
```
```sql
-- ON : Match Function
SELECT u.id, u.name, p.title
FROM users u
LEFT JOIN posts p ON u.id = p.user_id;
```
* `ON u.id = p.user_id` 부분이 바로 알고리즘 내부의 `match_function(row_left, row_right)` 역할을 한다.
* Match Function의 평가 결과는 `TRUE` / `FALSE` 두 가지가 아니라 `TRUE` / `FALSE` / `UNKNOWN` 세 가지다(3-Valued Logic). 조인 컬럼이 `NULL`이면 비교 결과가 `UNKNOWN`이 되고, 결합은 `TRUE`인 행만 이루어진다.(그래서 NULL끼리는 =로 조인되지 않는다)

#### MySQL JOIN - Hash Join (MySQL 8.0.18+)

* 조인 컬럼에 인덱스가 없는 동등 조인(equi-join)에서 Nested Loop의 `O(N × M)`을 피하기 위한 알고리즘으로, MySQL 8.0.18에 도입되어 8.0.20부터 Block Nested Loop을 완전히 대체했다.
* 동작 : 두 테이블 중 작은 쪽으로 메모리에 해시 테이블을 만들고(build), 큰 쪽 테이블을 스캔하며 해시 테이블을 조회(probe)한다 → 각 테이블을 한 번씩만 스캔하므로 `O(N + M)`.
* 해시 테이블은 `join_buffer_size` 안에 만들어지며, 넘치면 디스크 임시 파일로 분할(spill)되어 느려진다.
* 의미 : 조인 컬럼에 인덱스가 없으면 무조건 재앙이던 상황이 8.0부터는 상당히 완화되었다. 단, 동등 조인이 아닌 조건(<, LIKE 등)에는 적용되지 않는다. `EXPLAIN FORMAT=TREE`로 hash join 사용 여부를 확인할 수 있다.

#### MySQL JOIN - ON vs WHERE 평가 시점과 LEFT JOIN의 NULL 처리

```sql
FOR EACH row_left IN users:
    matched = false
    FOR EACH row_right IN posts:
        IF match_function(row_left, row_right):  -- ← ON 절이 여기서 평가
            OUTPUT (row_left + row_right)
            matched = true
    IF matched = false:
        OUTPUT (row_left + NULL)  -- ← NULL 패딩은 ON 절 평가 후
```
* `ON`절은 조인하는 동안(NULL 패딩이 만들어지기 전에) 평가되고, `WHERE`절은 조인 결과가 다 만들어진 후에 평가된다. 옵티마이저는 결과가 같다고 보장되면 조건을 앞당겨 실행하기도 한다.
* INNER JOIN에서는 조건을 `ON`에 두든 `WHERE`에 두든 결과와 실행 계획이 동일하다. NULL 패딩이 없으므로 두 평가 시점의 차이가 드러나지 않고, 옵티마이저도 동일하게 취급한다. * 차이가 발생하는 것은 OUTER JOIN에서만이다. LEFT JOIN 후 WHERE절에서 오른쪽 테이블의 컬럼에 조건을 걸면, NULL 패딩 행의 비교 결과가 3-Valued Logic에 의해 UNKNOWN이 되고 WHERE는 TRUE만 통과시키므로 걸러진다. 따라서 LEFT JOIN으로 얻었던 NULL 패딩 행들이 사라져 INNER JOIN처럼 동작하게 된다.

```sql
-- 의도 : '공지' 게시글만 붙이되, 게시글이 없는 유저도 결과에 유지

-- ❌ WHERE에 오른쪽 테이블 조건 → 게시글 없는 유저가 통째로 사라짐 (INNER JOIN화)
SELECT u.id, u.name, p.title
FROM users u
LEFT JOIN posts p ON u.id = p.user_id
WHERE p.category = 'notice';

-- ✅ 방법1 : 오른쪽 테이블 조건을 ON 절로 이동 (NULL 패딩 전에 posts만 필터링)
SELECT u.id, u.name, p.title
FROM users u
LEFT JOIN posts p ON u.id = p.user_id AND p.category = 'notice';

-- ✅ 방법2 : WHERE에 둬야 한다면 NULL 허용 조건을 함께 명시
SELECT u.id, u.name, p.title
FROM users u
LEFT JOIN posts p ON u.id = p.user_id
WHERE p.category = 'notice' OR p.id IS NULL;
```

#### MySQL JOIN - 다중 1:N JOIN의 집계 왜곡과 사전 집계(CTE)

```sql
SELECT
    u.id,
    u.name,
    COUNT(p.id) AS post_count,
    COUNT(c.id) AS comment_count
FROM users u
    LEFT JOIN posts p ON u.id = p.user_id         -- u.id = 1인 사람이 게시글 2개
    LEFT JOIN comments c ON u.id = c.user_id      -- u.id = 1인 사람이 댓글 3개
GROUP BY u.id, u.name;
```
* 데이터베이스는 GROUP BY로 숫자를 세기 전에 먼저 LEFT JOIN을 두 번 수행하여 데이터를 하나로 합친다. 이 때, `posts`와 `comments`는 서로 무관한 1:N 관계이므로 `u.id = 1`의 게시글 2개와 댓글 3개가 곱해지면서(2 × 3) 총 6줄의 중간 결과가 만들어진다.
* 집계(COUNT)의 왜곡 : 데이터베이스는 이 6개의 행 위에서 집계 함수를 실행하는데, 데이터가 이미 복제되어 늘어나 있으므로 게시글 수와 댓글 수 모두 6으로 잘못 계산되어 버린다.
* 임시방편으로 `COUNT(DISTINCT p.id)`, `COUNT(DISTINCT c.id)`를 쓰면 결과는 맞출 수 있지만, 곱해진 중간 결과를 만든 뒤 중복 제거하는 비용은 그대로이므로 데이터가 크면 여전히 느리다.
```sql
-- CTE로 사전 집계 (WITH 구문은 MySQL 8.0부터 지원, 5.7 이하는 파생 테이블(서브쿼리)로 동일 패턴 가능)
WITH user_base AS (
    SELECT id, name FROM users
),
     post_stats AS (
         SELECT user_id, COUNT(*) AS post_count
         FROM posts
         GROUP BY user_id
     ),
     comment_stats AS (
         SELECT user_id, COUNT(*) AS comment_count
         FROM comments
         GROUP BY user_id
     )
SELECT
    u.id,
    u.name,
    COALESCE(p.post_count, 0) AS post_count,       -- 기대값 : 2
    COALESCE(c.comment_count, 0) AS comment_count  -- 기대값 : 3
FROM user_base u
         LEFT JOIN post_stats p ON u.id = p.user_id
         LEFT JOIN comment_stats c ON u.id = c.user_id;
```
* CTE 쿼리는 먼저 다 세어놓고(COUNT) 나중에 이어 붙인다(JOIN).
* 각각의 1:N 테이블을 `GROUP BY`로 요약해서 1:1 관계로 차원 축소시킨 뒤에 메인 테이블에 조인하는 것이 실무에서 다중 1:N 통계를 낼 때 사용하는 가장 정확하고 정석적인 방법이다. * 집계가 없는 통계 대상이어도, 조인 결과 행 수가 users 행 수 × 1로 고정되므로 성능 예측도 쉬워진다.
