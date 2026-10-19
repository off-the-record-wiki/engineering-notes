# (Effective Kotlin Item 22) Kotlin - 타입 매개변수의 섀도잉을 피하라

#### 타입 매개변수의 섀도잉을 피하라

* 섀도잉 덕분에 동일한 이름으로 프로퍼티와 매개변수를 정의할 수 있다.
* 이렇게 되면 지역 매개변수는 외부 스코프와 프로퍼티를 가릴 수 있다.

```kotlin
class Forest(val name: String) {
    
    fun addTree(name: String) {
        // ...
    }
}
```

* 이런 상황은 눈에 잘 띄지 않으며 심각한 문제로 이어질 수 있다.

```kotlin
// ❌ Bad
class Box<T>(private var value: T) {
    // 메서드의 <T>가 클래스의 T를 가린다(섀도잉).
    // 여기서 T는 클래스 T와 무관한 '새로운' 타입 매개변수다.
    fun <T> setValue(newValue: T) {
        value = newValue 
    }
}
```
```kotlin
// ⭕ Good
class Box<T>(private var value: T) {
    // 메서드에 별도 <T>를 붙이지 않으면 클래스의 T를 그대로 사용한다.
    fun setValue(newValue: T) {
        value = newValue
    }
}
```
* 새로운 타입 매개변수를 도입해야 한다면 이름을 다르게 지정하는 것이 좋다.
* 다른 타입 매개변수의 하위 타입으로 제한할 수도 있다.
