# Spring Batch - SYSTEM DOMINATE

#### Spring Batch의 두 가지 생존 전략

* 대량 데이터를 처리할 때 `SELECT * FROM users WHERE created_at >= '2024-01-01'` 같은 단순한 쿼리를 바로 실행하면 서버 메모리는 순식간에 한계치를 찍고 시스템은 다운된다. Spring Batch에서는 커서 방식과 페이징 방식을 지원한다.

1. 커서 기반 처리(JdbcCursorItemReader)

* 데이터베이스와 연결을 유지하면서 데이터를 순차적으로 가져온다.
* 하나의 커넥션으로 데이터를 스트리밍하듯 처리한다.
* 메모리는 최소한으로 사용하면서 최대한의 성능을 뽑아낸다.

2. 페이징 기반 처리(JdbcPagingItemReader)

* 데이터를 정확한 크기로 잘라서 차근차근 처리한다.
* 각 페이지마다 새로운 쿼리를 날려 안정성을 보장한다.

#### JdbcCursorItemReader: 데이터를 스트리밍으로 제압하는 법

![](https://github.com/user-attachments/assets/8f272b3d-ccd4-4d8f-baef-d8483c89a06e)

* 커서 기반 처리 방식은 데이터베이스와 끊김 없는 연결을 유지하면서 데이터를 `ResultSet`을 통해 순차적으로 가져오는 방식이다. `JdbcCursorItemReader`가 초기화(open)될 때 빌더를 통해 지정된 SQL 쿼리를 실행하고 그 결과를 가리키는 커서를 생성한다. 이후 `read()`가 호출될 때마다 `ResultSet.next()`를 실행하며 한 행씩 데이터를 가져온다.
* 즉, `JdbcCursorItemReader`는 매번 새로운 쿼리를 실행하는 것이 아니라 이미 열린 `ResultSet`에서 커서를 이동하면서 데이터를 하나씩 읽어오는 구조다. 이 덕분에 대량의 데이터를 한 번에 메모리에 올리지 않고도 처리할 수 있다.
* 이 방식은 메모리 사용량을 최소화할 수 있지만 데이터베이스와의 연결을 유지한 채 진행되므로 긴 배치 작업 동안 커넥션이 너무 오래 유지된다는 단점이 있다.

```
JdbcCursorItemReader
    │
    ├────── DataSource  
    │        └─ (DB 연결 관리)  
    │
    ├────── SQL  
    │        └─ (데이터 조회 쿼리)  
    │
    ├────── RowMapper  
    │        └─ (ResultSet → Java 객체 변환)  
    │
    ├────── PreparedStatement  
    │        └─ (쿼리 실행 및 결과 조회)  
    │
    └────── PreparedStatementSetter (optional)  
             └─ (파라미터 동적 바인딩) 
```
* DataSource(데이터베이스 접속 통로) : `JdbcCursorItemReader`가 데이터베이스의 데이터를 읽으려면 당연히 접속할 곳을 알아야 한다. 이 연결을 담당하는 게 바로 `DataSource`다. `application.yml`에 `spring.datasource` 설정을 해뒀는데 Spring Boot가 이걸 읽어서 HikariCP(커넥션 풀) 기반의 `DataSource`를 자동으로 만들어준다.
* sql(데이터 추출 명령어(쿼리)) : `JdbcCursorItemReader`가 데이터 조회를 위해 사용할 SQL 쿼리다.
* rowMapper(결과 데이터를 객체로 변환) :` RowMapper`는 데이터베이스에서 가져온 원시 데이터(`ResultSet`)를 우리가 다룰 수 있는 객체로 변환
  * Spring Batch에서 RowMapper를 다루는 방법은 크게 세 가지로 나뉜다.
    * `BeanPropertyRowMapper` : 전통적인 `setter` 기반 매핑 방식이다. 자바빈 규약을 지킨 클래스를 대상으로 데이터베이스 컬럼명과 객체의 필드명이 일치하면 자동으로 매핑해준다.
    * `DataClassRowMapper` : Java Record나 Kotlin Data Class 같은 불변 객체를 위해 설계된 `RowMapper` 구현체로 생성자 파라미터를 통해 매핑을 수행한다. 생성자에 없는 필드는 `BeanPropertyRowMapper`와 마찬가지로 `setter` 방식으로 매핑한다.
    * `Custom RowMapper` : 별도의 복잡한 변환 로직이 필요할 때 직접 구현하여 사용한다.
* PreparedStatement(쿼리 실행기) : Spring JDBC를 통해 익숙한 `PreparedStatement`다. `JdbcCursorItemReader`는 내부적으로 이 `PreparedStatement`를 사용해 SQL을 실행한다. `PreparedStatement`는 데이터베이스에 쿼리를 실행하고 그 결과를 `ResultSet`으로 가져오는 JDBC의 핵심 컴포넌트다.
* PreparedStatementSetter(동적 쿼리 파라미터 주입기) : `PreparedStatementSetter`는 이름 그대로 `PreparedStatement`에 동적으로 파라미터 값을 주입하는 역할을 한다.

![](https://github.com/user-attachments/assets/9f887ab8-3c25-4737-9453-07e8a1f006bb)

#### JDBC 드라이버의 내부 최적화

1. ResultSet 내부 버퍼링

* JDBC 드라이버는 기본적으로 여러 개의 row를 미리 가져와 `ResultSet`의 내부 버퍼에 저장해둔다. 따라서 `ResultSet.next()`가 호출될 때마다 이 버퍼에서 데이터를 하나씩 꺼내는 구조다. 즉, 우리가 한 건씩 읽는다고 착각할 뿐 실제로는 한 번에 여러 건을 가져와서 처리하는 방식이다.

2. `JdbcCursorItemReader`의 `read()` 메서드가 호출되면?

![](https://github.com/user-attachments/assets/2883d1ba-5e99-46a3-919c-b343d1db0cca)

* 버퍼가 비었을 때만 데이터베이스에 새로운 데이터를 요청하고 이 때도 한 번에 여러 건의 데이터를 가져와 버퍼에 채운다.
* MySQL 드라이버는 `useCursorFetch` connection property 설정이 없으면 기본적으로 쿼리 결과의 모든 행을 한 번에 메모리로 가져온다. 따라서 조회 결과가 대용량이면 메모리에 전부 적재되어 시스템 자원을 고갈시킬 수 있다. 대량 데이터를 안전하게 처리하려면 `useCursorFetch=true`를 설정하여 데이터를 분할 로딩한다.

3. Fetch Size로 네트워크 비용 최적화

* `fetchSize`는 JDBC 드라이버가 한 번에 가져올 row 개수를 지정하는 값이다. 예를 들어 `fetchSize=1000`이면, 드라이버는 한 번의 네트워크 요청으로 최대 1000건의 데이터를 가져오려고 시도한다.
* 하지만 이 값은 JDBC 드라이버에게 주는 힌트일 뿐이며 실제로 가져오는 건수는 드라이버 구현체와 데이터베이스의 정책에 따라 달라질 수 있다. 드라이버는 이 설정을 참고해 데이터를 한 번에 가져와 내부 버퍼에 저장하고 이후 `read()`가 호출될 때마다 이 버퍼에서 하나씩 반환한다. 이를 통해 데이터베이스와의 네트워크 통신 횟수를 줄여 성능을 최적화할 수 있다.

#### 커서 연속성

![](https://github.com/user-attachments/assets/90e0e976-4b53-405f-bc30-ce04562e7fc0)

* 청크 처리마다 트랜잭션이 새로 시작되고 커밋될텐데 커서는 닫히지 않고 안전하게 데이터를 계속 읽는다.
* `JdbcCursorItemReader`가 기본적으로 `Step` 트랜잭션과 별도의 데이터베이스 커넥션을 사용하기 때문이다. 덕분에 `Step` 트랜잭션이 커밋되더라도 커서는 영향을 받지 않고 처음 열렸을 때의 상태 그대로 데이터를 끝까지 읽어갈 수 있다.

#### JdbcCursorItemReader의 SQL ORDER BY 설정

![](https://github.com/user-attachments/assets/20e282b2-6772-4fe8-9f49-a8cc772fe43a)

* `Step`이 재시작되면 `JdbcCursorItemReader`는 `jumpToItem()` 메서드를 호출해서 이전에 실패한 지점부터 다시 시작하게 된다. 이 때 `ResultSet.next()`를 호출해서 실패 지점까지 커서를 이동시키는데 만약 쿼리 결과의 순서가 매번 다르다면 `ResultSet.next()`로 이동한 지점이 이전 실행의 그 지점이라고 보장할 수 없다.
* `ORDER BY`가 없으면 재시작 시 동일한 데이터 순서를 보장할 수 없어서 어떤 데이터는 중복 처리되거나 어떤 데이터는 누락될 수 있다. 그래서 `JdbcCursorItemReader`를 사용할 때도 반드시 `ORDER BY` 절을 추가해야 하며 `ORDER BY` 절에는 유니크한 값(보통 PK)을 포함하도록 한다.

![](https://github.com/user-attachments/assets/b05b2027-c8ce-4337-b9c3-a108e1959132)

#### JdbcPagingItemReader: 데이터를 페이지 단위로 처리하는 법

![](https://github.com/user-attachments/assets/387c46b3-dfc9-439d-9929-379f461feb5c)

* 대량의 데이터를 메모리 걱정없이 안전하게 읽어오는 또 다른 방법은 바로 데이터를 페이지 단위로 나눠서 읽어오는 것으로 `dbcPagingItemReader`가 있다.
* `JdbcPagingItemReader`는 `Keyset` 기반 페이징을 수행한다.
  * Offset 기반 페이징 : 데이터베이스가 결과셋을 정렬한 후, `OFFSET`만큼 건너뛰고 `LIMIT` 개수만큼 데이터를 가져오는 방식. 이 과정에서 메모리를 많이 사용하고 시간도 오래 걸린다. 특히 페이지 번호가 뒤로 갈수록 스캔하고 버려지는 데이터가 많아져서 성능이 급격히 저하된다.
  * Keyset 기반 페이징 : `SELECT * FROM victims WHERE id > 1000 ORDER BY id LIMIT 10`처럼 이전 페이지의 마지막 키(`id`) 값을 기준으로 그 다음 데이터를 가져온다. 정확히 이전에 가져온 마지막 값 이후부터 읽어오므로 성능이 일정하게 유지된다.

```
JdbcPagingItemReader
    |
    - DataSource   
    |        └── (데이터베이스 연결)
    |
    - RowMapper
    |        └── (ResultSet → Java 객체 변환) 
    |
    - NamedParameterJdbcTemplate
    |        └── (SQL 실행 및 파라미터 바인딩)
    |
    - PagingQueryProvider
	       ├── (쿼리 생성 및 페이징 전략)
             └── (DB별 SQL 최적화)
```
* DataSource : `JdbcCursorItemReader`에서 살펴본 것과 동일하게 데이터베이스 연결을 담당하는 객체다. Spring Boot가 `application.yml`의 `spring.datasource` 설정을 읽어 자동으로 생성한 DataSource를 사용한다.
* RowMapper : `JdbcCursorItemReader`와 동일하게 조회 결과를 Java 객체로 매핑하는 역할을 한다.
* NamedParameterJdbcTemplate : `JdbcTemplate`에서 한 단계 발전한 이 템플릿은 위치 기반 파라미터(?) 대신 이름을 가진 파라미터를 사용할 수 있게 해준다.
* PagingQueryProvider(페이징 쿼리 생성) : `JdbcPagingItemReader`는 Keyset 기반 페이징을 위한 특별한 쿼리 생성 도구를 필요로 하며 이를 위해 Spring Batch는 각 데이터베이스에 최적화된 `PagingQueryProvider` 구현체를 제공한다. PostgreSQL에는 `PostgresPagingQueryProvider`가 MySQL에는 `MySqlPagingQueryProvider`가 필요한 식이다.

#### JdbcBatchItemWriter

* `JdbcBatchItemWriter`는 Spring Batch에서 제공하는 가장 기본적인 관계형 데이터베이스 쓰기 도구다. 내부적으로 `NamedParameterJdbcTemplate`을 사용하며 `JdbcTemplate`의 `batchUpdate`를 활용해 청크 단위로 모아진 아이템들을 효율적으로 데이터베이스에 저장한다.
* 일반 INSERT와 Multi-value INSERT와 Batch Update는 각각 차이가 있다.
  * 일반 INSERT : 매 쿼리마다 네트워크 패킷 발생
  * Multi-Value INSERT : 여러 값을 하나의 쿼리로 한 번에 전송
  * Batch Update : `PreparedStatement`를 재사용하여 쿼리 템플릿 하나와 여러 파라미터 세트를 함께 전송한다. 이렇게 전달된 배치 쿼리는 데이터베이스 서버에서 파싱되어 처리되며 이 모든 작업은 하나의 트랜잭션 내에서 수행된다. 이를 통해 청크 단위로 묶인 수백 건의 데이터가 모두 성공하거나 모두 실패하는 원자성이 보장된다.

```
JdbcBatchItemWriter
    │
    ├────── NamedParameterJdbcTemplate  
    │        └─ (쿼리 실행)  
    │
    ├────── SQL  
    │        └─ (데이터 삽입/수정을 위한 쿼리)  
    │
    ├──────  ItemSqlParameterSourceProvider or ItemPreparedStatementSetter
             └─ (Java 객체 → SQL 파라미터 매핑)
```
* NamedParameterJdbcTemplate : 네임드 파라미터 바인딩을 지원하는 `JdbcTemplate`이다. `JdbcBatchItemWriter`에서 SQL 실행과 결과 처리를 위해 사용된다.
* SQL : `JdbcBatchItemWriter`가 실행할 SQL 구문을 정의한다. 이 SQL은 `INSERT`, `UPDATE`, `DELETE`와 같은 DML 구문이 될 수 있으며 파라미터 바인딩을 위해 두 가지 방식을 지원한다. 단, 두 방식을 동일한 SQL 문에서 혼용하면 안 된다. 하나의 방식만 선택해서 사용해야 한다.
  * 물음표(`?`) 플레이스홀더
  * 네임드 파라미터(`:name`)
* ItemSqlParameterSourceProvider : `ItemSqlParameterSourceProvider`는 Java 객체의 프로퍼티를 SQL의 네임드 파라미터에 매핑하는 역할을 담당한다.
* BeanPropertyItemSqlParameterSourceProvider : Spring JDBC의 `BeanPropertySqlParameterSource`를 사용하여 리플렉션 기반으로 필드 값을 바인딩하는데 자바빈 규약을 따르는 객체의 프로퍼티를 자동으로 SQL 파라미터에 매핑한다.

#### JpaCursorItemReader: 엔티티를 스트리밍으로 처리

![](https://github.com/user-attachments/assets/6b402708-c350-42bc-99cb-94e0a0465b3b)

```
JpaCursorItemReader
    │
    ├────── queryString(JPQL) or JpaQueryProvider  
    │        └─ (Query 생성에 사용됨)  
    │
    ├────── EntityManager  
    │        └─ (JPA 핵심 엔진)
    │        
    └────── Query  
             └─ (EntityManager가 생성하는 실행 가능한 쿼리 인스턴스)
```
* queryString(JPA의 질의 언어) : `JpaCursorItemReader`가 데이터를 조회하기 위한 JPQL(Java Persistence Query Language) 쿼리다. `EntityManager`가 이 `queryString`을 사용하여 실제 실행 가능한 `Query` 객체를 생성한다. 만약 `queryString` 대신 `JpaQueryProvider`를 사용해 쿼리를 생성하고 싶다면 `JpaCursorItemReaderBuilder.queryProvider()`를 통해 `JpaQueryProvider` 구현체를 설정할 수 있다.
* EntityManager(JPA의 심장) : `EntityManager`는 JPA의 핵심 컴포넌트로 엔티티의 생명주기를 관리하고 실제 데이터베이스 작업을 수행한다. `JpaCursorItemReader`에서는 이 `EntityManager`를 통해 커서 기반의 데이터 조회를 실행한다. `JpaCursorItemReaderBuilder.entityManagerFactory()` 메서드를 통해 `EntityManagerFactory` 인스턴스를 전달하면 `JpaCursorItemReader`가 내부적으로 `EntityManager`를 생성하여 커서 기반의 데이터 조회에 사용한다.
* Query(실행 가능한 쿼리 인스턴스) : `JpaCursorItemReader`는 `EntityManager`를 통해 실행 가능한 `Query` 객체를 생성하고, 이를 사용해 데이터를 스트리밍 방식으로 읽어온다. 이 과정은 다음과 같이 `Query` 객체의 `getResultStream()` 메서드를 호출해 수행한다.

#### JpaQueryProvider를 사용한 쿼리 설정

* `JpaQueryProvider` 구현체를 통해 동적 쿼리 생성 등 더 유연한 쿼리 처리가 가능하다. `JpaQueryProvider`는 쿼리 생성을 담당하는 컴포넌트로 이를 통해 직접 작성한 JPQL 문자열 외에도 Named Query나 Native SQL 등 다양한 방식으로 쿼리를 생성할 수 있다.
  * `JpaNamedQueryProvider` : 엔티티 등에 미리 정의된 Named Query를 사용한다.
  * `JpaNativeQueryProvider` : Native SQL을 사용하여 데이터를 조회하도록 한다.

#### JpaPagingItemReader

![](https://github.com/user-attachments/assets/74aee84e-261d-4bc1-976f-bf0063387201)

* `JpaPagingItemReader`는 이름 그대로 데이터를 페이지 단위로 처형하는 `ItemReader`다. 전체 데이터를 일정 수의 페이지만큼 읽는다는 점에서 `JdbcPagingItemReader`와 유사하다. 그러나 내부적으로 JPA 구현체를 사용한다는 점, 그리고 가장 중요한 페이징 방식에서 `JdbcPagingItemReader`와 차이가 있다.
* `JdbcPagingItemReader`와 달리 `JpaPagingItemReader`는 offset 기반으로 페이징을 수행한다. offset 기반 페이징은 데이터베이스에서 특정 범위의 레코드를 가져오는 방법 중 하나로 SQL에서는 다음과 같이 `LIMIT`과 `OFFSET` 절을 사용해 구현한다.

```
JpaPagingItemReader 
    │ 
    ├────── queryString(JPQL) or JpaQueryProvider 
    │        └─ (Query 생성에 사용됨) 
    │ 
    ├────── EntityManager 
             └─ (JPA 핵심 엔진)
```

>❗IDENTITY 전략 사용 시 배치 처리 제약사항
* ID 생성 전략을 `IDENTITY`로 사용할 경우 하나의 중요한 제약사항이 있다. `IDENTITY` 전략에서는 엔티티의 ID가 데이터베이스에서 생성되기 때문에, Hibernate가 엔티티를 영속화하기 위해서는 반드시 `INSERT`를 먼저 실행해야 한다.
* 이는 결과적으로 모든 `INSERT`가 개별적으로 실행되어야 함을 의미한다. 따라서 Hibernate는 여러 개의 `INSERT` 문을 하나의 배치로 묶어서 처리할 수 없게 된다. 배치 처리 성능이 중요한 상황이라면 `SEQUENCE` 전략을 권장한다.
* `TABLE` 전략은 성능과 확장성 측면에서 심각한 문제를 가지고 있다. 새로운 ID를 생성할 때마다 별도의 트랜잭션이 필요하고, 이 과정에서 행(row) 단위의 잠금이 발생한다. 따라서 배치 처리와 같이 대량의 데이터를 다루는 상황에서는 가급적 `SEQUENCE` 전략을 사용하도록 한다.
