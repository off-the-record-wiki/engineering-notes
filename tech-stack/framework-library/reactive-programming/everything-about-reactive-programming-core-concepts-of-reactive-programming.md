# Reactive Programming - Everything About Reactive Programming & Core Concepts of Reactive Programming

#### Mono(단일 값 Publisher)

* 단 하나의 데이터만 발행하거나, 아예 비어있거나, 에러를 내고 종료되는 Publisher이다. 값을 하나 방출하면 즉시 완료된다.

**1. 생성(Creation)**
* `Mono.just(data)` — 이미 계산된 데이터를 감싼다.
* `Mono.empty()` — 값이 없음을 표현
* `Mono.defer(Supplier)` — 구독 시점까지 실행을 지연
* `Mono.fromCallable(Callable)` — 블로킹 작업을 어댑팅

**2. 변환(Transformation)**
* `map()` — 동기 값 변환
* `flatMap()` — 비동기 변환 + 평탄화
* `filter()` — 조건부 통과

**3. 에러 처리(Error Handling)**
* `defaultIfEmpty()` — 빈 스트림에 기본값 제공
* `switchIfEmpty()` — 빈 경우 대체 비동기 흐름으로 전환
* `onErrorReturn()` — 에러 시 폴백 값 반환
* `onErrorResume()` — 에러를 대체 `Mono`로 처리

#### Flux(0~N개 값 Publisher)

* 0개부터 무한 개까지 값을 방출하고 완료 또는 에러로 종료된다.

**1. 생성(Creation)**
* `Flux.just()` — 여러 개의 사전 정의 값
* `Flux.fromIterable()` — 컬렉션을 스트림으로 변환
* `Flux.range()` — 숫자 시퀀스 생성
* `Flux.interval()` — 시간 기반 방출

**2. 변환(Transformation)**
* `map()` — 원소별 동기 변환
* `flatMap()` — 비동기 처리, 순서 보장 안 됨(여러 내부 Publisher 동시 구독)
* `concatMap()` — 비동기 순차 처리, 순서 유지
* `collectList()` — `Mono<List<T>>`로 집계

> 주의 : `flatMap`은 병렬이 아니라 여러 내부 Publisher를 동시에 구독(interleaving)하는 것이다. 기본 동시성 제한(concurrency)이 있으며, 진짜 CPU 병렬은 `parallel().runOn(...)`을 쓴다.

**3. 스트림 에러 처리**
* `onErrorResume()` — 개별 원소 복구
* `onErrorContinue()` — Reactive Streams 계약을 깨는 특수 연산자라 지양. 원소별 복구는 flatMap 안에서 onErrorResume으로 처리하는 게 정석이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/38d80276-d4d8-4846-a7fd-aa8b9190f884.png)

| 구분      | `List<User>`              | `Flux<User>`               |
| --------- | ------------------------- | -------------------------- |
| 존재 방식 | 모든 원소가 지금 메모리에 | 하나씩 시간차를 두고 도착  |
| 전부 로드 | 네, 통째로 메모리에       | 아니오, 오는 대로 처리     |
| 무한 가능 | 불가능 (메모리 초과)      | 가능 (SSE, interval)       |
| 처리 시점 | 다 받은 뒤 순회           | 도착 즉시 처리             |

| 원하는 것              | 반환 타입          |
| ---------------------- | ------------------ |
| 단건                   | `Mono<T>`          |
| 오는 대로 처리·스트리밍 | `Flux<T>`          |
| 다 모은 리스트 (MVC식) | `Mono<List<T>>`    |

#### 구독 & Lazy Evaluation

* 하나 명심할 부분은 아무리 복잡한 Mono/Flux 체인을 만들어도, `subscribe()`가 호출되기 전엔 아무것도 실행되지 않는다.
* WebFlux 환경에서는 프레임워크가 구독을 자동 관리한다. 개발자가 직접 `subscribe()`를 호출할 일은 거의 없다. 컨트롤러가 `Mono/Flux`를 반환하면 Netty가 내부적으로 구독을 처리한다.

