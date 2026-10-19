# Spring - Bean Validation

#### Bean Validation

* Bean Validation을 사용하려면 아래와 같은 의존성을 추가해주면 된다.

```
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

> [📚Spring Bean Validation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
>
> * Bean Validation은 JavaBean 검증을 위한 메타데이터 모델과 API를 정의한다.
> * 메타데이터의 주요 소스는 어노테이션이며, XML 검증 기술서를 통해 이 메타데이터를 정의하거나 확장할 수도 있다.
> * 본래 Java EE의 일부로 정의되었으나, 버전 2부터는 Jave SE 애플리케이션에서도 동작하는 것을 목표로 하고 있다.
> 
> **1. Bean Validation의 핵심 개념**
> * 인터페이스 중심 : Bean Validation은 구체적인 구현 기술이 아니라, `@NotNull`, `@Min`과 같은 어노테이션과 그 동작 방식을 정의한 표준 인터페이스이다.
> * Hibernate Validator : Hibernate Validator는 이 표준 사양을 실제 코드로 구현한 가장 대표적인 구현체이다.

```java
@Data
public class ItemUpdateForm {

    @NotNull
    private Long id;

    @NotBlank(message = "빈값이 들어올 수 없습니다.")
    private String itemName;

    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;
}
```
```java
// Validator를 별도로 분리하지 않고 컨트롤러에서 검증하고자 하는 인자에 @Validated 어노테이션을 붙여준다.
public String addItem(@Validated @ModelAttribute Item item, BindingResult bindingResult) {
    // ...
}
```

>❓`@Valid` vs `@Validated` 심층 비교
>
> * `@Valid` : 자바 표준 어노테이션이다. 특정 구현체에 의존하지 않는 표준 사양이다.
> * `@Validated` : Spring 프레임워크에서 제공하는 전용 어노테이션이다. 표준 기능을 확장하여 Spring 환경에 최적화된 기능을 제공한다.
> * `@Valid`는 그룹 검증을 지원하지 않으나 `@Validated`는 그룹 검증을 지원한다. 추가로 `@Valid`는 `ArgumentResolver`에 의해 검증이 수행되나 `@Validated`는 AOP(Aspect Oriented Programming) 기반 검증을 수행한다.

#### HTTP 메시지 컨버터

* `@Valid`, `@Validated` 어노테이션은 HttpMessageConverter에도 적용할 수 있다.
* `@ModelAttribute`: HTTP 요청 파라미터를 다룰 때 사용한다.
* `@RequestBody`: HTTP Body 데이터를 객체로 변환할 때 사용한다.

>❓`@ModelAttribute` vs `@RequestBody` 바인딩 메커니즘
>
> **1. @ModelAttribute : 필드 단위 바인딩(Property-based)**
> * 동작 방식 : `ArgumentResolver`가 객체를 생성한 후, 쿼리 파라미터를 하나씩 필드에 대입한다.
> * 오류 수용성 : 특정 필드에 문자열이 들어와 타입 변환 오류가 발생해도 Spring은 이를 `BindingResult`에 담고 다음 필드 바인딩을 계속 진행한다.
> * 결과 : 컨트롤러가 호출되므로 `BindingResult.hasErrors()`를 통해 사용자에게 친절한 에러 페이지를 보여줄 수 있다.
>
> **2. @RequestBody : 객체 단위 메시지 컨버팅(MessageConverter-based)**
> * 동작 방식 : HttpMessageConverter(주로 Jackson)가 JSON 전체를 통째로 읽어 객체로 변환을 시도한다.
> * JSON 구조가 잘못되었거나, 특정 필드의 타입이 맞지 않아 객체 생성 자체가 실패하면 그 즉시 `HttpMessageNotReadableException`이 발생한다.
> * 결과 : 객체 생성에 실패했으므로 컨트롤러 메서드 자체가 실행되지 않으며, 당연히 `@Validated`에 의한 검증 기회조차 얻지 못한다.
