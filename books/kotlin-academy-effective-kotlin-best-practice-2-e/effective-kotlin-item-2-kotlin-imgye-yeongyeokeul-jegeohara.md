# (Effective Kotlin Item 2) Kotlin - 임계 영역을 제거하라

#### 코틀린/JVM의 동기화

* 동기화는 코틀린/JVM 플랫폼에서 공유 상태를 다루는 가장 중요한 도구이다.
* 이 메커니즘은 한 번에 오직 하나의 쓰레드만 특정 코드 블록을 실행할 수 있도록 보장한다.
* 이는 `synchronized` 키워드를 기반으로 하며, 이 함수에는 잠금 객체와 동기화해야 하는 코드를 담은 람다 표현식이 필요하다.
* 이 메커니즘은 동시에 동일한 잠금 객체를 가진 동기화 블록에 오직 하나의 쓰레드만 진입할 수 있도록 보장한다.
* 쓰레드가 동기화 블록에 도달했지만 다른 쓰레드가 이미 동일한 잠금 객체로 블록을 실행 중인 경우 해당 쓰레드는 다른 쓰레드의 실행이 끝날 때까지 기다린다.

> 코틀린 코루틴을 사용할 때는 `synchronized` 키워드 대신 뮤텍스나 싱글 쓰레드로 제한된 디스패처를 사용한다. 쓰레드 전환은 비용이 발생하며, 일부 클래스에서는 여러 쓰레드에서 실행을 동기화하는 것보다 싱글 쓰레드가 더 효율적이다.

#### 아토믹 객체

* 자바는 주로 사용되는 자바 클래스에 원자적 연산이 포함된 `AtomicInteger`, `AtomicLong`, `AtomicBoolean`, `AtomicReference` 등의 원자성 클래스들을 제공한다.
* 이들은 각각 원자적으로 실행되도록 보장된 메서드를 제공한다. 예를 들어, `AtomicInteger`는 값을 증가시키고 새로운 값을 반환하는 `incrementAndGet()` 메서드를 제공한다.
* 이처럼 아토믹 객체는 빠르며, 상태는 단순한 값 또는 의존성이 없는 값들인 경우에 사용할 수 있다.
* 하지만 다수의 객체를 대상으로 하는 연산들을 동기화하는 데는 아토믹 객체를 사용할 수 없다. 이런 경우에는 동기화 블록을 사용해야 한다.

```kotlin
// ⭕ Good
val num = AtomicInteger(0)

for (i in 1..1000) {
    thread {
        Thread.sleep(10)
        num.incrementAndGet()
    }
}
Thread.sleep(5000)
print(num.get())
```

#### 동시성 컬렉션

* 자바는 동시성을 지원하는 컬렉션도 제공한다.
* 이런 컬렉션 중 가장 중요한 것은 `HashMap`의 쓰레드 안전 버전인 `ConcurrentHashMap`이다.
* 충돌 걱정 없이 `ConcurrentHashMap`의 모든 연산을 안전하게 사용할 수 있다.
* 이 컬렉션을 순회하면 해당 시점의 상태를 스냅샷으로 얻을 수 있으므로 `ConcurrentModificationException` 예외가 발생하지 않는다.

```kotlin
// ⭕ Good
val map = ConcurrentHashMap<Int, String>()
for (i in 1..1000) {
    thread {
        Thread.sleep(1)
        map.put(i, "E$i")
    }
 
    thread {
        Thread.sleep(1)
        print(map.toList())
    }
}
```

* 동시성을 지원하는 `Set`가 필요할 때, 많이 사용되는 선택지는 `ConcurrentHashMap`의 `newKeySet`이 있다.
* `newKeySet`는 `MutableSet` 인터페이스를 구현하므로 일반적인 `Set`처럼 사용이 가능하다.

```kotlin
// ⭕ Good
val set = ConcurrentHashMap.newKeySet<Int>()
for (i in 1.1000) {
    thread {
        Thread.sleep(1)
        set += i
    }
}

Thread.sleep(5000)
println(set.size)
```

* 중복이 허용되는 동시성 컬렉션이 필요한 경우 일반적으로 리스트 대신에 `ConcurrentLinkedQueue`를 사용한다.
* 이는 가변 상태에 대한 문제를 처리하기 위해 JVM에서 사용할 수 있는 필수 도구이다.

#### 변경 가능한 지점을 유출하지 말자.

* 다음과 같이 공개 상태를 나타내는 데 가변 객체를 사용하여 노출하는 것은 특히 위험하다.

```kotlin
// ❌ Bad
data class User(val name: String)

class UserRepository {

    // ❌ Bad(위험)
    private val users: MutableList<User> = mutableListOf()

    fun loadAll(): MutableList<User> = users
}

val userRepository = UserRepository()
val users = userRepository.loadAll()
users.add(User("Kirill")
print(userRepository.loadAll()) // [User(name=Kirill)]
```
* 이런 상황은 수정이 우연히 발생하는 경우라면 특히 위험하다. 먼저 해야 할 일은 가변 객체를 읽기 전용 타입으로 형변환하는 것이다.
* 이 구현만으로 클래스를 안전하게 만들기에는 충분하지 않다.
  * 첫째, 읽기 전용 리스트를 받은 것처럼 보이지만 실제로는 가변 리스트의 참조이므로 값이 변경될 수 있다.
  * 둘째, 하나의 쓰레드가 `loadAll()`을 사용하여 반환받은 리스트를 읽고 동시에 다른 쓰레드가 이를 수정하는 상황이라면 문제가 될 수 있다.
* 이런 문제들을 처리하는 방법으로 두 가지가 있다.
  * 첫째, 실제 참조 대신 객체의 복사본을 반환하는 것으로 이를 방어적 복사라고 부른다. 
    * 복사하는 동안 다른 쓰레드에서 목록에 새로운 요소를 추가하게 되면 충돌이 발생할 수 있다.
    * 따라서 멀티쓰레드 접근을 지원하려면 이 작업을 동기화해야 한다. 컬렉션의 경우 `toList`와 같은 변환 함수를 사용해 복사할 수 있고, 데이터 클래스는 `copy` 메서드를 사용하여 복사할 수 있다.
  * 둘째, 읽기 전용 리스트를 사용한다.
    * 읽기 전용 리스트는 안전할 뿐만 아니라 객체의 변경 사항을 추적하기에 용이하다.
    * 읽기 전용 리스트에 멀티쓰레드 접근을 허용하려면 리스트 수정 연산만 동기화하면 된다.
    * 이렇게 하면 요소 추가는 느려지지만 리스트에 대한 접근은 빨라진다. 이는 쓰기보다 읽기가 많을 때, 좋은 절충안이 된다.
```kotlin
// ⭕ Good
class UserRepository {
    private val lock = Any()
    private val users: MutableList<User> = mutableListOf()

    fun loadAll(): List<User> = synchronized(lock) {
        // ⭕ Good
        users.toList()   // 복사본 반환 — 복사 자체도 동기화로 충돌 방지
    }

    fun add(user: User) = synchronized(lock) {
        users.add(user)
    }
}
```
```kotlin
// ⭕ Good
class UserRepository {
    private val lock = Any()

    var users: List<User> = listOf()    // 읽기 전용 컬렉션
        private set                     // 외부 변경 차단

    fun loadAll(): List<User> = users   // 불변이라 그대로 반환해도 안전

    fun add(user: User) = synchronized(lock) {
        users = users + user            // 새 리스트로 교체 (쓰기만 동기화)
    }
}
```
