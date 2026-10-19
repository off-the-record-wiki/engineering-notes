# JPA - Java Persistence API

## 📚 JPA(Java Persistence API)

* 자바 진영의 ORM 기술 표준

> 핵심1 : 영속성 컨텍스트는 눈에 보이지 않지만 1차 캐시라는 것이 존재한다.
>
> 핵심2 : 하나의 트랜잭션 내에서 동일성을 보장한다.(단, 2차 캐시는 예외)
>
> 핵심3 : 트랜잭션을 지원하는 쓰기 지연 SQL 저장소가 존재한다.
>
> 핵심4 : 변경 감지(식별자를 통한 Dirty Checking)
>
> 핵심5 : 지연 로딩(Lazy Loading)

## 📚 ORM(Object-Relational Mapping)

* 객체 관계 매핑
* 객체는 객체대로 설계, 관계형 데이터베이스는 관계형 데이터베이스대로 설계
* ORM 프레임워크가 중간에서 매핑

## 📚 영속성 컨텍스트(Persistence Context)

* 영속성 컨텍스트의 내부에는 1차 캐시가 존재한다.
* 이 1차 캐시의 `@Id` 값은 DB의 PK로 매핑한 값이 들어가고 Entity에는 객체가 들어간다.
* 조회 작업을 할 때, 아래와 같이 1차 캐시를 조회한다. 조회하는 값 기준은 `@Id`가 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/30a4eb0b-0b5e-4300-991c-38720339af55.png)

* 만약 1차 캐시에 원하는 값이 없다면 DB로부터 조회하여 영속성 컨텍스트에 올린다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3c88b93d-7c67-47a8-a5c9-b6d27dd481de.png)

## 📚 변경 감지(Dirty Checking)

* 식별자를 통해 조회한 엔티티의 상태를 변경하는 것

```java
try {

    Member member = new Member();
    member.setName("test");

    em.persist(member);

    Member findMember = em.find(Member.class, 1L);

    // flush()를 호출하지 않았으므로 1차 캐시에 값이 존재하는 상황
    // 1L이란 식별자 값을 활용하여 1차 캐시로부터 조회한 후 변경 감지가 일어남
    findMember.setName("test - 수정");  

    tx.commit();
    } catch (Exception e) {
        tx.rollback();
    } finally {
        em.close();
    }

    emf.close();
}
```

## 📚 flush

* 영속성 컨텍스트 변경 내용을 DB에 반영하는 것
* 영속성 컨텍스트를 비우는 것이 아니라는 점
* 트랜잭션이라는 작업 단위가 중요하며 커밋 직전에만 동기화하면 된다.

```text
1. EntityManager.flush()를 통한 강제 플러시
2. Transaction.commit()를 통한 트랜잭션 커밋
3. JPQL 쿼리 실행
```

## 📚 준영속 상태

* 영속성 컨텍스트에서 관리되지 않는 상태
* 준영속 상태로 전환하는 방법은 다음과 같이 3가지가 있다.

```text
1. EntityManager.detach()를 통한 준영속 상태 전환
2. EntityManager.clear()를 통한 영속성 컨텍스트 초기화
3. EntityManager.close()를 통한 영속성 컨텍스트 종료
```
