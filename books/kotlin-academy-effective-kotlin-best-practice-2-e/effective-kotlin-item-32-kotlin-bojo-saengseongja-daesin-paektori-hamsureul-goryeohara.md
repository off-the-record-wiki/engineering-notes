# (Effective Kotlin Item 32) Kotlin - 보조 생성자 대신 팩토리 함수를 고려하라

#### 보조 생성자 대신 팩토리 함수를 고려하라

* 클래스에서 객체를 생성하려면 생성자를 사용해야 한다. 코틀린에서는 일반적으로 기본 생성자가 이런 역할을 한다.
```kotlin
// sealed class로 표현한 단방향 연결 리스트
sealed class LinkedList<out T>

// 노드: 머리(head) 하나 + 나머지 리스트(tail)
class Node<out T>(
    val head: T,
    val tail: LinkedList<T>
) : LinkedList<T>()

// 빈 리스트를 나타내는 객체
object Empty : LinkedList<Nothing>()
```
* 코틀린 클래스의 기본 생성자는 주로 객체 상태를 나타내는 데 필요한 프로퍼티를 정의한다.
* 따라서 기본 생성자의 매개변수는 객체 구조와 강결합되어있다.
* 위와 같이 객체 생성 방법을 생성자로 정의하는 것은 나쁜 습관으로 권장하지 않는다. 함수로 정의하는 것이 좋으며 그 이유는 다음과 같다.
  * **생성자와 달리 함수에는 이름이 있다** - 이름은 객체가 생성되는 방법과 인수가 무엇인지 설명한다. 이름을 통해 인수의 의미 또는 객체 생성 방식을 알 수 있다. 동일한 매개변수 타입으로 객체 생성을 정의하는 경우에도 함수 이름으로 객체 생성 방식을 구분할 수 있다.
  * **생성자와 달리 함수는 반환 타입의 모든 하위타입 객체를 반환할 수 있다** - 함수의 반환 타입을 인터페이스로 설정해 인터페이스만 노출하고 실제 객체는 숨길 수 있다. 인터페이스를 이용하여 실제 타입을 숨기는 방법은 코틀린이 제공하는 최적화 기능 중 하나이며 제약없이 세부사항을 구현할 수 있다.
  * **생성자와 달리 함수는 호출될 때마다 새 객체를 생성할 필요가 없다** - 함수를 사용하면 캐싱처럼 객체 생성을 최적화할 수도 있고 객체를 재사용할 수 있다. 또한 객체를 생성할 수 없다면 `null`을 반환하는 정적 팩토리 함수를 정의할 수도 있다. `Connection`을 생성할 수 없는 경우 `null`을 반환하는 `Connections.createOrNull()`과 같은 함수를 예시로 들 수 있다.
  * **팩토리 함수는 아직 존재하지 않을 수도 있는 객체를 제공할 수도 있다.** - 라이브러리 제작자들이 어노테이션 프로세싱에 기반하여 사용하는 방법이다.
  * **객체 외부에서 팩토리 함수를 정의하면 객체 가시성을 제어할 수도 있다.** - 동일한 파일 또는 동일한 모듈에서만 접근할 수 있는 최상위 팩토리 함수를 만들 수 있다.
  * **팩토리 함수는 인라인될 수 있으므로 타입 매개변수를 구체화할 수 있다.** 
  * **생성자는 슈퍼클래스의 생성자나 기본 생성자를 즉시 호출해야 한다. 팩토리 함수를 사용하면 생성자 호출을 뒤로 미룰 수 있다.**
* 객체를 생성하는 데 사용되는 함수를 팩토리 함수라고 한다.

#### 동반 객체 팩토리 함수(companion object factory function)

* 팩토리 함수를 생성자와 비슷하지만 이름이 있어 이름 있는 생성자라고 부른다.
* 함수 앞에 `@JvmStatic` 어노테이션만 사용하면 되며, 코틀린과 마찬가지로 그루비나 자바에서도 팩토리 함수를 쉽게 사용할 수 있다.
* 단점은 복잡하다는 것이다.
* 동반 객체 팩토리 함수에는 몇 가지 명명 규칙이 있다. 이 규칙들은 자바에서도 오랫동안 사용되던 방식이지만 코틀린에서도 널리 사용되고 있다.

```kotlin
class User private constructor(
    val id: Long,
    val name: String
) {
    companion object {
        // 팩토리 함수 — 생성자 대신 이걸로 만든다
        fun create(name: String): User {
            val id = generateId()
            return User(id, name)
        }
    }
}
```

* 정적 요소를 대신해 동반 객체를 사용하는 경우가 많지만 동반 객체에는 더 많은 기능이 있다.
* 동반 객체는 인터페이스를 구현할 수도 있고 클래스를 확장할 수도 있다. '정적' 요소를 상속할 수 없어 불편하다는 의견이 많아 동반 객체로 만든 것이다.

```kotlin
// 추상 빌더: 모든 빌더가 공유하는 공통 단계를 정의
// SELF는 '자기 자신의 구체 타입'을 가리키는 재귀적 타입 파라미터
abstract class AbstractRequestBuilder<SELF : AbstractRequestBuilder<SELF>> {
    protected var timeout: Int = 30_000
    protected val headers = mutableMapOf<String, String>()

    // 공통 메서드 — SELF를 반환해 하위 빌더에서도 체이닝이 이어진다
    fun timeout(ms: Int): SELF {
        this.timeout = ms
        return self()
    }

    fun header(key: String, value: String): SELF {
        headers[key] = value
        return self()
    }

    // 하위 클래스가 자기 자신을 SELF 타입으로 돌려주도록 강제
    protected abstract fun self(): SELF
}
```

