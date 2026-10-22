# MicroService Architecture - CQRS Patterns

#### CQRS(Command Query Responsibility Segregation) 패턴

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fc7603a5-d70c-4386-b182-06ec9d57eb5c.png)

* DB를 읽기 및 쓰기 작업으로 분리하는데 이렇게 하는 이유는 비효율적인 조인이나 복잡한 쿼리를 피하기 위해서이다.
  * Command : 데이터 상태를 애플리케이션에 반영 및 변경
  * Query : 복잡한 조인 작업 처리, 결과 반환 용도. 데이터 상태를 애플리케이션에서 변경하지 않는다.

#### CQRS Level 1 정리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fa777209-af60-49e2-a488-0e34babdcfa6.png)

* 분리 수준 : Controller 및 Service만 분리
* 장점 : 간단한 구조, 구현 난이도 낮음
* 단점 : 성능상 큰 장점은 없음, 확장성 제한적

#### CQRS Level 2 정리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f974e323-26b8-4773-9c49-afa0abe2124c.png)

* 분리 수준 : Controller, Service, DB 스키마 분리
* 장점 : 읽기 모델을 조회 최적화하여 성능 향상 가능
* 단점 : 구현 및 관리 복잡성 증가

#### CQRS Level 3 정리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a2183cc5-20d6-4b1a-94c8-79293453a346.png)

* 분리 수준 : Controller, Service, DB 스키마, 물리적 DB 분리
* 장점 : 읽기/쓰기 성능 및 확장성 극대화
* 단점 : 복잡성, 비용, 관리 난이도 증가

#### CQRS(Command Query Responsibility Segregation) - Read and Write Operations

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fc7603a5-d70c-4386-b182-06ec9d57eb5c.png)

* DB 읽기 및 쓰기에 서로 다른 접근 방식으로 서로 다른 전략을 정의한다.
* 이렇게 하는 이유는 읽기 및 쓰기 작업이 성능과 확장성 요구 사항이 다르기 때문이다.

#### Event Sourcing

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/81eb55a5-2314-498a-ac0c-3a227e81555a.png)

* 데이터 변경 이력(상태 변화)을 DB(Event Store)에 저장한다.
* 상태를 직접 저장하지 않고, 발생한 이벤트들을 저장한다.
* 저장된 이벤트를 재생해서 상태를 복원한다.
* 최신 데이터 상태만 DB에 저장하는 것이 아니라 모든 이벤트를 순차적으로 DB에 저장한다.
* Event Store : 이벤트 데이터베이스 역할로 이벤트 저장소가 된다.
* Write DB에는 이벤트를 저장하고 Read DB에서는 Write DB에서 발행한 이벤트를 소비하는 구조가 된다.
* 시스템은 쿼리 성능을 높이고 DB에 대한 독립적 확장이 가능하다.
* 장점
  * 독립적인 확장성, 분리된 읽기 및 쓰기 기능
  * 쿼리 성능 향상
  * Read DB의 비정규화된 데이터로 복잡하고 오래 실행되는 조인 쿼리를 최소화할 수 있다.
  * 유지 관리 및 유연성 향상
  * Kubernetes 배포에 적합한 분산 수평 확장 DB 적용
* 단점
  * 구현 복잡성이 증가 - CQRS 자체가 시스템을 더 복잡하게 설계하게 된다는 것을 인지해야 한다.
  * Eventual Consistency 문제가 있다.
  * Distributed Transaction Management 문제가 있다.

#### CQRS 전체 흐름 정리 1

