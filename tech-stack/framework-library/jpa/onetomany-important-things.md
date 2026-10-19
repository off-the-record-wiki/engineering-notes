# JPA - OneToMany Important Things

## 📚 API 개발 - V1(엔티티를 직접 노출)

```java
@GetMapping("/api/v1/orders")
public List<Order> ordersV1() {
	List<Order> all = orderRepository.findAllByString(new OrderSearch());
	for (Order order : all) {

                // Member 엔티티(지연로딩 설정) 강제 초기화
		order.getMember().getName();
		
                // Delivery 엔티티(지연로딩 설정) 강제 초기화
                order.getDelivery().getAddress();
		List<OrderItem> orderItems = order.getOrderItems();

		for (OrderItem orderItem : orderItems) {

                        // Item 엔티티(지연로딩 설정) 강제 초기화
			orderItem.getItem().getName();
		}
	}
	return all;
}
```

```java
// 프록시 초기화를 위한 스프링 빈을 등록
@Bean
public Hibernate5JakartaModule hibernate5Module() {
	return new Hibernate5JakartaModule();
}
```

* 프록시를 강제 초기화하는 방법과 엔티티를 직접 외부에 노출하는 방법을 사용해 구현했으나 역시 API 스펙이 변함에 따라 엔티티의 수정이 수반되므로 지양하도록 하자.

## 📚 API 개발 - V2(엔티티를 DTO로 변환)

* Order 엔티티와 연관 관계를 맺고 있는 Member, Delivery, OrderItems에 대한 DTO를 구성해서 반환하도록 한다.
* DTO로 발라내는 작업이 힘들긴 하나 성능 이슈를 방지하기 위해선 어쩔 수 없는 듯하다.

```java
@GetMapping("/api/v2/orders")
public List<OrderDto> ordersV2() {
	List<Order> all = orderRepository.findAllByString(new OrderSearch());
	List<OrderDto> list = all.stream().map(OrderDto::new).toList();
	return list;
}

@Data
static class OrderDto {

	private Long orderId;
	private String name;
	private LocalDateTime orderDate;
	private OrderStatus orderStatus;
	private Address address;
	// private List<OrderItem> orderItems;(엔티티 X, DTO O)
	private List<OrderItemDto> orderItems;

	public OrderDto(Order order) {
		this.orderId = order.getId();
		this.name = order.getMember().getName();
		this.orderDate = order.getOrderDate();
		this.orderStatus = order.getStatus();
		this.address = order.getDelivery().getAddress();
		this.orderItems = order.getOrderItems().stream()
				.map(OrderItemDto::new)
				.collect(Collectors.toList());
	}
}

@Data
static class OrderItemDto {

	private String itemName;
	private int orderPrice;
	private int count;

	public OrderItemDto(OrderItem orderItem) {
		this.itemName = orderItem.getItem().getName();
		this.orderPrice = orderItem.getOrderPrice();
		this.count = orderItem.getCount();
	}
}
```

> 실행 결과

```json
[
    {
        "orderId": 1,
        "name": "userA",
        "orderDate": "2025-01-02T01:49:22.212195",
        "orderStatus": "ORDER",
        "address": {
            "city": "서울",
            "street": "1",
            "zipcode": "1111"
        },
        "orderItems": [
            {
                "itemName": "JPA1 BOOK",
                "orderPrice": 10000,
                "count": 1
            },
            {
                "itemName": "JPA2 BOOK",
                "orderPrice": 20000,
                "count": 2
            }
        ]
    },
    {
        "orderId": 2,
        "name": "userB",
        "orderDate": "2025-01-02T01:49:22.228231",
        "orderStatus": "ORDER",
        "address": {
            "city": "진주",
            "street": "2",
            "zipcode": "2222"
        },
        "orderItems": [
            {
                "itemName": "SPRING1 BOOK",
                "orderPrice": 20000,
                "count": 3
            },
            {
                "itemName": "SPRING2 BOOK",
                "orderPrice": 40000,
                "count": 4
            }
        ]
    }
]
```

