# Reactive Programming - Practical WebFlux Patterns with Spring Boot & Building Applications with Spring Boot and WebFlux

#### 의존성 — MVC 어노테이션 그대로, 런타임만 논블로킹

```gradle
dependencies {
    // webflux 스타터가 spring-boot-starter를 전이(transitive)로 포함하므로 starter 단독 명시는 불필요
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    testImplementation 'io.projectreactor:reactor-test'   // StepVerifier
}
```

> 주의 : spring-boot-starter-web(MVC)와 함께 넣지 말 것. 둘 다 클래스패스에 있으면 Spring Boot는 MVC(서블릿 스택)로 부팅해버린다. WebFlux로 뜨려면 webflux 스타터만 있어야 한다.

#### 컨트롤러 — Mono / Flux 반환과 응답 핸들링

```java
@RestController
public class ProductController {

    // 단일 객체: Mono<T>
    @GetMapping("/products/first")
    public Mono<Product> getFirst() {
        return Mono.just(new Product("1", "노트북", 1_500_000));
    }

    // 여러 객체: Flux<T> → 기본적으로 JSON 배열로 직렬화되어 응답
    @GetMapping("/products")
    public Flux<Product> getAll() {
        return Flux.just(
                new Product("1", "노트북", 1_500_000),
                new Product("2", "마우스", 50_000),
                new Product("3", "키보드", 120_000));
    }

    // 데이터 유무로 상태 코드 분기: 있으면 200, 없으면 404
    @GetMapping("/products/available")
    public Mono<ResponseEntity<Product>> getAvailable() {
        return findAvailableProduct()                            // Mono<Product> (없으면 empty)
                .map(ResponseEntity::ok)                         // 있으면 200 OK
                .defaultIfEmpty(ResponseEntity.notFound().build());  // 비었으면 404
    }

    // 실제로는 repository.findById(...) 등 논블로킹 조회. 여기선 재고 유무만 흉내
    private Mono<Product> findAvailableProduct() {
        boolean inStock = true;
        return inStock ? Mono.just(new Product("1", "노트북", 1_500_000)) : Mono.empty();
    }
}
```

* `switchIfEmpty(Mono.just(...))` → `defaultIfEmpty(...)` : 비었을 때 넣을 게 정적 값이면 Mono로 감쌀 필요가 없다. `defaultIfEmpty`가 정확한 도구다.
* `Flux` 반환은 기본적으로 JSON 배열로 모아서 응답한다. 원소를 도착하는 대로 흘려보내려면 produces를 `APPLICATION_NDJSON_VALUE`(줄 단위 JSON)나 `TEXT_EVENT_STREAM_VALUE`(SSE)로 지정해야 한다.
* 컨트롤러가 `Mono/Flux`를 반환만 하면 실제 구독(subscribe)은 프레임워크가 한다. 개발자가 직접 `subscribe()/block()`을 호출하지 않는다.

#### SSE (Server-Sent Events) — 서버 → 클라이언트 단방향 스트림

