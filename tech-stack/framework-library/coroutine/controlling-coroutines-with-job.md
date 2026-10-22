# Coroutine - Controlling Coroutines with Job

#### 코루틴 순차 처리 - `join()` 함수 사용해 순차 처리하기

* `Job` 객체의 `join()` 함수를 호출하면 코루틴 순차 처리가 가능하다.
* 예를 들어, A 코루틴이 완료된 후 B 코루틴이 실행되어야 한다면 B 코루틴이 실행되기 전에 A 코루틴에 대해 `join()` 함수를 호출하면 된다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    val updateTokenJob = launch(Dispatchers.IO) {
        println("[${Thread.currentThread().name}] 토큰 업데이트 시작")
        delay(100L)
        println("[${Thread.currentThread().name}] 토큰 업데이트 완료")
    }

    updateTokenJob.join() // networkCallJob 실행 전 updateTokenJob.join() 호출

    val networkCallJob = launch(Dispatchers.IO) {
        println("[${Thread.currentThread().name}] 네트워크 요청")
    }
}
```
![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/aa2a43f0-f2e8-416d-ae7a-03c9d5977766.png)

#### 코루틴 순차 처리 - `joinAll()` 함수 사용해 복수의 코루틴 순차 처리하기

* 서로 독립적인 복수의 코루틴을 병렬 실행한 후 이들이 모두 완료됐을 때 다음 작업을 실행해야 하는 경우가 많다.
* `joinAll(job1, job2)`은 내부적으로 가변인자(vararg) 배열을 순회하며 각 Job에 대해 `join()`을 연속으로 호출하는 확장 함수이다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    val convertImageJob1: Job = launch(Dispatchers.Default) {
        Thread.sleep(1000L) // 이미지1 변환 작업 실행
        println("[${Thread.currentThread().name}] 이미지1 변환 완료")
    }

    val convertImageJob2: Job = launch(Dispatchers.Default) {
        Thread.sleep(1000L) // 이미지2 변환 작업 실행
        println("[${Thread.currentThread().name}] 이미지2 변환 완료")
    }

    joinAll(convertImageJob1, convertImageJob2) // 이미지 1,2가 모두 변환될 때까지 대기

    val uploadImageJob: Job = launch(Dispatchers.IO) {
        println("[${Thread.currentThread().name}] 이미지1,2 업로드")
    }
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8378ad42-9c64-4028-af57-61de1db57973.png)

#### CorountineStart.LAZY

* 지연 코루틴(Lazy Coroutine)이란, 코루틴 빌더를 통해 코루틴을 생성하는 즉시 실행 대기열(Queue)에 적재되지 않고, 개발자가 명시적으로 시작 시그널을 보내거나 결과 값을 요구하는 시점까지 실행을 미루는(Deferred) 코루틴을 의미한다.
* Kotlin Coroutines의 기본 동작은 빌더가 호출되자마자 디스패처의 큐로 코루틴을 밀어 넣는 즉시 실행되는 방식이지만 필요에 따라 즉시 호출 메커니즘을 지연 방식으로 전환할 수 있다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    // 💡 1. launch 빌더 기반의 지연 코루틴 (Job 반환)
    val lazyJob = launch(start = CoroutineStart.LAZY) {
        println("Lazy Job 실행 완료")
    }

    // 💡 2. async 빌더 기반의 지연 코루틴 (Deferred 반환)
    val lazyDeferred = async(start = CoroutineStart.LAZY) {
        "Lazy Result Data"
    }
    
    // 이 시점까지는 위 2개의 코루틴 블록 내부 코드가 절대 실행되지 않고 메모리상에 대기만 함
}
```
* `launch` 기반 지연 트리거 : `start()`, `join()` 함수를 직접 호출해야 디스패처의 작업 대기열에 코루틴을 밀어넣는다.
* `async` 기반 지연 트리거 : 연산의 결과물이 실제로 필요한 시점에 `await()`를 호출하면 그제야 힙 메모리에 있던 코루틴이 깨어나 디스패처 큐로 이동하고 연산을 시작한다. 결과가 나올 때까지 호출한 코루틴은 중단 상태로 대기한다.

#### 코루틴 취소

* 코루틴 실행 도중 더 이상 코루틴을 실행할 필요가 없다면 즉시 취소해야 한다.
* 만약 취소하지 않으면 코루틴이 쓰레드를 계속해서 사용하기 때문에 애플리케이션의 성능 저하로 이어진다.

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    val startTime = System.currentTimeMillis()
    val longJob: Job = launch(Dispatchers.Default) {
        repeat(10) { repeatTime ->
            delay(1000L) // 1000밀리초 대기
            println("[${getElapsedTime(startTime)}] 반복횟수 ${repeatTime}")
        }
    }
    
    delay(2500L)     // 2500밀리초 대기
    longJob.cancel() // 취소
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a690a704-cfec-430a-9e0e-55001e8a32fa.png)

