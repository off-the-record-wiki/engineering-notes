# Kotlin - Annotations and Reflection in Kotlin

## 📚 Kotlin - Annotation

* 어노테이션을 만드는 방법
```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Fancy
```
* `@Target`: 주석(annotation)을 달 수 있는 요소의 종류(클래스, 함수, 속성, 표현식 등)를 지정한다.
* `@Retention`: 주석이 컴파일된 클래스 파일에 저장되는지 여부와 런타임 시 리플렉션을 통해 볼 수 있는지 여부를 지정한다.(기본값은 둘 다 true)
* `@Repeatable`: 동일한 요소에 동일한 애너테이션을 여러 번 사용할 수 있게 한다.
* `@MustBeDocumented`: 해당 주석이 공개 API의 일부이며, 생성된 API 문서에 표시되는 클래스 또는 메서드 서명에 포함되어야 함을 지정한다.

## 📚 Kotlin - Reflection

* 리플렉션이란, 실행 시점(런타임)에 프로그램의 구조를 조사하고 조작할 수 있도록 하는 기능이다.
* 코틀린의 클래스, 프로퍼티, 함수 등이 일급 시민(first-class citizen)으로 취급되므로, 리플렉션을 통해 프로그램이 실행되는 동안 이들의 내부 구조를 파악하고 사용할 수 있다.
  * 독립적인 API: 코틀린은 자바와 별도로 `kotlin.reflect` 패키지를 통해 고유한 리플렉션 API를 제공한다.
  * 자바 리플렉션 지원: 코틀린 클래스는 자바 바이트코드로 컴파일되기 때문에, 자바 리플렉션 API(`java.lang.reflect`)도 코틀린 클래스에 사용할 수 있다.
  * 컴파일러 의존성: 코틀린 리플렉션을 사용하려면 프로젝트에 kotlin-reflect.jar 라이브러리 의존성을 추가해야한다.
* `KClass` : 자바의 Class에 해당하는 코틀린 버전으로, 클래스의 메타데이터를 표현한다.
  * 획득 방법: 클래스 이름 뒤에 `::class`를 붙여 얻을 수 있다. 
* `KProperty` : 클래스의 프로퍼티(변수)에 대한 정보를 제공한다.
  * 주요 기능: 프로퍼티의 이름, 타입, 접근자(getter, setter) 등을 조회하고 동적으로 값을 가져오거나 설정할 수 있다.
* `KFunction` : 클래스 내 함수(메서드)에 대한 정보를 제공한다.
  * 주요 기능: 함수의 이름, 매개변수, 반환 타입 등을 조회하고 동적으로 함수를 호출할 수 있다.

### 리플렉션 사용 시 주의할 점❗

* 성능 저하: 리플렉션은 컴파일 시점에 결정되지 않고 런타임에 동적으로 분석되기 때문에, 일반적인 코드에 비해 성능 오버헤드가 발생할 수 있다.
* 컴파일 타임 안정성 부재: 컴파일러의 타입 검사를 우회하므로, 런타임에 오류가 발생할 가능성이 높아진다.
* 유지보수 어려움: 코드가 복잡해지고 가독성이 떨어져 유지보수가 어려워질 수 있다.
