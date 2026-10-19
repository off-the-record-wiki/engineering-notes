# (Effective Kotlin Item 34) Kotlin - 복잡한 객체 생성을 위해 DSL 정의를 고려하라

#### 복잡한 객체 생성을 위해 DSL 정의를 고려하라

* 코틀린 기능을 활용하여 도메인 특화 언어(DSL)를 구성할 수 있다.
* DSL은 더 복잡한 객체나 객체의 계층 구조를 정의할 때 유용하다.
* 정의하기는 쉽지 않지만 일단 완료되면 보일러플레이트 코드와 코드의 복잡성이 숨겨진다.
* 그래서 개발자는 자신의 의도를 명확하게 표현할 수 있다.

```kotlin
val page = html {
    body {
        p {
            attr("class", "intro")
            +"안녕하세요"        // unaryPlus로 텍스트 추가
        }
        p {
            +"DSL로 만든 문단"
        }
    }
}
```

#### 자신만의 DSL 정의하기

* 함수타입에 수신 객체를 붙인 `Type.() -> Unit` 형태이다. 블록 안에서 그 객체가 `this`가 되어 멤버를 직접 부를 수 있다.
```kotlin
// ⭕ Good - 수신 객체 지정 람다
class TableBuilder {
    fun row(block: RowBuilder.() -> Unit) { /* ... */ }
}

// TableBuilder.() -> Unit : 블록 안에서 TableBuilder가 this
fun table(block: TableBuilder.() -> Unit): TableBuilder =
    TableBuilder().apply(block)   // apply가 수신 객체 람다를 실행하고 객체를 반환
```
* `apply()`로 객체를 만들고 그 객체를 수신 객체로 람다를 실행한 뒤, 객체를 돌려준다. DSL 진입 함수의 표준 패턴이다.

```kotlin
// ⭕ Good - 연산자 오버로딩
class TextBuilder {
    var content = ""
    // +"문자열" 문법을 가능하게 함
    operator fun String.unaryPlus() {
        content += this
    }
}
```
* `+,` `in`, `invoke`같은 연산자를 오버로딩하면 자연스러운 문법이 된다.

```kotlin
// ⭕ Good - 람다의 마지막 인자 관례 + 확장 함수
fun build(block: Builder.() -> Unit) { }
```
* 코틀린은 함수의 마지막 인자가 람다면 괄호 밖으로 뺄 수 있다.

```kotlin
// ⭕ Good - @DslMarker (스코프 안전성)
@DslMarker
annotation class MyDsl
```
* 중첩 시 안쪽에서 바깥 수신 객체를 실수로 부르지 않도록 막는다.

#### DSL은 언제 사용해야 하는지?

* DSL을 사용하여 모든 정보를 명확하게 표현하고 구조화할 수 있다.
* DSL을 사용하면 익숙하지 않은 사람들에게 혼란을 일으킬 수 있는데, 유지 관리까지 고려하면 더욱 그렇다.
* 또한 DSL을 어떻게 정의하느냐에 따라 성능과 개발자의 혼란이라는 측면에서 부담이 될 수 있다.
* DSL을 대신할 기능이 있다면 DSL은 과한 선택이다. 그러나 다음과 같은 것을 표현해야 할 때는 매우 유용하다.
  * **복잡한 데이터 구조**
  * **계층구조**
  * **엄청난 양의 데이터**
* DSL과 같은 구조는 빌더나 생성자를 사용해 모든 것을 표현할 수 있다.
* DSL은 보일러플레이트 코드를 제거할 수 있는 방법 중 하나이다.
* 반복 가능한 보일러플레이트 코드가 보이고 이를 해결할 수 있는 코틀린 기능이 없다면 DSL 사용을 고려해야 한다.
