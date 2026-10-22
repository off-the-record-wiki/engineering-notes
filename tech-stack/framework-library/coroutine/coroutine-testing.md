# Coroutine - Coroutine Testing

#### 코틀린 단위 테스트 기초 - 테스트 더블

* 테스트 더블은 객체에 대한 대체물로 객체의 행동을 모방하는 객체이다.
* 다른 객체와 의존성을 가진 객체를 테스트하기 위해 테스트 더블이 필요하다.
* 테스트 더블의 종류로는 대표적으로 스텁(Stub), 페이크(Fake), 목(Mock) 이 3가지가 있다.
* **Stub**
  * 스텁이란, 미리 정의된 데이터를 반환하는 모방 객체이다. 반환 값이 없는 함수는 구현하지 않고 반환 값이 있는 동작만 미리 정의된 데이터를 반환하도록 구현한다.
  * 이 때, 스텁을 만들 때 미리 정의된 데이터를 내부 프로퍼티로 고정하면 유연하지 못해진다.
  * 유연하게 만들기 위해서는 미리 정의된 데이터를 주입 받는 형태로 만들어야 한다.
* **Fake**
  * 페이크란, 실제 객체와 비슷하게 동작하도록 구현된 모방 객체이다.

#### 코루틴 단위 테스트

* 간단한 일시 중단 함수를 테스트 할 때는 `runBlocking()` 함수를 사용해 코루틴을 만들면 된다.
* `runBlocking()` 함수를 사용해 실행에 오랜 시간이 걸리는 일시 중단 함수를 테스트하면 문제가 생긴다.

```kotlin
class RepeatAddUseCaseTest {
    @Test
    fun `100번 더하면 100이 반환된다`(): Unit = runBlocking {
        // Given
        val repeatAddUseCase = RepeatAddUseCase()

        // When
        val result: Int = repeatAddUseCase.add(100)

        // Then
        assertEquals(100, result)
    }
}
```

#### 코루틴 테스트 라이브러리

* 코루틴 테스트 라이브러리는 `TestCoroutineScheduler` 객체를 통해 가상 시간에서 테스트를 진행할 수 있도록 하는 기능을 제공한다.
* `TestCoroutineScheduler` 객체를 사용하면 시간을 자유자재로 다룰 수 있다.
* `TestCoroutineScheduler.advanceTimeBy()` : 가상 시간을 흐르게 만들 수 있다.
* `TestCoroutineScheduler.currentTime` : 현재 시간(가상 시간)을 알 수 있다.

#### 코루틴 테스트 라이브러리 활용 1 - StandardTestDispatcher 사용해 가상 시간 위에서 테스트하기

* `TestCoroutineScheduler` 객체는 `TestDispatcher` 객체를 만드는 `StandardTestDispatcher` 함수와 함께 사용할 수 있다.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTest {

    @Test
    fun `가상 시간 위에서 테스트 진행`() {
        // Given: 테스트 환경 설정
        val testCoroutineScheduler = TestCoroutineScheduler()
        val testDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)
        val testCoroutineScope = CoroutineScope(context = testDispatcher)

        var result = 0

        // When: 코루틴 실행 (즉시 시작되지 않고 스케줄러에 등록됨)
        testCoroutineScope.launch {
            delay(10000L) // 10초 대기
            result = 1
            delay(10000L) // 추가 10초 대기
            result = 2
        }

        // Then: 가상 시간을 흐르게 하며 결과 검증
        
        // 1. 5초 흐르게 함 (총 5초 지남) -> delay(10000) 중이므로 result는 아직 0
        testCoroutineScheduler.advanceTimeBy(5000L)
        assertEquals(0, result)

        // 2. 추가 6초 흐르게 함 (총 11초 지남) -> 첫 번째 delay(10000) 종료, result는 1이 됨
        testCoroutineScheduler.advanceTimeBy(6000L)
        assertEquals(1, result)

        // 3. 추가 10초 흐르게 함 (총 21초 지남) -> 두 번째 delay(10000) 종료, result는 2가 됨
        testCoroutineScheduler.advanceTimeBy(10000L)
        assertEquals(2, result)
    }
}
```

#### 코루틴 테스트 라이브러리 활용 2 - advanceUntilIdle 사용해 모든 코루틴 실행

* `TestCoroutineScheduler`의 `advanceUntilIdle()` 함수가 호출되면 가상 시간 스케줄러를 사용하는 모든 코루틴이 완료될 때까지 시간이 흐른다.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineAdvanceTest {

    @Test
    fun `advanceUntilIdle의 동작 살펴보기`() {
        // Given: 테스트 환경 설정
        val testCoroutineScheduler = TestCoroutineScheduler()
        val testDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)
        val testCoroutineScope = CoroutineScope(context = testDispatcher)

        var result = 0

        // When: 코루틴 실행 (총 20초의 대기 시간이 포함됨)
        testCoroutineScope.launch {
            delay(10_000L) // 10초 대기
            result = 1
            delay(10_000L) // 추가 10초 대기
            result = 2
        }

        // advanceUntilIdle() 호출: 
        // 스케줄러에 남아있는 모든 작업(위의 두 번의 delay 포함)이 끝날 때까지 
        // 시간을 자동으로 점프합니다.
        testCoroutineScheduler.advanceUntilIdle()

        // Then: 모든 작업이 완료되었으므로 result는 최종값인 2가 됨
        assertEquals(2, result)
    }
}
```