* 이러한 추상 동반 객체 팩토리는 값을 가질 수 있으므로 캐싱을 구현할 수도 있고 테스트 용도로 가짜 객체를 만들 수도 있다.

> 동반 객체 팩토리 함수 네이밍 규칙

| 접두/형태 | 의미 | 예시 |
|---|---|---|
| `of` | 주어진 인자(들)로부터 인스턴스 생성 | `IntList.of(1, 2, 3)` |
| `from` | 다른 타입의 단일 값을 변환해 생성 | `Date.from(instant)` |
| `valueOf` | 값에 대응하는 인스턴스 반환 | `Color.valueOf("#FFF")` |
| `create` / `newInstance` | 새 인스턴스를 생성(매번 새 객체 보장) | `User.create(name)` |
| `getInstance` | 인스턴스 반환(캐시/싱글톤일 수 있음) | `Settings.getInstance()` |
| `createOrNull` | 생성하되 실패 시 null 반환 | `Email.createOrNull(str)` |
| `xxxOf` (최상위) | 컬렉션/값 생성용 관용 표현 | `listOf()`, `setOf()` |

> 보충 규칙
* **`create` vs `getInstance`**: `create`는 호출 시 항상 새 객체, `getInstance`는 재사용 가능성 있음(캐싱·싱글톤)을 암시한다.
* **`of` vs `from`**: `of`는 여러 인자를 모아 생성(`of(a, b, c)`), `from`은 하나의 다른 타입을 변환해 생성(`from(other)`)할 때 쓴다.
* **변환 의미를 이름에 담기**: `fromCartesian`, `fromPolar`처럼 같은 시그니처라도 의도를 이름으로 구분한다.
* **부정적 결과 표현**: 실패 가능성은 `xxxOrNull`(null 반환) 또는 `tryXxx`(Result 반환) 접미사로 드러낸다.
* **최상위 팩토리**: 클래스에 강하게 묶이지 않으면 `userOf()`, `listOf()`처럼 `xxxOf` 형태의 최상위 함수도 관례다.

#### 최상위 수준 팩토리 함수

* 객체를 생성하는 가장 일반적인 방법은 최상위 수준에서 정의한 팩토리 함수를 사용하는 것이다.

```kotlin
class User(val name: String, val age: Int)

// 최상위 팩토리 함수 — 클래스에 묶이지 않고 전역에서 호출
fun userOf(name: String, age: Int): User {
    return User(name, age)
}
```
* 최상위 수준 함수를 사용하여 객체를 생성하는 방식은 크기가 작고 생성이 빈번한 객체를 만들 때 가장 좋은 방법이다.
* 그러나 어디에서나 사용할 수 있으므로 곳곳에서 최상위 함수를 제안해 복잡해진다.
* 최상위 수준 함수가 클래스 메서드와 이름이 동일하여 혼동될 때 더욱 심각해진다. 따라서 최상위 수준 함수의 이름은 신중하게 정해야 한다.

#### 빌더

* 빌더는 최상위 수준 팩토리 함수로 아주 중요한 역할을 한다.

```kotlin
// 표준 라이브러리의 빌더 함수들도 같은 원리
val list = buildList {
    add(1)
    add(2)
    addAll(listOf(3, 4))
}

val text = buildString {
    append("Hello")
    append(", ")
    append("World")
}
```
* 코틀린에서 빌더를 구현하는 일반적인 방법은 최상위 수준 함수와 DSL 패턴을 사용하는 것이다.

#### 변환 메서드

* 변환 메서드는 기존 타입을 다른 타입으로 변환하는 데 사용되는 메서드이다.
* 일반적으로 `to{Type}`, `as{Type}`이라는 이름이 명명된다.
* `to` 접두사는 다른 타입을 가진 새 객체를 생성한다는 의미이다. `as` 접두사는 새로 생성된 객체가 래퍼이거나 원본 객체의 추출된 부분임을 의미한다.

```kotlin
val array = arrayOf(1, 2, 3)

// asList: 배열을 List '뷰'로 비춤 — 복사 안 함, 원본과 연결됨
val listView = array.asList()
array[0] = 99
println(listView[0])   // 99 — 원본 변경이 뷰에 반영된다!

// toList: 새 List로 복사 — 원본과 독립
val listCopy = array.toList()
array[1] = 88
println(listCopy[1])   // 2 — 복사본이라 영향 없음
```

#### 복사 메서드

* 객체 복사본을 만들어야 하는 경우 복사 생성자를 정의하는 대신 복사 메서드를 정의한다.
* 그대로 복사하고 싶다면 메서드 이름을 `copy`로 정의하는 게 좋다.
* 객체에 약간의 변경 사항을 추가하고 싶다면 `with`로 시작하여 변경하는 프로퍼티를 명시하는 것이 좋다.

```kotlin
val older = user.copy(age = 31)
```
```kotlin
data class Point(val x: Int, val y: Int) {
    // "x만 바꾼 새 Point"를 명시적으로 드러내는 복사 메서드
    fun withX(newX: Int): Point = copy(x = newX)
    fun withY(newY: Int): Point = copy(y = newY)
}
```

#### 가짜 생성자

* 생성자처럼 보이지만 실제로는 함수. 즉, 대문자로 시작하는 함수 이름을 지어서 호출하는 쪽에서 마치 생성자를 부르는 것처럼 보이는 기법이다.
```kotlin
// 이게 생성자처럼 보이지만 실제로는 '함수'다
val list = List(3) { it * 2 }   // [0, 2, 4]
val mutable = MutableList(3) { 0 }  // [0, 0, 0]
```
