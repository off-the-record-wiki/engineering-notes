# Coroutine - Understanding Coroutines

#### 루틴, 서브루틴, 코루틴이란?

* 서브루틴이란, 함수(루틴) 내에서 함수가 호출될 경우 호출된 함수를 서브루틴이라고 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/de8ca9f9-b699-4aa7-a1ea-82402f098d37.png)

* 루틴에 의해 서브루틴이 호출되면 서브루틴의 실행이 완료될 때까지 다른 작업을 할 수 없는 것과 다르게 코루틴은 함께 실행되는 루틴으로 서로 간에 쓰레드 사용을 양보하며 함께 실행된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d28d105e-bcd6-42d2-8cd4-a111b0ad45d7.png)

#### 코루틴의 쓰레드 양보

* 코루틴은 작업 중간에 쓰레드 사용이 필요가 없어지면 쓰레드를 양보한다. 이 떄, 양보된 쓰레드는 다른 코루틴이 점유해 사용할 수 있다.
* 쓰레드를 양보하는 주체는 코루틴이다. 따라서 쓰레드를 양보하기 위해서는 코루틴이 직접 쓰레들 양보를 위한 함수를 호출해야 한다.

#### `delay()` 함수를 통한 쓰레드 양보

```kotlin
fun main(): Unit = runBlocking<Unit> {
    val startTime: Long = System.currentTimeMillis()
    repeat(10) { repeatTime ->
        launch {
            delay(1000L) // 1초 동안 코루틴 일시 중단
            println("[${getElapsedTime(startTime)}] 코루틴${repeatTime} 실행 완료")
        }
    }
}

fun getElapsedTime(startTime: Long): String = "지난 시간: ${System.currentTimeMillis() - startTime}ms"
```
* 작업을 일정 시간 동안 일시 중단해야 할 경우 `delay()` 함수를 사용할 수 있다.
* 코루틴이 `delay()` 함수를 호출하면 코루틴은 사용하던 쓰레드를 양보하고 설정된 시간 동안 일시 중단된다.
* 여기서 `Thread.sleep()`을 사용하면 쓰레드를 양보하지 않는다. 코루틴이 양보의 주체가 되는 것이지 쓰레드가 양보의 주체가 되는 것이 아니다.

#### `join()`과 `await()` 함수를 통한 양보

* `join()` 함수나 `await()` 함수가 호출되면 해당 함수를 호출한 코루틴은 쓰레드를 양보하고 `join()` 또는 `await()`의 대상이 된 코루틴이 완료될 때까지 일시 중단된다.
* 하나의 코루틴이 쓰레드를 양보하지 않으면 다른 코루틴은 쓰레드를 점유하지 못한다.

#### `yield()` 함수를 통한 양보

* `yield()` 함수를 통해 직접 쓰레드를 양보하는 경우도 필요하다.
* 단일 쓰레드만 사용하는 상황에서 명시적으로 `yield()`를 사용하는 경우가 종종 생긴다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/49d1033e-303d-410f-a7a3-d00d09656c02.png)

**1. 취소 안되는 코드 - 같은 쓰레드 공유(디스패처 X)**

* `launch()` 내부에 디스패처를 적지 않으면 부모 코루틴인 `runBlocking()`의 컨텍스트를 그대로 상속받는다.
* `runBlocking()`은 메인 쓰레드만을 사용한다.
* `runBlocking()` 코루틴이 메인 쓰레드에서 실행되다가 `delay(100L)`을 만나면 메인 쓰레드의 사용권을 양보한다.
* 대기하고 있던 `launch()` 코루틴이 메인 쓰레드의 제어권을 넘겨받아 while문을 실행한다.
* while문 안에 일시 중단 함수(delay나 yield)가 없기 때문에 일반적인 연산만 무한 반복하므로 메인 쓰레드를 쥐고 절대 놓아주지 않는다.
* 100ms가 지나 `runBlocking()`이 깨어나려 해도, 메인 쓰레드를 `launch()` 코루틴이 점유하고 있어 깨어날 수가 없다.
* `job.cancel()` 코드에 도달하지 못하므로 취소가 불가능하다.

**2. 취소되는 코드 - 쓰레드 분리(디스패처 O)**

* `launch()` 내부에 `Dispatchers.Default`를 지정하면 이 코루틴은 부모와 쓰레드를 공유하지 않고 백그라운드 쓰레드 풀에서 완전히 독립적으로 실행된다.
* `runBlocking()` 코루틴은 메인 스레드에서, `launch()` 코루틴은 백그라운드 워커 쓰레드에서 각각 동시에 실행된다.
* `runBlocking()`이 `delay(100L)`을 만나면 메인 쓰레드에서 100ms동안 잠들게 된다.
* 100ms 뒤, 메인 쓰레드가 정상적으로 깨어나 다음 줄인 `whileJob.cancel()`을 호출한다.
* 백그라운드 워커 쓰레드에서 돌고 있던 while문에서 `isActive`를 검사하는데 메인 쓰레드에서 취소 요청을 했기 때문에 `isActive`가 false가 되면서 루프를 탈출하고 종료하게 된다.

#### 코루틴의 실행 쓰레드

* 코루틴이 일시 중단된 후 재개되면 `CoroutineDispatcher` 객체는 재개된 코루틴을 다시 쓰레드에 보낸다.
* 이 때, `CoroutineDispatcher` 객체는 코루틴을 사용할 수 있는 쓰레드 중 하나에 보내기 때문에 코루틴이 재개된 후의 실행 쓰레드는 일시 중단되기 전의 쓰레드와 다를 수가 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cb416b81-7b55-4e5e-a849-6d09e9e1482f.png)
