# (Effective Kotlin Item 20) Kotlin - The Beauty of the Type System

#### Any

* 코틀린에서 모든 클래스의 슈퍼 클래스는 `Any`이며, 자바의 `Object`와 비슷하다.
* 모든 타입의 슈퍼 타입은 `Any`가 아니라 `Any?`이다.
* `Any`는 널 가능하지 않은 모든 타입의 슈퍼 타입이다.

> **Any와 Any?의 구조적 차이**
>
> * 코틀린의 모든 객체는 `Any`를 상속받는다.
> * 자바의 `Object`와 같지만, `Any`는 `equals()`, `hashCode()`, `toString()`만 가지고 있다.
> * `wait()`와 `notify()` 같은 쓰레드 관련 메서드는 자바의 `Object`에는 있지만 코틀린의 `Any`는 없다는 점이 차이이다.

#### Unit

* `Unit`은 타입 계층구조에서 어떤 위치도 차지하지 않는다.
* 함수가 결과 타입을 명시하지 않았을 때, 선언되는 객체이다.

> **Unit vs void**
>
> * 코틀린의 `Unit`은 실제로 존재하는 타입(싱글톤 객체)이다.
> * 타입이기 때문에 제네릭 매개변수로 넘길 수 있다. `Callback<Unit>`처럼 정의하면 '반환값은 없지만 성공했다'는 의미를 타입 시스템에서 완벽하게 표현할 수 있어 설계가 매우 정교해진다.

#### Nothing: 모든 타입의 서브타입

* `Nothing`은 모든 타입의 서브타입이다.
* 그래서 `Nothing` 타입의 인스턴스가 있다면 어떤 것이든지 대체하여 사용할 수 있다.
* 영원히 실행되거나, 프로그램을 종료하거나, 예외를 던지는 경우에 사용할 수 있다.
```kotlin
fun runForever(): Nothing {
    while (true) {
        // 연산이 없다.
    }
}

fun endProgram(): Nothing {
    exitProcess(0)
}
```

#### return과 throw 결과 타입

```kotlin
fun main() {
    val a = return
    val b = throw Error()
}
```

* `return`과 `throw`는 모두 함수를 종료시키기 때문에 아무 의미가 없으며 따라서, 변수 a와 b에 할당되는 값도 없다.
* `return`과 `throw`는 결과 타입으로 Nothing을 선언한다.

#### 도달할 수 없는 코드가 있는 경우라면?

* 결과 타입이 `Nothing`이라고 선언한 요소를 호출한 뒤의 코드는 전부 도달할 수 없다.
* `Nothing`인 인스턴스는 없으니 반환될 수가 없다는 점에서 합리적이다.
* `Nothing`을 결과 타입으로 선언한 문장은 정상적인 방법으로는 완료되지 않기 때문에 그 다음 문장에 도달할 수 없게 된다.
* `fail`과 `throw` 다음에 오는 모든 코드가 도달할 수 없는 이유이다.
```kotlin
fun test1() {
    print("Before")
    fail()
    print("After")
}

fun test2() {
    print("Before")
    throw Error()
    print("After")
}
```