#### 코루틴 테스트 라이브러리 활용 3 - TestCoroutineScheduler을 포함하는 StandardTestDispatcher

* `StandardTestDispatcher` 함수는 `TestCoroutineScheduler`을 생성하는 부분을 포함한다.

```kotlin
@Suppress("FunctionName")
public fun StandardTestDispatcher(
    scheduler: TestCoroutineScheduler? = null,
    name: String? = null
): TestDispatcher = StandardTestDispatcherImpl(
    scheduler ?: TestMainDispatcher.currentTestScheduler ?: TestCoroutineScheduler(), name)
```

#### TestScope

* `TestScope`를 사용하면 내부에 `TestDispatcher`을 가진 `TestScope` 객체가 반환된다.
* `TestScope`은 확장 함수를 통해 `advancedUntilIdle`, `advanceTimeBy`를 직접 호출할 수 있도록 한다.

```kotlin
/**
 * A coroutine scope that for launching test coroutines.
 *
 * The scope provides the following functionality:
 * - The [coroutineContext] includes a [coroutine dispatcher][TestDispatcher] that supports delay-skipping, using
 *   a [TestCoroutineScheduler] for orchestrating the virtual time.
 *   This scheduler is also available via the [testScheduler] property, and some helper extension
 *   methods are defined to more conveniently interact with it: see [TestScope.currentTime], [TestScope.runCurrent],
 *   [TestScope.advanceTimeBy], and [TestScope.advanceUntilIdle].
 * - When inside [runTest], uncaught exceptions from the child coroutines of this scope will be reported at the end of
 *   the test.
 *   It is invalid for child coroutines to throw uncaught exceptions when outside the call to [TestScope.runTest]:
 *   the only guarantee in this case is the best effort to deliver the exception.
 *
 * The usual way to access a [TestScope] is to call [runTest], but it can also be constructed manually, in order to
 * use it to initialize the components that participate in the test.
 *
 * #### Differences from the deprecated [TestCoroutineScope]
 *
 * - This doesn't provide an equivalent of [TestCoroutineScope.cleanupTestCoroutines], and so can't be used as a
 *   standalone mechanism for writing tests: it does require that [runTest] is eventually called.
 *   The reason for this is that a proper cleanup procedure that supports using non-test dispatchers and arbitrary
 *   coroutine suspensions would be equivalent to [runTest], but would also be more error-prone, due to the potential
 *   for forgetting to perform the cleanup.
 * - [TestCoroutineScope.advanceTimeBy] also calls [TestCoroutineScheduler.runCurrent] after advancing the virtual time.
 * - No support for dispatcher pausing, like [DelayController] allows. [TestCoroutineDispatcher], which supported
 *   pausing, is deprecated; now, instead of pausing a dispatcher, one can use [withContext] to run a dispatcher that's
 *   paused by default, like [StandardTestDispatcher].
 * - No access to the list of unhandled exceptions.
 */
public sealed interface TestScope : CoroutineScope {
    // ...
}
```

#### runTest 사용해 테스트 만들기

