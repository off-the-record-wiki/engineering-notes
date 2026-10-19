# (Effective Kotlin Item 36) Kotlin - 상속보다 합성을 선호하라

#### 상속보다 합성을 선호하라

* 상속은 강력한 기능이지만 `is a`의 관계로 객체의 계층 구조를 만들기 때문에 관계가 명확하지 않으면 위험할 수 있다.
* 간단한 코드 추출 또는 재사용이 목적이라면 상속 대신에 더 가벼운 클래스 합성을 선호한다.

#### 간단한 동작 재사용

```kotlin
// ❌ Bad - 공통 로직을 부모 클래스에 두고 상속받게 함
abstract class LoaderWithProgress {

    fun load() {
        // 로딩 전후 공통 처리
        println("로딩 시작")
        innerLoad()
        println("로딩 완료")
    }

    abstract fun innerLoad()
}

class ProfileLoader : LoaderWithProgress() {
    override fun innerLoad() {
        println("프로필 불러오는 중")
    }
}

class ImageLoader : LoaderWithProgress() {
    override fun innerLoad() {
        println("이미지 불러오는 중")
    }
}
```
* 이와 같은 슈퍼 클래스는 매우 간단한 경우라면 효과적이지만 중요한 단점이 있다.
  * **하나의 클래스만 상속할 수 있다. 상속을 사용하여 기능을 추출하면 타입의 계층구조가 지나치게 복잡해지며, 수많은 기능을 가지고 있는 클래스가 탄생하는 경우가 많다.**
  * **상속할 때 클래스에서 모든 것을 가져온다. 이로 인해 필요하지 않은 기능과 메서드가 있는 클래스가 생성되면서 ISP를 위반할 여지가 있다.**
  * **슈퍼 클래스를 사용하면 코드를 이해하기 힘들어진다. 일반적으로 개발자가 메서드를 읽고 이 메서드의 작동 방식을 이해하기 위해 슈퍼 클래스로 이동해야 한다면 좋지 못한 설계가 될 수 있다.**
* 이를 대체할 좋은 방법으로 합성(Composition)이 제시되었다. 합성이란, 각각의 기능을 가진 객체들을 프로퍼티로 구성하여 사용하는 것을 뜻한다.

```kotlin
// ⭕ Good - 공통 기능을 독립된 클래스로 분리
class ProgressReporter {
    fun reportStart() = println("로딩 시작")
    fun reportEnd() = println("로딩 완료")
}

class ProfileLoader {
    private val progress = ProgressReporter()   // 합성

    fun load() {
        progress.reportStart()
        println("프로필 불러오는 중")
        progress.reportEnd()
    }
}

class ImageLoader {
    private val progress = ProgressReporter()   // 같은 기능을 품어서 재사용

    fun load() {
        progress.reportStart()
        println("이미지 불러오는 중")
        progress.reportEnd()
    }
}
```

#### 상속은 클래스의 모든 것을 가져온다

* 상속을 하게 되면 슈퍼 클래스에서 메서드, 기대(규약), 동작과 같은 모든 것을 가져온다.
* 따라서 상속은 객체의 계층구조를 표현하기에는 좋지만 공통 부분을 재사용하려는 경우에는 그리 좋지 않다.
* 공통 부분을 추출할 때는 필요한 기능을 선택할 수 있으므로 합성이 더 좋다.

```kotlin
// ❌ Bad - 추상 클래스(상속이므로 하나만 가능, 구현/상태가 딸려옴)
abstract class Loader {
    protected val cache = mutableMapOf<String, Any>()  // 상태까지 물려받음
    abstract fun load()
    fun clearCache() { cache.clear() }  // 원치 않아도 노출됨
}

class ProfileLoader : Loader() {  // 다른 걸 또 상속할 수 없음
    override fun load() { }
}
```
```kotlin
// ⭕ Good - 인터페이스(계약만 정의, 여러 개 동시 구현 가능)
interface Loader {
    fun load()
}
interface Cacheable {
    fun clearCache()
}

// 둘 다 구현 가능 — 추상 클래스로는 불가능했던 조합
class ProfileLoader : Loader, Cacheable {
    override fun load() { }
    override fun clearCache() { }
}
```
* 합성을 사용하면 규칙을 위반하지 않고 제약도 크지 않다.
* 합성을 사용할 때는 재사용할 항목을 선택한다.
* 타입 계층을 표현할 때도 상속 대신 인터페이스가 더 나은 방법이다.
* 인터페이스를 사용하는 편이 안전하며, 여러 개의 인터페이스를 동시에 구현할 수도 있기 때문이다.

#### 상속은 캡슐화를 깨뜨린다

* 클래스를 상속하게 되면 외부에서 사용하는 법뿐 아니라 내부 구현 방식도 함께 고려해야 하기에 캡슐화가 깨진다.

```kotlin
// ⭕ Good - MutableSet을 상속하지 않고, 내부 set에 위임(by)
class CounterSet<T>(
    private val innerSet: MutableSet<T> = mutableSetOf()
) : MutableSet<T> by innerSet {   // Set의 모든 메서드를 innerSet에 자동 위임

    var elementsAdded: Int = 0
        private set

    override fun add(element: T): Boolean {
        elementsAdded++
        return innerSet.add(element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        elementsAdded += elements.size
        return innerSet.addAll(elements)
    }
}
```

#### 오버라이딩 제한

* 상속 용도로 설계되지 않은 클래스가 상속되는 상황을 막고 싶다면 클래스에 접근 제어자를 추가하지 않으면 된다.
* 하지만 상황을 허용해야 한다면 모든 메서드가 기본적으로 상속이 불가능하므로 클래스 메서드에 open 제어자를 추가하면 된다.

```kotlin
class Animal {
    fun sound() = "..."   // 기본 final, 오버라이딩 불가
}
```
```kotlin
open class Animal {
    open fun sound() = "..."   // open이라 오버라이딩 가능
}

class Dog : Animal() {
    override fun sound() = "멍멍"
}
```
