# Coroutine - Coroutine Context

#### CoroutineContext

```kotlin
/**
 * Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a [Job].
 * The coroutine is cancelled when the resulting job is [cancelled][Job.cancel].
 *
 * The coroutine context is inherited from a [CoroutineScope]. Additional context elements can be specified with [context] argument.
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [Dispatchers.Default] is used.
 * The parent job is inherited from a [CoroutineScope] as well, but it can also be overridden
 * with a corresponding [context] element.
 *
 * By default, the coroutine is immediately scheduled for execution.
 * Other start options can be specified via `start` parameter. See [CoroutineStart] for details.
 * An optional [start] parameter can be set to [CoroutineStart.LAZY] to start coroutine _lazily_. In this case,
 * the coroutine [Job] is created in _new_ state. It can be explicitly started with [start][Job.start] function
 * and will be started implicitly on the first invocation of [join][Job.join].
 *
 * Uncaught exceptions in this coroutine cancel the parent job in the context by default
 * (unless [CoroutineExceptionHandler] is explicitly specified), which means that when `launch` is used with
 * the context of another coroutine, then any uncaught exception leads to the cancellation of the parent coroutine.
 *
 * See [newCoroutineContext] for a description of debugging facilities that are available for a newly created coroutine.
 *
 * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code which will be invoked in the context of the provided scope.
 **/
public fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val newContext = newCoroutineContext(context)
    val coroutine = if (start.isLazy)
        LazyStandaloneCoroutine(newContext, block) else
        StandaloneCoroutine(newContext, active = true)
    coroutine.start(start, coroutine, block)
    return coroutine
}
```
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
* `launch()`와 `async()` 함수의 선언부의 첫 매개변수는 `CoroutineContext`이다.
* `CoroutineContext` 객체는 크게 4가지 주요 구성요소를 가진다.

```kotlin
package kotlin.coroutines

/**
 * Persistent context for the coroutine. It is an indexed set of [Element] instances.
 * An indexed set is a mix between a set and a map.
 * Every element in this set has a unique [Key].
 */
@SinceKotlin("1.3")
public interface CoroutineContext {
    /**
     * Returns the element with the given [key] from this context or `null`.
     */
    public operator fun <E : Element> get(key: Key<E>): E?

    /**
     * Accumulates entries of this context starting with [initial] value and applying [operation]
     * from left to right to current accumulator value and each element of this context.
     */
    public fun <R> fold(initial: R, operation: (R, Element) -> R): R

    /**
     * Returns a context containing elements from this context and elements from  other [context].
     * The elements from this context with the same key as in the other one are dropped.
     */
    public operator fun plus(context: CoroutineContext): CoroutineContext =
        if (context === EmptyCoroutineContext) this else // fast path -- avoid lambda creation
            context.fold(this) { acc, element ->
                val removed = acc.minusKey(element.key)
                if (removed === EmptyCoroutineContext) element else {
                    // make sure interceptor is always last in the context (and thus is fast to get when present)
                    val interceptor = removed[ContinuationInterceptor]
                    if (interceptor == null) CombinedContext(removed, element) else {
                        val left = removed.minusKey(ContinuationInterceptor)
                        if (left === EmptyCoroutineContext) CombinedContext(element, interceptor) else
                            CombinedContext(CombinedContext(left, element), interceptor)
                    }
                }
            }

    /**
     * Returns a context containing elements from this context, but without an element with
     * the specified [key].
     */
    public fun minusKey(key: Key<*>): CoroutineContext

    /**
     * Key for the elements of [CoroutineContext]. [E] is a type of element with this key.
     */
    public interface Key<E : Element>

    /**
     * An element of the [CoroutineContext]. An element of the coroutine context is a singleton context by itself.
     */
    public interface Element : CoroutineContext {
        /**
         * A key of this coroutine context element.
         */
        public val key: Key<*>

        public override operator fun <E : Element> get(key: Key<E>): E? =
            @Suppress("UNCHECKED_CAST")
            if (this.key == key) this as E else null

        public override fun <R> fold(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        public override fun minusKey(key: Key<*>): CoroutineContext =
            if (this.key == key) EmptyCoroutineContext else this
    }
}
```

