# Coroutine - Receiving Results from Coroutines

```kotlin
fun main() = runBlocking<Unit> {
    val job: Job = launch {
        println("[${Thread.currentThread().name}] 실행")
    }
}
```
* `launch` 함수로 생성되는 코루틴은 결과값을 반환하지 않는다.

```kotlin
fun main() = runBlocking<Unit> {
    val networkDeferred: Deferred<String> = async(Dispatchers.IO) {
        delay(1000L) // 네트워크 요청
        return@async "Dummy Response" // Dummy Response 반환
    }
}
```
* 코루틴으로부터 결과값을 반환받기 위해서는 `async` 코루틴 빌더를 사용해야 한다.
* `async` 코루틴 빌더는 `Deferred` 객체를 반환하고 결과값이 이 객체에 포함된다.

#### async-await 사용해 코루틴으로부터 결과 수신받기

* `async` 코루틴 빌더를 호출하면 코루틴이 생성되고 `Deferred<T>` 타입의 객체가 반환된다.
* `Deferred`는 `Job`과 같이 코루틴을 추상화한 객체지만, 코루틴으로부터 생성된 결과값을 감싸는 기능을 추가로 가진다.
* 결과값의 타입은 제네릭 타입인 `T`로 표현한다.

```kotlin
/**
 * Creates a coroutine and returns its future result as an implementation of [Deferred].
 * The running coroutine is cancelled when the resulting deferred is [cancelled][Job.cancel].
 * The resulting coroutine has a key difference compared with similar primitives in other languages
 * and frameworks: it cancels the parent job (or outer scope) on failure to enforce *structured concurrency* paradigm.
 * To change that behaviour, supervising parent ([SupervisorJob] or [supervisorScope]) can be used.
 *
 * Coroutine context is inherited from a [CoroutineScope], additional context elements can be specified with [context] argument.
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [Dispatchers.Default] is used.
 * The parent job is inherited from a [CoroutineScope] as well, but it can also be overridden
 * with corresponding [context] element.
 *
 * By default, the coroutine is immediately scheduled for execution.
 * Other options can be specified via `start` parameter. See [CoroutineStart] for details.
 * An optional [start] parameter can be set to [CoroutineStart.LAZY] to start coroutine _lazily_. In this case,
 * the resulting [Deferred] is created in _new_ state. It can be explicitly started with [start][Job.start]
 * function and will be started implicitly on the first invocation of [join][Job.join], [await][Deferred.await] or [awaitAll].
 *
 * @param block the coroutine code.
 */
public fun <T> CoroutineScope.async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    val newContext = newCoroutineContext(context)
    val coroutine = if (start.isLazy)
        LazyDeferredCoroutine(newContext, block) else
        DeferredCoroutine<T>(newContext, active = true)
    coroutine.start(start, coroutine, block)
    return coroutine
}
```
* `async()` 함수도 `launch()` 함수와 마찬가지로 `context` 인자로 `CoroutineName`이나 `CoroutineDispatcher`를 설정 가능하고 `start` 인자로 `CoroutieStart.LAZY`를 설정해 지연 코루틴을 만들 수 있다.
* `async()` 함수가 `launch()` 함수와 다른 점은 block 람다식에서 T를 반환한다는 점과 반환 객체가 `Deferred<T>`라는 점이다.

#### `Deferred` 객체

* `Deferred` 타입의 객체는 미래의 어느 시점에 결과값이 반환될 수 있음을 표현하는 코루틴 객체이다.
* 코루틴이 실행 완료되었을 때 결과값이 반환되므로 언제 수신될 지 알 수 없다.
* 따라서 만약 결과값이 필요하다면 결과값이 수신될 때까지 대기해야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d8d56f39-7f08-4827-9fe0-d6f5dfeb2d97.png)

```kotlin
fun main() = runBlocking<Unit> {
    val networkDeferred: Deferred<String> = async(Dispatchers.IO) {
        delay(1000L) // 네트워크 요청
        return@async "Dummy Response" // 결과값 반환
    }
    
    // networkDeferred로부터 결과값이 반환될 때까지 runBlocking 일시 중단
    val result: String = networkDeferred.await() 
    println(result) // Dummy Response 출력
}
```
* `Deferred` 객체의 `await()` 함수를 호출하면 `Deferred` 코루틴이 실행 완료될 때까지 `await()` 함수를 호출한 코루틴이 일시 중단된다.
* `Deferred` 코루틴이 실행 완료되면 결과값이 반환되고 호출부의 코루틴이 재개된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/87d73802-5d8a-4868-91aa-7663bb8a5946.png)

#### 복수의 코루틴으로부터 결과를 수신받기

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0443a974-ab55-4590-a741-77dba427fc82.png)

* `async-await` 구조를 연속적으로 호출하게 되면 병렬 실행이 되지 않고 순차 실행이 된다.
* 코루틴을 병렬로 실행하기 위해서는 코루틴을 모두 실행한 다음 `await()`을 호출해야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/65c60aa7-1215-4954-9035-022beba133fb.png)

* 서로 간에 종속성이 없는 코루틴들에 대한 `await()` 호출은 모든 코루틴이 실행 요청된 후에 해야한다.

