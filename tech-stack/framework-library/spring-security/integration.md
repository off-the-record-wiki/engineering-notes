# Spring Security - Integration

### Servlet API 통합

* Spring Security는 다양한 프레임워크 및 API와의 통합을 제공하고 있으며, Servlet 3과 Spring MVC와 통합을 통해 편리한 기능들을 사용할 수 있다.
* 인증 관련 기능들을 필터가 아닌 서블릿 영역에서도 처리할 수 있다.

#### SecurityContextHolderAwareRequestFilter

* HTTP 요청이 처리될 때 `HttpServletRequest`에 보안 관련 메서드를 추가적으로 제공하는 래퍼(Wrapper) 클래스를 적용한다.
* 이를 통해 개발자는 서블릿 API의 보안 메서드를 사용하여 인증, 로그인, 로그아웃 등의 작업을 수행할 수 있다.

#### HttpServlet3RequestFactory

* Servlet 3 API와의 통합을 위해 `Servlet3SecurityContextHolderAwareRequestWrapper` 객체를 생성한다.

#### Servlet3SecurityContextHolderAwareRequestWrapper

* `HttpServletRequest`의 래퍼 클래스로서 Servlet 3.0 기능을 지원하면서 동시에 `SecurityContextHolder`와의 통합을 제공한다.
* 이 래퍼를 사용함으로써 `SecurityContext`에 접근할 수 있고 Servlet 3.0 비동기 처리와 같은 기능을 사용하는 동안 보안 컨텍스트를 올바르게 관리할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8de83ba5-cdb2-4457-b504-ab9f374de9cf.png)

### @AuthenticationPrincipal

* Spring Security는 Spring MVC 인수에 대한 현재 `Authentication.getPrincipal()`을 자동으로 해결할 수 있는 `AuthenticationPrincipalArgumentResolver`를 제공한다.
* Spring MVC에서 `@AuthenticationPrincipal`을 메서드 인수에 선언하게 되면 Spring Security와 독립적으로 사용 가능하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/16c84057-6ffa-41a5-9fa3-46dc8b7d88c8.png)

#### @AuthenticationPrincipal(expression="표현식")

* principal 객체 내부에서 특정 필드나 메서드에 접근하고자 할 때 사용할 수 있으며 사용자 세부 정보가 principal 내부의 중첩된 객체에 있는 경우 유용하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/43520e1e-7d24-4fc9-9d35-ad53a389ad27.png)
![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c297afda-ccb0-492c-941a-c1a244dad3c7.png)

* `@AuthenticationPrincipal`을 자체 주석으로 메타 주석화하여 Spring Security에 대한 종속성을 제거할 수도 있다.

### WebAsyncManagerIntegrationFilter

* Spring Security는 Spring MVC Controller에서 Callable을 실행하는 비동기 쓰레드에 SecurityContext를 자동으로 설정하도록 지원한다.
* Spring Security는 `WebAsyncManager`와 통합해 SecurityContextHolder에서 사용 가능한 SecurityContext를 Callable에서 접근가능하도록 지원해준다.
* `WebAsyncManagerIntegrationFilter`
  * `SecurityContext`와 `WebAsyncManger` 사이의 통합을 제공하며 `WebAsyncManager`를 생성하고 `SecurityContextCallableProcessingInterceptor`를 `WebAsyncManager`에 등록한다.
* `WebAsyncManager`
  * 쓰레드 풀의 비동기 쓰레드를 생성하고 Callable를 받아 실행시키는 주체로서 등록된 `SecurityContextCallableProcessingInterceptor`를 통해 현재 쓰레드가 보유하고 있는 SecurityContext 객체를 비동기 쓰레드의 Local Thread에 저장시킨다.
