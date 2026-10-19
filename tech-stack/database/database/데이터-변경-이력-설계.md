# Database - Data Change History Design

### Database - Data Change History Design

#### 변경 추적 컬럼 - 기본

* `created_at` : 데이터가 처음 등록된 시간
* `created_by` : 데이터를 처음 등록한 사람(또는 시스템)
* `updated_at` : 데이터가 마지막으로 수정된 시간
* `updated_by` : 데이터를 마지막으로 수정한 사람(또는 시스템)
> 데이터가 최초로 등록되어도 수정일을 보통 함께 저장해둔다. 왜냐하면 데이터를 조회하거나 정렬하는 로직을 단순화하기 위해서다. 만약 등록 시점에 수정일을 `NULL`로 비워두면 "가장 최근에 변경된 순서"로 데이터를 조회할 때, 쿼리가 복잡해진다. 생성도 일종의 '첫 번째 수정' 상태로 간주해 값을 채우는 것이 개발 편의성 면에서 훨씬 유리하다.

#### 변경 추적 컬럼 - 변경 사유

* `change_type` : 변경 유형(PRICE_CHANGE, STATUS_CHANGE, STOCK_ADJUST)
* `change_reason` : 변경 사유(자유 형식의 텍스트)
> `change_type`은 시스템을 위한 분류 코드로 사용하고 `change_reason`은 사람을 위한 상세 설명이다.

#### 변경 추적 컬럼 - 감사 컬럼

* `source_system` : 변경이 발생한 시스템(WEB_ADMIN, API, BATCH, MOBILE)
* `client_ip` : 요청이 발생한 클라이언트 IP 주소
> 감사 컬럼은 여러 시스템에서 데이터를 변경하는 경우, 보안 감사가 필요한 경우에 사용한다.

```sql
CREATE TABLE product (
    -- 비즈니스 컬럼
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    price INT NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 기본 변경 추적 컬럼
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,

    -- 변경 사유 컬럼
    change_type VARCHAR(50),
    change_reason VARCHAR(500),

     -- 감사(Audit) 컬럼
    source_system VARCHAR(50),
    client_ip VARCHAR(50)
);
```

#### 현재 테이블로 이력 관리

* 현재 테이블에 이력을 함께 쌓는 방식은 데이터 입력은 쉽지만 특정 시점의 데이터를 조회하거나 통계를 낼 때 쿼리가 매우 복잡해지고 성능 이슈가 발생할 수 있다.
* 이 방식은 시점 조회와 통계 쿼리 작성이 복잡하다는 문제와 성능 상의 문제가 둘 다 존재한다.
* 추가로 하나의 테이블에 너무 많은 데이터를 보유하게 된다.

#### 전체 행 스냅샷 이력 테이블

* 가장 많이 사용하는 패턴으로 현재 테이블과 이력 테이블을 분리하는 것이다.
* 현재 데이터 조회 99% + 이력 조회 1%의 비율로 현재 데이터를 압도적으로 많이 조회한다.
* 현재 데이터 조회 : 일상적인 업무
* 이력 조회 : 문제가 발생했거나 감사 요청이 있거나 특별한 분석이 필요한 경우

```sql
CREATE TABLE product (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    price INT NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL
);

CREATE TABLE product_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    price INT NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    created_by VARCHAR(100) NOT NULL,

    -- 이력 관리 컬럼
    history_created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    history_created_by VARCHAR(100) NOT NULL,
    change_type VARCHAR(50),
    change_reason VARCHAR(500),
    INDEX idx_product_id (product_id),
    INDEX idx_history_created_at (history_created_at)
);
```

|구분|created_at(원본 데이터 유지)|history_created_at(이력 시점)|
|:---:|:---:|:---:|
|의미|원본 테이블의 `createed_at`값|이력 데이터가 이력 테이블에 INSERT되는 순간의 시간|
|값의 변화|변화 X|변경이 발생할 때마다 매번 새로운 시간이 기록된다|
|비고|데이터의 속성에 해당한다|데이터 변경의 타임라인 역할을 한다|

