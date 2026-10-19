# Database - Hierarchical Structure Design

### Database - Hierarchical Structure Design

#### 인접 리스트 모델

* 계층 구조를 데이터베이스에 저장하는 가장 직관적이고 널리 사용되는 방법이 바로 인접 리스트 모델(Adjacency List Model)이다.
* 인접 리스트 모델은 각 행이 자신의 부모를 참조하는 방식이다.
  * 각 노드는 자신의 부모 노드를 가리키는 외래 키를 가진다.
  * 최상위 노드는 부모가 없으므로 `NULL`을 가진다.

```sql
CREATE TABLE category (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    PRIMARY KEY (category_id),
    FOREIGN KEY (parent_id) REFERENCES category(category_id)
);
```
* 여기서 `parent_id`가 부모 카테고리를 참조하는 외래키이고 자기 자신의 테이블을 참조하는 자기 참조 관계가 된다. 
* 특정 카테고리의 부모를 조회하려면 셀프 조인을 사용한다.
```sql
SELECT p.*
FROM category c
JOIN category p ON c.parent_id = p.category_id
WHERE c.category_id = 7;
```
* 인접 리스트 모델은 다음과 같은 장점이 있다.
  * 직관적인 구조 : 부모-자식 관계를 외래 키 하나로 표현하므로 이해하기 쉽다.
  * 간단한 데이터 추가/수정/삭제 : 노드를 추가하거나 이동하는 작업이 단순하다.
  * 저장 공간 효율성 : 각 노드당 하나의 행만 저장하므로 저장 공간이 효율적이다.
  * 외래 키 제약 조건 활용 : 데이터베이스 외래 키 제약 조건을 활용하여 참조 무결성을 보장할 수 있다.
* 그러나 인접 리스트 모델은 깊은 계층의 데이터를 한 번에 조회하기 어렵다는 단점이 있다.

> 실제로는 Materialized Path가 권장된다. 읽기가 핵심이고 depth + 페이징이 필요한 경우 Materialized Path가 권장되며 인접 리스트는 구조가 단순하지만 무한 depth 조회를 재귀(CTE)에 의존하게 되는 부분이 약점으로 지목된다.

#### 계층 구조 조회 어려움

#### 방법 1 : 가장 단순한 방법은 다음과 같이 애플리케이션 코드에서 데이터베이스를 여러 번 호출하는 것이다.

```sql
SELECT * FROM category WHERE category_id = 1;

SELECT * FROM category WHERE parent_id = 1;

SELECT * FROM category WHERE parent_id IN (4, 5, 6);

SELECT * FROM category WHERE parent_id IN (7, 8, 9, 10, 11, 12, 13);
```
* 이 방식은 구현이 간단하지만, 계층의 깊이만큼 데이터베이스를 호출해야 한다.
* 계층이 10단계라면 10번의 쿼리가 필요한데 이는 네트워크 비용과 성능 문제가 발생할 수 있다.

#### 방법 2 : JOIN을 사용한 고정 깊이 조회

* 계층의 최대 깊이가 정해져 있다면, JOIN을 사용하여 한 번의 쿼리로 조회할 수 있다.
```sql
SELECT
    c1.category_id AS level1_id,
    c1.name AS level1_name,
    c2.category_id AS level2_id,
    c2.name AS level2_name,
    c3.category_id AS level3_id,
    c3.name AS level3_name
FROM category c1
LEFT JOIN category c2 ON c2.parent_id = c1.category_id
LEFT JOIN category c3 ON c3.parent_id = c2.category_id
WHERE c1.category_id = 1;
```
* 이 방식은 한 번의 쿼리로 결과를 얻을 수 있지만 다음과 같은 문제가 있다.
  * 계층의 깊이가 고정되어야만 한다.
  * 깊이가 늘어나면 그에 따라 JOIN도 늘어나야 한다.
  * 결과가 중복으로 표시되어 처리가 필요하다.

#### 방법 3 : UNION ALL을 사용한 조회