```kotlin
fun main() = runBlocking<Unit> {
    val startTime = System.currentTimeMillis() // 1. 시작 시간 기록
    
    // 2. 플랫폼1에서 등록한 관람객 목록을 가져오는 코루틴 실행
    val participantDeferred1: Deferred<Array<String>> = async(Dispatchers.IO) {
        delay(1000L)
        return@async arrayOf("철수", "영수")
    }
    
    // 3. 플랫폼2에서 등록한 관람객 목록을 가져오는 코루틴 실행
    val participantDeferred2: Deferred<Array<String>> = async(Dispatchers.IO) {
        delay(1000L)
        return@async arrayOf("영희")
    }
    
    val participants1: Array<String> = participantDeferred1.await() // 4. 결과가 수신 될 때까지 대기
    val participants2: Array<String> = participantDeferred2.await() // 5. 결과가 수신 될 때까지 대기
    
    // 6. 지난 시간 표시 및 참여자 목록을 병합해 출력
    val totalParticipants = listOf(*participants1, *participants2)
    println("[${getElapsedTime(startTime)}] 참여자 목록: $totalParticipants")
}

// 경과 시간 유틸리티 함수 보완
fun getElapsedTime(startTime: Long): String {
    val elapsed = System.currentTimeMillis() - startTime
    return "${elapsed / 1000}초"
}
```

* `awaitAll()`을 사용하면 복수의 `Deferred` 객체로부터 결과값을 수신할 수 있다.
* `awaitAll()` 함수는 가변 인자로 `Deferred` 타입의 객체를 받아 인자로 받은 모든 `Deferred`로부터 결과가 수신될 때까지 호출부의 코루틴을 일시 중단한다.

```kotlin
fun main() = runBlocking<Unit> {
    val startTime = System.currentTimeMillis() // 1. 시작 시간 기록
    
    // 2. 플랫폼1에서 등록한 관람객 목록을 가져오는 코루틴 실행
    val participantDeferred1: Deferred<Array<String>> = async(Dispatchers.IO) {
        delay(1000L)
        return@async arrayOf("철수", "영수")
    }
    
    // 3. 플랫폼2에서 등록한 관람객 목록을 가져오는 코루틴 실행
    val participantDeferred2: Deferred<Array<String>> = async(Dispatchers.IO) {
        delay(1000L)
        return@async arrayOf("영희")
    }
    
    // 4. 💡 두 개의 코루틴으로부터 결과가 수신될 때까지 대기
    val results: List<Array<String>> = awaitAll(participantDeferred1, participantDeferred2)
    
    // 5. 지난 시간 표시 및 참여자 목록을 병합해 출력
    val totalParticipants = listOf(*results[0], *results[1])
    println("[${getElapsedTime(startTime)}] 참여자 목록: $totalParticipants")
}

fun getElapsedTime(startTime: Long): String {
    val elapsed = System.currentTimeMillis() - startTime
    return "${elapsed / 1000}초"
}
```

#### `withContext` 함수를 사용한 결과 수신받기

* 인자로 받은 `CoroutineDispatcher`를 사용해 코루틴의 실행 쓰레드를 전환하고 람다식의 코드를 실행한 후 결과값을 반환하는 함수이다.
* 람다식을 실행한 후에는 쓰레드가 다시 이전의 `Dispatcher`를 사용하도록 전환한다.
* `withContext`는 코루틴을 유지한채 인자로 받은 `CoroutineDispatcher`를 사용해 코루틴의 실행 쓰레드를 전환하는데 사용한다.

> **백엔드 아키텍처 관점에서 `withContext`를 반드시 써야 하는 이유?**
> 
> * 쓰레드 전환 비용을 감수하고서라도 서버 전체의 처리량(Throughput)과 안정성을 압도적으로 끌어올리는 실무 이점이 있기 때문이다.
> 
> **1. 무거운 작업을 격리하되, 결과를 받아야 한다.**
```kotlin
// launch: 던지고 끝, 결과 못 받음
launch(Dispatchers.IO) { heavyWork() }

// withContext: 던지고 결과까지 받음
val result = withContext(Dispatchers.IO) { heavyWork() }
```
> * 결과를 받아야 하는 작업이면 `withContext`가 유일한 선택지이다.
>
> **2. 취소/예외가 부모와 연결된다.**
```kotlin
// 부모 코루틴이 취소되면?
val result = withContext(Dispatchers.IO) {
    heavyWork() // ← 여기도 즉시 취소됨
}
// 취소됐으면 여기 절대 안 옴
updateUI(result)
```
> * 기존 `Executor`이나 `launch()`의 경우 부모가 죽어도 자식이 계속 돌지만 `withContext`는 부모와 자식의 생명주기가 묶여있어 통일이 가능하다.
> 
> **3. 쓰레드가 바뀌어도 코드는 순차적**
```kotlin
val user = findUser()                                               // 메인 스레드
val decrypted = withContext(Dispatchers.Default) { decrypt(user) }  // 워커 스레드
updateUI(decrypted)                                                 // 메인 스레드
```
> * 실제로는 쓰레드가 2번 바뀌지만 순차적 코드 작성으로 쉬운 이해를 도모할 수 있다.
