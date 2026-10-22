# Spring ‐ Spring MVC

#### 스프링 MVC 구조 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a2a52082-a954-4803-bf6f-2bbe5193f378.png)

**1. DispatcherServlet 상세 동작 로직**

* 핸들러 조회 : `HandlerMapping`을 통해 요청 URL을 처리할 적절한 핸들러(컨트롤러) 객체를 찾는다.
* 핸들러 어댑터 조회 : 찾은 핸들러를 실행할 수 있는 `HandlerAdapter`를 찾는다.
* 핸들러 어댑터 실행 : 디스패처 서블릿이 어댑터에게 실행 권한을 넘긴다.
* 핸들러 실행 : 어댑터가 실제 컨트롤러의 메서드를 호출하여 비즈니스 로직을 수행한다.
* ModelAndView 반환 : 어댑터는 컨트롤러가 반환한 값을 `ModelAndView` 객체로 변환해 서블릿에 돌려준다.
* ViewResolver 호출 : 서블릿은 전달받은 논리적 뷰 이름을 해석하기 위해 `ViewResolver`를 실행한다.
* View 반환 : `ViewResolver`는 논리적 뷰 이름을 물리적 뷰 이름으로 바꾸고 렌더링을 담당하는 `View` 객체를 반환한다.
* 뷰 렌더링 : `View`객체가 모델 데이터를 사용하여 실제 HTML 등의 응답 화면을 생성한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/eab8da3e-4edc-4a06-afc9-046e335fe72d.png)

#### 로깅

