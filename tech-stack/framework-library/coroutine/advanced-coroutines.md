# Coroutine - Advanced Coroutines

#### 공유 상태를 사용하는 코루틴의 문제와 해결책

* 쓰레드 간에 데이터를 전달하거나 자원을 공유하는 경우에는 가변 변수를 통해 상태를 공유하고 업데이트 해야 한다.
* 여러 쓰레드에서 가변 변수에 동시에 접근해 값을 변경하면 데이터 손실이나 불일치로 버그가 발생할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/62019aa8-3a4e-4885-afdf-270702e7ba54.png)

* JVM은 쓰레드마다 스택 영역이라고 불리는 메모리 공간을 갖고 있고, 이 스택 영역에서는 원시 타입의 데이터나 힙 영역에 저장된 객체에 대한 참조가 저장된다.
* 힙 영역은 JVM에 올라간 쓰레드들에서 공통으로 사용되는 메모리 공간으로 복잡한 데이터가 저장된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bade2003-ddd0-4e12-ba0c-449c544ecac2.png)

* 컴퓨터는 CPU 레지스터, CPU 캐시 메모리, 메인 메모리 영역으로 구성된다.
* 각 CPU는 CPU 캐시 메모리를 중간에 두고 데이터 조회 시 메인 메모리까지 가지 않고 CPU 캐시 메모리를 조회할 수 있도록 해서 메모리 접근 속도를 향상시킨다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/769e12d6-2bf4-4c08-b361-ce7196ca35c1.png)

* JVM의 스택 영역과 힙 영역에 저장되는 데이터는 컴퓨터의 각 메모리 공간 중 어디에나 저장될 수 있다.
* 이로 인해 '공유 상태의 메모리 가시성 문제'와 '공유 상태에 대한 경쟁 상태 문제'가 발생할 수 있다.
* ** 공유 상태의 메모리 가시성 문제**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c66581fe-86b8-4237-8631-4483f3749a92.png)

* 공유 상태의 메모리 가시성 문제란, 하나의 쓰레드가 다른 쓰레드가 변경한 상태를 확인하지 못하는 문제이다.
* 이 문제는 서로 다른 CPU에서 실행되는 쓰레드들에서 공유 상태를 조회하고 업데이트할 때 발생한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/73a35e6d-d85f-471a-be40-7e3f2f02dbbc.png)

* 공유 상태의 메모리 가시성 문제를 해결하려면 변수 선언 시 `@Volatile` 어노테이션을 설정하면 해당 어노테이션이 설정된 변수의 값을 읽고 쓸 때는 CPU 캐시 메모리를 사용하지 않는다.
* 직접 메인 메모리에 접근해서 가져오기 때문에 문제 해결이 가능하나 이 방식은 성능 저하가 유발된다.

**공유 상태에 대한 경쟁 상태 문제**

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b370ab22-fd20-44e2-a7ed-7ac8fddf9cb1.png)

* 메인 메모리에서만 `count` 변수의 값을 변경시키더라도 여러 쓰레드가 동시에 메인 메모리에 저장된 값에 접근할 수 있다.
* 이렇게 되면 생각했던 결과인 1002가 아니라 1001이 나오게 된다. 공유 상태의 변수를 읽고 업데이트할 때 많이 발생한다.

```kotlin
var count = 0
val mutex = Mutex()

fun main(): Unit = runBlocking<Unit> {
    withContext(Dispatchers.Default) {
        repeat(10_000) {
            launch {
                mutex.lock()
                // --- 임계 영역 시작 ---
                count += 1
                // --- 임계 영역 끝 ---
                mutex.unlock()
            }
        }
    }
    println("count = $count")
}
```
* 공유 변수의 변경 가능 지점을 임계 영역으로 만들어 동시 접근을 제한할 수 있다.
* 코루틴에 대한 임계 영역을 만들기 위해서는 `Mutex` 객체를 만들어 사용하면 된다.
* 코루틴이 `Mutex` 객체의 `lock()` 일시 중단 함수를 호출하면 락이 획득되며, 이후 해당 `Mutex` 객체에 대해 `unlock()`이 호출될 때까지 다른 코루틴이 해당 임계 영역에 진입할 수 없다.
* `lock-unlock` 쌍을 직접 호출하는 대신 `withLock()` 함수를 호출하는 것이 안전하다.

