# JPA - Lazy Loading and Performance Optimization in JPA

## 📚 API 개발 - V1(엔티티를 직접 노출)

```java
@GetMapping("/api/v1/simple-orders")
public List<Order> ordersV1() {
	List<Order> all = orderRepository.findAllByString(new OrderSearch());
	return all;
}
```

> 실행 결과

```text
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS) (through reference chain: java.util.ArrayList[0]->com.example.younghanapi2.domain.Order["member"]->com.example.younghanapi2.domain.Member$HibernateProxy$wc8tDZV2["hibernateLazyInitializer"])
```
* Jackson 라이브러리가 Hibernate 프록시 객체를 JSON으로 직렬화할 때, 발생하는 문제
* 양방향 연관관계에서 지연 로딩이 설정된 엔티티 관계에서 자주 발생
* Order : Member는 다대일(`@ManyToOne`) 관계로 Order → Member로, 다시 Member → Order로 가면서 양방향 무한 순환이 된다.
* 이와 같은 이유로 엔티티를 직접 외부에 노출시키기보다 DTO를 만들어 필요한 필드만을 수취하는 것이 가장 좋다.
* @JsonIgnore 사용은 지양한다.

## 📚 API 개발 - V2(엔티티를 DTO로 변환)

```java
@GetMapping("/api/v2/simple-orders")
public List<SimpleOrderDto> ordersV2() {
	List<Order> all = orderRepository.findAllByString(new OrderSearch());
	List<SimpleOrderDto> list = all.stream().map(SimpleOrderDto::new).toList();
	return list;
}

@Data
static class SimpleOrderDto {

	private Long orderId;
	private String name;
	private LocalDateTime orderDate;
	private OrderStatus orderStatus;
	private Address address;

	public SimpleOrderDto(Order order) {
		this.orderId = order.getId();
		this.name = order.getMember().getName();
		this.orderDate = order.getOrderDate();
		this.orderStatus = order.getStatus();
		this.address = order.getDelivery().getAddress();
	}
}
```

* 현재 `@PostConstruct` 어노테이션에 의해 애플리케이션이 실행될 때 초기 데이터 2개가 들어가는 상황이다.
* 조회 결과를 들여다보면 아래와 같다.

```sql
Hibernate: 
    /* select
        o 
    from
        
    Order o join
        o.member m */ select
            o1_0.order_id,
            o1_0.delivery_id,
            o1_0.member_id,
            o1_0.order_date,
            o1_0.status 
        from
            orders o1_0 
        join
            member m1_0 
                on m1_0.member_id=o1_0.member_id 
        fetch
            first ? rows only
Hibernate: 
    select
        m1_0.member_id,
        m1_0.city,
        m1_0.street,
        m1_0.zipcode,
        m1_0.name 
    from
        member m1_0 
    where
        m1_0.member_id=?
Hibernate: 
    select
        d1_0.delivery_id,
        d1_0.city,
        d1_0.street,
        d1_0.zipcode,
        d1_0.status 
    from
        delivery d1_0 
    where
        d1_0.delivery_id=?
Hibernate: 
    select
        m1_0.member_id,
        m1_0.city,
        m1_0.street,
        m1_0.zipcode,
        m1_0.name 
    from
        member m1_0 
    where
        m1_0.member_id=?
Hibernate: 
    select
        d1_0.delivery_id,
        d1_0.city,
        d1_0.street,
        d1_0.zipcode,
        d1_0.status 
    from
        delivery d1_0 
    where
        d1_0.delivery_id=?
```

* 로그를 보면 현재 주문 건수는 2개이다.
* 하나의 주문을 조회하면 여기서 쿼리 1번, 이 주문과 연관 관계를 맺고 있는 회원 쪽 쿼리 2번(주문 건수가 2개이므로), 또 이 주문과 연관 관계를 맺고 있는 배달 쪽 쿼리 2번(주문 건수가 2개이므로) 총 1 + N(2) + N(2) = 5번의 쿼리가 찍히는 것을 볼 수 있다.
* 엔티티를 외부에 노출시키는 것을 방지하기 위해 현재 DTO를 만들어 반환했으나 N + 1 이란 성능 이슈가 발생한다.
* 지연로딩은 영속성 컨텍스트에서 조회하므로 이미 조회된 경우에는 쿼리를 생략한다.

## 📚 API 개발 - V3(DTO 그대로 & 페치 조인)

```java
public List<Order> findAllWithMemberDelivery() {
    return em.createQuery(
        "select o from Order o" +
            " join fetch o.member m" +
            " join fetch o.delivery d", Order.class)
        .getResultList();
}
```

* JPQL로 작성한 쿼리문이다. Order를 조회할 때, 연관 관계를 맺고 있는 Member와 Delivery에 대해서 페치 조인을 처리한다는 의미이다.
* 페치 조인(Fetch Join)이란, SQL에서 지원하는 것이 아닌 JPQL에서 지원하는 것으로 연관된 엔티티나 컬렉션을 한 번에 같이 조회할 수 있다.

```java
Hibernate: 
    /* select
        o 
    from
        
    Order o join
        
    fetch
        o.member m 
    join
        
    fetch
        o.delivery d */ select
            o1_0.order_id,
            d1_0.delivery_id,
            d1_0.city,
            d1_0.street,
            d1_0.zipcode,
            d1_0.status,
            m1_0.member_id,
            m1_0.city,
            m1_0.street,
            m1_0.zipcode,
            m1_0.name,
            o1_0.order_date,
            o1_0.status 
        from
            orders o1_0 
        join
            member m1_0 
                on m1_0.member_id=o1_0.member_id 
        join
            delivery d1_0 
                on d1_0.delivery_id=o1_0.delivery_id
```

* 위에서도 언급했듯이 페치 조인은 연관된 엔티티나 컬렉션을 한 번에 조회해서 가져오기 때문에 지연 로딩이 적용되지 않는다.
* N + 1 문제가 개선되어 쿼리가 1번만 나오는 것을 볼 수 있다.

## 📚 API 개발 - V4(JPA에서 DTO로 바로 조회)

* V1 ~ V3까지의 코드는 리포지토리로부터 조회할 때 타입이 엔티티라는 것을 볼 수 있다.
* 해당 코드는 JPA로부터 DTO를 바로 조회하는 코드이다. 코드의 재사용성이 낮다는 단점이 있다.

```java
public List<SimpleQueryOrderDto> findOrderDtos() {
    return em.createQuery(
        "select new com.example.younghanapi2.api.dto.SimpleQueryOrderDto(o.id, o.member.name, o.orderDate, o.status, o.delivery.address) from Order o" +
        " join o.member m" +
        " join o.delivery d", SimpleQueryOrderDto.class)
          .getResultList();
}
```

## 📚 조회 API 개발 순서

1. 엔티티를 DTO로 변환
2. N + 1 성능 이슈 발생시, 페치 조인 도입
3. 그래도 안되면 DTO로 직접 조회
4. JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template를 사용해서 SQL을 직접 사용
