# Spring - API Exception Handling

#### 서블릿 예외 처리

* **1. WAS의 재호출**
  * 컨트롤러에서 발생한 예외가 WAS까지 전파되면, WAS는 단순히 페이지를 보여주는게 아니라 오류 페이지 경로로 다시 요청을 보낸다.
  * 웹 브라우저는 내부적인 재호출 사실을 인지하지 못한다. 오직 서버 내부에서만 일어나는 일이다.
* **2. 필터 : DispatcherType의 정교한 제어**
  * REQUEST : 클라이언트의 일반 요청
  * ERROR : 오류 발생 시 재호출
  * FORWARD : 서블릿에서 다른 서블릿/JSP로 전달할 때
  * INCLUDE : 다른 서블릿/JSP 결과를 포함할 때
* **3. 인터셉터 : 스프링만의 세밀한 제외 패턴**
  * 인터셉터는 서블릿 기술이 아닌 Spring MVC 기술이기 때문에 DispatcherType에 영향을 받지 않는다.
  * `excludePathPatterns("/error-page/**")`와 같이 경로 기반 제외 설정을 사용하는 것이 표준이다.

#### ExceptionResolver

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/95af57e4-e7ba-4f65-8901-fc58f64ffaef.png)

* 스프링에서 제공하는 ExceptionResolver는 다음과 같다.
  * `ExceptionHandlerExceptionResolver`
  * `ResponseStatusExceptionResolver`
  * `DefaultHandlerExceptionResolver`

#### @ResponseStatus 어노테이션 적용하여 HTTP 상태 코드를 변경

```java
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "잘못된 요청 오류") 
public class BadRequestException extends RuntimeException {
    // ...
}
```

#### ExceptionHandlerExceptionResolver - @RestControllerAdvice(@ControllerAdvice) & @ExceptionHandler]

```java
@Data
@AllArgsConstructor
public class ErrorResult {
    private String code;
    private String message;
}

@Slf4j
@RestControllerAdvice(basePackages = "hello.exception.api")
public class ExControllerAdvice {

    /**
     * 특정 사용자 정의 예외(UserException) 처리
     * ResponseEntity를 반환하여 동적으로 HTTP 상태 코드를 제어함
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResult> userExHandler(UserException e) {
        log.error("[exceptionHandler] ex", e);
        ErrorResult errorResult = new ErrorResult("USER-EX", e.getMessage());
        
        // 400 Bad Request와 함께 JSON 데이터 반환
        return new ResponseEntity<>(errorResult, HttpStatus.BAD_REQUEST);
    }

    /**
     * 상위 예외(Exception) 처리
     * @ResponseStatus 애노테이션을 사용하여 고정된 HTTP 상태 코드를 반환함
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler
    public ErrorResult exHandler(Exception e) {
        log.error("[exceptionHandler] ex", e);
        
        // 500 Internal Server Error와 함께 JSON 데이터 반환
        return new ErrorResult("EX", "내부 오류");
    }
}
```

* 가장 많이 사용되는 방식으로 바로 응답값을 변환해서 내린다.
* 클래스 단에는 `@RestControllerAdvice`를, 메서드 단에는 `@ExceptionHandler`를 붙여주고 인자로 처리하고자하는 Exception을 받아준다.
* 메서드를 반환할 때, ResponseEntity로 반환하면서 상태값을 명시해주거나 객체를 반환하면서 @ResponseStatus를 사용해서 상태값을 명시해줘도 된다.
* `@ExceptionHandler` 어노테이션은 자세한 것부터 먼저 우선권을 가진다.

#### @ControllerAdvice

```java
// 1. 특정 애노테이션이 붙은 컨트롤러만 대상으로 지정
// 예: @RestController가 붙은 모든 컨트롤러에만 적용할 때 사용합니다.
@ControllerAdvice(annotations = RestController.class)
public class ExampleAdvice1 {}

// 2. 특정 패키지 및 그 하위 패키지를 대상으로 지정
// 가장 흔히 사용되는 방식으로, API 전용 컨트롤러들이 모인 패키지를 지정하기 좋습니다.
@ControllerAdvice("org.example.controllers")
public class ExampleAdvice2 {}

// 3. 특정 클래스나 인터페이스를 상속/구현한 컨트롤러만 대상으로 지정
// 공통 부모 클래스가 있거나 특정 인터페이스를 구현한 컨트롤러에 정밀하게 적용할 때 사용합니다.
@ControllerAdvice(assignableTypes = {ControllerInterface.class, AbstractController.class})
public class ExampleAdvice3 {}
```

> [📚@ControllerAdvice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
>
> **1. 핵심 개념**
> * `@ControllerAdvice`는 여러 컨트롤러에서 공통으로 사용되는 `@ExceptionHandler`, `@InitBinder`, `@ModelAttribute` 메서드들을 하나의 클래스에 모아 전역적으로 적용하기 위한 특수화된 `@Component`이다.
> * AOP의 개념을 사용해 런타임에 컨트롤러 동작을 가로채고 보완한다.
>
> **2. 주요 기능 및 실행 시점**
> * `@ExceptionHandler` : 컨트롤러에서 예외가 발생했을 때 이를 가로채서 공통된 응답을 생성한다.
> * `@InitBinder` : 모든 컨트롤러 요청에 대해 데이터 바인딩 설정을 일괄 적용한다.
> * `@ModelAttribute` : 모든 컨트롤러의 Model 객체에 공통으로 들어갈 데이터를 미리 담아둡니다.
