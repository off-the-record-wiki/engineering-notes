# (Effective Kotlin Item 51) Kotlin - 함수형 타입 매개변수를 갖는 함수에 inline 한정자를 사용하라

#### 함수형 타입 매개변수를 갖는 함수에 inline 한정자를 사용하라

* 코틀린 표준 라이브러리의 거의 모든 고차 함수들은 `inline` 한정자를 사용한다.

```kotlin
public inline fun <T, R> T.let(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(this)
}

public inline fun repeat(times: Int, action: (Int) -> Unit) {
    contract { callsInPlace(action) }
    for (index in 0 until times) action(index)
}

public inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e; throw e
    } finally {
        closeFinally(exception)
    }
}
```
* `inline` 한정자를 사용하면 컴파일할 때 함수 호출부가 함수 본문으로 대체된다.
* `repeat`를 호출할 때 입력한 인수 또한 함수 본문으로 대체된다.
* 결국 `inline` 한정자가 붙은 함수는 일반적으로 실행되는 과정과 완전히 다르다고 할 수 있다. 일반 함수를 실행할 때는 함수의 본문으로 점프하고 본문의 모든 명령문을 실행한 다음, 다시 함수가 호출된 곳으로 점프한다.
* 함수 호출부를 함수의 본문으로 대체하는 것과 다른 동작이다.
* 정리하면 `inline` 한정자를 사용하면 다음과 같은 장점들을 얻을 수 있다.
  * 타입 인수가 구체화될 수 있다.
  * 함수형 매개변수가 있는 함수는 인라인화되었을 때 더 빠르다.
  * 비지역 반환(non-local return)이 허용된다.
* 하지만 `inline` 한정자 또한 비용이 발생한다.

#### 타입 인수가 구체화될 수 있다

* 자바의 예전 버전에서는 제네릭이라는 개념이 없었다.
* 제네릭은 2004년 자바 J2SE 5.0 버전에 추가되었지만 JVM 바이트 코드에는 아직까지도 반영되지 않았다.
* 따라서 컴파일하면 제네릭 타입이 사라지게 된다.
* 그러나 함수를 인라인으로 만들면 이런 문제가 발생하지 않고 런타임에도 T가 무슨 타입인지 알 수 있다.

```kotlin
inline fun <reified T> Any.isType(): Boolean = this is T

val ok = value.isType<String>()
```
* `reified`는 유용한 한정자이다. 예를 들어, 표준 라이브러리의 `filterIsInstance`에서 특정 타입의 요소를 필터링하는 데 사용된다.
* 다양한 라이브러리와 유틸 함수에서도 `reified` 한정자를 사용한다.

#### 함수형 매개변수가 있는 함수는 인라인화되었을 때 더 빠르다

* 모든 함수는 인라인화되었을 때 더 빠르다. 실행 시에 점프하거나 백스택을 추적할 필요가 없기 때문이다.
* 따라서 표준 라이브러리는 성능을 위해 규모가 작은 함수들을 인라인화했다.

```kotlin
inline fun print(message: Any?) {
    System.out.print(message)
}
```
* 하지만 함수형 매개변수가 없다면 성능 차이는 거의 나지 않는다.
* 함수형 매개변수가 없는 함수를 인라인화하면 IDE는 다음과 같은 경고를 볼 수 있다.

```text
warning: expected performance impact from inlining is insignificant. Inlining works best for functions with parameters of functional types
```
* 함수형 매개변수를 가진 함수가 인라인화되었을 때 왜 특별히 빠른가를 이해하려면 함수를 객체로 다룰 때 어떤 문제가 발생하는지 알아야 한다.
* 함수 리터럴로 정의된 함수 객체는 반드시 어딘가에 저장되어야 한다.
* 코틀린/JVM에서는 익명 JVM 클래스나 일반 클래스를 사용해 객체를 생성해야 한다.
* 따라서 다음과 같이 람다 표현식은 클래스로 컴파일이 된다.

```kotlin
// 코틀린
val lambda: () -> Unit = {
    // code
}

// JVM에서 컴파일될 때의 코드
Function0<Unit> lambda = new Function0<Unit>() {
    public Unit invoke() {
        // code
    }
};
```

