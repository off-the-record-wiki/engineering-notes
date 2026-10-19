# (Effective Kotlin Item 15) Kotlin - 프로퍼티는 동작이 아닌 상태를 나타내야 한다

#### 프로퍼티는 동작이 아닌 상태를 나타내야 한다

* 코틀린 프로퍼티는 자바의 필드와 비슷해보이나 사실은 다른 개념을 나타낸다.
```kotlin
// 코틀린 프로퍼티
var name: String? = null

// 자바 필드
String name = null;
```
* 데이터 저장이라는 동일한 용도로 사용될 수 있긴 하지만 프로퍼티에는 훨씬 더 많은 기능이 있다.

#### 프로퍼티는 항상 사용자 정의 세터와 게터를 가질 수 있다는 점이다.

* 읽기-쓰기가 가능한 `var` 프로퍼티는 게터와 세터를 정의할 수 있다.
* 이런 프로퍼티를 파생 프로퍼티라 하며, 자주 사용된다.
```kotlin
var name: String? = null
    get() = field?.toUpperCase()
    set(value) {
        if (!value.isNullOrBlank()) {
            field = value
        }
    }
```

> 원칙적으로 프로퍼티는 상태를 나타내거나 설정하는 데 사용해야 하며, 다른 로직을 포함하지 않아야 한다.

```kotlin
// ❌ Bad
class UserProfile {
    var name: String = ""
        get() {
            // 나쁜 예: 프로퍼티 조회 시마다 DB 조회 및 복잡한 변환 수행
            return database.fetchName(id) 
        }
        set(value) {
            // 나쁜 예: 설정 시마다 서버로 변경 사항 즉시 전송
            field = value
            api.updateNameOnServer(value) 
        }
}
```
```kotlin
// ⭕ Good
class UserProfile(val id: String) {
    // 1. 프로퍼티는 상태 보관만 수행
    var name: String = ""
        private set // 외부에서 마음대로 수정 못하게 막음

    // 2. 복잡한 로직은 명확한 함수(메서드)로 정의
    fun reloadNameFromDatabase() {
        this.name = database.fetchName(id)
    }

    fun updateName(newName: String) {
        api.updateNameOnServer(newName)
        this.name = newName
    }
}
```

> * 변수(Variable) : 함수 내부에 선언된 경우
> * 프로퍼티(Property) : 클래스 내부에 선언되어 객체 상태를 나타내는 경우

#### 프로퍼티 대신 함수를 사용해야 하는 가장 일반적인 경우를 정리

* 연산 비용이 크거나 복잡도가 O(1)보다 높은 경우

```kotlin
// ❌ Bad
val Tree<Int>.sum: Int
    get() = when (this) {
        is Leaf -> value
        is Node -> left.sum + right.sum
    }
```
```kotlin
// ⭕ Good
fun Tree<Int>.sum(): Int = when (this) {
    is Leaf -> value
    is Node -> left.sum() + right.sum()
}
```

* 연산이 예외를 던지는 경우

```kotlin
// ❌ Bad
val String.parsedJson: JsonObject
    get() = JsonParser.parse(this)
```
```kotlin
// ⭕ Good
fun String.parseJson(): JsonObject = JsonParser.parse(this)
```

* 비즈니스 로직을 포함하는 경우

```kotlin
// ❌ Bad
val User.fullName: String
    get() {
        logger.info("Accessing full name of $id")   // 비즈니스 로직
        return "$firstName $lastName"
    }
```
```kotlin
// ⭕ Good
val User.fullName: String
    get() = "$firstName $lastName"                  // 부수 효과 없는 순수 게산
```

* 호출할 때마다 다른 결과가 나올 수 있는 경우

```kotlin
// ❌ Bad
val now: LocalDateTime
    get() = LocalDateTime.now()
```
```kotlin
// ⭕ Good
fun now(): LocalDateTime = LocalDateTime.now()
```

* `Int.toDouble()`와 같은 변환이 있는 경우

```kotlin
// ❌ Bad
val Int.double: Double
    get() = this.toDouble()
```
```kotlin
// ⭕ Good
fun User.toDto(): UserDto = UserDto(id, name)
```

* 프로퍼티 상태가 변경되는 경우

```kotlin
// ❌ Bad
class Counter {
    private var count = 0
    val next: Int
        get() = ++count        // 접근할 때마다 상태 변경
}
```
```kotlin
// ⭕ Good
class Counter {
    private var count = 0
    fun next(): Int = ++count
}
```