```kotlin
var count = 0
val mutex = Mutex()

fun main(): Unit = runBlocking<Unit> {
    withContext(Dispatchers.Default) {
        repeat(10_000) {
            launch {
                // 상자 안의 코드가 실행되기 전 자동으로 lock(), 완료 후 자동으로 unlock() 처리됨
                mutex.withLock {
                    count += 1
                }
            }
        }
    }
    println("count = $count")
}
```

* `ReentrantLock`의 `lock()` 함수는 특정 쓰레드에서 락을 획득했다면, 다른 쓰레드에서 `lock()`을 걸었을 경우 해당 쓰레드를 락이 해제될 떄까지 블로킹시킨다.
* `Mutex`의 `lock()` 함수는 일시 중단 함수로, 특정 코루틴이 락을 획득했다면 다른 코루틴에서 `lock()`을 걸었을 경우 해당 코루틴은 락이 해제될 때까지 일시 중단된다.(블로킹이 되는 것이 아니다.)

```kotlin
var count = 0
val reentrantLock = ReentrantLock()

fun main(): Unit = runBlocking<Unit> {
    withContext(Dispatchers.Default) {
        repeat(10_000) {
            launch {
                // 만약 특정 스레드가 락을 획득했다면,
                // lock 함수가 호출됐을 때 스레드가 블로킹됨
                reentrantLock.lock()
                count += 1
                reentrantLock.unlock()
            }
        }
    }
    println("count = $count")
}
```
```kotlin
var count = 0
val mutex = Mutex()

fun main(): Unit = runBlocking<Unit> {
    withContext(Dispatchers.Default) {
        repeat(10_000) {
            launch {
                // 만약 특정 코루틴이 락을 획득했다면,
                // lock 함수가 호출됐을 때 (스레드 차단 없이) 코루틴만 일시 중단됨
                mutex.lock()
                count += 1
                mutex.unlock()
            }
        }
    }
    println("count = $count")
}
```

* 경쟁 상태 문제가 생기는 이유는 결국 복수의 쓰레드가 공유 상태에 동시에 접근하기 때문이다.
* 따라서 공유 상태 접근 시 하나의 쓰레드만 사용하도록 한다면 경쟁 상태 문제를 해결할 수 있다.

```kotlin
var count = 0

// 1. 구방식: 새로운 전용 스레드를 하나 생성하여 사용 (자원 소모가 있음)
val countChangeDispatcher = newSingleThreadContext("전용 스레드")

// 2. 신방식 (권장): 기존 스레드 풀을 활용하되, 동시에 1개의 스레드만 접근하도록 제한
// val countChangeDispatcher = Dispatchers.IO.limitedParallelism(1)
// val countChangeDispatcher = Dispatchers.Default.limitedParallelism(1)

fun main(): Unit = runBlocking<Unit> {
    withContext(Dispatchers.Default) {
        repeat(10_000) {
            // 코루틴을 단일 스레드 환경(countChangeDispatcher)으로 보내서 실행하므로 동시성 문제 해결
            launch(countChangeDispatcher) {
                count += 1
            }
        }
    }
    println("count = $count")
}
```

#### 원자성 있는 데이터 구조 사용해 경쟁 상태 문제 해결

* 원자성 있는 객체는 여러 쓰레드가 동시에 접근하더라도 안전하게 값을 변경하거나 읽을 수 있도록 하는 객체이다.
* 원자성 있는 객체에서 연산이 실행 중인 경우 쓰레드가 블로킹 될 수 있다.
  * Mutex나 특정 쓰레드로 제한하는 방식과 달리, CPU 명령 수준에서 연산의 원자성을 보장하는 하드웨어 지원 방식을 사용한다.
  * 락을 걸거나 코루틴을 일시 중단하는 과정이 없으므로 가장 가볍고 속도가 빠르다는 장점이 있다.
