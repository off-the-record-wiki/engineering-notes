# Database - JSON Schema Design

### Database - JSON Schema Design

#### MySQL에서 JSON 사용하기

```sql
CREATE TABLE product_json (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    attributes JSON,
    created_at DATETIME NOT NULL DEFAULT NOW()
);
```
* MySQL은 이 컬럼에 유효한 JSON만 저장되도록 자동으로 검증한다.
* JSON 데이터를 문자열로 직접 삽입하는 경우 JSON 형식을 자유롭게 입력할 수 있다.
* `JSON_OBJECT` 함수 : `JSON_OBJECT` 함수를 사용하면 키-값 쌍을 지정할 수 있다.
* `JSON_ARRAY` 함수 : 배열을 만들 때는 `JSON_ARRAY` 함수를 사용한다.
* `JSON_EXTRACT` 함수 : `JSON_EXTRACT` 함수를 사용하면 JSON에서 특정 경로의 값을 추출할 수 있는데 이 때, 경로는 `$`로 시작하며 `.`으로 키를 지정한다.

#### JSON 인덱스와 성능 최적화

* JSON 내부 데이터도 인덱스를 걸 수 있고 매우 빠르게 조회할 수 있다.
* JSON 데이터의 검색 성능을 최적화하는 두 가지 방법으로 가상 컬럼(Virtual Column) 인덱스와 멀티 밸류(Multi-Valued) 인덱스가 존재한다.

```sql
ALTER TABLE product_json
ADD COLUMN v_storage INT GENERATED ALWAYS AS (attributes->'$.storage') VIRTUAL;
```
* `GENERATED ALWAYS AS ( ... )` : 괄호 안의 식을 통해 값이 자동으로 생성된다는 뜻이다.
* `VIRTUAL` : 이 컬럼은 실제로 디스크에 저장되지 않고 조회할 때만 계산된다. 따라서 추가적인 저장 공간을 거의 차지하지 않는다.
* 이 방식은 두 가지 장점이 있다.
  * 저장 공간 절약 : 데이터를 중복해서 저장하지 않기 때문에 디스크 공간을 아낄 수 있다. JSON 문서가 매우 크고 데이터가 많을수록 장점이 커진다.
  * 데이터 무결성 유지 : 원본 JSON 데이터가 변경되면 가상 컬럼의 값도 자동으로 변경되기 때문에 두 곳의 데이터를 맞추기 위해 별도로 업데이트 로직을 짤 필요가 없다.

#### VIRTUAL vs STORED

* MySQL의 생성 컬럼은 데이터를 저장하는 방식에 따라 VIRTUAL과 STORED 두 가지 타입으로 나뉜다.
```sql
-- VIRTUAL은 기본값으로 생략 가능하다.
ALTER TABLE product_json
ADD COLUMN v_storage INT GENERATED ALWAYS AS (attributes->'$.storage') VIRTUAL;
```
```sql
ALTER TABLE product_json
ADD COLUMN v_storage INT GENERATED ALWAYS AS (attributes->'$.storage') STORED;
```
* 두 방식의 가장 큰 차이는 바로 "데이터를 디스크에 실제로 저장하느냐"이다.
* `VIRUTAL`
  * 데이터를 디스크에 저장하지 않는다.(메타 데이터만 저장)
  * INSERT나 UPDATE 시에도 계산하지 않고 데이터를 SELECT 할 때마다 CPU가 계산해서 보여준다.
  * 저장 공간을 차지하지 않아 효율적이다.
* `STORED`
  * 데이터를 디스크에 물리적으로 저장한다.
  * INSERT나 UPDATE가 될 때 미리 계산해서 저장한다.
  * 저장 공간을 추가로 차지하지만, 읽을 때 CPU 연산이 필요 없다.

|특징|VIRTUAL(가상)|STORED(저장)|
|:---:|:---:|:---:|
|저장 공간|거의 없음|추가 공간 필요|
|값 계산 시점|데이터를 읽을 때|데이터를 쓸 때|
|쓰기 성능|빠름(계산 부하 적음)|느림(계산 저장 및 부하 발생)|
|동기화|원본 변경 시 자동 반영|원본 변경 시 자동 업데이트|
|실무 활용도|높음|낮음|

