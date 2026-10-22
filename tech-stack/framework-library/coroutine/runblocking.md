# Coroutine - runBlocking

#### `runBlocking()`

* `runBlocking()` 함수는 이 함수를 호출한 쓰레드를 사용해 실행되는 코루틴을 만들어낸다.
* `runBlocking()` 함수는 코루틴이 종료될 때 쓰레드 점유가 해제된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b4f9dea1-ccbb-49cf-a5c9-e923b90d084f.png)

> [📚runBlocking](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html)
> 
```kotlin
public fun <T> runBlocking(
    context: CoroutineContext = EmptyCoroutineContext, 
    block: suspend CoroutineScope.() -> T
): T
```
> **1. runBlocking의 API 정의 및 시그니처**
> * `runBlocking()`은 새로운 코루틴을 생성하고 내부에 정의된 블록 연산이 끝날 때까지 현재 실행 중인 쓰레드를 호출 완료 시점까지 블로킹하는 코루틴 빌더이다.
> * `context` : 코루틴이 실행될 컨텍스트를 지정한다. 기본값은 `EmptyCoroutineContext`이며, 쓰레드 인터럽트 처리를 위한 내부 이벤트 루프가 자동으로 할당된다.
> * `block` : 수신 객체 지정 람다 형태로 `CoroutineScope` 내에서 실행할 비동기 로직을 작성한다. 이 블록은 `suspend`를 호출할 수 있는 스코프를 제공한다.
> * 반환 타입(T) : `block` 람다식의 최하단에 위치한 최종 결과물을 그대로 반환한다.
> 
> **2. Event Loop와 쓰레드 블로킹**
> * 일반적인 코루틴 빌더는 호출 즉시 비동기적으로 코루틴을 큐에 적재하고 쓰레드의 제어권을 바로 반환하지만 `runBlocking()`은 완전히 반대로 동작한다.
> * 쓰레드 점유 : `runBlocking()`이 호출되면 해당 함수를 호출한 현재 쓰레드의 제어권을 런타임이 즉시 강탈한다.
> * 이벤트 루프 생성 : 내부적으로 현재 쓰레드 전용의 동기식 이벤트 루프를 구동한다.
> * 태스크 처리 : `runBlocking()` 내부 및 하위에서 생성된 코루틴들의 실행 신호, 중단 후 재개 신호들이 이 이벤트 루프 큐에 적재된다.
> * 루프 탈출 조건 : 이벤트 루프는 내부 큐가 완전히 비워지고, `runBlocking()` 내부의 모든 자식 코루틴들이 성공 또는 실패로 종료될 때까지 쓰레드를 WAKE/SLEEP 상태로 오가며 제어권을 놓아주지 않는다. 연산이 끝나야지만 쓰레드 블로킹이 해제된다.
> 
> **3. runBlocking 2가지 정석 유스케이스**
> * `runBlocking()`은 사용자 공간의 동시성을 제어하는 코루틴 세계와 물리적인 쓰레드 기반의 전통적인 자바/코틀린 세계를 연결하는 경계선 역할을 한다. 메인 비즈니스 로직에서는 쓰레드 블로킹을 유발하므로 지양해야하며, 다음과 같은 2가지 상황에서만 정석으로 사용된다.
> * 애플리케이션 최초 진입점 : 최신 비동기 스타일로 작성된 코루틴 컴포넌트들을 구동하기 위해 최초 진입 쓰레드인 `main` 쓰레드를 붙잡아둔다.
> * JUnit 기반 단위 테스트 : 테스트 프레임워크 엔진은 기본적으로 쓰레드 기반으로 구동된다. 테스트 메서드가 비동기 코루틴의 완료를 기다리지 않고 바로 통과 처리되는 것을 막기 위해 테스트 쓰레드를 강제로 잡아둔다.