```java
@Service
public class EventCommandService {
    private final OrderEventRepository orderEventRepository;
    private final ApplicationEventPublisher publisher;
    private final Gson gson = new Gson();

    public EventCommandService(OrderEventRepository orderEventRepository, ApplicationEventPublisher publisher) {
        this.orderEventRepository = orderEventRepository;
        this.publisher = publisher;
    }

    // 주문 생성/수정 시 OrderEvent를 DB에 저장하고 ApplicationEventPublisher로 이벤트 발행한다.
    public void createOrder(OrderDto orderDto) {
        OrderEvent event = new OrderEvent();
        event.setEventType(EventType.EVENT_CREATED);

        event.setOrderId(orderDto.getOrderId());
        event.setUserId(orderDto.getUserId());
        event.setPayload(gson.toJson(EventPayload.builder()
                .productId(orderDto.getProductId())
                .qty(orderDto.getQty())
                .unitPrice(orderDto.getUnitPrice())
                .totalPrice(orderDto.getTotalPrice())
                .build()));
        event.setTimestamp(LocalDateTime.now());

        orderEventRepository.save(event);
        publisher.publishEvent(event);
    }

    public void updateUser(String orderId, OrderDto orderDto) {
        OrderEvent event = new OrderEvent();
        event.setEventType(EventType.EVENT_UPDATED);
        event.setOrderId(orderId);
        event.setUserId(orderDto.getUserId());
        event.setPayload(gson.toJson(EventPayload.builder()
                .productId(orderDto.getProductId())
                .qty(orderDto.getQty())
                .unitPrice(orderDto.getUnitPrice())
                .totalPrice(orderDto.getTotalPrice())
                .build()));
        event.setTimestamp(LocalDateTime.now());

        orderEventRepository.save(event);
        publisher.publishEvent(event);
    }
}
```
```java
@Service
public class EventQueryService {
    private final OrderEventRepository orderEventRepository;

    public EventQueryService(OrderEventRepository orderEventRepository) {
        this.orderEventRepository = orderEventRepository;
    }

    // DB에 쌓인 이벤트들을 순서대로 replay해서 현재 상태를 재구성한다.
    public OrderDto replayOrder(String orderId) {
        List<OrderEvent> events = orderEventRepository.findByOrderIdOrderByTimestamp(orderId);

        OrderDto order = new OrderDto();
        events.forEach(order::apply);  // apply 메소드로 상태 복원
//        for (OrderEvent event : events) {
//            order.apply(event);
//        }
        order.setOrderId(orderId);

        return order;
    }

    // DB에 쌓인 이벤트들을 순서대로 replay해서 현재 상태를 재구성한다.
    public List<OrderDto> replayAllOrder(String userId) {
        List<String> orderIds = orderEventRepository.findDistinctByUserIdOrderByTimestamp(userId);

        List<OrderDto> orderList = new ArrayList<>();
        // 이벤트 순차적 replay
        for (String orderId : orderIds) {
            List<OrderEvent> orderEvents = orderEventRepository.findByOrderIdOrderByTimestamp(orderId);

            OrderDto order = new OrderDto();
            orderEvents.forEach(order::apply);  // apply 메소드로 상태 복원
            order.setOrderId(orderId);

            orderList.add(order);
        }

        return orderList;
    }
}
```
```java
@Data
public class OrderDto implements Serializable {
    private String productId;
    private Integer qty;
    private Integer unitPrice;
    private Integer totalPrice;

    private String orderId;
    private String userId;

    private boolean deleted;

    private static final Gson gson = new Gson();

    // 이벤트를 받아 상태를 변환하는 메서드
    public void apply(OrderEvent event) {
        switch (event.getEventType()) {
            case EVENT_CREATED:
                applyOrderCreated(event);
                break;

            case EVENT_UPDATED:
                applyOrderUpdated(event);
                break;

            case EVENT_DELETED:
                applyOrderDeleted(event);
                break;

            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }

    private void applyOrderCreated(OrderEvent event) {
        OrderCreatedData data = gson.fromJson(event.getPayload(), OrderCreatedData.class);
        this.userId = data.getUserId();
        this.orderId = data.getOrderId();
        this.productId = data.getProductId();
        this.qty = data.getQty();
        this.unitPrice = data.getUnitPrice();
        this.totalPrice = data.getTotalPrice();

        this.deleted = false;
    }

    private void applyOrderUpdated(OrderEvent event) {
        OrderUpdatedData data = gson.fromJson(event.getPayload(), OrderUpdatedData.class);
        this.productId = data.getProductId();
        this.qty = data.getQty();
        this.unitPrice = data.getUnitPrice();
        this.totalPrice = data.getTotalPrice();
    }

    private void applyOrderDeleted(OrderEvent event) {
        this.deleted = true;
    }
}
```
```java
// Orders 테이블에 저장하는 것 대신, OrderEvent 테이블에 상태를 기록한다.
@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    List<OrderEvent> findByOrderIdOrderByTimestamp(String orderId);

    @Query("SELECT DISTINCT o.orderId FROM OrderEvent o WHERE o.userId = :userId")
    List<String> findDistinctByUserIdOrderByTimestamp(@Param("userId") String userId);
}
```