* 원자성 있는 객체를 사용할 때, 원자성 있는 객체의 읽기/쓰기를 따로 실행하면 연산이 손실될 수 있다.

```kotlin
// 일반 Int 대신 원자적 연산을 지원하는 AtomicInteger 사용
var count = AtomicInteger(0)

fun main(): Unit = runBlocking<Unit> {
    withContext(Dispatchers.Default) {
        repeat(10_000) {
            launch {
                // 락(Lock) 없이도 스레드 안전하게 값을 1씩 증가
                count.getAndUpdate { it + 1 }
                
                // 참고: 단순히 1을 더하는 거라면 count.incrementAndGet()을 써도 됩니다.
            }
        }
    }
    println("count = ${count.get()}")
}
```

#### 코루틴 실행 옵션

* 코루틴에 실행 옵션을 주기 위해서는 `launch()`나 `async()`와 같은 코루틴 빌더 함수의 `start` 인자로 `CoroutineStart` 옵션을 전달하면 된다.
* 전달할 수 있는 옵션 목록으로 `CoroutineStart.DEFAULT`, `CoroutineStart.ATOMIC`, `CoroutineStart.UNDISPATCHED`, `CoroutineStart.LAZY` 옵션이 있다.

> [📚CoroutineStart](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-start/)
>
> **CoroutineStart.Default**
> * `launch()`나 `async()`의 start 인자로 아무런 값이 전달되지 않으면 기본 실행 옵션인 `CoroutineStart.DEFAULT` 옵션이 설정된다.
> * `CoroutineStart.DEFAULT` 옵션이 사용되면 코루틴 빌더 함수를 호출한 즉시 코루틴이 생성되고 코루틴의 실행이 `CoroutineDispatcher`에 요청된다.
> * 코루틴 빌더 함수를 호출한 코루틴은 계속해서 실행된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b915e656-8c04-43ea-9d8c-eb32028fc020.png)

> **CoroutineStart.ATOMIC**
> * 코루틴이 실행 요청됐지만 `CoroutineDispatcher`가 사용할 수 있는 쓰레드가 모두 사용 중이어서 쓰레드로 보내지지 않는 경우 코루틴은 생성 상태에 머물게 된다.
> * 생성 상태 코루틴에 취소가 요청되면 해당 코루틴은 취소되고 종료된다.
> * 이 `CoroutineStart.ATOMIC` 옵션을 적용한 코루틴은 생성 상태일 때 취소되지 않는다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0cf034c6-7ddc-4c13-9fbf-ea9f54a22102.png)

> **CoroutineStart.UNDISPATCHED**
> * `CoroutineStart.UNDISPATCHED` 옵션이 적용된 코루틴은 `CoroutineDispatcher` 객체의 작업 대기열을 거치지 않고 호출자 쓰레드에서 즉시 실행된다.
> * `CoroutineStart.UNDISPATCHED` 옵션이 적용되더라도, 일시 중단 후 재개될 때는 `CoroutineDispatcher`에 실행 요청된다.
![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/28e20dda-379b-4185-a2b7-633080287e74.png)

#### 무제한 디스패처

* 무제한 디스패처란, 코루틴을 자신이 실행시킨 쓰레드에서 즉시 실행하도록 만드는 디스패처이다.
* 무제한 디스패처를 사용해 실행된 코루틴은 중단 시점 이후 재개를 코루틴을 재개시킨 쓰레드에서 처리한다.
* `CoroutineStart.UNDISPATCHED` 옵션이 적용된 코루틴은 재개 시 `CoroutineDispatcher`에 실행 요청된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ad9c860a-6684-4439-9fa4-1be0a6dd89d8.png)

