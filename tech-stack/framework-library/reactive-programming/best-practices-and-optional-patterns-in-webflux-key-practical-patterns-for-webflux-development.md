# Reactive Programming - Best Practices and Optional Patterns in WebFlux & Key Practical Patterns for WebFlux Development

#### `onErrorReturn` — 에러를 고정 기본값으로

* 에러가 터지는 순간, 준비해 둔 Fallback 값을 방출하고 스트림을 정상 종료(onComplete) 시킨다.

```java
import reactor.core.publisher.Flux;

public class OnErrorReturnExample {

    public static void main(String[] args) {
        // 1) ArithmeticException 매칭 → -1로 복구 후 정상 종료
        divide()
                .onErrorReturn(ArithmeticException.class, -1)
                .subscribe(
                        data  -> System.out.println("[case1] onNext: " + data),
                        error -> System.err.println("[case1] onError: " + error),
                        ()    -> System.out.println("[case1] onComplete"));

        System.out.println("--------------------------------");

        // 2) NPE만 매칭 → 실제 터진 건 ArithmeticException이라 매칭 실패 → 그대로 전파
        divide()
                .onErrorReturn(NullPointerException.class, -1)
                .subscribe(
                        data  -> System.out.println("[case2] onNext: " + data),
                        error -> System.err.println("[case2] onError: " + error),
                        ()    -> System.out.println("[case2] onComplete"));
    }

    // 두 케이스가 공유하는 파이프라인을 추출 (중복 제거)
    private static Flux<Integer> divide() {
        return Flux.just(1, 2, 0, 4, 5).map(n -> 10 / n);
    }
}
```
* 신호 전환 : `onError` 신호를 가로채 `onNext(fallback) → onComplete`로 바꿔 Graceful Shutdown 한다.
* 조기 종료 : 에러 지점에서 업스트림이 취소되므로, 뒤에 남아있던 4, 5는 발행되지 않는다.
* 타입 필터 : `onErrorReturn(예외타입.class, 값)`은 그 예외 타입일 때만 복구한다. case 2처럼 매칭 실패하면 복구하지 않고 에러를 그대로 흘려보낸다.
* 사실 `onErrorReturn(v)`는 `onErrorResume(e -> Mono.just(v))`의 축약형이다.

#### `onErrorResume` — 에러를 대체 스트림으로

* 에러가 터지면 다른 Publisher로 갈아탄다. Java의 try/catch를 리액티브 명세에 맞게 구현한 것이다.

```java
import reactor.core.publisher.Flux;

public class OnErrorResumeExample {

    public static void main(String[] args) {
        Flux.just(1, 2, 0, 4, 5)
                .map(n -> 10 / n)
                .onErrorResume(error -> {
                    System.out.println("가로챈 에러: " + error.getMessage());
                    // 에러 시점에 대안 스트림을 만들어 바톤 터치
                    return Flux.just(100, 200, 300);
                })
                .subscribe(
                        data  -> System.out.println("onNext: " + data),
                        error -> System.out.println("onError: " + error.getMessage()),
                        ()    -> System.out.println("onComplete"));
    }
}
```



```java
import reactor.core.publisher.Flux;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // onErrorMap: 발생한 에러 신호를 다른 예외(Exception) 타입으로 변환하여 다운스트림으로 전파

        Flux.just(1, 2, 0, 4, 5)
                .map(n -> 10 / n)
                .onErrorMap(ArithmeticException.class, 
                        e -> new IllegalArgumentException("0으로 나눌 수 없습니다.", e))
                .subscribe(
                        System.out::println,
                        error -> {
                            System.out.println("에러 발생: " + error.getMessage());
                            System.out.println("에러 타입: " + error.getClass().getSimpleName());
                        });
    }
}
```

* 동적 라우팅 : 에러 객체를 람다 인자로 받으므로, 예외 종류에 따라 서로 다른 대체 스트림으로 분기할 수 있다.
* `onErrorReturn`과 마찬가지로 원본 뒤쪽 데이터는 발행되지 않는다.
* 대체 스트림에 실제 호출/부작용이 있으면 `Flux.defer(...)`로 감싸 구독 시점에 생성되게 하는 게 안전하다(eager 평가 함정).
* 실무 핵심 패턴 : `flatMap()` 안에서 개별 원소 단위로 `onErrorResume()`을 걸면, 한 원소의 실패가 전체 스트림을 죽이지 않고 그 원소만 대체·건너뛸 수 있다.(전체를 살리려고 `onErrorContinue()`를 쓰는 것보다 이 방식이 권장된다.)

#### `onErrorMap` — 에러 타입을 변환

* 복구가 아니라 에러를 다른 예외로 바꿔 다시 던진다. 스트림은 여전히 에러로 종료된다.

```java
import reactor.core.publisher.Flux;

public class OnErrorMapExample {

    public static void main(String[] args) {
        Flux.just(1, 2, 0, 4, 5)
                .map(n -> 10 / n)
                .onErrorMap(ArithmeticException.class,
                        e -> new IllegalArgumentException("0으로 나눌 수 없습니다.", e))
                .subscribe(
                        System.out::println,
                        error -> {
                            System.out.println("에러 메시지: " + error.getMessage());
                            System.out.println("에러 타입: " + error.getClass().getSimpleName());
                        });
    }
}
```

* 에러 상태 유지 : 변환 결과도 결국 `Throwable`이므로 스트림은 성공 종료되지 않고 최종 에러 종료된다.
* 원인 보존 : 두 번째 인자로 원본 예외(`e`)를 넘겨 `cause`로 감싸면 스택트레이스가 유실되지 않는다.(실무에서 꼭 넘길 것)
* 용도 : 하위 레이어의 기술 예외(`SQLException`, `WebClientResponseException` 등)를 상위 레이어가 이해하는 도메인 예외로 번역할 때. 계층 경계에서 예외를 정제하는 역할을 한다.

