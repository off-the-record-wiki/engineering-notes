# (Effective Kotlin Item 11) Kotlin - 연산자의 의미는 함수의 이름과 일치해야 한다

#### 연산자의 의미는 함수의 이름과 일치해야 한다

* 연산자 오버로딩은 강력한 기능이지만 다른 강력한 기능들과 마찬가지로 위험하기도 하다.

#### 분명하지 않은 경우

* 예를 들어, '함수를 3배로 만든다'는 표현은 무슨 뜻일까?
* 어떤 이들에게 이 표현은 '해당 함수를 3번 반복하는 또 다른 함수를 만드는 것'이라는 의미로 받아들인다.
```kotlin
operator fun Int.times(operation: () -> Unit): () -> Unit = 
    { repeat(this) { operation() }

val tripledHello = 3 * { print("Hello") }
tripledHello()
```
* 다른 이들은 해당 함수는 3번 호출하는 것이라고 생각할 수 있다.
```kotlin
operation fun Int.times(operation: () -> Unit) {
    repeat(this) { operation() }
3 * { print("Hello")}
```
* 위와 같이 의미가 불분명할 때는 의미를 드러낼 수 있는 확장 함수를 사용하는 것이 좋다.
* 연산자처럼 사용하고 싶다면 `infix` 함수로 만들면 된다.
```kotlin
infix fun Int.timesRepeated(operation:() -> Unit) = {
    repeat(this) { operation() }
```

#### 규칙을 어겨도 되는 경우

* 연산자 오버로딩을 사용해도 되는 매우 중요한 경우가 있는데 바로 DSL을 설계할 때이다.
```html
body {
    div { 
        +"Some text"
    }
}
```
* 요소에 텍스트를 추가하기 위해 `String.unaryPlus`가 사용된 것을 볼 수 있다.
* 명확하게 DSL로 사용되었기 때문에 연산자 오버로딩이 허용된다.
