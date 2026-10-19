# MySQL - Learning Data Modeling Through Practical Examples

#### Data Modeling: Logical Design for Boolean Attributes

* 논리 모델링에서 `Boolean(True/False)` 타입은 양날의 검과 같다.
* 비즈니스 요구사항이 정말 단순한 Yes/No로 완벽하게 설명될 때는 최고의 선택이지만, 복잡한 개념을 억지로 압축하려고 할 때는 미래의 기술 부채(Technical Debt)가 된다.
* 참고로 MySQL의 `BOOLEAN`은 독립된 타입이 아니라 `TINYINT(1)`의 별칭이며, `TRUE/FALSE`는 `1/0`으로 저장된다.

> 🟢 단순 Yes/No라면 쓰는 것이 좋다(Good Cases)

* 비즈니스 로직상 더 이상의 부가 정보가 절대 필요 없는 순수한 상태나 플래그(Flag)일 때는 Boolean이 가장 직관적이고 효율적이다.
  * `is_deleted(논리적 삭제 여부)` : 데이터베이스에서 실제로 레코드를 지우지 않고 숨김 처리만 할 때 사용한다. "삭제되었는가?"에 대한 답은 철저하게 O/X로 나뉜다.
  * `is_email_verified(이메일 인증 여부)` : 인증을 했거나, 안 했거나 둘 중 하나이다.
  * `is_default_address(기본 배송지 여부)` : 여러 배송지 중 기본 배송지인지 판별하는 순수한 논리적 플래그이다.
* 이런 경우 `Boolean` 속성은 모델을 단순하게 유지해 주고 쿼리의 가독성을 높여주는 아주 좋은 설계이다.

> 🔴 함축적으로 가져가면 안 좋은 경우(Bad Cases)

* 현재는 `Yes/No`처럼 보이지만, 속성 내부에 더 복잡한 의미나 '확장 가능성'이 숨어있는 경우이다.
  * `has_discount(할인 적용 여부)` : 처음 기획할 때는 "이 상품 할인해?"라는 질문에서 시작하여 Boolean으로 만들기 쉽다. 하지만 서비스가 성장하면 필연적으로 "할인율은 몇 %야?", "할인 기간은 언제까지야?", "쿠폰 할인이야, 정률 할인이야?" 같은 요구사항이 붙는다. 이 경우 Boolean 대신 '할인(Discount)'이라는 별도 테이블이나 더 구체적인 속성들로 도출했어야 한다.
  * `is_premium_member(프리미엄 회원 여부)` : "유료 회원인가요?"라는 질문엔 Yes/No가 맞지만, 나중에 '골드', '플래티넘' 같은 등급(Tier)이 추가되는 순간 이 컬럼은 쓸모가 없어진다. 처음부터 membership_level(VARCHAR/ENUM 등)이나 별도의 등급 엔티티로 설계하는 것이 안전하다.
  * `has_wifi(와이파이 유무)` : 식당 정보에서 has_wifi가 True라고 해보자. 사용자가 "비밀번호는 뭔가요?"라고 묻는다면 이 데이터 모델은 대답할 수 없다.
* 판단 기준 : "이 속성에 대해 다음 질문이 나올 수 있는가?"라는 질문에 대해 나올 수 있다면 Boolean이 아니라 엔티티(테이블)나 구체적 속성이 정답이다.

#### Data Modeling: Physical Design Strategy – One Table per Anchor

```sql
CREATE TABLE restaurants (
    restaurant_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200),
    phone VARCHAR(20),
    has_parking BOOLEAN DEFAULT FALSE,
    has_wifi BOOLEAN DEFAULT FALSE,
    has_delivery BOOLEAN DEFAULT FALSE,
    is_vegetarian_friendly BOOLEAN DEFAULT FALSE,
    has_outdoor_seating BOOLEAN DEFAULT FALSE,
    has_live_music BOOLEAN DEFAULT FALSE,
    accepts_credit_card BOOLEAN DEFAULT FALSE,
    is_24_hours BOOLEAN DEFAULT FALSE,
    has_rooftop BOOLEAN DEFAULT FALSE
);
```

* 장점 : JOIN이 필요 없고 별도 테이블이 필요 없다. 초기 개발 단계에서는 굉장히 큰 장점을 가진다.
* 단점 : 유지보수 및 확장성 관점에서는 굉장히 불리하다. 
  * 속성이 추가될 때마다 운영 중인 테이블에 ALTER TABLE(스키마 변경)이 필요하다.
  * 속성이 늘수록 컬럼 수가 계속 불어나고, 대부분의 행에서 의미 없는 기본값(FALSE) 컬럼이 쌓이는 와이드 테이블이 된다.