```java
// (함정) just는 인자를 assembly time에 즉시 평가한다 → 구독 전에 실행됨
Mono.just(blockingCall());                // ❌ eager
Mono.fromCallable(() -> blockingCall());  // ✅ lazy (그래서 defer/fromCallable이 필요)
```

#### Backpressure

* 소비자가 감당할 수 있는 양만 요청(request)하여 데이터 흐름을 조절해 메모리 오버플로우를 방지한다.
* 실체는 `Subscription.request(n)` — 구독자가 N개를 더 달라고 신호를 보낸다.
* 오버플로우 전략 : `onBackpressureBuffer()` / `onBackpressureDrop()` / `onBackpressureLatest()`
* BaseSubscriber로 `request(n)`을 직접 제어할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d9f97804-1c4d-474d-8b20-312c7322a17e.png)

#### Disposable & 리소스 관리

* `subscribe()`가 반환하는 `Disposable`로 스트림을 취소할 수 있다.
* `dispose()` — 상위(upstream)로 취소 신호를 전파한다.
* `isDisposed()` — 구독 상태를 확인한다.
* 무한 스트림(SSE, Kafka 리스너)에서 특히 중요
* 취소 신호가 상위 생산자까지 전파되어 즉시 리소스 정리를 유발한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2c9b4c30-8fd9-4ba5-96ac-1cbcd518f82e.png)

* `subscribe()`를 하는 순간, 각 단계가 서로를 참조하는 연결 사슬이 만들어진다.
* 여기서 `cancel()`을 호출하게 되면 상위 `Subscription`을 들고 있으므로 연쇄적으로 취소가 된다.

#### 스레드 모델

* 이벤트 루프 : WebFlux는 소수의 이벤트 루프 스레드(Netty, 보통 코어 수)로 모든 요청을 처리한다.
* MVC는 요청 1개당 쓰레드 1개를 할당해 블로킹돼도 그 쓰레드만 멈추지만 WebFlux는 쓰레드 몇 개가 수천 요청을 번갈아 처리한다.

```java
// ❌ 이벤트 루프에서 블로킹 금지(안티패턴 - 이벤트 루프 스레드를 점유해 전체 처리량이 붕괴)
User user = jdbcUserRepository.findById(id); // 블로킹 JDBC!
```
```java
// ✅ 불가피하면 격리한다.
Mono.fromCallable(() -> jdbcUserRepository.findById(id))
    .subscribeOn(Schedulers.boundedElastic());                // boundedElastic() — 블로킹 IO 격리
```
> 블로킹 대상: JDBC, RestTemplate, `Thread.sleep()`, 동기 파일 IO 등.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/bc4b2dc1-010d-4293-872c-c5c85ef2d666.png)

* Netty 하나 안에 스레드(이벤트 루프)가 여러 개가 있는 구조이며, 하나의 이벤트 루프(스레드)가 수천 개의 커넥션을 번갈아가며 처리한다.
* 스레드가 각 커넥션마다 대기(블로킹)하지 않고, 현재 처리할 데이터가 준비된 커넥션만 골라서 빠르게 돌아가며 처리하기 때문에 가능하다.

#### Reactive Streams 신호 흐름

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/df8ae54a-241f-4564-8fc5-940c29b73d50.png)

#### 언제 WebFlux를 쓰는가(트레이드오프)

* 리액티브는 자동으로 빠르지 않다. 적은 스레드로 고동시성을 버티는 게 본질이다.
* 적합 : 고동시성 + I/O 바운드(게이트웨이/BFF), SSE/WebSocket 스트리밍, 논블로킹 스택 전체
* 부적합 : 단순 CRUD, CPU 바운드, 팀 미숙 → MVC + 가상 스레드(Loom)가 더 단순한 경우가 많다.
