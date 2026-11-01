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