* 스프링 부트에서 지원하는 라이브러리는 기본적으로 다음 로깅 라이브러리를 사용한다.
  * [Slf4j](https://www.slf4j.org/)
  * [Logback](https://logback.qos.ch/)

>❗로그 레벨 (Log Level)과 활용
> 
> **1. 로그 레벨 (Log Level)과 활용**
> * `TRACE` : 가장 상세한 정보
> * `DEBUG` : 개발 단계에서 문제를 확인하기 위한 정보
> * `INFO` : 상태 변경 등 정보성 메시지
> * `WARN` : 잠재적인 문제, 경고 사항
> * `ERROR` : 즉각적인 조치가 필요한 심각한 문제
>
> **2. 권장 사항**
> * `System.out` 사용 금지 : 콘솔 출력의 경우 리소스를 많이 점유하며 로그 파일로 남지 않는다.
> * 로그 출력 시 연산 방지 : 문자열 더하기 연산 지양하고 슬롯 방식을 사용하여 연산 비용을 절감한다.
> * 적절한 레벨 준수 : 운영 서버에서 주로 INFO나 WARN 이상으로 설정하여 불필요한 I/O 발생을 막고 스토리지 용량을 관리한다.

> [📚Slf4j](https://www.slf4j.org/docs.html)
> 
> * Facade 패턴 : 실제 로깅 구현이 아닌 추상화 계층이다.
> * 다양한 로깅 라이브러리(Logback, Log4j, Log4j2 등)를 동일한 인터페이스로 사용 가능하다.
> * 런타임에 구현체를 선택할 수 있다.
> 
> **1. 구현 독립성**
```java
// 같은 코드로 여러 구현체 사용 가능
// logback-classic.jar 있으면 LogBack 사용
// log4j.jar 있으면 Log4j 사용
// commons-logging.jar 있으면 Commons Logging 사용

logger.info("로그 메시지"); // 어떤 구현체든 동작
```
> **2. 의존성 충돌 해결**
> * 라이브러리 간 로깅 시스템 충돌 방지
```xml
<!-- 서로 다른 로깅 라이브러리를 사용하는 의존성들을 통합 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 내부적으로 SLF4J + LogBack 사용 -->
</dependency>

<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk</artifactId>
    <!-- 내부적으로 commons-logging 사용 -->
    <!-- SLF4J가 호환성을 제공 -->
</dependency>
```
> **3. 성능 최적화**
> * 불필요한 문자열 생성 방지, 가비지 컬렉션 감소
```java
// Parameterized Logging - 효율적인 포맷팅
// 문자열 연결이 아니라 배열 기반 처리
logger.debug("Processing file: {} with size: {} bytes", 
    fileName, fileSize);

// 내부적으로:
// 1. 레벨 체크 (TRACE/DEBUG가 비활성화면 String.format 실행 안 함)
// 2. MessageFormatter.format()만 호출
// 3. 불필요한 객체 생성 최소화
```
> **4. 추상화 레이어 오버헤드**
> * 극도로 높은 처리량이 필요한 경우 미세한 성능 차이 발생
```java
// SLF4J는 Facade이므로 한 단계 더 거침
// LogBack 직접 사용 vs SLF4J + LogBack

// SLF4J 호출 흐름
logger.info(...) 
  → LoggerFactory.getLogger()
  → ILoggerFactory (구현체 찾음)
  → 실제 Logger (LogBack)
```
> **5. 기능 제한**
> * 고급 설정이 필요하면 구현체 의존 코드 필요
```java
// SLF4J는 최소한의 기능만 제공
// 구현체의 고급 기능을 직접 사용할 수 없음

// ❌ 불가능
logger.setAdditivity(false);

// ✅ 가능 (LogBack 직접 접근)
LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
ch.qos.logback.classic.Logger logger = 
    loggerContext.getLogger("com.example");
logger.setAdditivity(false);
```
> **6. 에러 처리 모호함**
> * 예외 처리 전략이 구현체에 따라 달라진다.
```java
// SLF4J는 에러 처리를 구현체에 위임
try {
    logger.info("메시지");
} catch (Exception e) {
    // 어떤 구현체를 사용하느냐에 따라 다른 예외 발생 가능
}
```

> [📚Logback](https://logback.qos.ch/documentation.html)
>
> * Slf4j의 공식 구현체
> * Log4j 개발자가 만들어 더 빠르고 메모리 효율적
> * Spring Boot의 기본 로깅 라이브러리
> **1. 높은 성능**
> * Log4j 대비 10배 빠른 필터링, 메모리 효율적인 구조, 낮은 CPU 사용률
>
> **2. 자동 리로딩**
> * 개발 중 설정 변경 후 즉시 반영, 프로덕션 다운타임 최소화
```xml
<!-- logback-spring.xml -->
<!-- automatic=true로 설정 시 파일 변경 자동 감지 -->
<configuration>
    <property resource="application.properties"/>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}</file>
    </appender>
</configuration>

<!-- 파일 변경 시 애플리케이션 재시작 없이 자동 적용 -->
```
> **3. 스프링 부트 통합**
> * Zero-Config 기본 설정, Spring 환경과 완벽 호환
```xml
// Spring Boot는 기본적으로 LogBack 포함
// pom.xml이나 build.gradle에 명시하지 않아도 자동 설정

// application.properties 통합
logging.level.root=INFO
logging.level.com.example=DEBUG
logging.file=logs/app.log

// logback-spring.xml로 더 세밀한 제어
<springProfile name="prod">
    <property name="LOG_LEVEL" value="WARN"/>
</springProfile>
```
> **4. Slf4j 의존성 필수**
> * 라이브러리 수 증가, 버전 관리 필요
```xml
<!-- LogBack 단독 사용 불가능 -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <!-- 내부적으로 slf4j-api 의존 -->
</dependency>

<!-- 결과: 2개 라이브러리 필요 -->
```
> **5. 로깅 구현체에 종속**
> * Slf4j의 주요 장점(구현체 변경 유연성) 상실
```java
// LogBack 특화 기능을 사용하면 변경 어려움
LoggerContext loggerContext = 
    (LoggerContext) LoggerFactory.getILoggerFactory();
// ↑ LogBack 인스턴스 직접 접근
// 이 코드는 다른 구현체(Log4j 등)로 변경 불가능
```
> **6. 메모리 누수 위험**
> * 개발자 실수 시 메모리 누수 발생 가능성
```java
// MDC 정리 안 하면 메모리 누수
MDC.put("userId", user.getId());
logger.info("User action");
// MDC.clear(); 안 하면 스레드풀에서 스레드 재사용 시 데이터 유지됨

// 비동기 Appender 종료 안 하면 스레드 남음
AsyncAppender asyncAppender = new AsyncAppender();
// 애플리케이션 종료 시 명시적으로 종료해야 함
```

#### 매핑 정보

* @RestController vs @Controller
  * @RestController의 경우 : 반환 값으로 뷰를 찾는 것이 아니라 HTTP 메시지 바디에 바로 입력한다.
  * @Controller의 경우 : 반환 값이 String이면 뷰를 찾아 뷰가 렌더링된다.

#### HTTP 메시지 컨버터

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7717a13d-5688-4d75-9178-231eab42c526.png)

* `@ResponseBody`를 사용
  * HTTP BODY에 문자 내용을 직접 반환한다. 
  * 뷰 리졸버 대신에 `HttpMessageConverter`가 동작한다.
  * 기본 문자 처리 : `StringHttpMessageConverter`
  * 기본 객체 처리 : `MappingJackson2HttpMessageConverter`

```java
// HttpMessageConverter 인터페이스는 HTTP 요청, HTTP 응답 둘 다 사용된다.
public interface HttpMessageConverter<T> {

    /**
     * Indicates whether the given class can be read by this converter.
     * @param clazz the class to test for readability
     * @param mediaType the media type to read (can be {@code null} if not specified);
     * typically the value of a {@code Content-Type} header.
     * @return {@code true} if readable; {@code false} otherwise
     */
    boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);

    /**
     * Indicates whether the given class can be written by this converter.
     * @param clazz the class to test for writability
     * @param mediaType the media type to write (can be {@code null} if not specified);
     * typically the value of an {@code Accept} header.
     * @return {@code true} if writable; {@code false} otherwise
     */
    boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);

    /**
     * Return the list of media types supported by this converter. The list may
     * not apply to every possible target element type and calls to this method
     * should typically be guarded via {@link #canWrite(Class, MediaType)
     * canWrite(clazz, null}. The list may also exclude MIME types supported
     * only for a specific class. Alternatively, use
     * {@link #getSupportedMediaTypes(Class)} for a more precise list.
     * @return the list of supported media types
     */
    List<MediaType> getSupportedMediaTypes();

    /**
     * Return the list of media types supported by this converter for the given
     * class. The list may differ from {@link #getSupportedMediaTypes()} if the
     * converter does not support the given Class or if it supports it only for
     * a subset of media types.
     * @param clazz the type of class to check
     * @return the list of media types supported for the given class
     * @since 5.3.4
     */
    default List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
        return (canRead(clazz, null) || canWrite(clazz, null) ?
                getSupportedMediaTypes() : Collections.emptyList());
    }

    /**
     * Read an object of the given type from the given input message, and returns it.
     * @param clazz the type of object to return. This type must have previously been passed to the
     * {@link #canRead canRead} method of this interface, which must have returned {@code true}.
     * @param inputMessage the HTTP input message to read from
     * @return the converted object
     * @throws IOException in case of I/O errors
     * @throws HttpMessageNotReadableException in case of conversion errors
     */
    T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException;

    /**
     * Write a given object to the given output message.
     * @param t the object to write to the output message. The type of this object must have previously been
     * passed to the {@link #canWrite canWrite} method of this interface, which must have returned {@code true}.
     * @param contentType the content type to use when writing. May be {@code null} to indicate that the
     * default content type of the converter must be used. If not {@code null}, this media type must have
     * previously been passed to the {@link #canWrite canWrite} method of this interface, which must have
     * returned {@code true}.
     * @param outputMessage the message to write to
     * @throws IOException in case of I/O errors
     * @throws HttpMessageNotWritableException in case of conversion errors
     */
    void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException;

}
```

#### 요청 매핑 핸들러 어댑터 구조

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/70186acb-bf4f-46e5-bedd-3f78ef6206d4.png)

**1. 파라미터 해석**

* 컨트롤러 메서드에서 정의된 다양한 파라미터(@RequestParam, @RequestBody 등)를 처리하기 위해 `HandlerMethodArgumentResolver` 전략을 사용한다.
* 등록된 리스트를 순회하면서 `supportsParameter()`가 true인 리졸버를 찾아 실제 데이터를 바인딩한다.

**2. 반환 값 처리**

* 메서드 실행 후 `ModelAndView`, `ResponseEntity`, `@ResponseBody` 등 응답형식에 맞게 변환하기 위해 `HandlerMethodReturnValueHandler`를 사용한다.

**3. 메시지 컨버팅**

* `@RequestBody`나 `@ResponseBody`가 사용될 경우, 내부적으로 `HttpMessageConverter`를 호출하여 JSON, XML 등의 데이터를 객체로 변환하거나 그 반대로 수행한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/016d2cc2-ff93-4ada-9585-e0e7977d1d25.png)
