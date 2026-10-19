# JPA - Entity Mapping & PK Strategy

## 📚 @Entity

* JPA가 관리하는 클래스로 엔티티라고 한다.
* 기본 생성자는 필수(가급적이면 `protected`를 권장)

## 📚 데이터베이스 스키마 자동 생성

* DDL을 애플리케이션 실행 시점에 자동으로 생성
* 각 데이터베이스별 데이터베이스 방언을 활용해서 적절한 DDL을 생성

| 옵션 | 설명 |
|:---------|:----------:|
| create|기존 테이블 삭제 후 다시 생성(DROP + CREATE)|
| create-drop|create와 같으나 종료 시점에 테이블 DROP|
| update|변경 부분만 반영(운영 DB에서 사용 X)|
| validate|엔티티와 테이블이 정상 매핑되었는지만 확인|
| none|사용하지 않음|

> 신중히 선택해서 사용할 것

## 📚 JPA 기본키 생성 전략⚒️

* 직접 할당하는 방법 : @Id만 사용
* 자동 생성하는 방법 : @GeneratedValue 사용
   * IDENTITY : 데이터베이스에 위임, MYSQL
   * SEQUENCE : 데이터베이스 시퀀스 오브젝트 사용, ORACLE - @SequenceGenerator 사용
   * TABLE : 키 생성용 테이블 사용, 모든 DB에서 사용 - @TableGenerator 사용
   * AUTO : 방언에 따라 자동 지정

## 📚 IDENTITY 전략 특징 정리⚒️

* 기본 키 생성을 데이터베이스에 위임
* 주로 MYSQL, PostgreSQL, SQL Server, DB2에서 사용
* JPA는 `EntityManager.persist()` 호출 시점이 아닌 트랜잭션 커밋 시점에 INSERT 쿼리문이 날아간다.
* AUTO_INCREMENT는 데이터베이스에 INSERT SQL을 실행한 이후에 ID 값을 알 수 있다.
* 그러나 IDENTITY 전략은 `EntityManager.persist()` 호출 시점에 INSERT 쿼리문이 실행되어 DB에서 식별자 조회가 가능하다.
* 따라서 IDENTITY 전략의 경우 쓰기 지연 SQL 저장소에 쿼리가 쌓이지 않고 `persist()` 호출 시점마다 쿼리가 날아가기 때문에 그 때마다 DB에 반영이 된다.

## 📚 SEQUENCE 전략 특징 정리⚒️

```java
@Entity
@Getter @Setter
@SequenceGenerator(
		name = "MEMBER_SEQ_GENERATOR",
		sequenceName = "MEMBER_SEQ",
		initialValue = 1, allocationSize = 1)
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MEMBER_SEQ_GENERATOR")
	private Long id;

	@Column(name = "name", nullable = false)
	private String username;
}
```

## 📚 TABLE 전략 특징 정리

* 키 생성 테이블을 하나 만들어서 데이터베이스 시퀀스를 모방한 전략
* 모든 데이터베이스에 적용 가능하나 성능 이슈가 발생

## 📚 SEQUENCE 전략에서의 최적화 방법

* SEQUENCE 전략을 사용하기 위해선 DB에 SEQUENCE를 미리 생성해주어야 한다.
* DB를 통해 조회해야 PK 값을 알 수 있다.
* `EntityManager.persist()`를 호출하기 전에 DB에서 Sequence 값을 가져오면 문제가 없다.
* 하지만 이 SEQUENCE 전략의 단점으로 DB에서 필요할 때마다 Sequence 값을 조회해서 가져와야되기 때문에 이걸 자주하게 된다면 성능 이슈가 발생하게 된다.
* 이 성능 저하를 해결하기 위해 도입된 것이 바로 allocationSize 속성으로 이 속성의 기본 값은 50이다. 처음 DB에 접근해서 Sequence 값을 가져오도록 하면 한 번에 50을 더해놓고 메모리 상에서 1개씩 차감해서 사용하는 방식이다.
* JPA/Hibernate는 시퀀스 값을 조회할 때마다 DB에 접근하지 않고 메모리에 미리 일정 범위의 시퀀스 값을 할당받아 사용하는데 DB로의 접근을 줄인다는 명목으로 만약 allocationSize의 값을 크게 한다면 서버 다운 시 데이터 정합성에서의 문제가 발생할 수 있다.