* 상태(State)를 저장하는 대신 이벤트(Event)를 저장하고, 조회 시 이벤트를 처음부터 재생(Replay)해 현재 상태를 도출하는 Event Sourcing + CQRS 패턴의 예시이다.

#### CQRS 전체 흐름 정리 2

```java
@RestController
@RequestMapping("/")
@Slf4j
public class OrderCommandController {
    Environment env;
//    OrderCommandServiceImpl orderCommandService;
    EventCommandService eventCommandService;

    @Autowired
    public OrderCommandController(Environment env, EventCommandService eventCommandService) {
        this.env = env;
        this.eventCommandService = eventCommandService;
    }

    @PostMapping("/{userId}/orders")
    public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId,
                                                     @RequestBody RequestOrder orderDetails) {
        log.info("Before add orders data");
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
        orderDto.setUserId(userId);
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDetails.getQty() * orderDetails.getUnitPrice());

        /* event publish */
        eventCommandService.createOrder(orderDto);

        log.info("After added orders data");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{userId}/orders/{orderId}")
    public ResponseEntity<ResponseOrder> updateOrder(@PathVariable("userId") String userId,
                                                     @PathVariable("orderId") String orderId,
                                                     @RequestBody RequestOrder orderDetails) {
        log.info("Before update orders data");
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
        orderDto.setUserId(userId);
        orderDto.setOrderId(orderId);
        orderDto.setTotalPrice(orderDetails.getQty() * orderDetails.getUnitPrice());

        /* event publish */
        eventCommandService.updateUser(orderId, orderDto);

        log.info("After updated orders data");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
```
```java
@Slf4j
@Service
public class EventCommandService {
    private final OrderEventRepository orderEventRepository;
    private final ApplicationEventPublisher publisher;
    private final Gson gson = new Gson();

    public EventCommandService(OrderEventRepository orderEventRepository, ApplicationEventPublisher publisher) {
        this.orderEventRepository = orderEventRepository;
        this.publisher = publisher;
    }

    public void createOrder(OrderDto orderDto) {
        OrderEvent event = new OrderEvent();
        event.setEventType(EventType.EVENT_CREATED);

        event.setOrderId(orderDto.getOrderId());
        event.setUserId(orderDto.getUserId());
        event.setPayload(gson.toJson(EventPayload.builder()
                .productId(orderDto.getProductId())
                .qty(orderDto.getQty())
                .unitPrice(orderDto.getUnitPrice())
                .totalPrice(orderDto.getTotalPrice())
                .build()));
        event.setTimestamp(LocalDateTime.now());

        orderEventRepository.save(event);

        log.info("이벤트 발행 {}", event.getUserId());
        publisher.publishEvent(event);
    }

    public void updateUser(String orderId, OrderDto orderDto) {
        OrderEvent event = new OrderEvent();
        event.setEventType(EventType.EVENT_UPDATED);
        event.setOrderId(orderId);
        event.setUserId(orderDto.getUserId());
        event.setPayload(gson.toJson(EventPayload.builder()
                .productId(orderDto.getProductId())
                .qty(orderDto.getQty())
                .unitPrice(orderDto.getUnitPrice())
                .totalPrice(orderDto.getTotalPrice())
                .build()));
        event.setTimestamp(LocalDateTime.now());

        orderEventRepository.save(event);
        publisher.publishEvent(event);
    }
}
```
```java
@Entity
@Data
public class OrderEvent {
    @Id
    @GeneratedValue
    private Long eventId;
    private String orderId;
    private String userId;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private String payload; // JSON 형식으로 이벤트 데이터 저장

    private LocalDateTime timestamp;
}
```
```java
@RestController
@RequestMapping("/")
@Slf4j
public class OrderQueryController {
    Environment env;
    EventQueryService eventQueryService;

    @Autowired
    public OrderQueryController(Environment env, EventQueryService eventQueryService) {
        this.env = env;
        this.eventQueryService = eventQueryService;
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<OrderDto>> getOrdersByUserId(@PathVariable("userId") String userId) throws Exception {
        log.info("Before retrieve orders data");
        List<OrderDto> orderList = eventQueryService.replayAllOrder(userId);

        log.info("After retrieved orders data");

        return ResponseEntity.status(HttpStatus.OK).body(orderList);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable("orderId") String orderId) {
        log.info("Before retrieve order data");

        OrderDto orderDto = eventQueryService.replayOrder(orderId);

        log.info("After retrieve order data");
        return ResponseEntity.status(HttpStatus.OK).body(orderDto);
    }
}
```
```java
@Service
public class EventQueryService {
    private final OrderEventRepository orderEventRepository;

    public EventQueryService(OrderEventRepository orderEventRepository) {
        this.orderEventRepository = orderEventRepository;
    }

    public OrderDto replayOrder(String orderId) {
        List<OrderEvent> events = orderEventRepository.findByOrderIdOrderByTimestamp(orderId);

        OrderDto order = new OrderDto();
        events.forEach(order::apply);  // apply 메소드로 상태 복원
//        for (OrderEvent event : events) {
//            order.apply(event);
//        }
        order.setOrderId(orderId);

        return order;
    }

    public List<OrderDto> replayAllOrder(String userId) {
        List<String> orderIds = orderEventRepository.findDistinctByUserIdOrderByTimestamp(userId);

        List<OrderDto> orderList = new ArrayList<>();
        // 이벤트 순차적 replay
        for (String orderId : orderIds) {
            List<OrderEvent> orderEvents = orderEventRepository.findByOrderIdOrderByTimestamp(orderId);

            OrderDto order = new OrderDto();
            orderEvents.forEach(order::apply);  // apply 메소드로 상태 복원
            order.setOrderId(orderId);

            orderList.add(order);
        }

        return orderList;
    }
}
```
```java
@Data
public class OrderDto implements Serializable {
    private String productId;
    private Integer qty;
    private Integer unitPrice;
    private Integer totalPrice;

    private String orderId;
    private String userId;

    private boolean deleted;

    private static final Gson gson = new Gson();

    public void apply(OrderEvent event) {
        switch (event.getEventType()) {
            case EVENT_CREATED:
                applyOrderCreated(event);
                break;

            case EVENT_UPDATED:
                applyOrderUpdated(event);
                break;

            case EVENT_DELETED:
                applyOrderDeleted(event);
                break;

            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }

    private void applyOrderCreated(OrderEvent event) {
        OrderCreatedData data = gson.fromJson(event.getPayload(), OrderCreatedData.class);
        this.userId = data.getUserId();
        this.orderId = data.getOrderId();
        this.productId = data.getProductId();
        this.qty = data.getQty();
        this.unitPrice = data.getUnitPrice();
        this.totalPrice = data.getTotalPrice();

        this.deleted = false;
    }

    private void applyOrderUpdated(OrderEvent event) {
        OrderUpdatedData data = gson.fromJson(event.getPayload(), OrderUpdatedData.class);
        this.productId = data.getProductId();
        this.qty = data.getQty();
        this.unitPrice = data.getUnitPrice();
        this.totalPrice = data.getTotalPrice();
    }

    private void applyOrderDeleted(OrderEvent event) {
        this.deleted = true;
    }
}
```

* 쓰기는 이벤트를 DB에 저장하고, 읽기는 그 이벤트를 순서대로 재생(replay)해서 현재 상태를 만든다.
