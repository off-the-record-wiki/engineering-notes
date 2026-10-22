# Coroutine - Structured Concurrency

#### 실행 환경 상속

* 부모 코루틴은 자식 코루틴에게 실행 환경을 상속한다.
* 부모 코루틴이 자식 코루틴을 생성하면 부모 코루틴의 `CoroutineContext`가 자식 코루틴에게 전달된다.

```kotlin
fun main() = runBlocking<Unit> {
    // 1. 단일 스레드 디스패처와 코루틴 이름이 결합된 커스텀 컨텍스트 생성
    val coroutineContext: CoroutineContext = newSingleThreadContext("MyThread") + CoroutineName("CoroutineA")
    
    // 2. 부모 코루틴 생성 (커스텀 컨텍스트 주입)
    launch(coroutineContext) { 
        println("[${Thread.currentThread().name}] 부모 코루틴 실행")
        
        // 3. 자식 코루틴 생성 (컨텍스트를 명시하지 않음 -> 부모로부터 상속)
        launch { 
            println("[${Thread.currentThread().name}] 자식 코루틴 실행")
        }
    }
}
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/eb5562ae-6d2e-41f7-a949-7d67b04c83e5.png)

* 자식 코루틴을 생성하는 코루틴 빌더 함수에 새로운 `CoroutineContext` 객체가 전달되면 부모 코루틴에서 전달받은 `CoroutineContext` 구성 요소들은 자식 코루틴 빌더 함수로 전달된 `CoroutineContext` 객체의 구성 요소들로 덮어 씌여진다.

```kotlin
fun main() = runBlocking<Unit> {
    // 1. 단일 스레드 디스패처와 부모 이름을 결합한 컨텍스트 정의
    val coroutineContext: CoroutineContext = newSingleThreadContext("MyThread") + CoroutineName("ParentCoroutine")
    
    // 2. 부모 코루틴 생성 및 실행
    launch(coroutineContext) { 
        println("[${Thread.currentThread().name}] 부모 코루틴 실행")
        
        // 3. 자식 코루틴 생성 시 새로운 CoroutineName을 인자로 주입 (우선순위 조작)
        launch(CoroutineName("ChildCoroutine")) { 
            println("[${Thread.currentThread().name}] 자식 코루틴 실행")
        }
    }
}
```

* `launch()`나 `async()`를 포함한 모든 코루틴 빌더 함수는 호출 시마다 코루틴 추상체인 `Job` 객체를 새롭게 생성한다.
* 코루틴 제어에 `Job` 객체가 필요하기 때문에 `Job` 객체를 부모 코루틴으로부터 상속받지 않는다.
* 즉, 코루틴 빌더를 통해 생성된 코루틴들은 서로 다른 `Job`을 가진다.

#### 코루틴의 구조화와 작업 제어

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/09390154-4cce-4a32-823a-e83cfdeeb28b.png)

* 코루틴의 구조화는 하나의 큰 비동기 작업을 작은 비동기 작업으로 나눌 때 일어난다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5a11f2b4-a043-44f4-b48c-faa40038b5f2.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/dee7fb84-ee75-47f1-92f6-a70f5256af49.png)

* 코루틴으로 취소가 요청되면 자식 코루틴에 취소가 전파된다.
* 부모 코루틴은 모든 자식 코루틴이 실행 완료되어야 완료될 수 있다.
* 코루틴의 구조화는 큰 작업을 연관된 여러 작은 작업으로 나누는 방식으로 일어나는데 작은 작업이 완료되어야 큰 작업이 완료될 수 있기 때문이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9866e982-512f-4a7f-b1ee-30836340dced.png)

* 실행 완료 중(Completing) 상태란 부모 코루틴의 모든 코드가 실행됐지만, 자식 코루틴이 실행 중인 경우 부모 코루틴이 갖는 상태이다.
* 부모 코루틴은 더 이상 실행할 코드가 없더라도 자식 코루틴들이 모두 완료될 때까지 기다리는데 이 때, '실행 완료 중' 상태에 머무른다.
* '실행 완료 중' 상태의 부모 코루틴은 자식 코루틴이 모두 완료되면 자동으로 '실행 완료' 상태로 바뀐다.

#### CoroutineScope 사용해 코루틴 관리하기

```kotlin
public interface CoroutineScope {
    /**
     * The context of this scope.
     * Context is encapsulated by the scope and used for implementation of coroutine builders that are extensions on the scope.
     * Accessing this property in general code is not recommended for any purposes except accessing the [Job] instance for advanced usages.
     *
     * By convention, should contain an instance of a [job][Job] to enforce structured concurrency.
     */
    public val coroutineContext: CoroutineContext
}
```
* `CoroutineScope`는 내부에 코루틴 실행 환경인 `CoroutineContext`를 가지는 단순한 인터페이스이다.
* `CoroutineScope` 인터페이스를 구현한 구체 클래스를 사용하면 `CoroutineScope` 객체를 생성할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f23f43c9-fbf0-40e0-9c3f-65c606d062ef.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c4a765ea-2d54-4b6f-b724-edd56ac189eb.png)

* `CoroutineScope`를 취소하려면 다음과 같이 취소할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9c99ae58-1015-470f-b8eb-ad5388371ab6.png)

* `CoroutineScope` 객체는 현재 `CoroutineScope`이 활성화 되어있는지 확인하는 `isActive` 프로퍼티를 제공한다.
* `isActive` 프로퍼티는 `CoroutineContext`로부터 `Job` 객체를 꺼내 `isActive`를 확인한다.

```kotlin
fun main() = runBlocking<Unit> {
    // 1. Dispatchers.Default 백그라운드 트랙에서 무한 루프를 도는 자식 코루틴 생성
    val whileJob: Job = launch(Dispatchers.Default) { 
        // 2. 주기적으로 현재 코루틴 스코프의 활성화 상태(isActive)를 체크
        while (this.isActive) {
            println("작업 중")
        }
    }
    
    // 3. 메인 스레드에서 100ms 동안 대기하며 자식 코루틴이 동작할 시간을 줌
    delay(100L)
    
    // 4. 자식 코루틴에 취소(cancel) 시그널 전파
    whileJob.cancel()
}
```

#### 구조화와 Job

* `runBlocking()` 함수를 호출하면 부모 `Job`이 없는 루트 `Job` 객체가 생성된다.
  * 루트 `Job` : 부모 `Job` 객체가 없는 구조화의 시작점 역할을 하는 `Job` 객체
  * 루트 코루틴 : 이 `Job` 객체에 의해 제어되는 코루틴

#### Job 구조화 깨기 1 - CoroutineScope 사용해 구조화 깨기

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/454a20b8-331f-4c20-8c1d-60ef98d8b222.png)

* `CoroutineScope` 생성 함수는 새로운 루트 `Job`을 가진 `CoroutineContext`를 생성한다.

#### Job 구조화 깨기 2 - Job 객체 생성해 구조화 깨기

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1a611aea-e75d-48ac-ae27-9e866f06c4ca.png)

* `Job` 생성 함수로 새로운 루트 `Job`을 생성할 수 있다.

#### 구조화 깨서 일부 코루틴만 취소되지 않도록 하기

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/67e702e1-f4fc-4bfc-9375-99fbd28f19f2.png)

* `Job` 생성 함수는 부모 `Job`을 인자로 받을 수 있다.
  * 만약 parent 인자가 입력되지 않으면 `parent = null`이 되어 루트 `Job`이 생성된다.
  * 만약 parent 인자가 입력되면 해당 `Job`을 부모로 하는 `Job`이 생성된다.

```kotlin
/**
 * Creates a job object in an active state.
 * @param parent optional parent job to enforce structured concurrency.
 */
public fun Job(parent: Job? = null): CompletableJob = JobImpl(parent)
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fe011d9a-937d-4d73-9171-ba5df22cbd8d.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/77ae6d68-1acc-46e1-8a45-d017d98b4a0b.png)

* 이 때, 생성함수로 생성된 `Job` 객체는 자동으로 실행 완료되지 않기 때문에 명시적으로 `complete()` 함수를 호출해야 한다.



[runBlocking 함수의 이해](runblocking.md)
