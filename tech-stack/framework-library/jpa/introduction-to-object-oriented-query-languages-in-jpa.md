# JPA - Introduction to Object-Oriented Query Languages in JPA

## 📚 JPQL(Java Persistence Query Language)

* 테이블이 아닌 객체를 대상으로 검색하는 객체 지향 쿼리
* SQL을 추상화해서 특정 데이터베이스 SQL에 의존X
* JPQL을 한 마디로 정의하면 객체 지향 SQL

```java
List<Member> resultList = em.createQuery("select m from Member m where m.name like '%kim%'", Member.class)
                    .getResultList();
```

## 📚 TypedQuery, Query

* `TypeQuery` : 반환 타입이 명확할 때 사용
* `Query` : 반환 타입이 명확하지 않을 떄 사용

```java
TypedQuery<Member> query1 = em.createQuery("select m from Member m", Member.class);
Query query2 = em.createQuery("select m from Member m");
```

## 📚 결과 조회 - getResultList, getSingleResult

* `getResultList()` : 결과가 하나 이상일 때, 리스트 반환
* `getSingleResult()` : 결과가 정확히 하나일 때, 단일 객체 반환

## 📚 파라미터 바인딩 - 이름 기준, 위치 기준(가급적이면 이름 기준으로 사용할 것)

```java
List<Member> resultList = em.createQuery("select m from Member m where m.name = :name", Member.class)
                    .setParameter("name", "member1")
                    .getResultList();
```
 
## 📚 프로젝션(SELECT)

* SELECT 절에 조회할 대상을 지정하는 것
* 프로젝션 대상 : 엔티티, 임베디드 타입, 스칼라 타입
* `distinct` 키워드로 중복을 제거할 수 있다.

```java
SELECT m FROM Member m
SELECT m.team FROM Member m
SELECT m.address Member m
SELECT m.username, m.age FROM Member m
```

```java
// 전체 패키지 경로를 그대로 작성(DTO 생성자 필드 순서를 주의)
List<MemberDto> results = em.createQuery("select new hellojpa.dto.MemberDto(m.age, m.name) from Member m", MemberDto.class)
                    .getResultList();
```

## 📚 페이징(Paging)

* `setFirstResult(int startPosition)`: 조회 시작 위치 지정(0부터 시작)
* `setMaxResults(int maxResult)` : 조회할 데이터의 수

```java
List<Member> list = em.createQuery("select m from Member m order by m.age desc", Member.class)
                    .setFirstResult(0)    // 시작 위치
                    .setMaxResults(10)    // 최대 데이터 개수
                    .getResultList();
```

## 📚 조인(Join)

* 내부 조인

```sql
select m from Member m join m.team t
```

* 외부 조인(JPA는 기본적으로 외부 조인을 지원한다.)

```sql
select m from Member m left join m.team t
```

* 세타 조인

```sql
select count(m) from Member m, Team t where m.username = t.name
```

## 📚 서브 쿼리(Sub Query)

* JPQL도 SQL처럼 서브쿼리를 지원한다. WHERE, HAVING절에는 사용이 가능하나, SELECT, FROM절에는 사용이 불가능하다.
* 조인으로 풀 수 있으면 풀어서 해결하도록 한다.

## 📚 경로 표현식

* 상태 필드 : 경로 탐색의 끝, 탐색 X
* 단일 값 연관 경로 : 묵시적 내부 조인(Inner Join) 발생, 간편하나 쿼리 튜닝이 어렵다는 문제가 발생
* 컬렉션 값 연관 경로 : 묵시적 내부 조인(Inner Join) 발생, 탐색 불가(FROM절에서 명시적 조인을 통해 별칭을 얻으면 별칭을 통해 탐색 가능)

```java
// 명시적 조인(join t.members m)을 통해 별칭을 얻으면 탐색이 가능
List<Team> list = em.createQuery("select m.name, m.age from Team t join t.members m", Team.class)
                    .getResultList();
```