* 그러나 이 방식 또한 최적화가 되어있지 않기에 여전히 적지 않은 쿼리가 나가게 된다.

## 📚 API 개발 - V3.1(페이징이 가능한 컬렉션 조회)

* 다대일 관계 뿐만 아니라 일대다 관계(컬렉션)의 경우에도 페치 조인을 통한 최적화가 가능하다.
* 일대다 관계를 조회하게 되면 일(1)에 해당하는 엔티티가 다(N)에 해당하는 엔티티만큼 데이터가 뻥튀기가 된다.
* 예를 들어, 주문 : 주문상품은 일대다 관계를 가지는데 하나의 주문에 포함된 주문상품을 조회할 때, 주문상품의 개수만큼 주문 데이터가 증가하는 것이다.
* 일에 해당하는 데이터의 뻥튀기 현상을 방지하기 위해 `distinct` 키워드를 사용하면 된다.

```java
public List<Order> findAllWithItem() {
    return em.createQuery(
        "select distinct o from Order o" +
            " join fetch o.member m" +
            " join fetch o.delivery d" +
            " join fetch o.orderItems oi" +
            " join fetch oi.item i", Order.class)
        .getResultList();
}
```

> 쿼리 실행 결과

```sql
Hibernate: 
    /* select
        distinct o 
    from
        
    Order o join
        
    fetch
        o.member m 
    join
        
    fetch
        o.delivery d 
    join
        
    fetch
        o.orderItems oi 
    join
        
    fetch
        oi.item i */ select
            distinct o1_0.order_id,
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
            oi1_0.order_id,
            oi1_0.order_item_id,
            oi1_0.count,
            i1_0.item_id,
            i1_0.dtype,
            i1_0.name,
            i1_0.price,
            i1_0.stock_quantity,
            i1_0.artist,
            i1_0.etc,
            i1_0.author,
            i1_0.isbn,
            i1_0.actor,
            i1_0.director,
            oi1_0.order_price,
            o1_0.status 
        from
            orders o1_0 
        join
            member m1_0 
                on m1_0.member_id=o1_0.member_id 
        join
            delivery d1_0 
                on d1_0.delivery_id=o1_0.delivery_id 
        join
            order_item oi1_0 
                on o1_0.order_id=oi1_0.order_id 
        join
            item i1_0 
                on i1_0.item_id=oi1_0.item_id
```

* 페치 조인으로 SQL이 1번만 실행된다.
* `distinct`키워드의 역할 : 일대다 조인을 수행하는 경우 기본적으로 일에 해당하는 데이터의 row가 증가한다. 
* 사실 한 번만 조회하면 되지만 데이터의 row가 증가함에 따라 일에 해당하는 엔티티의 조회 수도 증가하게 된다.
* JPA의 `distinct` 키워드는 SQL에 `distinct` 키워드를 추가하고 같은 엔티티가 조회될 경우 애플리케이션에서 중복을 걸러준다.
* 주문과 주문상품의 관계에서 주문 엔티티에 대해 중복을 걸러주는 것이다.
* 하지만 이 컬렉션 페치 조인을 사용하면 페이징이 불가능하다. 
* 컬렉션 페치 조인은 1개만 사용할 수 있다. 컬렉션 둘 이상에 페치 조인을 사용하면 데이터 부정합으로 인해 정확하지 않을 수 있다.

## 📚 API 개발 - V4(JPA에서 DTO 직접 조회)

* 컬렉션(일대다 관계)을 페치 조인하면 페이징이 불가능하다.
* 일대다 관계의 경우 데이터의 row가 증가하는 반면, 다대일 혹은 일대일 관계의 경우 데이터의 row가 증가하지 않는다.

