# Database - SOFT DELETE

### Database - SOFT DELETE

#### SOFT DELETE - is_deleted 컬럼 방식

* `SOFT DELETE`란 데이터를 실제로 삭제하지 않고, 삭제되었다는 표시만 해두는 방식이다.
* 데이터베이스에서 물리적으로 제거하는 대신, "이 데이터는 삭제된 것으로 간주해라"라는 표시를 남기는데 이를 "논리적 삭제"라고 한다.

```sql
CREATE TABLE member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```
* 만약 삭제되지 않은 데이터만 조회를 원한다면 일반적인 조회에서는 삭제된 데이터를 보여주면 안 된다.
* 조회할 때, `is_deleted = FALSE` 조건을 추가한다.

#### SOFT DELETE - deleted_at 컬럼 방식

```sql
CREATE TABLE member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL
);
```
* `is_deleted` 컬럼은 "삭제되었는가?"라는 상태만 저장한다.
* 반면 `deleted_at` 컬럼은 "언제 삭제되었는가?"라는 시점을 저장한다. 그리고 이 시점 정보만으로 삭제 여부도 함께 판단할 수 있다.
* `deleted_at`이 `NULL`이면 삭제되지 않은 것이고, 값이 있으면 그 시점에 삭제되는 것이다.
* 하나의 컬럼으로 삭제 여부와 삭제 시점을 모두 알 수 있다.
* 만약 삭제되지 않은 데이터를 조회하려면 `WHERE deleted_at IS NULL` 조건을 사용한다.

#### SOFT DELETE vs HARD DELETE

* HARD DELETE의 장점은 다음과 같다.
  * 저장 공간 절약 : SOFT DELETE의 경우 삭제된 데이터도 계속 보관하기 때문에 테이블 크기가 계속 증가한다. 반면 HARD DELETE는 데이터를 실제로 제거하기에 저장 공간을 절약할 수 있다.
  * 쿼리 성능 : SOFT DELETE를 사용하면 모든 조회 쿼리에 `WHERE deleted_at IS NULL` 조건이 추가된다. 데이터가 많아지면 이 조건으로 인한 성능 저하가 발생할 수 있다.
  * 단순한 데이터 모델 : SOFT DELETE는 모든 테이블에 `deleted_at` 컬럼을 추가해야 한다. 테이블이 많아지면 관리해야 할 컬럼도 높아진다.
  * 쿼리 작성의 단순함과 실수 방지 : SOFT DELETE의 경우 `deleted_at IS NULL` 조건을 항상 붙여야 하고 HARD DELETE는 데이터가 물리적으로 사라지기 때문에 별도 조건이 필요가 없다.

#### SOFT vs HARD vs STATUS

* 설계 차원에서 '삭제'라는 행위보다 더 구체적인 '상태' 변경이 필요하다.
* 상태 기반 관리 : 비즈니스 프로세스가 복잡하고 데이터 생명주기가 중요한 경우
* SOFT DELETE : 단순히 데이터 존재 유무가 중요하고 복구 가능성만 열어두면 되는 경우
* HARD DELETE : 데이터 보존 가치가 없는 경우
> 모든 테이블에 기계적으로 SOFT DELETE를 적용하기보다 비즈니스 핵심이 되는 데이터는 '삭제'라는 개념보다 '상태 변경'이라는 개념이 훨씬 더 적합하다.

#### SOFT DELETE와 인덱스 설계

* SOFT DELETE를 적용하면 모든 조회 쿼리에 `WHERE deleted_at IS NULL` 조건을 붙이게 되는데 이는 데이터베이스 성능, 특히 인덱스 설계에 큰 영향을 미친다.
* 다음과 같은 문제 상황이 발생할 가능성이 높다.
  * 인덱스 효율성 저하 : 현재 대부분의 데이터가 삭제되지 않은 상태(NULL)라는 점이다. 데이터베이스 입장에서 `deleted_at IS NULL`조건은 변별력이 낮다.
* 이런 경우 인덱스 설계 전략을 다음과 같이 가져가야 한다.
  * 일반적인 경우 : `deleted_at`은 인덱스에 포함하지 않거나 마지막에 둔다.
    * 대부분의 경우 비즈니스 키의 변별력이 훨씬 높기 때문에 기존 인덱스를 그대로 사용해도 무방하다.
    * 데이터베이스 옵티마이저는 name으로 데이터를 거른 뒤, 남은 소수의 데이터 중에서 `deleted_at`을 필터링하는 것이 훨씬 빠르다고 판단하기 때문이다.
  * 커버링 인덱스가 필요한 경우 : 쿼리 성능을 극대화하기 위해 인덱스만으로 쿼리를 끝내는 커버링 인덱스를 사용한다면 `deleted_at`을 인덱스 뒤쪽에 포함시켜야 한다.
    * 커버링 인덱스에서 `deleted_at` 컬럼을 맨 마지막에 놔두게 되면 앞쪽 순서에 위치한 컬럼으로 먼저 구분이 되기 때문에 성능에 문제가 발생할 가능성이 줄어든다.