#### Data Modeling: Physical Design Strategy – Side Tables

```sql
-- 메인 테이블: 핵심 비즈니스 정보만 포함
CREATE TABLE restaurants (
    restaurant_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200),
    phone VARCHAR(20),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Side Table: Boolean 속성들을 별도 테이블로 분리 (PK = FK 공유로 1:1 관계 강제)
CREATE TABLE restaurant_flags (
    restaurant_id INT PRIMARY KEY,
    has_parking BOOLEAN DEFAULT FALSE,
    has_wifi BOOLEAN DEFAULT FALSE,
    has_delivery BOOLEAN DEFAULT FALSE,
    is_vegetarian_friendly BOOLEAN DEFAULT FALSE,
    has_outdoor_seating BOOLEAN DEFAULT FALSE,
    has_live_music BOOLEAN DEFAULT FALSE,
    accepts_credit_card BOOLEAN DEFAULT FALSE,
    is_24_hours BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id)
);

-- 두 테이블에 걸친 변경은 하나의 트랜잭션으로 묶어야 정합성이 보장된다
-- (스프링에서는 @Transactional 선언이 이 begin/commit 역할을 한다)
begin;

UPDATE restaurants SET name = 'New Name' WHERE restaurant_id = 1;
UPDATE restaurant_flags SET has_parking = true WHERE restaurant_id = 1;

commit;
```

* 장점 : 메인 테이블의 경량화 및 I/O 성능 향상, 메인 테이블 잠금 및 장애 리스크 감소, 확고한 데이터 정합성 보장
* 단점 : 조회 시 필연적인 JOIN 오버헤드, 근본적인 스키마 유연성 문제 미해결(속성 추가 시 여전히 ALTER 필요), 애플리케이션 로직의 복잡도 증가
  * 주의 : `flags` 행이 아직 없는 식당이 있을 수 있으므로 조회는 `LEFT JOIN`을 쓰거나, 메인 행 INSERT 시 `flags` 행도 같은 트랜잭션에서 함께 만들어 1:1을 항상 유지하는 규칙을 정해야 한다.

#### Data Modeling: Physical Design Strategy – Entity-Attribute-Value(EAV)

```sql
-- 메인 엔티티 테이블
CREATE TABLE restaurants (
    restaurant_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200),
    phone VARCHAR(20),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- EAV 테이블: 속성을 행으로 저장
CREATE TABLE restaurant_attributes (
    restaurant_id INT NOT NULL,
    attr_name VARCHAR(50) NOT NULL,
    attr_value VARCHAR(100),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (restaurant_id, attr_name),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id)
);

-- 'test' 같은 오타·비정의 속성도 아무 제약 없이 들어간다 → 정합성 파괴의 실례
INSERT INTO restaurant_attributes (restaurant_id, attr_name, attr_value) VALUES (1, 'test', 'true');
INSERT INTO restaurant_attributes (restaurant_id, attr_name, attr_value) VALUES (1, 'has_parking', 'true');
```

* 장점 : 무한한 확장성과 궁극의 유연성을 가진다 - 속성 추가에 스키마 변경(ALTER)이 전혀 필요 없다.
* 단점 : 데이터 정합성 파괴 가능성이 매우 높으며 쿼리 지옥과 성능 저하를 유발한다.
  * 모든 값이 VARCHAR로 저장되므로 타입이 사라진다. 'true' / '1' / 'yes'가 혼재해도 막을 수 없고, 숫자 비교·정렬마다 캐스팅이 필요하다.
  * `attr_name`의 오타·비정의 속성을 막을 방법이 없다. 방어하려면 속성 정의 테이블 + FK를 추가해야 해서 복잡도가 다시 올라간다.
  * "주차와 와이파이가 모두 되는 식당" 같은 다중 속성 조건 조회는 속성 개수만큼 self-join(또는 GROUP BY + HAVING)이 필요해 쿼리가 급격히 복잡해지고 느려진다.

#### Database Design Deep Dive: Schema Evolution