* `VIRTUAL` 컬럼에 인덱스를 걸면 그 인덱스 정보는 디스크에 물리적으로 저장된다.
* 인덱스를 만드는 순간, 이미 `storage` 값들이 B-Tree 형태로 디스크에 정렬되어 저장된다는 뜻이다.
* 인덱스를 통해 조회할 때는 매번 계산하는 것이 아니라 이미 저장된 인덱스 값을 바로 찾아가므로 `STORED`와 성능 차이가 거의 없다.
* `STORED` 방식은 JSON 추출 비용보다 훨씬 더 복잡하고 무거운 계산이 필요하거나 해당 컬럼을 PK로 설정해야 하는 등의 제약 사항이 

#### JSON 설계와 장단점, 그리고 한계

* JSON 설계가 가져다주는 장단점은 다음과 같다.
* 장점
  * 스키마 유연성 : 스키마 변경 없이 새로운 속성을 추가할 수 있다.
  * 복잡한 중첩 구조 표현 : 관계형 모델에서는 여러 테이블로 분리해야 하는 중첩 구조를 하나의 컬럼에 표현할 수 있다.
  * 개발 생산성 향상 : 애플리케이션에서 객체나 JSON을 그대로 저장하고 조회할 수 있어 개발이 단순해진다.
* 단점
  * 데이터 무결성의 부재 : JSON은 스키마가 없으므로 데이터베이스 수준에서 데이터 무결성을 보장하지 않는다.
  * 참조 무결성 불가 : JSON 내부 값으로는 외래 키를 설정할 수가 없다.
  * 집계와 분석의 어려움 : JSON 데이터에 대한 집계 쿼리 자체는 복잡하고 성능이 떨어진다.
> JSON 분석이 더 비효율적인 이유
> 
> * 파싱(Parsing) 오버헤드 : 일반 컬럼은 해당 위치에 '숫자'가 있다는 것을 이미 알고 있지만 JSON은 매 행마다 JSON 문서를 열고 내부 구조를 순회하여 `$.price`라는 키를 찾는 과정을 필요로 한다. 데이터가 많을수록 찾는 과정 역시 그만큼 반복될 수 밖에 없다.<br>
> * 데이터 용량과 I/O : JSON은 값뿐만 아니라 키(Key) 정보도 함께 저장한다. 단순히 숫자 데이터만 저장하는 것보다 훨씬 더 많은 저장 공간을 차지한다. 분석을 위해 대량의 데이터를 메모리로 로드할 때, 불필요한 정보까지 함께 읽어야 하기 때문에 디스크 I/O와 메모리 효율성이 떨어진다.<br>
> * 결론적으로 대규모 데이터 분석이나 통계가 주 목적이라면 JSON 설계는 별로 권장되지 않는다.<br>

#### 관계형 데이터베이스 vs NoSQL

* MongoDB, CouchDB와 같은 문서 데이터베이스는 JSON과 유사한 문서(Document)를 기본 저장 단위로 사용한다.
* 스키마가 고정되어 있지 않기 때문에 유연하게 데이터를 저장할 수 있다.

#### 관계형 데이터베이스 vs NoSQL - 데이터 모델 측면

* 관계형 DB
  * 테이블, 행, 열로 구성
  * 정규화된 스키마
  * 관계(JOIN)로 데이터 연결
  * 스키마 변경에 따른 ALTER TABLE이 필요
* NoSQL
  * 컬렉션, 문서로 구성
  * 유연한 스키마
  * 임베딩 또는 참조로 데이터 연결
  * 스키마 변경이 자유로움

#### 관계형 데이터베이스 vs NoSQL - 트랜잭션과 일관성

* 관계형 DB
  * 강력한 ACID 트랜잭션
  * 다중 테이블 트랜잭션 지원
  * 강한 일관성 보장
* NoSQL
  * 기본적으로 단일 문서 원자성
  * 다중 문서 트랜잭션은 제한적으로 지원
  * 최종 일관성 모델

#### 관계형 데이터베이스 vs NoSQL - 확장성

* 관계형 DB
  * 수직 확장 중심
  * 수평 확장은 복잡하다.
  * 읽기 확장은 복제로 가능
* NoSQL
  * 수평 확장 용이
  * 내장된 샤딩 지원
  * 분산 환경에 최적화