#### 

#### `retry` — 실패하면 처음부터 재구독

* 일시적 오류(네트워크 지연, DB 타임아웃)에 대한 회복 탄력성(Resilience) 연산자. 에러가 나면 파이프라인 전체를 처음부터 다시 구독한다.

```java
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Mono;

public class RetryExample {

    public static void main(String[] args) {
        AtomicInteger callCount = new AtomicInteger();

        // defer: 재구독할 때마다 내부 블록이 매번 새로 평가되도록 (retry의 필수 짝)
        Mono.defer(() -> {
                    int count = callCount.incrementAndGet();
                    System.out.println("호출 #" + count);
                    return count < 3
                            ? Mono.error(new RuntimeException("일시적 오류"))
                            : Mono.just("성공");
                })
                .retry(3)   // ⚠️ 인자 없는 retry()는 무한 재시도 → 실무 금지. 반드시 횟수 제한.
                .subscribe(
                        data  -> System.out.println("onNext: " + data),
                        error -> System.out.println("onError: " + error.getMessage()));
    }
}
```
* **`retry()` (무한)은 실무 금지.** 장애가 지속되면 재시도가 폭주해 오히려 시스템을 무너뜨린다. 반드시 `retry(n)` 또는 `retryWhen`으로 상한을 둔다.
* **재구독 = 전체 재실행.** `map`, `doOnNext`, DB 저장 같은 **모든 사이드 이펙트가 다시 실행**된다. 비멱등(non-idempotent) 작업(결제, 주문 생성)에 무턱대고 걸면 중복 실행 위험이 있다.
* **`Mono.defer`가 필요한 이유**: `Mono.just(...)`는 조립 시점에 값이 고정되지만, `defer`는 재구독마다 람다를 다시 평가해 카운터 증가 같은 상태 변화를 반영한다.

#### `retryWhen` — 백오프 전략을 주입한 재시도

* 단순 반복을 넘어 **지연·횟수·조건**을 제어하는 재시도의 실전형이다.

```java
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class RetryWhenExample {

    public static void main(String[] args) {
        AtomicInteger callCount = new AtomicInteger();

        String result = Mono.defer(() -> {
                    int count = callCount.incrementAndGet();
                    System.out.println("callCount: " + count);
                    return count < 4
                            ? Mono.error(new RuntimeException("일시적 오류"))
                            : Mono.just("성공");
                })
                .retryWhen(Retry.fixedDelay(5, Duration.ofMillis(500)))
                // 데모용: 종료 신호까지 대기. block()으로 Thread.sleep(임의 대기)을 대체.
                // (실무 파이프라인 내부에서는 block() 금지 — 여기선 main 종료 방지 용도)
                .block();

        System.out.println("결과: " + result);
    }
}
```

* `retryWhen`의 지연은 **parallel 스케줄러(별도 스레드)에서 비동기로** 돈다.

> **지수 백오프 + 지터 + 조건 필터**
> * `fixedDelay`(고정 간격)보다 실전에서는 **지수 백오프(backoff)** 가 표준이다. 재시도 간격을 점점 늘리고, **지터(jitter)** 로 여러 인스턴스가 동시에 몰리는 thundering herd를 방지한다.
> * **`filter`가 핵심** : 5xx·타임아웃 같은 **일시적** 에러만 재시도하고, 400·인증 실패 같은 **영구적** 에러는 재시도해도 소용없으니 즉시 실패시켜야 한다. 조건 없이 다 재시도하면 리소스 낭비가 된다.
> * `onRetryExhaustedThrow`를 안 주면 재시도 소진 시 `RetryExhaustedException`으로 감싸져, 원인 예외를 다루기 번거로워진다.

#### `doOnXxx` — 흐름을 바꾸지 않는 사이드 이펙트

* 데이터를 변형하지 않고 특정 신호가 지나갈 때 부수 행동(로깅, 메트릭, 디버깅)만 수행한다. 
* Consumer를 받으므로 값을 교체하거나 흐름을 제어할 수 없다.

```java
import reactor.core.publisher.Flux;

public class DoOnNextExample {

    public static void main(String[] args) {
        Flux.just("apple", "banana", "cherry")
                .doOnNext(v -> System.out.println("변형 전: " + v))
                .map(String::toUpperCase)
                .doOnNext(v -> System.out.println("변형 후: " + v))
                .subscribe(v -> System.out.println("최종: " + v));
    }
}
```

#### 사이드 이펙트 연산자 발화 시점

| 연산자 | 발화 시점 |
| --- | --- |
| `doFirst` | 구독 프로세스 **최초 진입** 시 (체인 위치와 무관하게 가장 먼저) |
| `doOnSubscribe` | `Subscription` 객체가 전달될 때 |
| `doOnRequest` | 다운스트림이 `request(n)`을 보낼 때 |
| `doOnNext` | `onNext`(데이터)가 통과할 때 |
| `doOnError` | `onError`가 통과할 때 (**관찰만**, 에러는 계속 전파됨) |
| `doOnComplete` | `onComplete` 시 (Flux) |
| `doOnSuccess` | 성공 종료 시 (Mono, 방출값 포함) |
| `doOnCancel` | 다운스트림이 `cancel()` 했을 때 |
| `doOnEach` | 위 모든 개별 시그널마다 |
| `doOnTerminate` | 완료/에러로 종료되기 **직전** (취소 제외) |
| `doFinally` | 완료/에러/**취소** 어떤 이유로든 종료된 **직후** |