* 코틀린 컴파일러는 함수를 인터페이스에 맞춰 생성한다.
* 함수를 정의한 인터페이스는 컴파일 중에 생성되기 때문에 명시적으로 사용할 수 없지만 함수 타입을 사용하는 건 가능하다.
* 함수 타입이 단지 인터페이스일뿐이라는 사실을 다양한 방식으로 활용할 수 있다.
* 결국 함수가 객체가 되면 실행될 때마다 부하가 누적되어 성능이 크게 저하되어 버린다. 
* 특히 함수형 타입 매개변수가 있는 유틸리티 함수는 인라인으로 만드는 것이 좋다.

#### 비지역 반환이 허용된다

* 람다 안의 `return`은 바깥 함수를 끝낸다.

```kotlin
fun findAdmin(users: List<User>): User? {
    users.forEach {
        if (it.isAdmin) return it   // 람다가 아니라 findAdmin이 끝남
    }
    return null
}
```
* 람다 코드가 함수 안에 복사되니까 결국 for문 안의 return과 똑같은 상황이 된다.
* 특별한 장치가 아니라 그냥 평범한 반환이다.

```kotlin
return         // 바깥 함수 끝
return@forEach // 람다만 끝 (continue처럼)
```

#### inline 한정자의 비용

* `inline`은 유용하지만, 비용도 들고 한계점이 있어 아무때나 사용할 수 없다.
* `inline` 함수는 제한된 가시성을 가진 요소를 사용할 수 없다. `private` 또는 `internal` 제어자가 있는 함수나 프로퍼티를 `public inline` 함수에서 사용할 수 없다. `public` 또는 `internal`인 `inline` 함수에서 `private` 프로퍼티를 사용할 수도 없다.

```kotlin
internal inline fun read() {
    val reader = Reader() // 에러 발생!
    // ...
}

private class Reader {

}
```

* 인라인 함수로 세부 사항을 숨길 수 없으므로, 클래스 내부에서는 인라인 함수를 거의 사용하지 않는다.
* 인라인 함수가 대부분 유틸리티 함수인 이유도 바로 이 때문이다.
* 인라인 함수를 재귀적으로 사용할 수 없다. 재귀적으로 사용할 경우 호출부가 무한히 대체될 수 있기 때문이다. 재귀적으로 순환 호출하게 되는 경우가 특히나 위험한데, IDE에서는 에러 처리로 하지 않기 때문이다.
* 인라인 함수는 코드의 규모를 증가시킨다. 인라인 함수가 인라인 함수를 호출하게 되면 코드 규모가 기하급수적으로 증가할 위험성이 높다.

#### crossinline과 noinline

* 함수를 인라인화하고 싶지만, 인수로 들어온 일부 함수를 인라인화할 수 없는 경우가 있다.
* 이런 경우 다음과 같은 한정자를 사용할 수 있다.
  * `crossinline` : 인수로 들어온 함수가 인라인이어야 하지만, 비지역 반환은 허용하지 않습니다. 비지역 반환이 허용되지 않는 스코프에서 `crossinline` 제어자가 있는 함수를 사용하기 위함이다. 인라인이 아닌 람다에서 `crossline` 제어자가 있는 함수를 사용하는 경우를 예로 들 수 있다.
  * `noinline` : 인수로 들어온 함수가 인라인 함수가 되면 안 된다. 주로 인라인이 아닌 함수의 인수로 `noline` 제어자가 있는 함수를 넣기 위한 목적으로 사용한다.

```kotlin
inline fun requestNewToken(
    hasToken: Boolean,
    crossinline onRefresh:() -> Unit,
    noinline onGenerate:() -> Unit
) {
    if (hasToken) {
        httpCall("get-token", onGenerate)
        // 인라인화되지 않는 함수의 인수로 함수를 전달하려면 noinline을 사용해야 한다.
    } else {
        httpCall("refresh-token") {
            onRefresh()
            // 비지역 반환이 허용되지 않는 컨텍스트에서 인라인 함수를 사용하려면 crossinline을 사용해야 한다.
            onGenerate()
        }
    }
}

fun httpCall(url: String, callback: () -> Unit) {
    // ...
}
```
