# Kotlin - Lazy Initialization and Delegation in Kotlin

## 📚 lateinit과 lazy()

* **lateinit** : 초기값을 지정하지 않고, null이 들어갈 수 없는 변수를 선언하는 키워드이다.
  * lateinit 변수가 초기화되지 않은 상태에서 접근하면 UninitializedPropertyAccessException이 발생한다. 이는 컴파일 타임에 널 가능성 체크를 하지 않는 대신, 런타임에 초기화 여부를 명시적으로 확인하는 방식이기 때문이다.
  * Kotlin의 Int/Long은 Java의 int/long으로 변환된다. 그런데 lateinit은 nullable 변수로 변환되어야 한다. 그렇기에 lateinit을 primitive type에 사용할 수 없다.
* Kotlin에서 Spring을 사용할 때 `@Value` 어노테이션을 처리하는 좋은 방법은 생성자 주입을 사용하는 것이다. 이는 불변성을 유지하고 테스트를 용이하게 하며 의존성을 명확하게 보여줄 수 있다.
* lateinit을 `@Value`와 함께 사용하는 것이 완전히 잘못된 방식은 아니지만 Kotlin과 Spring의 권장 패턴에서 생성자 주입을 통해 불변성을 확보하고 의존성을 명확히 하는 것이 좋다.
  * lateinit의 경우 var을 사용해 값 변경을 가능하게 해주지만 by lazy의 경우 val을 사용하기 때문에 값 변경이 불가능하다.
  * lateinit의 경우 초기화 이후에 계속해 값이 바뀔 수 있을 때 사용하는 것을 추천하고 by lazy의 경우 초기화 이후에 읽기 전용 값으로 사용할 때 사용하는 것을 추천한다.
> Kotlin Documentation
> * Normally, you must initialize properties in the constructor. However, this isn't always convenient. For example, you might initialize properties through dependency injection or inside the setup method of a unit test.
```kotlin
public class OrderServiceTest {
    lateinit var orderService: OrderService

    @SetUp fun setup() {
        orderService = OrderService()
    }

    @Test fun processesOrderSuccessfully() {
        // Calls orderService directly without checking for null
        // or initialization
        orderService.processOrder()
    }
}
```

## 📚 by lazy의 원리와 위임 프로퍼티

* by lazy는 Kotlin의 지연 초기화 기능을 구현하는 가장 일반적인 방법이다.
* 이는 변수가 처음 사용되는 시점에 초기화가 이루어지도록 하며, 초기화에 비용이 많이 드는 객체를 효율적으로 다룰 때 유용하다.
  * **지연 초기화** : lazy 블록 내 코드는 해당 변수가 처음으로 호출될 때 실행된다. 그전까지는 변수가 메모리에 할당되지 않는다.
  * **단 한 번의 초기화** : 변수가 일단 초기화되고 나면, 이후에 다시 호출될 때는 이미 초기화된 값을 재사용한다. 초기화 블록은 다시 실행되지 않는다.
  * **불변성** : by lazy는 읽기 전용 속성 즉, `val`에만 사용할 수 있다. 따라서 초기화된 값은 변경이 불가능하다.
  * **쓰레드 안전성** : by lazy는 쓰레드 안전을 보장한다. 여러 스레드에서 동시에 접근해도 초기화 로직은 한 번만 실행되도록 설계되어 있다.
  * **모든 타입 사용 가능** : lateinit과 달리, 원시 타입과 nullable 타입을 포함한 모든 타입에 사용할 수 있다.

## 📚 코틀린의 표준 위임 객체❓

### [Lazy Properties](https://kotlinlang.org/docs/delegated-properties.html#lazy-properties)

* `by lazy { ... }` 문법에서 lazy는 람다를 인자로 받아 Lazy\<T> 타입의 위임 객체를 생성하고 반환한다.
* lazy는 프로퍼티에 최초 접근(getter 호출)할 때만 람다 코드를 실행해 값을 계산하고, 이후 접근부터는 캐시된 값을 사용한다.
* 기본적으로 지연 속성(lazy properties)의 평가는 동기화된다.
* lazy 함수의 기본 동작 모드는 LazyThreadSafetyMode.SYNCHRONIZED로, 멀티스레드 환경에서 여러 스레드가 동시에 접근하더라도 단 한 번만 초기화가 이루어지도록 보장한다.
* LazyThreadSafetyMode.SYNCHRONIZED 모드에서는 동기화 메커니즘을 통해 하나의 스레드만 초기화 작업을 수행하게 하고, 초기화가 완료된 후에는 모든 스레드가 동일하고 올바르게 초기화된 값을 공유한다.