* 엄밀히 말하자면 `cancel()` 함수는 코루틴을 곧바로 취소하지 않고 취소 확인용 플래그를 '취소 요청됨'으로 바꾸는 역할만 한다.
* 이후 미래에 취소 확인용 플래그가 확인되는 시점이 되어서야 코루틴이 취소된다.
* 이 `cancel()` 함수를 사용하는 것은 순차성 관점에서 중요한 문제를 가진다.

#### `cancelAndJoin()` 함수를 사용한 취소 순차처리

* 코루틴이 취소된 후 실행되어야 하는 코루틴이 있다면 코루틴을 취소할 때, `cancelAndJoin()` 함수를 사용하면 된다.
* `cancelAndJoin()` = 취소 요청한 후 취소가 완료될 때까지 호출 코루틴 일시 중단

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/452fd9e9-ed03-4a78-9c06-2423ed3d4402.png)

* `cancel()` 함수를 호출한다고 해서 코루틴이 즉시 취소되는 개념이 아니다.
* 정확히 말하자면 `cancel()` 함수를 호출하면 Job 객체의 `isCancelled` 플래그의 값만 바뀌고 워커 쓰레드에서 이걸 인지하려면 `suspend` 블록에서 직접 확인을 해야하는데, `Thread.sleep()`은 JVM 쓰레드 블로킹이기 때문에 플래그를 체크하지 않는다.
* 그래서 취소 협력을 하려면 `Thread.sleep()`가 아니라 `delay()`를 써야한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0e41cf8a-9381-4c4b-a609-9df9daf313d3.png)

#### 코루틴 취소 확인 시점

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/674009e1-8b2c-4750-ae44-88293a48b798.png)

* 이 `cancel()` 함수를 호출한다고 하더라도 코루틴 즉시 호출 취소가 되지 않기 때문에 워커 쓰레드에서 무한 루프가 종료되지 않는 문제가 발생한다.
* 코루틴 취소 확인 시점을 만드는 방법은 다음과 같이 3가지 방법이 있다.
  * `delay()` 함수를 사용해 취소 확인 시점 만들기 : `delay()` 함수는 특정 시간만큼 코루틴을 일시 중단하게 만든다. 그렇게 되면 일시 중단 시점에 코루틴의 취소가 확인되어 취소된다.
  * `yield()` 함수를 사용해 취소 확인 시점 만들기 : `yield()` 함수를 호출한 코루틴은 자신이 사용하던 쓰레드를 양보한다. 쓰레드를 양보한다는 것은 코루틴이 쓰레드 사용을 중단하고 일시 중단한다는 뜻으로 이 때, 코루틴의 취소가 확인된다. 재개 시에는 `CoroutineDispatcher`에 의해 다시 쓰레드로 보내지는 과정을 거치기 때문에 비효율적이다.
  * `CoroutineScope.isAcitve`를 사용한 취소 확인 : `CoroutineScope`의 `isActive` 프로퍼티를 사용하면 코루틴에 취소가 요청됐는지 확인할 수 있다. 
* `delay()`와 `yield()` 함수는 모두 일시 중단 시점을 만들어 재개되는 과정을 거치기 때문에 비효율적이다.
* `CoroutineScope` 객체의 `isActive` 확장 프로퍼티를 사용하면 코루틴을 일시 중단 시키지 않고 취소를 확인할 수 있다.

#### 코루틴의 상태와 Job 객체의 상태 변수 정리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/57a5c48b-4df6-4c3d-9f0c-ab762119b0e4.png)

* `Job` 객체는 코루틴을 추상화한 객체여서 코루틴의 상태를 간접적으로 나타내는 3가지 상태 변수를 외부로 공개한다.
* `isActive` : 코루틴이 활성화 되어있는지 여부. 코루틴이 실행 중 상태일 때 `true`이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a6038a21-6161-4992-adc4-12cc2f6bd4e9.png)

* `isCancelled` : 코루틴에 취소가 요청됐는지 여부. `cancel()` 함수가 호출되기만 하면 `true`를 반환하므로 취소 중인 상태도 포함된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bfe63652-31e2-4133-af31-2c878f925519.png)

* `isCompleted` : 코루틴이 완료되었는지 여부. 코루틴이 실행 완료되거나 취소 완료되면 `true`를 반환한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5b733de2-c078-4752-862b-0e267bae6471.png)