```text
1. `@XToOne` 관계를 모두 페치조인한다.
2. 컬렉션은 지연 로딩으로 조회한다.
3. 지연 로딩 성능 최적화를 위해 `@BatchSize`를 적용한다.
```

```java
public List<Order> findAllWithMemberDelivery(int offset, int limit) {
    return em.createQuery(
        "select o from Order o" +
            " join fetch o.member m" +
            " join fetch o.delivery d", Order.class)
        .setFirstResult(offset)    // 페이징
        .setMaxResults(limit)      // 페이징
        .getResultList();
}
```

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 1000
```

* 전역적으로 설정하고 싶지 않은 경우 `@BatchSize` 어노테이션을 적용하면 된다.

> 실행 결과

```sql
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
        offset
            ? rows 
        fetch
            first ? rows only

Hibernate: 
    select
        oi1_0.order_id,
        oi1_0.order_item_id,
        oi1_0.count,
        oi1_0.item_id,
        oi1_0.order_price 
    from
        order_item oi1_0 
    where
        oi1_0.order_id in (?, ?)

Hibernate: 
    select
        i1_0.item_id,
        i1_0.dtype,
        i1_0.name,
        i1_0.price,
        i1_0.stock_quantity,
        i1_0.artist,
        i1_0.etc,
        i1_0.author,
        i1_0.isbn,
        i1_0.actor,
        i1_0.director 
    from
        item i1_0 
    where
        i1_0.item_id in (?, ?)
```

* Order를 조회할 떄 연관 관계를 맺고 있는 Member, Delivery를 페치 조인한다.
* 이 때, 만들어둔 OrderDto에서 스트림을 사용해서 하나하나 발라내는데 배치 사이즈를 조절해주면 IN문이 사이즈만큼 한 번에 조회하게 된다.
* 쿼리 호출 수가 월등히 개선되어 N + 1 성능 이슈가 개선된다.
* V3에서 개발했던 컬렉션 패치 조인의 경우 페이징이 불가능하지만 이 방법은 페이징이 가능하다. 컨트롤러 측 코드는 아래와 같다.

```java
@GetMapping("/api/v3.1/orders")
public List<OrderDto> ordersV3_page(
		@RequestParam(value = "offset", defaultValue = "0") int offset,
		@RequestParam(value = "limit", defaultValue = "100") int limit
) {
	List<Order> all = orderRepository.findAllWithMemberDelivery(offset, limit);
	List<OrderDto> list = all.stream().map(OrderDto::new).toList();
	return list;
}	
```

> 참고 : default_batch_fetch_size 크기 선정

* 적당한 사이즈를 골라야 하는데 100 ~ 1000 사이를 선택하는 것을 권장한다.
* 이 전략은 SQL문의 IN절을 사용하는데 데이터베이스에 따라 IN절 파라미터를 1000으로 제한하기 때문이다.
* 1000으로 잡으면 한 번에 1000개를 DB에서 애플리케이션으로 불러오므로 DB에 순간 부하가 증가할 수 있으나 결국 전체 데이터를 로딩하기 때문에 메모리 사용량이 같다.

## 📚 API 개발 - V4(JPA에서 DTO로 바로 조회)

```java
public List<OrderQueryDto> findOrderItemQueryDtos() {
    
    // 쿼리 1번 OrderQueryDto 호출
    List<OrderQueryDto> result = findOrders();

    // for문 루프 반복을 통한 쿼리 N번 호출
    result.forEach(orderQueryDto -> {
	List<OrderItemQueryDto> orderItems = findOrderItems(orderQueryDto.getOrderId());
	orderQueryDto.setOrderItems(orderItems);
    });
    return result;
}

// 1 : N 관계 조회
private List<OrderItemQueryDto> findOrderItems(Long orderId) {
	return em.createQuery(
		"select new com.example.younghanapi2.api.dto.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) " +
		"from OrderItem oi " +
		"join oi.item i " +
		"where oi.order.id = :orderId", OrderItemQueryDto.class)
	.setParameter("orderId", orderId)
	.getResultList();
}