* `runTest()` 함수는 `TestScope` 객체를 사용해 코루틴을 실행시키고 그 코루틴 내부에서 일시 중단 함수가 실행되더라도 가상 시간을 자동으로 흐르게 해 곧바로 실행 완료될 수 있도록 하는 코루틴 빌더 함수이다.
* 즉, `advanceUntilIdle`을 호출하지 않더라도 가상 시간이 흐른다.
* `runTest()` 함수는 테스트 전체를 감싸는 방식으로 자주 사용된다.
* `runTest()` 함수는 `runTest()` 함수로 생성된 코루틴의 시간만 흐르게 만든다. `runTest()` 함수를 호출해 생성된 `TestScope`을 사용해 새로운 코루틴이 실행된다면 이 코루틴은 자동으로 시간이 흐르지 않게 된다. 이런 경우 `advanceUntilIdle()` 함수를 명시적으로 호출해줘야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/64d1d1b1-c478-4563-925b-62d8871a8fe1.png)

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineRunTest {

    @Test
    fun `runTest 사용하기`() = runTest {
        // Given
        var result = 0

        // When: runTest 내부에서는 delay를 자동으로 건너뜁니다.
        // 별도의 스케줄러 설정이나 advanceTimeBy 호출 없이도 
        // 20초가 지난 후의 상태를 즉시 확인할 수 있습니다.
        delay(10_000L) // 10초 대기
        result = 1
        delay(10_000L) // 10초 대기
        result = 2

        // Then
        assertEquals(2, result)
    }
}
```
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineAdvanceTest {

    @Test
    fun `runTest 내부에서 advanceUntilIdle 사용하기`() = runTest {
        var result = 0

        // 별도의 코루틴을 시작 (runTest는 이 안의 코드가 끝날 때까지 기다리지 않음)
        launch {
            delay(1000L) // 1초 대기
            result = 1
        }

        // 1. 아직 자식 코루틴이 수행되지 않음
        println("가상 시간: ${this.currentTime}ms, result = $result")

        // 2. 자식 코루틴이 완료될 때까지 가상 시간을 이동
        advanceUntilIdle()

        // 3. 자식 코루틴이 모두 완료됨
        println("가상 시간: ${this.currentTime}ms, result = $result")
    }
}
```

#### 코루틴 단위테스트 심화

* 일시 중단 함수 내부에서 새로운 코루틴을 생성하는 경우는 테스트가 쉽지만, 일시 중단 함수가 아닌 일반 함수 내부에서 새로운 코루틴을 실행하는 경우도 있다.
* 이런 경우 `StringStateHolder` 내부의 `CoroutineScope` 객체는 별도의 루트 `Job` 객체를 가져 `runTest`로 실행되는 코루틴과 구조화가 되지 않고, `Dispatchers.IO`를 사용해 코루틴을 실행하기 때문에 실제 시간 위에서 실행된다.
* `StringStateHolder`가 `CoroutineDispatcher`을 주입받도록 구현을 변경해 해결할 수 있다.

```kotlin
class StringStateHolder(
    // 테스트 가능하도록 디스패처를 주입받음 (기본값은 Dispatchers.IO)
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val coroutineScope = CoroutineScope(dispatcher)

    var stringState = ""
        private set

    fun updateStringWithDelay(string: String) {
        coroutineScope.launch {
            delay(1000L)
            stringState = string
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StringStateHolderTestSuccess {

    @Test
    fun `updateStringWithDelay(ABC)가 호출되면 문자열이 ABC로 변경된다`() {
        // Given: 테스트용 디스패처 생성 및 주입
        val testDispatcher = StandardTestDispatcher()
        val stringStateHolder = StringStateHolder(dispatcher = testDispatcher)

        // When: 비동기 메서드 호출
        stringStateHolder.updateStringWithDelay("ABC")

        // Then: 가상 시간을 끝까지 전진시킨 후 결과 검증
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("ABC", stringStateHolder.stringState)
    }
}
```

* `runTest`를 호출해 실행되는 코루틴은 호출 쓰레드를 블로킹하며, 내부의 모든 코루틴이 실행 완료될 때까지 종료되지 않도록 한다.
* `runTest` 코루틴 내부에서 새롭게 `launch()` 함수가 호출된 다음, 이 `launch()` 코루틴 내부에서 while문과 같이 무한히 실행되는 작업이 실행되면 `runTest` 코루틴은 끝나지 않고 계속해서 실행되기 때문에 테스트가 실패한다.
* `runTest` 람다식의 수신 객체인 `TestScope`은 BackGroundScope를 제공한다.
* BackGroundScope은 `runTest` 모든 코드가 실행되면 자동으로 취소되고 이를 통해 테스트가 무한히 실행되는 것을 방지할 수 있다.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTimeoutTest {

    @Test
    fun `backgroundScope를 사용하여 타임아웃 해결하기`() = runTest {
        var result = 0

        // runTest가 제공하는 backgroundScope에서 launch를 실행합니다.
        // 이 코루틴은 테스트가 끝날 때 자동으로 cancel되므로 무한 루프여도 안전합니다.
        backgroundScope.launch {
            while (true) {
                delay(1000L)
                result += 1
            }
        }

        // 1. 가상 시간을 1.5초(1500ms) 전진 -> result가 1이 됨 (성공)
        advanceTimeBy(1500L)
        assertEquals(1, result)

        // 2. 가상 시간을 추가로 1초(1000ms) 전진 -> result가 2가 됨 (성공)
        advanceTimeBy(1000L)
        assertEquals(2, result)
        
        // 테스트 본문이 여기서 끝나면, runTest가 backgroundScope에 있던 
        // 무한 루프 코루틴을 알아서 취소시키며 테스트가 정상적으로 '성공' 종료됩니다.
    }
}
```
