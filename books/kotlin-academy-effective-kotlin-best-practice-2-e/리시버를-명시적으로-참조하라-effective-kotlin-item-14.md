# (Effective Kotlin Item 14) Kotlin - 리시버를 명시적으로 참조하라

#### 리시버를 명시적으로 참조하라

* 명시적인 리시버없이 작성된 예제 코드와 리시버를 명시적으로 작성한 예제 코드를 비교해본다.
```kotlin
fun <T : Comparable<T>> List<T>.quickSort(): List<T> {
    if (size < 2) return this
    val pivot = first()
    val (smaller, bigger) = drop(1)
        .partition { it < pivot }
    return smaller.quickSort() + pivot + bigger.quickSort()
}
```
```kotlin
fun <T : Comparable<T>> List<T>.quickSort(): List<T> {
    if (this.size < 2) return this
    val pivot = this.first()
    val (smaller, bigger) = this.drop(1)
        .partition { it < pivot }
    return smaller.quickSort() + pivot + bigger.quickSort()
}
```

#### 여러 개의 리시버

* 항상 명시적으로 리시버 사용을 권장하는 것은 아니지만 명시적 리시버 사용이 특별히 유용한 경우가 있는데, 둘 이상의 리시버를 사용하고 스코프가 중첩되어 있을 경우 그렇다.
* 특히 `apply`, `with`, `run`처럼 리시버를 암시적으로 지정할 수 있는 함수를 사용할 경우 중첩된 스코프 내에서 리시버 여러 개가 있을 때가 많다.
* 이런 부류의 함수를 부득이하게 사용해야 한다면 객체를 다룰 때 리시버를 명시적으로 표기하는 것이 더 안전하다.
* 쉽게 말해 "이 함수나 람다가 어떤 객체를 기준으로 실행되느냐"를 말한다.

```kotlin
// ❌ Bad
fun process(db: Database, logger: Logger, network: Network, cache: Cache) {
    // 매번 인자를 다 전달해야 함 (의존성 지옥)
}
```
```kotlin
// ⭕ Good
context(Database, Logger)
fun saveAction(data: String) { ... }

context(NetworkClient)
fun syncAction() { ... }

fun execute() {
    with(db) {
        with(logger) {
            saveAction("data")
        }
    }
    with(network) {
        syncAction()
    }
}
```
