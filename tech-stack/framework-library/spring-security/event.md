# Spring Security - Event

### 인증 이벤트 - Authentication Events

* Spring Security는 인증이 성공하거나 실패하게 되면 `AuthenticationSuccessEvent` 또는 `AuthenticationFailureEvent`를 발생시킨다.
* 이벤트를 수신하려면 `ApplicationEventPublisher`를 사용하거나 시큐리티에서 제공하는 `AuthenticationEventPublihser`를 사용해서 발행해야 한다.
* `AuthenticationEventPublisher`의 구현체로 `DefaultAuthenticationEventPublisher`가 제공된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/932324c8-2f50-44f4-bebb-d3ef4f900e82.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/39e4a7a5-93a7-45a3-8ecb-71e53cbfe586.png)

### 인증 이벤트 - AuthenticationEventPublisher 활용

* `AuthenticationException`이 발생했을 때 해당 예외에 매핑된 이벤트가 발행이 안되어 있을 경우 기본 `AuthenticationFailureEvent`를 발행 및 수신할 수 있다.

### 인가 이벤트 - Authorization Events

* Spring Security는 권한 부여 이벤트 처리를 지원하며 권한이 부여되거나 거부된 경우에 발생하는 이벤트를 수신할 수 있다.
* 이벤트를 수신하려면 `ApplicationEventPublisher`를 사용하거나 시큐리티에서 제공하는 `AuthorizationEventPublisher`를 사용한다.
* `AuthorizationEventPublisher`의 구현체로 `SpringAuthorizationEventPublisher`가 제공된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/50e3667b-ff98-473e-918f-d9eb1317f166.png)

* 위와 같이 인가 이벤트를 발행하기 위해서는 `SpringAuthorizationEventPublisher`를 빈으로 등록해야 한다.
