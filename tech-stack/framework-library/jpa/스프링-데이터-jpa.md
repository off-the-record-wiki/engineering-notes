# JPA - Spring Data JPA

## 📚 공통 인터페이스 구성

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0f629665-44d7-4566-bd7c-3bc5ce9fd60e.png)

* 핵심 인터페이스 목록
  * JpaRepository
  * PagingAndSortingRepository
  * CrudRepository
  * Repository

## 📚 메서드 이름으로 쿼리 생성

* [스프링 데이터 JPA가 제공하는 쿼리 메서드 기능 정리](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
* 이 기능은 엔티티 필드명이 변경되면 인터페이스에 정의한 메서드 이름도 함께 변경해야 한다. 그렇지 않으면 애플리케이션 시작 시점에 오류가 발생한다.
* 애플리케이션 로딩 시점에 오류를 인지할 수 있도록 하는 것이 스프링 데이터 JPA의 가장 큰 장점이다.

## 📚 메서드 이름으로 @NamedQuery 호출 가능

* [NamedQuery란?](객체지향-쿼리-언어.md#-named-%EC%BF%BC%EB%A6%AC)
* 애플리케이션 로딩 시점에 오류를 잡아 준다.

## 📚 @Query, 리포지토리 메서드에 쿼리 정의하기

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	@Query("select m from Member m where m.name = :name and m.age = :age")
	List<Member> findUser(@Param(value = "username") String username, @Param(value = "age") int age);
}
```

## 📚 @Query, DTO로 직접 조회하기

* DTO로 직접 조회할 경우, JPA의 new 명령어를 사용해야 한다. 그리고 생성자가 반드시 필요하다.

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	@Query("select new com.jwj.springdatajpa.dto.MemberDto(m.id, m.name, m.age) from Member m join m.team t")
	List<MemberDto> findUserDto(String name, String age);
}
```

## 📚 파라미터 바인딩 - 이름 기반, 위치 기반

* [파라미터 바인딩이란?](객체지향-쿼리-언어.md#-%ED%8C%8C%EB%9D%BC%EB%AF%B8%ED%84%B0-%EB%B0%94%EC%9D%B8%EB%94%A9---%EC%9D%B4%EB%A6%84-%EA%B8%B0%EC%A4%80-%EC%9C%84%EC%B9%98-%EA%B8%B0%EC%A4%80%EA%B0%80%EA%B8%89%EC%A0%81%EC%9D%B4%EB%A9%B4-%EC%9D%B4%EB%A6%84-%EA%B8%B0%EC%A4%80%EC%9C%BC%EB%A1%9C-%EC%82%AC%EC%9A%A9%ED%95%A0-%EA%B2%83)
* 결론적으로 위치 기반은 데이터 순서가 바뀌면 큰 문제가 발생하므로 이름 기반을 사용하도록 한다.

## 📚 유연한 반환 타입

* [TypedQuery, Query](객체지향-쿼리-언어.md#-typedquery-query)
* [getResultList, getSingleResult](객체지향-쿼리-언어.md#-%EA%B2%B0%EA%B3%BC-%EC%A1%B0%ED%9A%8C---getresultlist-getsingleresult)

## 📚 순수 JPA 페이징과 정렬

> Example. 검색 조건 : 나이가 10살 / 정렬 조건 : 이름으로 내림차순 / 페이징 조건 : 첫 번째 페이지, 페이지당 보여줄 데이터는 3건

```java
public List<Member> findByPage(int age, int offset, int limit) {
	return em.createQuery("select m from Member m where m.age = :age order by m.name desc")
		.setParameter("age", age) // 검색 조건
		.setFirstResult(offset)   // 시작 위치(첫 번째 페이지)
		.setMaxResults(limit)     // 최대 데이터 개수(3건)
		.getResultList();  
}

public long totalCount(int age) {
	return em.createQuery("select count(m) from Member m where m.age = :age", Long.class)
                .setParameter("age", age)
                .getSingleResult();
}
```

## 📚 스프링 데이터 JPA 페이징과 정렬

* `org.springframework.data.domain.Sort` : 정렬 기능
* `org.springframework.data.domain.Pageable` : 페이징 기능(내부 Sort 포함)
* `org.springframework.data.domain.Page` : 추가 카운트 쿼리 결과를 포함하는 페이징 → Page는 1부터 시작이 아니라 0부터 시작이다.
* `org.springframework.data.domain.Slice` : 추가 카운트 쿼리 결과 없이 다음 페이지만 확인 가능
* `List(자바 컬렉션)` : 추가 카운트 쿼리 없이 결과만 반환

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	Page<Member> findByAge(int age, Pageable pageable);
}
```

> 하이버네이트6 left join 최적화 설명 추가

```java
@Query(value = "select m from Member m left join m.team t")
Page<Member> findByAge(int age, Pageable pageable);
```

* 해당 코드를 분석해보자면 Member를 기준으로 Team에 대해서 다대일 조인을 수행하는 내용이 된다.
* 따라서 다대일 조인이므로 결과적으로 모든 Member를 조회하는 SQL 쿼리문이 된다.
* 스프링 부트 3.0 이상의 경우 하이버네이트6이 적용되는데 이 때, 의미없는 LEFT JOIN을 제거하는 최적화를 하게 된다.

## 📚 벌크성 수정 쿼리

* 벌크성 수정, 삭제 쿼리는 `@Modifying` 어노테이션을 사용한다.
* 변경 감지를 통해 여러 데이터를 하나하나 바꾼다면 많은 SQL문이 발생하기에 다량의 데이터에 대한 적용을 위해서라면 이와 같은 벌크성 수정 쿼리가 효율적이다.
* [벌크 연산 주의사항 - 영속성 컨텍스트와 DB의 데이터 정합성 문제](객체지향-쿼리-언어.md#-%EB%B2%8C%ED%81%AC-%EC%97%B0%EC%82%B0bulk)

```java
@Modifying(clearAutomatically = true)	// 영속성 컨텍스트 초기화 자동 설정
@Query("update Member m set m.age = m.age + 1 where m.age >= :age")
int bulkAgePlus(@Param(value = "age") int age);
```

## 📚 @EntityGraph

* 페치 조인(Fetch Join) : 연관된 엔티티들을 SQL 한 번에 조회하는 방법

> Example Code. N + 1 문제란?

```java
@Test
@DisplayName("엔티티 그래프 예제")
void testEntityGraph() {

        Team teamA = new Team();
	teamA.setName("teamA");
	teamRepository.save(teamA);

	Team teamB = new Team();
	teamB.setName("teamB");
	teamRepository.save(teamB);

	Member member1 = new Member();
	member1.setName("member1");
	member1.setAge(10);
	member1.setTeam(teamA);
	memberRepository.save(member1);

	Member member2 = new Member();
	member2.setName("member2");
	member2.setAge(10);
	member2.setTeam(teamB);
	memberRepository.save(member2);

        em.flush();
	em.clear();

	List<Member> members = memberRepository.findAll();	// MEMBER SELECT 쿼리 1번

	for (Member member : members) {
		System.out.println("member = " + member.getName());	// 여기선 쿼리가 안 나감
		System.out.println("member.team = " + member.getTeam().getName()); // 여기선 프록시 초기화
	}
}
```

> 실행 결과 분석

```sql
2025-01-05T02:02:30.400+09:00 DEBUG 37564 --- [    Test worker] org.hibernate.SQL                        : 
    select
        m1_0.member_id,
        m1_0.age,
        m1_0.name,
        m1_0.team_id 
    from
        member m1_0
Hibernate:     // Member 쿼리 1번 호출
    select
        m1_0.member_id,
        m1_0.age,
        m1_0.name,
        m1_0.team_id 
    from
        member m1_0
member = member1
2025-01-05T02:02:30.412+09:00 DEBUG 37564 --- [    Test worker] org.hibernate.SQL                        : 
    select
        t1_0.member_id,
        t1_0.name 
    from
        team t1_0 
    where
        t1_0.member_id=?
Hibernate:     // member1과 연관된 팀 엔티티 쿼리 호출 1번 
    select
        t1_0.member_id,
        t1_0.name 
    from
        team t1_0 
    where
        t1_0.member_id=?
member.team = teamA
member = member2     // 이미 Member 전체를 조회하면서 영속성 컨텍스트에 존재하기 때문에 SELECT 쿼리가 나가지 않는 것
2025-01-05T02:02:30.414+09:00 DEBUG 37564 --- [    Test worker] org.hibernate.SQL                        : 
    select
        t1_0.member_id,
        t1_0.name 
    from
        team t1_0 
    where
        t1_0.member_id=?
Hibernate:     // member2와 연관된 팀 엔티티 쿼리 호출 2번 
    select
        t1_0.member_id,
        t1_0.name 
    from
        team t1_0 
    where
        t1_0.member_id=?
member.team = teamB
```

* 하나의 쿼리를 호출했는데 그 부수 효과로 N번의 쿼리가 더 나가고 있다.
* 이것을 N + 1 문제라고 한다.
* 이 N + 1 문제를 개선하는 방법으로 연관된 엔티티를 한 번에 조회할 수 있는 페치 조인(Fetch Join)이 있다.

> Fetch Join 적용 코드

```java
@Query("select m from Member m join fetch m.team")
List<Member> findMemberFetchJoin();
```

> 개선된 코드의 실행 결과

```sql
2025-01-05T02:50:57.897+09:00 DEBUG 38455 --- [    Test worker] org.hibernate.SQL                        : 
    select
        m1_0.member_id,
        m1_0.age,
        m1_0.name,
        t1_0.member_id,
        t1_0.name 
    from
        member m1_0 
    join
        team t1_0 
            on t1_0.member_id=m1_0.team_id
Hibernate:     // Member와 Team을 다대일 조인해서 연관된 엔티티를 한 번에 가져옴(따라서 쿼리는 1번만 호출됨)
    select
        m1_0.member_id,
        m1_0.age,
        m1_0.name,
        t1_0.member_id,
        t1_0.name 
    from
        member m1_0 
    join
        team t1_0 
            on t1_0.member_id=m1_0.team_id
member = member1     // 영속성 컨텍스트에 있기 때문에 SELECT 쿼리문 필요 없음
member.team = teamA  // 영속성 컨텍스트에 있기 때문에 SELECT 쿼리문 필요 없음
member = member2     // 영속성 컨텍스트에 있기 때문에 SELECT 쿼리문 필요 없음
member.team = teamB  // 영속성 컨텍스트에 있기 때문에 SELECT 쿼리문 필요 없음
```

* `@EntityGraph` 어노테이션을 다음과 같이 활용할 수 있다.
* `paths`에는 즉시로딩(EAGER)하고 싶은 엔티티를 적는다.

```java
@EntityGraph(attributePaths = "team")
@Query("select m from Member m")
List<Member> findMemberEntityGraph();
```

## 📚 사용자 정의 리포지토리 구현

* 사용자 정의 인터페이스 정의

```java
public interface MemberRepositoryCustom {
    List<Member> findMemberCustom();
}
```

* 사용자 정의 인터페이스 구현체 클래스(

```java
@RequiredArgsConstructor
// 이 때, 사용자 정의 구현 클래스의 네이밍 규칙이 존재하는데 (리포지토리 인터페이스 이름 + Impl)로 해야만 한다.
public class MemberRepositoryImpl implements MemberRepositoryCustom {
    private final EntityManager em;

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m")
            .getResultList();
    }
}
```

* 사용자 정의 인터페이스를 상속받아 같이 사용

```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

}
```

## 📚 Auditing

* 순수 JPA 사용 시

```java
@MappedSuperclass
public class JpaBaseEntity {

	@Column(updatable = false)
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;

	@PrePersist // 엔티티가 영속화되기 전에 실행되어야 하는 Entity 클래스의 메서드를 표시하는 데 사용한다. 데이터베이스에 저장되기 전에 자동으로 호출
	public void prePersist() {
            this.createdDate = LocalDateTime.now();
	    this.updatedDate = LocalDateTime.now();
        }

	@PreUpdate // 엔티티가 업데이트되기 전에 실행되어야 하는 Entity 클래스의 메서드를 표시하는 데 사용한다. 변경 사항이 데이터베이스에 동기화되기 전에 자동으로 호출
	public void preUpdate() {
            this.updatedDate = LocalDateTime.now();
        }
}
```

* 스프링 데이터 JPA 사용 시
  * `@EnableJpaAuditing` 어노테이션 사용

```java
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class BaseEntity {

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdDate;

	@LastModifiedDate
	private LocalDateTime lastModifiedDate;

	@CreatedBy
	@Column(updatable = false)
	private String createdBy;

	@LastModifiedBy
	private String lastModifiedBy;
}
```

## 📚 Web 확장 - 도메인 클래스 컨버터

```java
// 식별자를 통한 코드
@GetMapping("/member1/{id}")
public String findMember1(@PathVariable(name = "id") Long id) {
	Member member = memberRepository.findById(id).get();
	return member.getName();
}

// 도메인 클래스 컨버터 적용 코드
@GetMapping("/member2/{id}")
public String findMember2(@PathVariable(name = "id") Member member) {
	return member.getName();
}
```

## 📚 @Transactional vs @Transactional(readOnly = true)

* `@Transactional`
  * JPA의 모든 변경은 트랜잭션 안에서 적용되어야 한다.
  * 서비스 계층에서 트랜잭션을 시작하면 리포지토리는 해당 트랜잭션을 전파 받아서 사용한다.
  * 서비스 계층에서 트랜잭션을 시작하지 않으면 리포지토리에서 트랜잭션을 시작한다.
  * 서비스 계층에서 트랜잭션을 걸지 않아도 가능했던 이유는 바로 리포지토리에서 트랜잭션을 시작했기 때문이다.

* `@Transactional(readOnly = true)`
  * 데이터를 단순히 조회만 하고 변경하지 않는 트랜잭션에서 `readOnly = true` 옵션을 사용하면 플러시를 생략해서 약간의 성능 향상을 얻을 수 있다.
 
* `save()`
  * 새로운 엔티티면 저장(persist), 새로운 엔티티가 아니라면 병합(merge)

## 📚 save() 메서드 동작 이해

* `save()`메서드를 사용할 경우 새로운 엔티티라면 저장을, 새로운 엔티티가 아니라면 병합을 한다.
* [기본 키 생성 전략 정리](엔티티-매핑-및-기본키-생성-전략-정리.md)
* JPA 생성 전략을 @Id 직접 할당 방식이라면 `save()` 호출 시점에 이미 식별자 값이 있는 상태로 `save()` 메서드를 호출해 `merge`가 동작하게 된다. `merge`는 DB를 호출해서 값을 확인하고 DB에 값이 없으면 새로운 엔티티로 인지하므로 매우 비효율적이다. 이와 같은 경우 `Persistable`를 사용해서 새로운 엔티티 확인 여부를 직접 구현하는 것이 효율적이다.
