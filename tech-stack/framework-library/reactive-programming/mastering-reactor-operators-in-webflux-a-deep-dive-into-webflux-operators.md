# Reactive Programming - Mastering Reactor Operators in WebFlux & A Deep Dive into WebFlux Operators

#### Reactor에서 제공하는 데이터 변환 연산자 메서드 - `map()`

* `map()`은 각 원소를 1:1로 동기 변환하는 연산자이다.
* `onNext()`로 들어온 값 하나를 함수에 통과시켜 다른 값 하나로 바꿔서 그대로 아래로 흘려보낸다.

```java
// 단건 조회: Mono<User> → Mono<UserResponse>
public Mono<UserResponse> getUser(Long id) {
    return userRepository.findById(id)
            .map(UserResponse::from);   // User 하나 → UserResponse 하나 (1:1 동기 변환)
}
```

#### Reactor에서 제공하는 데이터 변환 연산자 메서드 - `flatMap()`

* `flatMap()`은 각 원소를 또 다른 비동기 스트림(Mono/Flux)으로 바꾼 뒤, 그것들을 구독해서 하나의 스트림으로 펼치는(flatten) 연산자이다.
* 리액티브에서 DB·HTTP 호출 결과는 값이 아니라 Mono/Flux이다. 그래서 "각 원소마다 또 다른 비동기 호출"을 하면 필연적으로 스트림이 중첩되고, 이걸 풀어주는 게 `flatMap()` 메서드의 역할이다.

```java
// flatMap: 함수가 'Publisher'를 반환 → 자동으로 펼쳐서 Flux<Order>
users.flatMap(user -> orderRepository.findByUserId(user.getId())) // ✅ Flux<Order>
```

#### Reactor에서 제공하는 데이터 변환 연산자 메서드 - `concatMap()`

* `concatMap()`은 `flatMap()`과 똑같이 각 원소를 Mono/Flux로 바꿔 펼치는데, 안쪽 스트림을 하나씩 순서대로(순차적으로) 구독한다. 
* `concatMap()`은 무조건 들어온 순서대로 하나씩 차례대로 처리한다.
* 만약 A, B, C가 순서대로 들어오면, A의 비동기 처리가 끝날 때까지 B, C는 시작도 하지 않고 기다린다.
* 비동기 처리 속도가 제각각이더라도 최종 결과물은 반드시 A ➔ B ➔ C라는 원래의 순서가 엄격하게 보장된다.

```java
users.concatMap(user ->
        orderRepository.findByUserId(user.getId())   // 반환은 flatMap과 동일 (Publisher)
);
```

#### Reactor에서 제공하는 데이터 변환 연산자 메서드 - `flatMapSequential()`

* `flatMapSequential`은 비동기로 동시에 쏘면서 성능을 챙기면서도 최종 결과물의 순서는 원본 순서대로 정렬한다.
* 순서를 맞추려고 버퍼를 쓰기 때문에, 앞 순서 원소가 느리면 뒤 결과들이 계속 쌓이게 된다. 

```java
users.flatMapSequential(user ->
        orderRepository.findByUserId(user.getId())   // 반환은 셋 다 동일 (Publisher)
);
```

#### Reactor에서 제공하는 데이터 변환 연산자 메서드 - `flatMapMany()`

* `flatMapMany()`는 Mono에서 쓰는 연산자로, 하나의 값을 Flux(여러 개)로 펼쳐주는 변환이다. 즉 `Mono<T>` → `Flux<R>`, 카디널리티가 1 → N으로 바뀐다.

```java
Mono<User> user = userRepository.findById(id);   // 유저 1명

Flux<Order> orders = user.flatMapMany(u ->
        orderRepository.findByUserId(u.getId())   // 그 유저의 주문 N건 → Flux
);                                                // Mono<User> → Flux<Order>
```

#### 데이터 스트림 환경에서 가공 및 조율을 위한 Reactor의 필터링 연산자 메서드 - `filter()`

* `filter()`는 조건(Predicate)을 만족하는 원소만 통과시키고, 나머지는 버리는 연산자이다. 
* 값을 바꾸지 않고(변환 아님), 개수만 줄인다.
* `filter()`의 Predicate도 `map()`처럼 동기로 즉시 실행된다. 그래서 그 안에서 블로킹 호출을 하면 이벤트 루프를 막게 된다.

```java
users
    .filter(User::isActive)        // 활성 유저만 골라서 (개수 ↓)
    .map(UserResponse::from);      // 그걸 DTO로 변환 (값 바꿈)
```

#### 데이터 스트림 환경에서 가공 및 조율을 위한 Reactor의 필터링 연산자 메서드 - `distinct()`

