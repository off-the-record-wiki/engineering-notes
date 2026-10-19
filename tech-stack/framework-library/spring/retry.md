# Spring - Retry

> Retry
> 
> * Spring Retry는 메소드를 호출해서 예외가 발생 했을 때, 자동으로 지정한 메소드를 다시 호출하는 기능을 제공한다. 일시적인 네트워크 접속 장애가 발생했을 때, **Failover**으로 유용하다.<br>
> * **Failover** : 시스템 대체 작동, 평소 사용하는 서버와 그 서버의 클론 서버를 가지고 있다가 사용 서버가 장애로 사용이 어렵게 되었을 경우 클론 서버로 그 일을 대신하게 해서 무정지 시스템을 구축하게 해주는 것을 의미한다.<br>

#### Retry

```java
@Retryable(
    // 1. 특정 예외 상황(예: 네트워크 타임아웃)에서만 재시도
    includes = {RemoteAccessException.class, ConnectException.class},
    // 2. 최대 재시도 횟수는 시스템 환경에 맞춰 신중히 결정 (보통 3~5회)
    maxRetries = 3,
    // 3. 지수 백오프 전략 사용
    delay = 1000,      // 초기 대기 1초
    multiplier = 2,    // 실패 시 대기 시간 2배 증가 (1s -> 2s -> 4s)
    maxDelay = 10000,  // 최대 대기 시간은 10초로 제한
    jitter = 100       // 100ms 범위 내 무작위성 추가
)
public void callExternalSystem() {
    // 외부 시스템 호출 로직
}
```

#### Retry(재시도) 전략

**1. 멱등성(Idempotency) 확보**

* 재시도 로직을 적용할 메서드는 반드시 멱등성이 보장되어야 한다.
* 외부 API 호출이나 DB 쓰기 작업에 재시도를 적용할 때는 유니크 키(Retry-Key) 등을 사용하여 여러 번 호출되어도 결과가 동일하도록 설계해야 한다.

**2. 백오프(Back-off) 전략과 지터(Jitter)**

* 네트워크 일시적 장애 상황에서 즉시 재시도를 하는 것은 서비스에 더 큰 부하를 줄 수 있다.
* Exponential Back-off : 실패할 때마다 대기 시간을 배수로 늘려 시스템이 회복할 시간을 벌어준다.
* Jitter : 모든 서버가 정확히 똑같은 시간 후에 재시도하면 다시 충돌이 발생할 수 있다. 대기 시간에 약간의 무작위 변수를 더해 재시도 시점을 분산시키는 방법이다.

**3. 재시도 대상 예외의 선별**

* 모든 예외에 대해 재시도하는 것은 자원 낭비이다.
* 재시도 가능 : 네트워크 타임아웃, 503 Service Unavilable, 낙관적 락 충돌
* 재시도 불필요 : 400 Bad Request, 401 Unauthorized, 비즈니스 로직 에러는 다시 시도해도 결과가 바뀌지 않는다.

#### 동시성 제한(Concurrency Limit)

```java
// 1. 단순 재시도: 모든 예외에 대해 최대 3회 시도
@Retryable
public void sendSimple() {
    this.jmsClient.destination("notifications").send(...);
}

// 2. 특정 예외 지정: 배달 예외 발생 시에만 최대 4회(총 5회) 재시도
@Retryable(MessageDeliveryException.class)
public void sendWithException() {
    this.jmsClient.destination("notifications").send(...);
}

// 3. 정석적인 지수 백오프 전략 (Best Practice)
@Retryable(
    includes = MessageDeliveryException.class,
    maxRetries = 4,      // 최대 4번 더 시도 (총 5번 실행)
    delay = 100,         // 초기 지연 100ms
    multiplier = 2,      // 실패할 때마다 대기 시간 2배 증가 (100 -> 200 -> 400...)
    maxDelay = 1000,     // 최대 대기 시간은 1초로 제한
    jitter = 10          // 대기 시간에 무작위성(10ms)을 추가해 병목 방지
)
public void sendNotification() {
    this.jmsClient.destination("notifications").send(...);
}
```

**1. 빠른 실패(Fail-fast)와 시스템 보호**

* 임계치를 넘는 요청이 들어왔을 때, 무한정 대기시키는 것은 전체 시스템의 쓰레드 고갈로 이어질 수 있다.
* 내부 자원이 감당할 수 있는 한계치를 `@ConcurrencyLimit`으로 설정하여 시스템이 마비되기 전에 요청을 차단하거나 대기열을 관리해야 한다. 이를 Bulkhead 패턴의 핵심이 된다.

**2. 외부 리소스 고려(Rate Limiting)**

* 자사 시스템에서 호출하는 외부 API나 공유 자원은 무한한 개념이 아니다.
* 외부 리소스의 스펙에 맞춰 `@ConcurrencyLimit`을 설정함으로써 외부 시스템에 과부하를 주지 않아야 한다.

**3. 스레드 풀 vs 동시성 제한**

* 쓰레드 풀 : 작업을 비동기로 처리하기 위해 미리 쓰레드를 할당하는 방식이다.
* 동시성 제한 : 호출하는 쪽의 쓰레드를 그대로 사용하되, 동시에 진입하는 수를 체크한다.