// DTO의 경우 페치 조인 적용 불가(엔티티가 아닌 데이터 전송용 객체이기 때문에)
// 1 : N 관계(여기선, 주문과 주문상품)를 제외한 나머지 한 번에 조회
public List<OrderQueryDto> findOrders() {
	return em.createQuery("select new com.example.younghanapi2.api.dto.OrderQueryDto(o.id, o.member.name, o.orderDate, o.status, o.delivery.address) from Order o" +
		" join o.member m" +
	        " join o.delivery d", OrderQueryDto.class)
	.getResultList();
}
```

* 결과적으로 루트 쿼리 1번과 컬렉션 조회 쿼리 N번이 호출되어 N + 1 성능 이슈가 발생한다.

## 📚 API 개발 - V5(컬렉션 조회 최적화)

```java
public List<OrderQueryDto> findAllByDto_optimization() {
	List<OrderQueryDto> orders = findOrders();
	List<Long> orderIds = orders.stream().map(OrderQueryDto::getOrderId).toList();	// orderId 리스트를 스트림을 사용해서 뽑아내기

	List<OrderItemQueryDto> orderItems = em.createQuery(
		"select new com.example.younghanapi2.api.dto.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) " +
		"from OrderItem oi " +
		"join oi.item i " +
		"where oi.order.id in :orderIds", OrderItemQueryDto.class)    // IN 절을 사용하여 한 번에 조회(N + 1 문제 개선)
	.setParameter("orderIds", orderIds)
	.getResultList();

	orders.forEach(orderQueryDto -> orderQueryDto.setOrderItems(orderItems));
	return orders;
}
```

* V4에서 V5로 리팩토링하면서 N + 1 문제가 해결된다.
* 루트 쿼리 1번과 컬렉션 조회 쿼리 1번(IN절 활용)이 발생한다.

## 📚 API 개발 - V6(플랫 데이터 최적화)

* 컬렉션 타입까지도 플랫(평평)하게 데이터를 가져와야 하므로 컬렉션을 풀어헤친다는 생각으로 이해하면 된다.

```java
@Data
public class OrderFlatDto {

	private Long orderId;
	private String name;
	private LocalDateTime orderDate;
	private OrderStatus orderStatus;
	private Address address;

	// OrderItem 컬렉션을 다 풀어헤친다.
	private String itemName;
	private int orderPrice;
	private int count;

	public OrderFlatDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address, String itemName, int orderPrice, int count) {
		this.orderId = orderId;
		this.name = name;
		this.orderDate = orderDate;
		this.orderStatus = orderStatus;
		this.address = address;
		this.itemName = itemName;
		this.orderPrice = orderPrice;
		this.count = count;
	}
}
```

```java
public List<OrderFlatDto> findAllByDto_flat() {
	return em.createQuery(
		"select new com.example.younghanapi2.api.dto.OrderFlatDto(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count) " +
		" from Order o" +
		" join o.member m" +
		" join o.delivery d" +
		" join o.orderItems oi" +
		" join oi.item i", OrderFlatDto.class)
	.getResultList();
}
```

* V5에서 V6로 리팩토링하면서 쿼리 1번으로 해결이 가능해졌다.
* 다만 여러 테이블에 걸쳐 조인 연산이 발생하기 때문에 성능 이슈가 발생할 수 있다.

## 📚 결론 정리

```text
1. 엔티티 조회 방식 우선
    1-1. 페치 조인으로 연관된 엔티티를 한 번에 조회
    1-2. 컬렉션 최적화
        1-2-1. 페이징이 필요한 경우라면 `@BatchSize` 어노테이션 혹은 `default_batch_fetch_size`로 최적화
        1-2-2. 페이징이 필요하지 않다면 페치 조인 사용
2. DTO 조회 방식
3. Native SQL 혹은 Jdbc Template
```
