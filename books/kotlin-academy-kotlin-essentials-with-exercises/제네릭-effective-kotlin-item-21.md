# (Effective Kotlin Item 21) Kotlin - Generic

#### 제네릭 함수

* 매개변수에 인수를 넣는 것처럼 타입을 타입 인수로 넣을 수 있다.
* 함수에서 타입 인수를 받으려면 fun 키워드 다음에 `<>`를 쓰고 그 안에 필요한 수 만큼의 타입 매개변수를 정의해야 한다.
* 표기법상 타입 매개변수의 이름은 대문자여야 한다.
* 함수가 타입 매개변수를 정의했다면 함수를 호출할 때, 타입 인수를 지정해야 한다.
* 타입 매개변수는 구체적인 타입이 들어갈 장소이며, 타입 인수는 함수를 호출할 때 사용할 실제 타입이다.
```kotlin
fun <T> a() {} // T는 타입 매개변수
a<Int>()       // Int를 타입 인수로 사용
a<String>()    // String을 타입 인수로 사용
```
* 타입 인수가 하나뿐이라면 주로 T를 쓰며, 인수가 여러 개라면 T에 일련번호를 붙이는 방식이 자주 사용된다.
* 하지만 정해진 규칙은 아니며 타입 매개변수의 이름을 정하는 표기법은 다양하다.
* 제네릭 함수를 호출하는 시점에 코틀린 컴파일러는 모든 타입 인수를 명확하게 알아야 한다.
```kotlin
fun <T> a() {}
fun <T1, T2> b() {}
```
```kotlin
import kotlin.random.Random

fun <T> id(value: T): T = value    // 결과 타입이 인수 타입과 같다.

fun <T> randomOf(a: T, b: T): T = 
if (Random.nextBoolean()) a else b // 결과 타입이 인수 타입들의 가장 가까운 슈퍼 타입이다.

fun main() {
    val a = id(10)
    val b = id("AAA")
    val c = randomOf("A", "B")
    val d = randomOf(1, 1.5)
}
```

#### 제네릭 클래스

* 클래스 이름 뒤에 타입 매개변수를 추가하면 클래스를 제네릭으로 만들 수 있다.
* 타입 매개변수는 특히 프로퍼티 타입, 매개변수 타입, 결과 타입을 지정할 때와 같이 클래스 내부 어디서든 사용 가능하다.
* 타입 매개변수는 인스턴스를 정의할 때 지정하며, 이후로는 바뀌지 않는다.
```kotlin
class ValueWithHistory<T>(
    private var value: T
) {
    private var history: List<T> = listOf(value)

    fun setValue(value: T) {
        this.value = value
        this.history += value
    }

    fun currentValue(): T = value
    fun history(): List<T> = history
}
```

#### 제네릭 클래스와 널 가능성

* 타입 인수는 널 가능하므로 `ValueWithHistory<String?>`과 같이 만들 수도 있다.
* 이렇게 하면 `null`값도 완벽하게 유효한 원소가 된다.
```kotlin
fun main() {
    val letter = ValueWithHistory<String?>(null)
    letter.setValue("A")
    letter.setValue(null)

    val l = letter.currentValue()
    println(l)

    val h = letter.history()
    println(h)
}
```
* 클래스나 함수 내부에서 제네릭 매개변수를 사용할 때, 물음표를 붙여 널 가능하게 만들 수도 있다.
* `T` 타입은 타입 인수가 무엇이냐에 따라 널 가능하거나 가능하지 않을 수 있지만, `T?` 타입은 항상 널 가능하다.
* 널 가능한 제네릭 타입 매개변수 `T?`를 `T`로 사용하려면 사용 전에 반드시 널 가능성을 벗겨내야만 한다.

```kotlin
class Box<T> {
    var value: T? = null
    fun getOrThrow(): T = value!!
}
```
```kotlin
fun <T> T.orThrow(): T & Any = this ?: throw Error()

fun main() {
    val a: Int? = if (Random.nextBoolean()) 42 else null
    val b: Int = a.orThrow()
    val c: Int = b.orThrow()
}
```

#### 제네릭 인터페이스

* 인터페이스도 제네릭이 될 수 있다.
* 지정한 타입 매개변수를 인터페이스 내부에서 프로퍼티 타입, 매개변수 타입, 결과 타입 등으로 사용할 수 있다.
```kotlin
interface List<out E> : Collection<E> {
    // ...
}
```

#### 타입 매개변수와 상속