* 각 레벨별로 쿼리를 작성하고 UNION ALL로 합칠 수 있다.
```sql
SELECT category_id, name, parent_id, 1 AS level
FROM category
WHERE category_id = 1

UNION ALL

SELECT category_id, name, parent_id, 2 AS level
FROM category
WHERE parent_id = 1

UNION ALL

SELECT c.category_id, c.name, c.parent_id, 3 AS level
FROM category c
WHERE c.parent_id IN (
SELECT category_id FROM category WHERE parent_id = 1);
```
* 그러나 이 방식도 계층의 깊이만큼 UNION을 추가해야 하므로 유연하지 않다.
* 결과적으로 계층 구조의 깊이가 가변적일 때 대응하기 어렵다는 것이다. 그렇다고 애플리케이션에서 반복 호출하게 되면 무수히 많은 네트워크 호출이 발생하고 결과적으로 성능이 저하된다.
* 이런 문제를 해결하기 위해 SQL 표준에 재귀 쿼리(Recursive Query)가 도입되었고, 이를 CTE(Common Table Expression)로 작성할 수 있다.

#### CTE와 재귀 쿼리

* MySQL 8.0부터 재귀 CTE를 지원하면서 앞서 경험한 문제를 SQL 한 번으로 해결할 수 있게 되었다.
* CTE는 특정 데이터베이스에만 존재하는 독자적인 기능이 아니다.

#### CTE란?

* CTE(Common Table Expression)는 쿼리 내에서 임시로 사용할 수 있는 이름 있는 결과 집합을 말한다.
* `WITH`절을 사용하여 정의하고 해당 SQL 내에만 잠시 존재하는 임시 뷰라고 볼 수 있다.
```sql
WITH top_categories AS (
    -- CTE를 정의하는 쿼리
    SELECT * FROM category WHERE parent_id IS NULL
)
-- CTE를 사용하는 메인 쿼리
SELECT * FROM top_categories;
```
* CTE는 다음과 같은 장점이 있다.
  * 쿼리의 가독성이 좋아진다.
  * 같은 CTE를 여러 번 참조할 수 있다.
  * 재귀 쿼리를 작성할 수 있다.

#### 재귀 CTE 문법

* CTE는 스스로 재귀 쿼리를 작성할 수 있는 재귀 문법을 제공한다.
* 재귀 CTE는 자기 자신을 참조하는 CTE다. `WITH RECURSIVE` 키워드를 사용한다.
```sql
WITH RECURSIVE cte_name AS (

    -- 기본 케이스 (Anchor Member): 재귀의 시작점
    SELECT ...

    UNION ALL

    -- 재귀 케이스 (Recursive Member): 자기 자신을 참조
    SELECT ... FROM cte_name WHERE ...
)
SELECT * FROM cte_name;
```
* 기본 케이스와 재귀 케이스를 구분하는 곳에 UNION 또는 UNION ALL을 반드시 적어주어야 한다.
* 재귀 CTE는 크게 두 부분으로 구성된다.
  * 기본 케이스 : 재귀 시작점. 처음 실행되는 쿼리이다.
  * 재귀 케이스 : CTE 자기 자신을 참조하여 반복 실행되는 쿼리이다.
* 실행 흐름은 다음과 같다.
  * 기본 케이스를 실행하여 초기 결과를 얻는다.
  * 재귀 케이스를 실행하여 새로운 행을 얻는다.
  * 새로운 행이 없을 때까지 2번을 반복한다.
  * 모든 결과를 합쳐서 반환한다.

#### 모든 자손 조회

```sql
WITH RECURSIVE descendants AS (

    -- 기본 케이스: 시작 노드 (전자제품)
    SELECT category_id, name, parent_id, 1 AS depth
    FROM category
    WHERE category_id = 1

    UNION ALL
    
    -- 재귀 케이스: 이전 결과의 자식들을 찾음
    SELECT c.category_id, c.name, c.parent_id, d.depth + 1
    FROM category c
    JOIN descendants d ON c.parent_id = d.category_id
)
SELECT * FROM descendants;
```
* `JOIN descendants`는 자신의 결과를 활용하는 역할을 한다.
* 기본 케이스를 실행하고 그 결과를 새로운 `descendants`에 담는다.
* 재귀 케이스를 실행한다. 이 때, 앞서 실행한 결과였던 `descendants`를 사용한다.
  * 만약 결과가 있다면 결과를 새로운 `descendants`에 담고 재귀 케이스를 다시 실행한다.
  * 만약 결과가 없다면 종료한다.
* 지금까지 기본 케이스, 재귀 케이스에서 반환한 모든 데이터를 새로운 `descendants`에 담아서 반환한다.
* 최종적으로 `SELECT * FROM descendants`를 통해 `descendants`를 출력한다.

#### 특정 깊이까지만 조회