* `distinct()`는 이미 지나간 값과 중복되는 원소를 걸러내고, 처음 보는 값만 통과시키는 연산자이다. 스트림 버전의 "중복 제거"이다.
* 그러나 대용량 아키텍처 관점에서 치명적인 트레이드 오프를 가지고 있다.
  * **메모리 누수 위험** : `Flux.interval`처럼 종료되지 않고 무한히 흐르는 스트림이거나 하루에 수억 건씩 쏟아지는 금융 트랜잭션 스트림에 걸게 되면 스트림이 유지되는 내내 내부 `Set`에 데이터가 계속 누적되면서 힙 메모리를 잡아먹게 되고 결국 서버가 `OutOfMemoryError(OOM)`을 뱉을 수 있다.
  * **`distinctUntilChanged()`** : 해당 메서드는 전체 데이터가 아니라 바로 직전에 통과한 데이터와만 비교해 연속으로 중복되는 데이터만 쳐내는 오퍼레이터이다.

```java
Flux.just(1, 2, 2, 3, 1, 3, 4)
    .distinct()
    .subscribe(System.out::println);   // 1, 2, 3, 4 (중복은 버려짐)
```

#### 데이터 스트림 환경에서 가공 및 조율을 위한 Reactor의 필터링 연산자 메서드 - `elementAt()`

* `elementAt()`은 Flux에서 특정 인덱스(N번째) 원소 하나만 꺼내는 연산자이다. 결과가 1개이므로 `Flux<T>` → `Mono<T>`로 바뀐다.
* 그러나 기본값 없는 인덱스 초과가 발생할 위험이 높아 치명적인 안티패턴으로 잘 쓰이지 않는다.

```java
Flux.just("a", "b", "c", "d")
    .elementAt(2)                      // 인덱스 2 = 세 번째
    .subscribe(System.out::println);   // "c"
```


#### 다중 스트림 구조에서 데이터 결합을 위한 Reactor 결합 연산자 메서드 - `concat()`

* `concat()`은 여러 개의 Publisher(스트림)를 순서대로 이어붙여 하나로 만드는 연산자이다.
* 앞 스트림이 완전히 끝나야 다음 스트림을 구독한다.

```java
Flux<Integer> flux1 = Flux.just(1, 2, 3);
Flux<Integer> flux2 = Flux.just(4, 5, 6);

Flux.concat(flux1, flux2)
    .subscribe(System.out::println);   // 1, 2, 3, 4, 5, 6
```

#### 다중 스트림 구조에서 데이터 결합을 위한 Reactor 결합 연산자 메서드 - `concatWith()`

* `concatWith()`는 `concat()`의 인스턴스 메서드 버전이다.
* 기존 스트림 뒤에 다른 스트림을 이어붙인다.

```java
// concat: 스트림들을 '나란히 나열'하는 느낌 — 여러 개를 한자리에 모을 때
Flux.concat(header, body, footer);

// concatWith: 기존 체이닝에 '이어서 덧붙이는' 느낌 — 파이프라인 끝에 자연스럽게
userRepository.findVip()
    .map(UserResponse::from)
    .concatWith(userRepository.findNormal().map(UserResponse::from));    //  ↑ 앞 파이프라인에 매끄럽게 이어짐
```

#### 다중 스트림 구조에서 데이터 결합을 위한 Reactor 결합 연산자 메서드 - `merge()`

* `merge()`는 여러 스트림을 동시에 구독해서, 각 스트림에서 원소가 도착하는 대로 하나로 합치는 결합 연산자이다.

```java
Flux<Integer> flux1 = Flux.just(1, 2, 3);
Flux<Integer> flux2 = Flux.just(4, 5, 6);

Flux.merge(flux1, flux2)
    .subscribe(System.out::println);   // 순서 보장 안 됨 (도착 순)
```

#### 다중 스트림 구조에서 데이터 결합을 위한 Reactor 결합 연산자 메서드 - `zip()`

* `zip()`은 여러 스트림에서 원소를 하나씩 짝지어(같은 순번끼리) 결합하는 연산자이다.

```java
Flux<String> names = Flux.just("김", "이", "박");
Flux<Integer> ages  = Flux.just(20, 30, 40);

Flux.zip(names, ages, (name, age) -> name + ":" + age)
    .subscribe(System.out::println);   // 김:20, 이:30, 박:40
```

#### 무한한 데이터 스트림 환경에서 조건을 검증하기 위한 연산자 메서드 - `defaultIfEmpty()`, `switchIfEmpty()`

