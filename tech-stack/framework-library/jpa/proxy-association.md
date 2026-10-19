# JPA - Proxy Association

## 📚 프록시

* 엔티티를 조회할 때 연관된 엔티티들이 함께 사용되는 것은 아니다. 
* 프록시라는 기술은 연관된 객체를 처음부터 데이터베이스에서 조회하는 것이 아니라 실제 사용하는 시점에 데이터베이스에서 조회할 수 있도록 한다.

## 📚 프록시 객체의 초기화

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/23253929-ddeb-4101-87da-8bc3587c3792.png)

* 초기화 요청(해당 엔티티의 메서드를 호출)을 하면 영속성 컨텍스트를 통해 DB 조회가 이루어지면서 초기화가 이루어진다.
* 프록시 객체는 처음 사용할 때 한 번만 초기화가 된다.
* 프록시 객체가 초기화되면서 실제 엔티티 객체로 전환되는 것이 아니다. 프록시 객체를 통해서 실제 엔티티에 접근하는 것이다.
* 겉으로 보면 실제 엔티티 객체와 프록시 객체는 구분이 어렵다. 따라서, 타입 비교시 `==` 비교가 아니라 `instanceof` 연산을 사용해야 한다.
* 만약 찾는 엔티티가 이미 영속성 컨텍스트에 있다면 `EntityManager.getReference()`를 호출해도 실제 엔티티가 반환된다.
* 영속성 컨텍스트의 도움을 받을 수 없는 비영속, 준영속 상태의 경우 프록시 객체 초기화시 오류가 발생한다.

```java
public class JpaMain {

    public static void main(String[] args) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {

            Member member = new Member();
            member.setUsername("member1");
            em.persist(member);

            em.flush();
            em.clear();

            Member findMember = em.getReference(Member.class, member.getId());
            System.out.println("member username: " + findMember.getClass());  // 예상되는 타입 : 프록시 타입

            em.detach(findMember);  // 준영속 엔티티로 전환

            findMember.getUsername();  // 영속성 컨텍스트의 지원을 받을 수 없기 때문에 프록시 초기화시 오류가 발생

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }

        emf.close();
    }
}
```

> 실행 결과

```text
org.hibernate.LazyInitializationException: could not initialize proxy [hellojpa.domain.Member#1] - no Session
```

## 📚 즉시 로딩과 지연 로딩

* 즉시로딩(EAGER) : 엔티티를 조회할 때 연관된 엔티티도 함께 조회한다.
* 지연로딩(LAZY) : 연관된 엔티티를 실제 사용할 때 조회한다.
* 즉시로딩을 적용할 시 JPQL에서 조회 성능 이슈를 발생시키는 N + 1 문제가 발생한다.
* `@XToOne` : 기본적으로 즉시로딩
* `@XToMany` : 기본적으로 지연로딩

## 📚 NULL 제약 조건과 JPA 조인 전략

* 즉시 로딩 실행의 경우 기본적으로 JPA에서 외부 조인(LEFT OUTER JOIN)을 사용한다.
* 내부 조인(INNER JOIN)과 외부 조인을 비교했을 떄 성능과 최적화 측면에서 내부 조인이 상대적으로 유리하다.
* 이 내부 조인을 사용하도록 하려면 `@JoinColumn`에 `nullable = false`를 지정하면 외부 조인이 아닌 내부 조인이 동작하게 된다.

> 결론 : JPA는 기본적으로 선택적 관계면 외부 조인을 사용하고, 필수 관계면 내부 조인을 사용한다.

## 📚 영속성 전이(CASCADE)와 고아 객체

* 특정 엔티티를 영속 상태로 만들 때, 연관된 엔티티도 함께 영속 상태로 만들고 싶으면 영속성 전이(CASCADE)를 사용하면 된다.
* 영속성 전이를 사용하면 자식 엔티티도 함께 저장할 수 있으며 이 영속 상태 설정은 부모 엔티티 측에서 설정한다.
* `@OneToOne`, `@OneToMany` 관계의 경우 부모 엔티티를 제거할 경우 자식 엔티티는 고아 객체가 된다. 만약 영속 상태 활성화를 하면 부모 엔티티가 제거될 때 자식 엔티티도 같이 제거된다.

## 📚 영속성 전이 + 고아 객체, 생명주기

* `CascadeType.ALL`과 orphanRemoval = true`를 동시에 사용하면 엔티티 스스로가 생명주기를 관리할 수 있게 된다.
* 이 두 옵션을 활성화하면 부모 엔티티 설정만을 통해서 자식 엔티티의 생명 주기를 관리할 수 있다.
