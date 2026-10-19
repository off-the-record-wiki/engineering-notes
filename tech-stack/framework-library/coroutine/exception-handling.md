# Coroutine - Exception Handling

#### 예외 전파

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/de4fa1dc-c143-41dd-834b-8f912cf7100f.png)

* 코루틴 실행 도중 예외가 밣생하면 예외가 발생한 코루틴이 취소되고 예외가 부모 코루틴으로 전파된다.
* 이 때, 만약 예외를 전파받은 부모 코루틴도 예외를 적절히 처리하지 않는다면 취소되고, 그 상위의 코루틴으로 예외가 전파된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e7cb87d8-a688-4f18-b10d-514ea53ef109.png)

* 코루틴이 예외를 전파받아 상위 코루틴으로 전파 후 취소되면 취소가 해당 코루틴의 하위에 있는 자식 코루틴들에게 전파된다.

#### 예외 전파 제한

* 코루틴의 구조화를 깨면 예외 전파를 제한할 수 있다.
* 다음과 같이 `Job`객체를 새로 만들어 구조화를 깨고 싶은 코루틴에 연결하면 구조화가 깨진다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d1420029-8578-4f24-9832-ad4d970885d0.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0b4d9c8d-57af-4bae-9fc9-500fd39b5ed6.png)

* 하지만 실제로는 위와 같이 사용을 하진 않는다.
* 코루틴의 구조화가 깨지면 예외 전파 뿐만 아니라 취소 전파도 제한된다. 이는 비동기 작업의 안정성이 깨진다.
* `SupervisorJob` 객체는 자식 코루틴으로부터 예외를 전파 받지 않는 특수한 `Job` 객체이다. 이 객체는 예외 전파를 받지 않아 자식 코루틴에게서 예외가 발생하더라도 취소되지 않는다.
* `SupervisorJob` 객체는 자식 코루틴에게서 발생한 예외가 다른 자식 코루틴에게 영향을 미치지 못하게 만드는데 사용된다. `SupervisorJob` 객체는 `CoroutineScope` 생성함수와 자주 사용된다.
```kotlin
public fun SupervisorJob(parent: Job? = null) : CompletableJob = SupervisorJobImpl(parent)
```

* 단일 코루틴 빌더 함수의 context 인자로 `SupervisorJob` 객체를 넘기고 그 하위에 자식 코루틴들을 생성할 경우 `SupervisorJob` 객체는 아무런 역할도 하지 못한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f02e1d1d-4de1-46c9-9bf4-add652c640d4.png)

* `supervisorScope()` 함수는 `SupervisorJob` 객체를 가진 `CoroutineScope` 객체를 생성한다.
* `supervisorScope()` 함수를 통해 생성된 `SupervisorJob` 객체는 `supervisorScope()` 함수를 호출한 코루틴을 부모로 가진다.
* `supervisorScope()` 함수를 통해 생성된 `SupervisorJob` 객체는 코드가 모두 실행되고 나서 자식 코루틴의 실행도 완료되면 자동으로 완료된다. 복잡한 설정 없이 구조화를 꺠지 않고 예외 전파를 제한할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e1514cb9-b7c4-45a6-8162-576674eee5bd.png)

#### 예외 처리 1 - CoroutineExceptionHandler 사용해 예외 처리 하기

* `CoroutineExceptionHandler`는 `CoroutineContext` 구성 요소 중 하나이다.
* `CoroutineExceptionHandler`는 처리되지 않은 예외만 처리한다.
* `CoroutineExceptionHandler`는 `launch()` 코루틴으로 시작되는 코루틴 계층의 공통 예외 처리기로 동작하는 구성요소이다.
* 예외를 로깅하거나 오류 메시지를 표시하기 위해 구조화된 코루틴들에 공통으로 동작하는 예외 처리기를 설정해야 하는 경우 사용된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/46011eb4-dd27-46a7-b852-6be91c793573.png)

#### 예외 처리 2 - try ~ catch문을 사용한 예외 처리

* `try ~ catch`문을 사용하면 코루틴에서 발생한 예외를 처리할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a651d38a-914d-498c-94eb-97078eb7b861.png)

* 그러나 다음과 같이 코루틴 빌더 함수에 `try ~ catch`문을 사용하면 예외를 잡지 못한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/dba16395-950a-4f82-9600-49de1294b32a.png)

#### 예외 처리 3 - async 예외 처리

* `async()` 코루틴 빌더 함수는 코루틴의 결과값을 `Deferred` 객체에 감싸고, `await()` 함수 호출 시점에 결과값을 노출한다.
* 만약 코루틴 실행 도중에 예외가 발생해 결과값이 없다면 `Deferred`에 대한 `await()` 함수 호출 시 예외가 노출된다.
* `async()` 코루틴 빌더 함수 사용 시 `await()` 함수 호출부에서만 예외 처리를 하는 경우가 많은데 `async()` 코루틴 빌더 함수도 `launch()` 코루틴 빌더 함수와 마찬가지로 예외가 발생하면 부모 코루틴으로 예외를 전파한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/59eb744b-d4f9-4bfd-a535-e56254411fe1.png)

#### 전파되지 않는 예외

* `CancellationException`은 부모 코루틴으로 전파되지 않는다.
* `CancellationException`은 코루틴을 취소하기 위한 특별한 예외이기 때문이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1120f15f-edf9-4c23-ab97-c07fec06903b.png)

```kotlin
@SinceKotlin("1.4")
public expect open class CancellationException : IllegalStateException {
    public constructor()
    public constructor(message: String?)
}

/**
 * Creates an instance of [CancellationException] with the given [message] and [cause].
 */
@SinceKotlin("1.4")
public expect fun CancellationException(message: String?, cause: Throwable?): CancellationException

/**
 * Creates an instance of [CancellationException] with the given [cause].
 */
@SinceKotlin("1.4")
public expect fun CancellationException(cause: Throwable?): CancellationException
```
* `CancellationException`은 `Exception`으로 예외를 처리해선 안 된다.
