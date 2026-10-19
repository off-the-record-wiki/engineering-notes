# (Effective Kotlin Item 17) Kotlin - Extensions

#### 확장

* 코틀린에서는 인스턴스에서 호출하는 함수와 프로퍼티를 다른 방식으로 정의할 수 있다.
* 확장 함수는 일반 함수처럼 정의하지만, 함수 이름 앞에 타입을 추가한다.
```kotlin
class Telephone(val number: String)

fun Telephone.call() {
    print("Calling $number")
}
```

* 확장 함수는 `String`처럼 통제할 수 없는 타입에도 추가할 수 있다.
```kotlin
fun String.remove(value: String) = this.replace(value, "")
```

* String에 remove라는 확장 함수를 정의했다.
* String 객체에서 확장 함수를 호출할 수 있다.
* 확장 함수 안에서는 멤버 함수처럼 this 키워드를 이용해 현재 객체를 참조할 수 있다.
* 물론 this 키워드를 생략하더라도 암묵적으로 사용할 수 있다.

```kotlin
// 명시적으로 this를 사용
fun String.remove(value: String) = this.replace(value, "")

// 암묵적으로 this를 사용
fun String.remove(value: String) = replace(value, "")
```

> **확장 함수의 정적 바인딩**
>
> * 확장 함수는 클래스 멤버가 아니라 클래스 외부에서 호출되는 정적 함수이다. 따라서 서브 클래스에서 오버라이딩할 수 없다.
> * 호출 시점에 참조하는 타입이 무엇이냐에 따라 호출되는 함수가 결정된다.

#### 확장함수의 구조

```kotlin
fun String.remove(value: String) : String {
    return this.replace(value, "")
}
```

#### 확장 프로퍼티

* 확장은 상태가 없기 때문에 필드를 가질 수 없다.
* 프로퍼티는 필드 실체 없이 Getter/Setter만으로 정의할 수 있다.
```kotlin
class User {
    // ...
    var birthdateMillis: Long? = null
}

var User.birthdate: Date?
    get() [
        val millis = birthdateMillis
        return if (millis == null) null else Date(millis)
    }
    set(value) {
        birthdateMillis = value?.time
    }
```

> **확장 프로퍼티와 상태의 관계**
>
> * 확장 프로퍼티는 기존 클래스에 새로운 필드들 추가하는 것이 아니다.
> * 따라서 `get()` 내에서 `this.field`처럼 값을 저장할 수 없다. 대신 `get()`과 `set()` 내부에서 기존 클래스의 공개 멤버들을 조합해 새로운 연산된 값을 만들어낼 뿐이다.
