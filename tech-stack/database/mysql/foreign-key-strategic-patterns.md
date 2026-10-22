# MySQL - Foreign Key & Strategic Patterns

#### Foreign Key Fundamentals: Data Consistency & Historical Context

* 외래키란, 다른 테이블의 기본키(또는 UNIQUE 키)를 참조하는 컬럼이다. 참조되는 부모 컬럼에는 반드시 인덱스(PK/UNIQUE)가 있어야 한다.
* MySQL은 외래키를 만들 때 자식 테이블의 FK 컬럼에 인덱스가 없으면 자동으로 생성해 준다. FK 체크와 CASCADE 동작이 인덱스를 타야 하기 때문이다.
* MySQL의 예전 기본 엔진 MyISAM은 FK 문법을 받아들이기만 하고 강제하지 않았다. FK가 실제로 동작하는 것은 InnoDB부터이며, InnoDB가 기본 엔진이 된 5.5 이후에야 "MySQL에서 FK는 지켜진다"가 당연해졌다.

```sql
CREATE TABLE users (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE posts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### Enforcing Foreign Keys: Reference Options & Constraints

```sql
CREATE TABLE users (
    id INTEGER NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE posts (
    id INTEGER NOT NULL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE comments (
    id INTEGER NOT NULL PRIMARY KEY,
    post_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);
```
* `ON DELETE CASCADE` : 부모 테이블의 레코드가 삭제될 때 해당 레코드를 참조하는 자식 테이블의 레코드도 자동으로 함께 삭제된다.
* `ON DELETE RESTRICT` : 부모 테이블의 레코드를 삭제하려 할 때 자식 테이블에 참조하는 레코드가 존재하면 삭제 자체를 거부한다. DB 레벨에서 에러를 던지는 방식이다.

> **❗선택 기준**
>
> * 자식 데이터가 부모에 종속적인 생명주기를 가진다면 `CASCADE`, 자식 데이터가 독립적인 의미를 가지거나 실수 삭제를 방지해야 한다면 `RESTRICT`가 적합하다.
> * 참고로 MySQL(InnoDB)에서 `NO ACTION`과 `RESTRICT`는 **완전히 동일하게 동작한다.** 표준 SQL의 `NO ACTION`은 참조 무결성 검사를 트랜잭션 끝으로 지연(deferred)할 수 있다는 의미지만, MySQL은 지연 검사를 지원하지 않고 둘 다 즉시 검사한다.(지연 검사의 차이가 실제로 존재하는 것은 PostgreSQL 등 다른 DBMS다)
> * `CASCADE`로 삭제·갱신된 자식 행에는 **트리거가 발화하지 않는다.** 자식 테이블의 DELETE 트리거로 후처리를 걸어뒀다면 CASCADE 경로에서는 그 로직이 조용히 건너뛰어진다.

```sql
-- RESTRICT: 참조하는 레코드가 있을 때 삭제를 거부
CREATE TABLE posts (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- CASCADE: 참조하는 레코드도 함께 삭제
CREATE TABLE comments (
    id INTEGER PRIMARY KEY,
    post_id INTEGER NOT NULL,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- SET NULL: 참조하는 컬럼을 NULL로 설정 (FK 컬럼이 NULL 허용이어야 사용 가능)
CREATE TABLE orders (
    id INTEGER PRIMARY KEY,
    customer_id INTEGER,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL
);

-- ⚠️ SET DEFAULT: 표준 SQL에는 있지만 MySQL(InnoDB)은 지원하지 않는다
--    파서는 문법을 인식하지만 InnoDB가 테이블 생성 자체를 거부한다
--    같은 효과가 필요하면 SET NULL + 애플리케이션에서 기본값 치환, 또는 트리거로 구현해야 한다

-- CASCADE: 기본키가 변경되면 외래키도 함께 변경
CREATE TABLE posts (
    id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE
);

-- SET NULL: 기본키가 변경되면 외래키를 NULL로 설정
CREATE TABLE orders (
    id INTEGER PRIMARY KEY,
    customer_id INTEGER,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON UPDATE SET NULL
);
```

#### Beyond the Basics: Is Foreign Key Always the Right Choice?

* 외래키는 본질적으로 DB가 애플리케이션을 믿지 않겠다는 선언이다.
* 애플리케이션 코드는 버그가 있고 여러 서비스가 같은 DB를 바라보고 누군가 직접 쿼리를 날리기도 한다. 외래키는 그 모든 경로에서 참조 무결성을 강제한다.
* 이 보호막이 없어진다면 고아 데이터가 언제 어떤 경로로 들어왔는가 추적하기가 어렵다.
* 하지만 외래키가 강제되어야만 한다는 것은 아니다. 외래키를 포기하는 현실적인 이유도 있다.

> **1. 대규모 트래픽에서의 락 경합**

* FK 체크가 잡는 부모 행의 S-Lock은 확인 후 바로 풀리는 것이 아니라 트랜잭션 종료까지 유지된다.
* S-Lock끼리는 호환되므로 댓글 INSERT들이 서로를 막지는 않는다. 문제는 두 가지다.
  * 건당 부모 행 조회(인덱스 탐색) 비용이 INSERT마다 추가된다.
  * 부모 행을 갱신하는 쿼리(댓글 수 카운터 증가, 게시글 수정 등)는 X-Lock이 필요하므로, 인기 게시글 하나에 동시 댓글 + 게시글 갱신이 섞이면 S-Lock ↔ X-Lock 경합과 데드락 위험이 실측으로 드러나게 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1922e6a3-eae7-4755-8e10-c3ed56b10d16.png)

> **2. 수평 확장(샤딩)·파티셔닝과의 충돌**

* 샤드가 분리되는 순간 DB 수준의 외래키는 물리적으로 불가능하다. 대형 서비스가 외래키를 제거하는 가장 흔한 이유이다.
* 같은 DB 안에서도 **MySQL의 파티션 테이블은 외래키를 지원하지 않는다.** 파티셔닝된 테이블은 FK를 가질 수도, 다른 테이블로부터 참조될 수도 없다. 대용량 테이블을 파티셔닝하는 시점에 FK를 포기해야 하는 경우가 생긴다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/aeaf195d-ec78-44df-b3e1-59e7c10c4687.png)

> **3. 이벤트 소싱 / MSA 아키텍처**

* 마이크로서비스 환경에서는 `posts`와 `comments`가 아예 다른 서비스, 다른 DB에 존재한다. 이 경우 참조 무결성은 DB가 아닌 이벤트와 애플리케이션 로직이 담당한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/52e16586-61c1-4898-abed-3088a082ca56.png)

> **4. 배치/마이그레이션 작업**

* 대량 데이터를 INSERT할 때 외래키 체크는 건당 조회를 유발하므로, 마이그레이션에서는 일시적으로 비활성화하는 것이 일반적이다.
* 주의 : `FOREIGN_KEY_CHECKS`를 다시 1로 켜도 **비활성화 동안 들어온 데이터를 소급 검증하지 않는다.** 이 사이에 위반 데이터가 들어왔다면 고아 데이터가 조용히 남는다. 또한 세션 변수이므로 다른 커넥션에는 영향이 없다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3b66f4a6-23bb-411c-bd16-b6c61a38e7e6.png)

> **5. 외래키 없이 갈 때의 보완책**

```sql
-- 고아 데이터 탐지 쿼리 (주기적 배치로 실행)
SELECT c.id
FROM comments c
LEFT JOIN posts p ON p.id = c.post_id
WHERE p.id IS NULL;
```

* FK를 제거하는 것은 무결성 책임을 DB에서 애플리케이션으로 옮기는 것이지, 책임 자체가 사라지는 것이 아니다.
  * 쓰기 경로 : 애플리케이션 레이어에서 참조 검증 (서비스 계층 강제, 직접 쿼리 금지 정책)
  * 사후 검증 : 위와 같은 고아 데이터 탐지 배치를 주기적으로 돌려 위반을 조기에 발견
  * 삭제 전파 : CASCADE 대신 이벤트 기반 후속 처리(soft delete 포함)를 명시적으로 설계
* 결과적으로 **외래키는 선택이지만 무결성은 선택이 아니다.**
