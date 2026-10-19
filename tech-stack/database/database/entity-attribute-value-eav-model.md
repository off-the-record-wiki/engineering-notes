# Database - Entity-Attribute-Value (EAV) Model

### Database - Entity-Attribute-Value (EAV) Model

#### EAV 설계 - 기존 방식 한계와 새로운 접근법

* 상속 관계 설계는 엔티티 간 계층 구조를 표현하는 좋은 방법이지만 끊임없이 늘어나는 속성을 커버하기엔 한계가 있다.
* 상속 관계가 적합한 경우와 적합하지 않은 경우는 다음과 같이 분류할 수 있다.
* 상속 관계가 적합한 경우
  * 서브타입의 종류가 명확하고 제한적이다.(예를 들어, 결제수단 - 카드/계좌이체/가상계좌)
  * 각 서브타입의 속성이 안정적이고 자주 변경되지 않는다.
  * 서브타입별 속성에 대한 제약조건이 중요하다.
  * 서브타입별로 복잡한 조인이나 조회가 자주 필요하다.
* 상속 관계가 적합하지 않은 경우
  * 서브타입이 계속 늘어날 수 있다.
  * 속성이 동적으로 추가, 수정, 삭제되어야 한다.
  * 필요한 속성을 미리 예측하기 어렵다.
  * 유연성이 데이터 무결성보다 더 중요하다.

#### EAV 패턴이란?

* EAV는 3가지 요소로 구성된다.
  * Entity(엔티티) : 속성을 가지는 주체
  * Attribute(속성) : 엔티티가 가지는 특성의 이름
  * Value(값) : 해당 속성의 실제 값
* 전통적인 테이블에서는 Attribute가 컬럼명이 되고 Value가 셀의 값이 된다. EAV에선ㄴ Attribute와 Value 모두 데이터로 저장된다. 즉, 컬럼과 값이 모두 저장되는 것이다.
```sql
-- Entity 테이블
CREATE TABLE product (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL
);

-- Attribute 테이블
CREATE TABLE product_attribute (
    attribute_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    attr_name VARCHAR(100) NOT NULL,
    attr_value VARCHAR(500),
    FOREIGN KEY (product_id) REFERENCES product(product_id)
);

-- 즉, 일대다 관계로 하나의 엔티티에 여러 속성들을 연결시키는 것이다.
```

#### EAV 장단점과 사용 시 주의사항

* EAV 패턴은 강력한 유연성을 제공하지만 그에 따른 대가가 크다. 트레이드 오프를 명확히 이해하고 적절한 상황에서 사용해야 한다.
* 장점
  * 스키마 유연성 : 테이블 구조 변경없이 새로운 속성을 추가할 수 있다.
  * 희소 데이터 효율성 : EAV는 실제 값이 있는 속성만 저장하므로 희소 데이터에 효율적이다.
  * 엔티티별 다른 속성 지원 : 원하는 속성드을 자유롭게 추가할 수 있다.
  * 런타임 속성 정의 : 새로운 속성을 정의하고 바로 사용할 수 있다.
* 단점
  * 쿼리 복잡성 증가 : 여러 속성을 조회하거나 조건으로 검색 시 복잡한 조인이나 서브쿼리가 필요하다.
  * 데이터 타입 제약 부족 : 모든 값이 VARCHAR로 저장되므로 데이터베이스 수준의 타입 검증이 불가능하다.
  * 참조 무결성 제약 어려움 : 외래 키 제약 조건을 걸기 어렵다.
  * 인덱싱 제한 : 특정 속성에 대한 효율적인 인덱스 생성이 어렵다.
  * 조인 성능 저하 : 속성이 많아질수록 다중 조인의 성능이 떨어진다.