#### 코루틴의 동작 방식과 Continuation

* 아래와 같은 일반적인 코드가 동작할 때는 작업이 쓰레드를 점유해 코드 라인이 순차적으로 동작한다.

```kotlin
fun main() {
    println("[${Thread.currentThread().name}] 작업 시작")
    Thread.sleep(1000L)
    println("[${Thread.currentThread().name}] 작업 종료")
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/babd629c-a2fd-4904-94fa-f69120ecabdb.png)

* 코루틴은 코드를 실행하는 도중 일시 중단하고 필요한 시점에 다시 재개하는 기능을 지원한다.
* 코루틴은 Coroutination Passing Style이라 불리는 프로그래밍 방식을 통해 실행 정보를 저장하고 전달한다.
* 코루틴의 일시 중단 시점에 남은 작업 정보가 `Continuation` 객체에 저장된다. 이 `Continuation`의 `resumeWith()` 함수가 호출되면 저장된 작업 정보가 복원되어 남은 작업들이 마저 실행된다.
* 즉, `resumeWith()` 함수는 코루틴의 재개를 일으킨다.

```kotlin
fun main() = runBlocking<Unit> {
    println("[${Thread.currentThread().name}] runBlocking 코루틴 시작")
    delay(1000L)
    println("[${Thread.currentThread().name}] runBlocking 코루틴 종료")
}
```

> [📚Continuation Passing Style](https://en.wikipedia.org/wiki/Continuation-passing_style)
> 
> **코루틴 동작의 핵심 : 일시 중단(Suspend)과 재개(Resume)**
> * 코루틴에서 가장 중요한 부분은 쓰레드를 양보한다는 것이다.
> * 전통적인 쓰레드 방식의 경우 쓰레드 자체를 물리적으로 멈추고 그 쓰레드는 대기하는 동안 다른 작업을 전혀 하지 못하고 자원을 낭비하게 된다.
> * 코루틴 방식의 경우 비동기 작업을 만나면 코루틴은 일시 중단된다. 이 때, 코루틴을 실행하던 쓰레드가 차단되지 않고, 다른 코루틴을 실행하게 된다. 대기가 끝나면 멈췄던 코루틴이 다시 재개되어 하던 작업을 이어 나간다.
>
> **Continuation**
> * Continuation은 프로그램 실행 중 특정 시점 이후에 남은 미래의 작업을 의미한다.
```kotlin
public interface Continuation<in T> {
    public val context: CoroutineContext
    public fun resumeWith(result: Result<T>) // 멈췄던 코루틴을 다시 깨우는 핵심 메서드
}
```
> * 프로그램 카운터/스냅샷 : 코루틴이 일시 중단될 때, 현재 어디까지 실행되었는지와 그 때 가지고 있던 지역 변수의 상태를 고스란히 저장하는 스냅샷 역할을 한다.
> * 콜백 고도화 : 비동기 작업이 끝났을 때 호출되는 콜백 함수와 유사하지만, 컴파일러 단에서 관리되므로 개발자가 직접 콜백 지옥을 만들 필요가 없다. 작업을 마치면 `resumeWith()`를 통해 멈춰 있던 코루틴에게 결과값과 제어권을 돌려준다.

* `suspendCancellableCoroutine()` 함수를 사용하면 코루틴이 일시 중단되고, 함수 람다식의 수신 객체인 `CancellableContinuation`에 `resume()` 함수가 호출되면 재개된다.

```kotlin
fun main(): Unit = runBlocking<Unit> {
    val result: String = suspendCancellableCoroutine<String> { continuation ->
        thread { // 새로운 스레드 생성
            Thread.sleep(1000L) // 1초간 대기
            continuation.resume("실행 결과") // runBlocking 코루틴 "실행 결과"와 함께 재개
        }
    }
    println(result) // 코루틴 재개 시 반환 받은 결과 출력
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ff407c20-cde5-45aa-b3e9-b74784e307da.png)