* `CoroutineName` : 코루틴의 이름 설정에 사용되는 객체
* `CoroutineDispatcher` : 코루틴을 쓰레드에 보내 실행하는 객체
* `Job` : 코루틴의 추상체로 코루틴을 조작하는데 사용되는 객체
* `CoroutineExceptionHandler` : 코루틴에서 발생된 예외를 처리한다.

#### CoroutineContext 구성

| 키 | 값 |
| :--- | :--- |
| CoroutineName 키 | CoroutineName 객체 |
| CoroutineDispatcher 키 | CoroutineDispatcher 객체 |
| Job 키 | Job 객체 |
| CoroutineExceptionHandler 키 | CoroutineExceptionHandler 객체 |

* `CoroutineContext` 객체에 구성요소를 추가하기 위해서는 더하기 연산자를 사용하면 된다.
* `CoroutineContext` 객체는 키-값 싸응로 구성 요소를 관리하지만 키에 값을 직접 대입하는 방법을 사용하지 않는다.
* `CoroutineContext` 객체에 같은 구성 요소가 둘 이상 더해지면 나중에 추가된 구성요소가 이전 값을 덮어씌운다.
* 여러 구성요소로 이루어진 `CoroutineContext` 2개가 합쳐질 때도 같은 키를 가진 구성요소가 있다면, 나중에 들어온 구성 요소가 먼저 들어온 구성 요소를 덮어씌운다.

```kotlin
val coroutineContext: CoroutineContext = newSingleThreadContext("MyThread") + CoroutineName("MyCoroutine")
```

* `Job` 객체는 기본적으로 `launch()`와 같은 코루틴 빌더 함수를 통해 자동으로 생성되지만 `Job()`을 호출해 생성할 수도 있다.
* `Job` 객체는 `CoroutineContext`의 구성요소로 `CoroutineContext`에 더해질 수 있다.

```kotlin
fun main() {
    // 1. 코루틴 빌더 없이 독립적으로 동작하는 새로운 Job 인스턴스 수동 생성
    val myJob: CompletableJob = Job()
    
    // 2. 기본 CPU 디스패처와 수동 생성한 Job을 + 연산자로 결합
    val coroutineContext: CoroutineContext = Dispatchers.Default + myJob
}
```

#### CoroutineContext 구성요소에 접근

```kotlin
/**
 * User-specified name of coroutine. This name is used in debugging mode.
 * See [newCoroutineContext][CoroutineScope.newCoroutineContext] for the description of coroutine debugging facilities.
 */
public data class CoroutineName(
    /**
     * User-defined coroutine name.
     */
    val name: String
) : AbstractCoroutineContextElement(CoroutineName) {
    /**
     * Key for [CoroutineName] instance in the coroutine context.
     */
    public companion object Key : CoroutineContext.Key<CoroutineName>

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String = "CoroutineName($name)"
}
```
* `CoroutineContext` 구성요소의 키는 `CoroutineContext.Key` 인터페이스를 구현한 결과이다.
* `CoroutineContext` 구성요소는 보통 `CoroutineContext.Key`를 자신의 내부에 싱글톤 객체로 구현한다.
* `get()` 함수는 연산자 함수로 선언되어 있어서 대괄호로 대체가 가능하다.
* 구성요소 인스턴스는 모두 key 프로퍼티를 가진다. 이를 사용해 싱글톤 Key에 접근할 수 있다.

#### CoroutineContext 구성요소 제거

* 원본 스펙에서 보여지는 `minusKey()` 함수를 호출하고 구성요소의 키를 넘기면 구성요소를 제거할 수 있다.
```kotlin
fun main() = runBlocking<Unit> {
    // 1. 이름, 디스패처, Job을 결합하여 복합 컨텍스트 생성
    val coroutineName = CoroutineName("MyCoroutine")
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
    val myJob: CompletableJob = Job()
    val coroutineContext: CoroutineContext = coroutineName + dispatcher + myJob
    
    // 2. 💡 minusKey를 사용하여 CoroutineName 요소만 컨텍스트에서 제거
    val deletedCoroutineContext: CoroutineContext = coroutineContext.minusKey(CoroutineName)
    
    // 3. 제거 후 남은 컨텍스트 구성 요소 출력
    println(deletedCoroutineContext)
}
```
* `minusKey()` 함수 사용 시 `minusKey()` 함수를 호출한 `CoroutineContext` 객체를 그대로 유지되고 구성요소가 제거된 새로운 `CoroutineContext` 객체가 반환된다.