* `defaultIfEmpty()`는 스트림이 비어 있으면 미리 정해둔 값 하나를 대신 방출한다.
* `switchIfEmpty()`는 스트림이 비어 있으면 다른 스트림으로 전환한다.

```java
userRepository.findByEmail(email)     // Mono<User> — 없으면 빈 Mono
    .defaultIfEmpty(User.guest());    // 비었으면 게스트 유저로 대체
```
```java
cacheRepository.findById(id)              // 1차: 캐시 조회 (없으면 빈 Mono)
    .switchIfEmpty(
        dbRepository.findById(id)         // 2차: 캐시에 없으면 DB에서 조회
    );
```

#### 무한한 데이터 스트림 환경에서 조건을 검증하기 위한 연산자 메서드 - `hasElement()`, `hasElements()`

* `hasElement()` / `hasElements()` 둘 다 스트림에 원소가 있는지 없는지를 `boolean`으로 알려주는 연산자이다. 값 자체가 아니라 존재 여부만 뽑아낸다.
```java
userRepository.findById(id)      // Mono<User>
    .hasElement();               // Mono<Boolean> — 있으면 true, 없으면 false
```
```java
orderRepository.findByUserId(id)   // Flux<Order>
    .hasElements();                // Mono<Boolean> — 하나라도 있으면 true
```

#### 무한한 데이터 스트림 환경에서 조건을 검증하기 위한 연산자 메서드 - `any()`, `all()`

* `any()` / `all()` 둘 다 스트림 전체를 조건(Predicate)으로 판정해서 `Mono<Boolean>` 하나로 만드는 연산자이다.
* `any()` : 하나라도 만족하면 `true`
* `all()` : 전부 만족해야 `true`

```java
orderRepository.findByUserId(id)   // Flux<Order>
    .any(Order::isPaid);           // 결제된 주문이 하나라도 있나? → Mono<Boolean>
```
```java
orderRepository.findByUserId(id)   // Flux<Order>
    .all(Order::isPaid);           // 모든 주문이 결제됐나? → Mono<Boolean>
```

#### 무한한 데이터 스트림 환경에서 조건을 검증하기 위한 연산자 메서드 - `then()`, `thenMany()`

* 둘 다 앞 작업의 결과값은 버리고, 앞이 끝난 뒤 다음 작업으로 넘어가는 연산자이다.
* `then()` — 앞이 끝나면 → `Mono` 하나
* `thenMany()` — 앞이 끝나면 → `Flux`

```java
// 1) then() — 앞 값 버리고 '완료 신호'만 (Mono<Void>)
saveUser(user)
    .then();                        // User 저장 끝나면 Mono<Void>로 완료만 알림

// 2) then(Mono) — 앞 끝나면 다음 Mono 실행
saveUser(user)                      // Mono<User> (결과값은 버림)
    .then(sendWelcomeMail(user));   // 저장 끝난 뒤 메일 발송
```
```java
deleteAllOldOrders(userId)          // Mono<Void> — 정리 작업 (값 없음)
    .thenMany(
        loadFreshOrders(userId)     // Flux<Order> — 끝난 뒤 새로 조회
    );   // → Flux<Order>
```

#### 다중 스트림 환경에서 데이터를 모아서 하나의 산출물을 만드는 집계 연산자 메서드 - `reduce()`, `scan()`

* `reduce()` / `scan()` 둘 다 원소들을 하나의 누적값으로 접어나가는 연산자이다. 앞 원소들을 계속 합쳐가는 누적 연산이다.
* `reduce()` : 모든 원소를 누적한 마지막 값 하나만 방출한다.
* `scan()` : 누적 과정을 매 단계마다 방출한다.

```java
Flux.just(1, 2, 3, 4)
    .reduce((acc, next) -> acc + next)   // 누적 합
    .subscribe(System.out::println);     // 10 (최종값 하나만)
```

#### 다중 스트림 환경에서 데이터를 모아서 하나의 산출물을 만드는 집계 연산자 메서드 - `groupBy()`

* `groupBy()`는 하나의 `Flux`를 키(key) 기준으로 여러 개의 하위 스트림으로 쪼개는 연산자이다. 
* SQL의 GROUP BY, Java Stream의 `Collectors.groupingBy`를 리액티브 스트림에서 하는 것이다.

```java
Flux.just(1, 2, 3, 4, 5, 6)
    .groupBy(n -> n % 2 == 0 ? "짝수" : "홀수")   // 키로 분류
    .flatMap(group ->
        group.collectList()
             .map(list -> group.key() + ": " + list)   // group.key()로 키 접근
    )
    .subscribe(System.out::println);
    // 홀수: [1, 3, 5]
    // 짝수: [2, 4, 6]
```