> 전체 행 스냅샷 이력 설계 시 주의사항
> 
> * 쿼리의 복잡도 증가 : 과거 시점 데이터를 완벽하게 복원하려면 항상 현재 테이블과 이력 테이블을 조합해서 봐야 하기 때문에 쿼리가 2배 이상 길어지고 버그가 발생할 확률이 높아진다.<br>
> * 이력의 불연속성 : INSERT 이벤트가 이력 테이블에 없기 때문에 "언제 생겨났는가"에 대한 기록이 이력 테이블에서 누락된다. 이력 테이블은 그 자체로 완결된 타임라인을 가져야 의미가 있다.<br>
> * 개발 생산성 저하 : 저장 공간을 조금 아끼려다가 데이터를 조회하고 분석하는 모든 개발자가 복잡한 로직을 매번 구현해야 한다.<br>
> * 원본 테이블 복원 불가 : 누군가 실수로 이력 테이블에 데이터를 보관하는 것을 깜빡하고 원본 테이블 데이터만 삭제하면 원본 테이블 데이터를 복원할 수 없다.<br>

* 위와 같은 문제점들이 있기에 처음 INSERT하는 순간에도 이력 테이블에 데이터를 똑같이 넣으면 된다.
* 하지만 전체 행 스냅샷 이력 설계는 다음과 같은 한계점을 지니고 있다.
  * 용량 문제 : 전체 행 스냅샷은 변경 시마다 모든 컬럼의 값을 저장한다. 하나의 컬럼만 변경되어도 전체 행이 복사된다.
  * 불필요한 중복 : 변경되지 않은 값들도 모두 중복되어 저장된다.
  * 무엇이 변경되었는지 알기 어렵다 : 전체 행이 저장되기 때문에 "무엇이 변경되었는가"를 파악하려면 이전 행과 비교해야 한다. 그렇게 되면 쿼리가 복잡하고 컬럼이 많아질수록 더 복잡해진다.
* 그럼에도 전체 행 스냅샷을 많이 사용하는 이유는 다음과 같다.
  * 복원이 쉽다 : 특정 시점 전체 상태를 그대로 복원할 수 있다.
  * 쿼리가 단순하다 : 이력 조회, 시점 조회 등이 직관적이다.
  * 다양한 분석이 가능하다 : 전체 데이터가 있기 때문에 어떤 분석이든 가능하다.

#### 컬럼 단위 변경 로그 테이블

* 전체 행 스냅샷 이력 설계에서 언급한 용량 문제를 해결하는 방법 중 하나가 "변경된 컬럼만" 저장하는 것이다.`
```sql
CREATE TABLE product_change_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    old_value VARCHAR(1000),
    new_value VARCHAR(1000),
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(100) NOT NULL,
    change_reason VARCHAR(500),
    INDEX idx_product_id (product_id),
    INDEX idx_changed_at (changed_at),
    INDEX idx_column_name (column_name)
);
```
* 변경된 컬럼만 저장하는 방식은 다음과 같은 장점들이 있다.
  * 용량 절약 : 변경된 컬럼만 저장하기에 용량이 크게 줄어든다.
  * 무엇이 변경되었는가가 명확하다 : 각 로그가 "어떤 컬럼이 어떻게 변경되었는가"를 명확히 보여준다.
* 그러나 다음과 같은 단점들도 있다.
  * 특정 시점 전체 상태를 복원하기 어렵다
  * 통계 쿼리가 매우 어렵다
  * 타입 정보가 손실된다
  * 첫 데이터 처리가 복잡하다

#### 공통 이력 테이블

```sql
CREATE TABLE audit_log (
    audit_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(100) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_data JSON,
    new_data JSON,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(100) NOT NULL,
    source_system VARCHAR(50),
    client_ip VARCHAR(50),
    request_id VARCHAR(100),

    INDEX idx_table_record (table_name, record_id),
    INDEX idx_changed_at (changed_at),
    INDEX idx_changed_by (changed_by)
);
```

* 모든 테이블의 변경 이력을 하나의 테이블에서 관리하는 방식으로 이렇게 되면 하나의 테이블만 관리하면 되므로 매우 편해진다.
* 공통 이력 테이블은 다음과 같은 장점이 있다.
  * 중앙 집중 관리가 가능하다.
  * 구현이 단순하다.
  * 전체 활동을 추적할 수 있다.
* 그러나 공통 이력 테이블은 다음과 같은 단점들도 있다.
  * 특정 테이블의 상세 이력 조회가 불편하다.
  * 시점 복원이 어렵다.
  * 테이블이 매우 비대해진다.
  * 인덱스 효율이 떨어질 수 있다.