```java
@RestController
public class SseController {

    // ⚠️ 원본은 Flux<String>이라 id/event/retry 필드를 못 쓴다.
    //    ServerSentEvent로 반환해야 아래 4개 필드를 실제로 활용할 수 있다.
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(tick))       // 이벤트 고유 ID (재연결 시 Last-Event-ID로 전달)
                        .event("time")                  // 이벤트 타입 이름
                        .data("이벤트 #" + tick)         // 실제 데이터
                        .retry(Duration.ofSeconds(3))   // 클라이언트 재연결 대기 시간
                        .build())
                .doOnCancel(() -> log.info("클라이언트 연결 종료 → interval 정리"));
    }
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9bee4c0b-4266-47ee-8639-cc672bdb64d1.png)

* SSE 응답 프레임의 4개 필드(data/id/event/retry)를 쓰려면 반환 타입이 `Flux<ServerSentEvent<T>>`여야 한다. `Flux<String>`은 data만 나가고 나머지는 못 붙인다.
* `Flux.interval`은 `parallel` 스케줄러의 별도 스레드에서 tick을 발행한다. 무한 스트림이므로 클라이언트가 끊으면 서버는 그걸 cancel 신호로 감지한다. 이 때, `doOnCancel/doFinally`로 리소스 정리 훅을 걸어두는 게 좋다.
* SSE는 텍스트(UTF-8) 전용이다. 바이너리가 필요하면 WebSocket을 써야 한다.

#### SSE vs WebSocket

**1. 프로토콜과 연결 구조**
* SSE는 일반 HTTP 위에서 동작한다. 클라이언트 요청 후 서버가 연결을 닫지 않고 text/event-stream으로 데이터를 계속 흘려보낸다.
* HTTP를 그대로 쓰므로 기존 로드밸런서·인증 미들웨어·CORS 설정이 추가 작업 없이 동작한다.
* WebSocket은 HTTP로 핸드셰이크 후 ws:///wss://로 업그레이드하고, 이후부터는 별도 프레임 구조로 통신한다.

**2. 데이터 흐름 방향**
* SSE는 서버 → 클라이언트 단방향. 클라이언트가 뭔가 보내려면 별도 HTTP 요청을 새로 만들어야 한다.
* WebSocket은 양방향. 하나의 연결에서 서버·클라이언트가 동시에 주고받는다.

**3. 재연결과 안정성**
* SSE는 브라우저가 자동 재연결한다. 끊기면 알아서 다시 연결하고 Last-Event-ID 헤더로 마지막 이벤트 이후부터 이어받을 수 있다.
* WebSocket은 재연결을 직접 구현해야 한다. 끊김 감지(ping/pong), 재연결 시도, 백오프를 애플리케이션 레벨에서 처리해야 해 코드가 복잡해진다.

**4. 구독 제어와 멀티플렉싱**
* SSE는 하나의 연결 = 하나의 스트림. 구독 대상을 바꾸려면 연결을 끊고 새 요청을 보내야 한다. 다만 HTTP/2면 한 TCP 연결에 여러 SSE 스트림을 다중화할 수 있어 연결 수 제한이 완화된다.
* WebSocket은 하나의 연결에서 메시지 타입으로 여러 채널을 논리적으로 운용한다. 예: {"type":"subscribe","symbol":"005930"}를 보내 구독 목록을 실시간 변경.

**5. Spring WebFlux 구현**
* SSE는 Flux\<ServerSentEvent>를 반환하는 컨트롤러로 간단히 구현된다.
* WebSocket은 WebSocketHandler를 구현하고 WebSocketHandlerAdapter를 등록해야 하며, 세션 관리·에러 핸들링 등 추가 작성이 필요하다.

> **선택 기준 — 클라이언트가 서버에 제어 신호를 보내야 하는가?**
> * YES → WebSocket (양방향 제어, 동적 구독, 저지연 인터랙션)
> * NO → SSE (구현 단순, 인프라 친화적, 재연결 자동)

#### WebSocket — 양방향 실시간 스트림

```java
// 핸들러 인터페이스
public interface WebSocketHandler {

    // 핸드셰이크 시 협상할 서브 프로토콜 목록 (기본: 미사용)
    default List<String> getSubProtocols() {
        return Collections.emptyList();
    }

    // WebSocket 세션 처리. 반환된 Mono가 완료되면 세션 종료, 에러면 비정상 종료.
    Mono<Void> handle(WebSocketSession session);
}
```
```java
// 구현 — 서버 시간 push + 클라이언트 메시지 수신
@Component
public class TimeWebSocketHandler implements WebSocketHandler {

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 서버 → 클라이언트: 1초마다 현재 시간 (무한 스트림)
        Flux<WebSocketMessage> serverToClient = Flux.interval(Duration.ofSeconds(1))
                .map(tick -> session.textMessage("서버 시간: " + LocalDateTime.now()));

        // 클라이언트 → 서버: 수신 로그만 남기고 완료 신호(then)만 전달
        Mono<Void> clientToServer = session.receive()
                .doOnNext(msg -> log.info("클라이언트: {}", msg.getPayloadAsText()))
                .then();

        // 두 방향을 함께 실행 — 한쪽이 끝나면 zip이 종료되며 세션이 닫힌다
        return Mono.zip(session.send(serverToClient), clientToServer).then();
    }
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/14237b38-5ccc-4e2c-9c14-f4178f0b9ab8.png)

* Phase 1 : HTTP 핸드셰이크로 WebSocket 수립. TCP 연결 후 WS 프로토콜로 전환.
* Phase 2 : `Mono.zip(send, receive)`이 핵심. `send()`는 interval 기반 무한 스트림이라 혼자서는 완료를 모른다. 클라이언트가 끊으면 `receive()`가 완료되고, `zip()`이 그 완료를 받아 나머지(send)를 취소하며 세션 전체를 종료시킨다.

#### 보완 — "진짜 서버 push"는 Sinks로

* `Flux.interval`은 타이머 기반이라 실제 이벤트(주문 발생, 가격 변동)를 밀어 넣진 못한다. 
* 외부에서 발생한 이벤트를 SSE/WebSocket 구독자들에게 실시간 브로드캐스트하려면 Sinks(리액티브 이벤트 버스)를 쓴다.

```java
// 여러 구독자에게 이벤트를 뿌리는 hot 스트림
private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

// 어딘가에서 이벤트 발생 시 push
public void publish(String event) {
    sink.tryEmitNext(event);
}

// SSE 컨트롤러는 이 sink를 구독
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream() {
    return sink.asFlux();   // 여러 클라이언트가 같은 이벤트 흐름을 공유(hot)
}
```

* `interval`이 1초마다 만들어내는 스트림이라면, `Sinks`는 외부 이벤트를 받아 구독자들에게 전달하는 hot 스트림이다. 이게 흔히 말하는 실시간 push의 실체다.
* `multicast()` = 여러 구독자 공유, `onBackpressureBuffer()` = 느린 구독자를 위한 버퍼링 전략.