### [Observable properties﻿](https://kotlinlang.org/docs/delegated-properties.html#observable-properties)

* 프로퍼티의 값이 변경될 때마다 특정 동작(콜백)을 실행하도록 하는 것이다.

## 📚 위임과 관련된 몇 가지 추가 기능❓

* ReadOnlyProperty는 읽기 전용(val) 프로퍼티의 위임 객체를 만들 때 사용되는 인터페이스이다.
  * 포함하는 메서드: getValue() 메서드만 정의되어 있다.
  * 동작 방식: 프로퍼티에 접근(get)할 때마다 getValue() 메서드가 호출되어 값을 반환한다.
  * 사용 대상: val 프로퍼티에 by 키워드로 위임할 객체를 만들 때 구현한다.
* ReadWriteProperty는 읽기/쓰기(var) 프로퍼티의 위임 객체를 만들 때 사용되는 인터페이스이다.
  * 포함하는 메서드: ReadOnlyProperty를 상속받으므로 getValue() 메서드와 함께 setValue() 메서드가 추가로 정의되어 있다.
  * 사용 대상: var 프로퍼티에 by 키워드로 위임할 객체를 만들 때 구현한다.
  * 동작 방식:
    * 프로퍼티에 접근(get)할 때는 getValue() 메서드가 호출된다.
    * 프로퍼티에 값을 할당(set)할 때는 setValue() 메서드가 호출된다. 

## 📚 Iterable과 Sequence(feat. JMH)

* Iterable과 Sequence의 중요한 차이점은 평가 방식에 있으며, 이로 인해 성능과 메모리 사용량에 큰 차이가 발생한다.

|특징|Iterable(컬렉션)|Sequence|
|:-------:|:-------:|:-------:|
|평가 방식|즉시 평가(각 중간 연산이 전체 컬렉션에 적용된 후 그 결과로 새로운 컬렉션이 생성된다.|지연 평가(최종 연산 호출되기 전까지 아무런 계산도 수행하지 않는다. 각 요소가 필요할 때마다 연산 체인이 순차적으로 적용된다.|
|연산 순서|단계별 연산|요소별 연산|
|메모리 사용|중간 컬렉션 생성된다.|중간 컬렉션이 생성되지 않는다.|
|성능 측면|오버헤드 적다.|오버헤드가 존재한다.|
|적합한 상황|데이터가 적고 연산 자체가 간단한 경우|데이터가 많거나 연산 단계가 많아 중간 컬렉션 생성이 부담스러운 경우|

* JMH는 Java Microbenchmark Harness의 약자로 JVM 기반 언어에서 작은 코드 조각의 성능을 정밀하게 측정하기 위한 벤치마킹 도구이다.
  * **마이크로 벤치마킹**: 개별 메서드나 코드 블록과 같이 작은 단위의 성능을 측정하는 데 특화되어 있습니다.
  * **정확성**: JVM의 복잡한 동작(최적화, 가비지 컬렉션 등)을 고려하여 일관성 없는 측정 결과를 피하고 신뢰성 높은 데이터를 얻을 수 있습니다.
  * **다양한 측정 모드**: 처리량(Throughput), 평균 실행 시간(Average Time), 샘플링 시간(Sample Time) 등 다양한 벤치마킹 모드를 지원합니다.
  * **워밍업**: 실제 측정 전에 JVM이 최적화될 수 있도록 코드를 미리 실행하는 워밍업 단계를 거칩니다.
  * **애노테이션 기반**: @Benchmark와 같은 애노테이션을 사용하여 벤치마크 코드를 쉽게 작성할 수 있습니다.
  * **JVM 언어 지원**: 자바뿐만 아니라 JVM을 대상으로 하는 코틀린, 스칼라 등에서도 사용 가능합니다. 


> 참고했던 내용들
> * **[[Kotlin] 🤚🏻 lateinit vs lazy, 정확히 아세요?](https://velog.io/@haero_kim/Kotlin-lateinit-vs-lazy-%EC%A0%95%ED%99%95%ED%9E%88-%EC%95%84%EC%84%B8%EC%9A%94)**
> * **[Kotlin BenchMarks](https://github.com/Kotlin/kotlin-benchmarks)**
