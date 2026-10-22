# (Effective Kotlin Item 16) Kotlin - Annotation Classes

#### 애너테이션 클래스

* 애너테이션은 스스로는 아무 일도 하지 않아서 코드 작동 방식에 아무런 영향을 주지 않는다.
* 단지 정보만 담고 있을 뿐이다.

#### 메타-애너테이션

* 애너테이션 클래스에 추가되는 애너테이션을 메나-애너테이션이라고 한다.
* **`@Target` : 애너테이션의 대상이 될 수 있는 코드 요소의 종류를 가리킨다. 인수로는 AnnotationTarget 열거형 값을 받는다.**
* **`@Retention` : 컴파일된 바이너리 결과에 애너테이션을 포함시킬지와 리플렉션으로 볼 수 있을지를 결정한다.**
* **`@Repeatable` : 코드 요소 하나에 애너테이션이 두 번 이상 적용 가능한지 정한다.**
* **`@MustBeDocumented` : 에너테이션이 공개 API인지 그래서 코드로부터 문서를 생성할 때, 애너테이션이 적용된 요소들의 설명에 애너테이션도 포함시킬지를 정한다.**

> **@RetentionPolicy**
>
> * `@Retention`은 어노테이션이 어느 시점까지 살아남을지를 결정한다.
> * `AnnotationRetention.SOURCE` : 컴파일 과정에서만 쓰이고 사라진다.
> * `AnnotationRetention.BINARY` : 클래스 파일에는 저장되지만 런타임에 리플렉션으로 접근할 수 없다.
> * `AnnotationRetention.RUNTIME` : 런타임에 리플렉션을 통해 어노테이션 정보를 읽어올 수 있다.

> **@Target**
>
> * `AnnotationTarget.FUNCTION` : 함수
> * `AnnotationTarget.CLASS` : 클래스
> * `AnnotationTarget.PROPERTY` : 프로퍼티
> * `AnnotationTarget.VALUE_PARAMETER` : 함수의 매개변수


> **어노테이션 매개변수 제약**
> 
> * 허용되는 타입 리스트 : 기본 타입(Int, Double, Boolean 등), String, KClass, 다른 어노테이션, 배열(이전에 나열된 타입들)
```kotlin
annotation class CacheConfig(
    val key: String,
    val expireSeconds: Int = 60, // 기본값 지정 가능
    val targetClass: KClass<*> // 클래스 정보도 담을 수 있음
)
```