* ALTER TABLE의 ALGORITHM 옵션
  * `COPY` : 새 테이블을 만들어 데이터를 통째로 복사하는 예전 방식. 복사하는 동안 쓰기가 차단된다.
  * `INPLACE` : 테이블 복사 없이 InnoDB 내부에서 변경. 대부분의 작업에서 동시 DML을 허용한다.
  * `INSTANT` : 데이터 파일은 건드리지 않고 메타데이터만 수정. 행 수와 무관하게 즉시 완료된다.
  * `DEFAULT` : 가능한 가장 가벼운 알고리즘을 MySQL이 자동 선택한다.(INSTANT → INPLACE → COPY 순으로 시도)
* LOCK 옵션
  * `NONE` : 동시 읽기/쓰기 허용
  * `SHARED` : 읽기만 허용
  * `EXCLUSIVE` : 읽기/쓰기 모두 차단
  * `DEFAULT` : 해당 작업이 허용하는 가장 느슨한 락을 자동 선택

```sql
-- 1. 컬럼 추가 : INSTANT 가능 (8.0.12+, 임의 위치 추가·컬럼 삭제는 8.0.29+)
ALTER TABLE restaurants ADD COLUMN memo VARCHAR(100), ALGORITHM=INSTANT;

-- 2. 컬럼 이름 변경 : 메타데이터만 수정하므로 INSTANT 가능
ALTER TABLE restaurants RENAME COLUMN name TO restaurant_name;

-- 3. 데이터 타입 변경 : 데이터 재작성이 필요해 대부분 COPY - 가장 비싼 스키마 변경
ALTER TABLE restaurants
    MODIFY COLUMN restaurant_name VARCHAR(200),
    ALGORITHM=COPY, LOCK=EXCLUSIVE;
```

> **❗ALTER TABLE이 서비스 장애로 이어지는 이유**
>
> * `ALGORITHM=COPY`는 테이블을 복사하는 동안 쓰기를 차단하므로 테이블이 클수록 다운타임이 길어진다. 
> * MySQL 5.6+의 Online DDL(INPLACE/INSTANT)은 대부분 동시 DML을 허용하지만, 그래도 시작·종료 시점에 짧은 메타데이터 락(MDL)을 잡아야 한다. 
> * 이 때, 오래 실행 중인 쿼리나 트랜잭션이 하나라도 있으면 ALTER가 MDL을 기다리고, 그 뒤에 들어오는 모든 쿼리가 ALTER 뒤에 줄줄이 대기하면서 장애처럼 보이는 상황이 온다. 
> * 실무에서는 COPY 자체보다 이 MDL 대기가 더 흔한 사고 원인이다. 대형 테이블의 무중단 변경에는 pt-online-schema-change, gh-ost 같은 외부 도구를 사용한다.

#### Database Design Deep Dive: Modern Alternatives

```sql
-- MySQL JSON 테이블 생성
CREATE TABLE restaurants (
    restaurant_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    attributes JSON
);

-- JSON 데이터 삽입
INSERT INTO restaurants (restaurant_id, name, attributes) VALUES
    (1, 'Pizza Palace', '{"has_parking": true, "has_wifi": true}'),
    (2, 'Burger King', '{"has_parking": false, "has_wifi": true}');
```
* 장점 : 스키마 리스(schema-less) 유연성, 데이터 타입 보존(EAV처럼 전부 문자열이 되지 않고 true/1/"text" 구분이 유지됨), 비정형 데이터의 효율적 관리, 강력한 JSON 내장 함수 지원
* 단점 : 데이터 무결성 제약 부재(키 오타·타입 혼용을 막지 못함), 자주 검색하는 경로마다 인덱스 준비 필요, 통계 및 집계 어려움, 저장 공간 오버헤드
* 검색 성능 보완 : JSON에는 무조건 인덱스를 걸기 어려운 것은 아니다. 자주 조회하는 경로를 생성 컬럼(Generated Column)으로 뽑아 인덱스를 걸 수 있다.

```sql
-- 자주 검색하는 JSON 경로를 가상 생성 컬럼으로 뽑아 인덱스 생성
ALTER TABLE restaurants
    ADD COLUMN has_parking BOOLEAN
        GENERATED ALWAYS AS (attributes->>'$.has_parking' = 'true') VIRTUAL,
    ADD INDEX idx_has_parking (has_parking);

SELECT * FROM restaurants WHERE has_parking = TRUE;  -- 인덱스 사용
```
* 무결성 보완 : CHECK 제약(8.0.16+)과 JSON_SCHEMA_VALID(8.0.17+)를 결합하면 JSON 컬럼에도 스키마 검증을 강제할 수 있다. "스키마 리스"와 "무결성" 사이의 절충안이다.