* 클래스는 열린 제네릭 클래스를 상속하거나 제네릭 인터페이스를 구현할 수 있다. 하지만 두 경우 모두 타입 인수를 명시해야 한다.
```kotlin
open class C<T>
interface I<T>
class A : C<Int>(), I<String> // 타입 인수 명시
```

#### 타입 소거

* 코틀린을 JVM 바이트코드로 컴파일하면 모든 타입 인수가 사라진다.
* `List<String>`은 JVM 바이트코드에서 List가 되고 `emptyList<Double>`은 emptyList가 된다.
* 이렇게 타입 인수를 삭제하는 과정을 타입 소거(type erasure)라고 한다.
* 타입 소거 때문에 타입 매개변수는 일반 타입에는 없는 제한이 있다.
* 타입 매개변수를 `is` 확인에 쓸 수 없고 참조할 수도 없으며 구체화된 타입 인수로 사용할 수 없다.

```kotlin
import kotlin.reflect.typeOf

fun <T> example(a: Any) {
    val check = a is T        // 에러
    val ref = T::class        // 에러
    val type = typeOf<T>()    // 에러
}
```

#### 제네릭 제약 조건

* 타입 매개변수의 중요한 특징은 허용 범위를 구체적인 타입의 서브타입으로 제한할 수 있다는 점이다.
* 콜론 다음에 슈퍼타입을 명시해 제약 조건을 설정하는 식이다.
```kotlin
class ListAdapter<T : ItemAdapter>() {}
```
* 위의 코드에서 ListAdapter를 보면 타입 인수가 ItemAdapter의 서브 타입이다.
* 제약 조건이 선사하는 중요한 특징은 인수로 받은 인스턴스들에서 슈퍼타입이 제공하는 모든 메서드를 사용할 수 있다는 점이다.
* 드물지만 상한선 경계를 두 개 이상 설정해야 하는 경우에는 where를 사용해 더 많은 제약 조건을 걸 수도 있다.

```kotlin
interface Animal {
    fun feed()
}

interface GoodTempered {
    fun pet()
}

fun <T> pet(animal: T) where T : Animal, T : GoodTempered {       // 상한선 경계를 2개 이상 설정한다.
    animal.pet()
    animal.feed()
}
```

#### 스타 프로젝션

* 타입에 구체적인 타입 인수를 지정하고 싶지 않은 경우도 있는데 이럴 때 어떤 타입이든지 허용하는 스타 프로젝션(*)을 사용한다.
* 스타 프로젝션이 유용한 상황은 다음과 같이 2가지가 있다.
  * 변수가 리스트인지 확인하는 경우 - `is List<*>`로 확인
  * 프로퍼티나 매개변수

> **스타 프로젝션의 동작 원리**
>
> * `List<*>`는 `List<Any?>`와 다르다.
> * `List<*>`는 리스트에 어떤 타입이 들어있는지 모르지만 적어도 리스트라는 사실을 알 수 있다.
> * 읽기 : `get()`을 호출하면 무조건 `Any?` 타입이 반환된다.
> * 쓰기 : `add()`를 호출할 수 없다. 컴파일러는 `*`가 어떤 타입인지 모르므로 데이터 오염을 막기 위해 쓰기 동작을 컴파일 단계에서 차단한다.

#### 타입 인수에서 사용하는 언더스코어 연산자

* 타입 인수는 명시적으로 지정할 수도 있고 컨텍스트에서 유추할 수도 있다.
* 여러 개의 타입 인수 중 하나만 지정하고 나머지는 컴파일러가 유추하게 만들고 싶을 때도 있는데 이 때, 언더스코어 연산자를 타입 인수로 사용한다.
* 언더스코어 연산자는 컴파일러가 유추해야 하는 타입 인수를 지정한다.
```kotlin
inline fun<K, reified V> Map<K, *>
    .filterValueIsInstance(): Map<K, V> = 
        filter { it.value is V } as Map<K, V>

fun main() {
    val props = mapOf()
    val strProps = props.filterValueIsInstance<_, String>()
}
```

> **reified**
>
> * `reified`는 인라인 함수에서 타입 인수를 구체화시켜 런타임에도 그 타입을 직접 참조하거나 is 체크를 할 수 있게 한다.
```kotlin
// reified를 사용한 타입 체크
inline fun <reified T> Any.isInstance(): Boolean = this is T

fun main() {
    println("Hello".isInstance<String>()) // true
    println("Hello".isInstance<Int>())    // false
}
```
