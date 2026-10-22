# (Effective Kotlin Item 7) Kotlin - for

#### 범위(Range)

* 코틀린에서 1..5처럼 두 숫자 사이에 점 2개를 찍어 정수 범위를 뜻하는 `IntRange` 클래스를 만들 수 있다.
* `IntRange` 클래스는 `Iterable`을 구현하고 있어 for문에서 사용이 가능하다.
* 코틀린 컴파일러가 내부적으로 성능을 최적화하기 때문에 이 방법은 편리하면서도 효율적이기도 하다.
* `..`를 사용해 만들어진 범위에는 마지막 값도 포함된다.
* 마지막 값 전에 멈추는 범위를 만들고 싶으면 `..<` 연산자나 `until` 중위 연산자를 사용해야 한다.
* 만약 역방향으로 즉, 큰 수에서 작은 수로 순회하고 싶다면 `downTo` 함수를 사용한다.

```kotlin
for (i in 1..5) {
    print(i) // 12345
}
```
```kotlin
for (i in 1..<5) {
    print(i) // 1234
}
```
```kotlin
for (i in 1 until 5) {
    print(i) // 1234
}
```
```kotlin
// 역방향 순회
for (i in 5 downTo 1) {
    print(i) // 54321
}
```
```kotlin
for (i in 1..10 step 3) {
    print("$i ") // 1 4 7 10 
}
```

#### break와 continue

* 컬렉션이나 Range를 다룰 때, for문과 break/continue를 쓰는 대신 함수형 API를 사용하는 것을 좀 더 코틀린스러운 코드라고 본다.
  * `break` : 가장 가까운 반복문을 종료한다.
  * `continue` : 가장 가까운 반복문의 다음 단계로 건너뛴다.
* `continue`는 `filter`로, `break`는 `takeWhile` 등으로 대체할 수 있다.
* 코틀린의 `forEach` 람다 안에서는 기본적으로 `break`, `continue`를 사용할 수 없다. 대신 `return@forEach`를 사용해 `continue`처럼 동작하도록 만든다.