* 재귀 CTE에서 `WHERE`절을 사용하면 특정 깊이까지만 조회할 수 있다.
```sql
WITH RECURSIVE descendants AS (
    SELECT category_id, name, parent_id, 1 AS depth
    FROM category
    WHERE category_id = 1

    UNION ALL
    
    SELECT c.category_id, c.name, c.parent_id, d.depth + 1
    FROM category c
    JOIN descendants d ON c.parent_id = d.category_id
    WHERE d.depth < 2 -- 2단계까지만 재귀(재귀 쿼리에서 깊이를 제한할 때는 "내가 원하는 깊이보다 작다"는 조건을 걸어야 한다)
)
SELECT * FROM descendants;
```

#### 폐쇄 테이블 모델

* 인접 리스트 모델과 CTE 조합으로 대부분의 계층 구조 요구사항을 처리할 수 있다.
* 하지만 다음과 같은 상황에서는 성능 문제가 발생할 수 있다.
  * 카테고리 데이터가 수 만 개 이상
  * 계층의 깊이가 10단계 이상
  * 매우 빈번한 계층 조회
* 이런 상황에서 조회 성능을 극대화하기 위해 폐쇄 테이블 모델을 사용할 수 잇다.
* 폐쇄 테이블 모델은 모든 조상-자손 관계를 미리 계산해서 별도의 테이블에 저장하는 방식이다.
```
인접 리스트 : 노트북 -> 컴퓨터(직속 부모만)
폐쇄 테이블 : 노트북 -> 노트북, 노트북 -> 컴퓨터, 노트북 -> 전자제품
```
* 핵심 아이디어는 조회 시점의 재귀를 저장 시점으로 옮기는 것이다.
* 데이터를 저장할 때 미리 모든 관계를 계산해두면 조회할 때는 단순히 JOIN만으로 매우 빠르게 결과를 얻을 수 있다.

#### 폐쇄 테이블 설계

* 폐쇄 테이블 모델은 2개의 테이블이 필요하다.
* 노드 테이블 : 실제 데이터를 저장
* 경로 테이블 : 모든 조상-자손 관계를 저장
```sql
-- 카테고리 테이블 (노드 테이블)
CREATE TABLE category_closure (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (category_id)
);

-- 경로 테이블 (관계 테이블)
CREATE TABLE category_path (
    ancestor_id BIGINT NOT NULL, -- 조상 노드
    descendant_id BIGINT NOT NULL, -- 자손 노드
    depth INT NOT NULL, -- 거리 (깊이 차이)
    PRIMARY KEY (ancestor_id, descendant_id),
    FOREIGN KEY (ancestor_id) REFERENCES category_closure(category_id),
    FOREIGN KEY (descendant_id) REFERENCES category_closure(category_id)
);
```
* 그러나 폐쇄 테이블에서 만약 자식이 있는 노드를 삭제하려면 자손들의 경로도 함께 삭제해야 한다.
* 이 부분은 복잡하기에 보통 애플리케이션 레벨에서 처리하거나 자식이 있는 노드는 삭제하지 못하도록 제약을 둔다.
* 폐쇄 테이블은 노드를 관리하는 부분에서 복잡함이 있다. 이렇게 관리가 복잡한 것이 폐쇄 테이블 모델의 주요 단점이다.

|항목|인접 리스트|폐쇄 테이블|
|:----:|:----:|:----:|
|저장 공간|효율적(n개 행)|비효율적(O(n^2))|
|직속 자식 조회|빠름|빠름|
|모든 자손 조회|CTE 필요(재귀)|매우 빠름(단순 JOIN)|
|모든 조상 조회|CTE 필요(재귀)|매우 빠름(단순 JOIN)|
|노드 추가|간단|경로 추가 필요|
|노드 삭제|간단|경로 삭제 필요|
|서브트리 이동|매우 간단|복잡|
|구현 복잡도|낮음|높음|

> 언제 폐쇄 테이블을 사용하는 것이 좋은지?
>
> * 조회가 매우 빈번한 경우 : 쓰기보다 읽기가 압도적으로 많은 경우<br>
> * 데이터가 매우 많은 경우 : 카테고리가 수만 개 이상이고 계층 깊이가 깊은 경우<br>
> * 재귀 쿼리를 사용할 수 없는 경우 : 레거시 시스템이나 MySQL 5.7 이하인 경우<br>
