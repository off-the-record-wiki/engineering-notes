# (Effective Kotlin Item 4) Kotlin - 변수의 스코프를 최소화하라

#### 변수의 스코프를 최소화하라

* 프로퍼티 대신에 지역 변수를 사용한다.
* 가능한 한 가장 좁은 스코프 안에서 변수를 사용한다.

#### 변수의 스코프를 좁히면 프로그램을 추적하고 관리하기 쉽게 유지할 수 있다.

```kotlin
// ❌ Bad
var user: User
for (i in users.indices) {
    user = users[i]
    print("User at $i is $user")
}
```

```kotlin
// ⭕ Good
for (i in users.indices) {
    val user = users[i]
    print("User at $i is $user")
}

```kotlin
// ⭕ Good
for ((i , user) in users.withIndex()) {
    print("User at $i is $user")
}
```

#### 또 다른 문제는 넓은 스코프의 변수를 다른 개발자가 남용할 가능성이 있다는 것이다.

* 예를 들어, 반복문이 끝난 이후에도 마지막 요소가 해당 변수에 남아있을 것으로 생각할 수 있다.
* 그렇게 되면 다른 개발자가 해당 값이 어떤 의미를 가지고 있는가를 이해해야 하기 때문에 스코프 전체를 파악해야 하므로 굉장히 힘들 것이다.

#### 변수가 읽기 전용이든 아니면 상관없이 항상 변수를 선언할 때, 변수를 초기화하는 것이 좋다.

```kotlin
// ❌ Bad
val user: User
if (hasValue) {
    user = getValue()
} else {
    user = User()
}
```

```kotlin
// ⭕ Good
val user: User = if (hasValue) {
    getValue()
} else {
    User()
}
```