* 쿼리 튜닝을 위해서 묵시적 조인보다는 명시적 조인을 사용하도록 하자.

## 📚 페치 조인(Fetch Join)

* SQL에서 지원하는 조인의 종류는 아니고 JPQL에서 성능 최적화를 위해 제공하는 기능이다.
* 연관된 엔티티나 컬렉션을 한 번에 같이 조회하는 기능으로 이 때는 지연 로딩이 발생하지 않는다.
* 다대일, 일대일 페치 조인의 경우 데이터 뻥튀기가 발생하지 않으나 일대다 페치 조인의 경우 다(N) 측에 해당하는 데이터의 개수만큼 뻥튀기가 발생한다.

> 주의할 부분 : 일대다 관계의 컬렉션 조인을 수행할 경우 다(N) 측의 개수만큼 일(1)의 데이터 개수가 뻥튀기가 되는 문제<br>
> Ex. **게시글과 댓글 구조의 컬렉션 조인의 경우**

```text
1. `@XToOne` 관계를 모두 페치조인한다.
2. 컬렉션은 지연 로딩으로 조회한다.
3. 지연 로딩 성능 최적화를 위해 `@BatchSize`를 적용한다.
```

## 📚 페치 조인의 특징과 한계

* 페치 조인 대상에는 별칭을 줄 수 없다.
* 둘 이상의 컬렉션은 페치 조인 할 수 없다.
* 컬렉션을 페치 조인하면 페이징 API를 사용할 수 없다. → 메모리에서 페이지네이션을 적용하게 되는 위험 밣생

## 📚 Named 쿼리

* 미리 정의해서 이름을 부여해두고 사용하는 JPQL로 정적 쿼리만 가능하다.
* 어노테이션, XML에 정의
* 애플리케이션 로딩 시점에 초기화 후 재사용하며, 애플리케이션 로딩 시점에 쿼리를 검증한다.

```java
@NamedQuery(
		name = "Member.findByUsername",
		query = "select m from Member m where m.name = : name"
)
public class Member {
    // ...
}
```

```java
List<Member> resultList = em.createNamedQuery("Member.findByUsername", Member.class)
                    .setParameter("name", "관리자1")
                    .getResultList();
```

## 📚 벌크 연산(Bulk)

* JPA 변경 감지 기능으로 실행할 경우 너무 많은 SQL문이 실행된다.
* 만약 N건의 데이터라면 변경 감지에 의한 UPDATE문 SQL쿼리가 N번이 호출된다.
* 이럴 경우 쿼리 한 번으로 여러 Row를 변경할 수 있는 것이 바로 벌크 연산이다.
* DELETE, UPDATE, INSERT문 수행 가능하다.

```java
for (int i = 0; i < 1000; i++) {
    Member member = new Member();
    member.setName("member" + i);
    member.setAge(20 + (i % 30)); // 20~49세 범위의 나이 설정
    em.persist(member);

    // 100건마다 영속성 컨텍스트 플러시 및 초기화
    if (i % 100 == 0) {
        em.flush();
        em.clear();
        }
    }

    // 1000건의 데이터에 대해서 나이를 모두 30살로 통일시켜라.
    // executeUpdate() 결과는 영향받은 엔티티 수를 반환
    int result = em.createQuery("update Member m set m.age = 30")
                    .executeUpdate();
```

> 실행 결과

```sql
Hibernate: 
    /* update
        Member m 
    set
        m.age = 30 */ update Member 
    set
        age=30
```

* 쿼리 하나로 대량의 데이터를 한 번에 변경할 수 있다.
* 하지만 이 벌크 연산은 영속성 컨텍스트를 무시하고 데이터베이스에 직접 쿼리를 날리게 된다.
* 이렇게 되면 JPA의 영속성 컨텍스트와 데이터베이스 간 데이터 정합성에서 문제가 발생할 수 있다.
* 따라서 벌크 연산을 안전하게 사용하려면 벌크 연산을 수행한 후 영속성 컨텍스트를 초기화해주는 것이 좋다.
