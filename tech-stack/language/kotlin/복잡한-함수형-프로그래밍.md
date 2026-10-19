# Kotlin - Advanced Functional Programming in Kotlin

## 📚 고차 함수와 함수 리터럴

```kotlin
fun compute(num1: Int, num2: Int, op: (Int, Int) -> Int): Int {
    return op(num1, num2)
}
```
* 대표적인 고차함수 예시1: 컬렉션 처리 함수(map, filter, forEach, sortedBy, groupBy)
* 대표적인 고차함수 예시2: 스코프 함수(let, run, apply, also, with)
  * **let**: 호출한 객체(it)를 람다의 인자로 전달하며, 널이 아닐 때만 실행하고 결과를 반환한다.
  * **run**: 람다 내에서 객체(this)를 사용하여 프로퍼티를 설정하거나 함수를 호출하고, 최종 결과를 반환한다.
  * **apply**: run과 비슷하지만, 객체(this) 자체를 반환한다. 객체를 생성하고 초기화하는 데 주로 사용된다.
  * **also**: bapply와 비슷하지만, 람다의 인자로 객체(it)를 전달하며, 객체 자체를 반환한다. 주로 부수 효과(side-effect)를 수행하는 데 사용된다.
  * **with**: 객체(this)를 인자로 받아 컨텍스트 내에서 작업을 수행한다. 반환값은 람다의 마지막 표현식이다.
* 대표적인 고차함수 예시3: 리소스 관리 함수(use)
  * **use**: 파일을 열고 닫거나 데이터베이스 커넥션을 관리하는 등 리소스 처리를 간편하게 처리해준다.
* 대표적인 고차함수 예시4: 코루틴 빌더 함수(launch, async)
  * **launch**: 새로운 코루틴을 시작하고 결과를 기다리지 않는다.
  * **async**: 새로운 코루틴을 시작하고 Deferred 객체를 반환하여 나중에 결과를 받을 수 있도록 한다.

## 📚 복잡한 함수 타입과 고차 함수의 단점❓

* 낮은 숙련도의 경우: 함수형 프로그래밍에 익숙하지 않은 개발자에게는 고차 함수로 작성된 코드가 직관적이지 않고 이해하기 어려울 수 있다. 특히 람다가 중첩되거나, 복잡한 연산 체인을 형성할 때 가독성이 떨어질 수 있다.
* 과도한 사용: 간단한 작업에 지나치게 추상적인 고차 함수를 사용하면 오히려 코드를 더 복잡하게 만들 수 있다. 때로는 단순한 for 루프가 훨씬 명확하고 이해하기 쉬울 수 있다.

## 📚 inline 함수❓

```kotlin
// 잘못된 예제: 람다의 성능 오버헤드
fun nonInlineOperation(count: Int, action: () -> Unit) {
    for (i in 1..count) {
        action() // 매번 함수 객체 할당 및 호출 오버헤드 발생
    }
}

fun main() {
    val startTime = System.nanoTime()

    // 람다를 직접 전달하여 100만 번 호출
    nonInlineOperation(1_000_000) {
        // 실제로는 매우 작은 연산
    }

    val endTime = System.nanoTime()
    println("경과 시간: ${(endTime - startTime) / 1_000_000.0}ms")
}
```
* nonInlineOperation 함수가 호출될 때마다 action 람다를 위한 새로운 함수 객체가 힙에 생성된다.
* 함수 객체를 생성하고 호출하는 과정에서 작은 성능 오버헤드가 발생합니다. 특히 반복문 안에서 수백만 번 호출될 경우 이 오버헤드가 누적되어 전체적인 성능에 영향을 줄 수 있다.
```kotlin
// 리팩터링된 예제: `inline`을 사용하여 오버헤드 제거
inline fun inlineOperation(count: Int, action: () -> Unit) {
    for (i in 1..count) {
        action() // 컴파일 시 이 위치에 action 람다의 코드가 직접 삽입됨
    }
}

fun main() {
    val startTime = System.nanoTime()

    // inline 함수 호출
    inlineOperation(1_000_000) {
        // 실제로는 매우 작은 연산
    }

    val endTime = System.nanoTime()
    println("경과 시간: ${(endTime - startTime) / 1_000_000.0}ms")
}
```
* inline 키워드 덕분에 inlineOperation 함수가 호출될 때마다 람다 객체를 생성하는 오버헤드가 사라진다.
* 컴파일 시점에 action() 호출이 람다의 본문 코드로 대체되므로, 마치 람다 없이 직접 코드를 작성한 것처럼 동작한다.
* 결과적으로 메모리 할당과 함수 호출 오버헤드가 줄어들어 성능이 향상된다.

## 📚 SAM과 reference❓

* SAM(Single Abstract Method) : 추상 메서드 하나만 있는 경우
