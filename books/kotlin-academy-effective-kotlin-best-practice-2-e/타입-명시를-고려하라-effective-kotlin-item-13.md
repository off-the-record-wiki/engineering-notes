# (Effective Kotlin Item 13) Kotlin - 타입 명시를 고려하라

#### 타입 명시를 고려하라

* Kotlin은 개발자가 해당 타입을 명확하게 알 수 있는 경우 이를 생략할 수 있는 훌륭한 타입 추론 시스템을 가지고 있다.
```kotlin
// ❌ Bad
val result = fetchDataFromService(userId) 
result.forEach { ... }
```
```kotlin
// ⭕ Good
val permissions: List<Permission> = fetchDataFromService(userId)
permissions.forEach { ... }
```

* 문맥상 타입이 명확하여 추가적으로 명시할 필요가 없는 경우 타입을 생략할 수 있어 가독성도 높여줄 수 있다.
* 타입은 개발자와 컴파일러 모두에게 중요한 정보가 될 수 있다. 그렇기에 언제든 주저하지 말고 타입을 명시하자.
* 비용은 거의 들지 않으면서 크게 도움이 된다.
* 공개 API를 설계할 때는 노출되는 타입을 항상 분명하게 명시해야 한다. 가독성뿐 아니라 안전성을 위해서라도 중요하다.

#### 라이브러리 작성자를 위한 명시적 API 모드

* Kotlin 1.4에서는 라이브러리 작성자를 위한 명시적 API 모드(explicit API mode)가 도입되었다.
* 이 모드를 활성화하면 코틀린은 공개 API를 대상으로 타입과 가시성 한정자를 명시하도록 강제한다.
```kotlin
kotlin {

    // 엄격 모드용
    explicitApi()

    // 경고 모드용
    explicitApiWarning()
}
```
