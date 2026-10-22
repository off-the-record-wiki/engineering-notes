# Spring Security - Authorization Architecture

### Authorization

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a21412dc-f206-42a0-8d7a-2fb019bc3a96.png)

* 인가 즉, 권한 부여는 특정 자원에 접근할 수 있는 사람을 결정하는 것을 의미한다.
* Spring Security는 `GrantedAuthority` 클래스를 통해 권한 목록을 관리하고 있으며, 사용자의 `Authentication` 객체와 연결된다.

#### [GrantedAuthority](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-granted-authority)

* Spring Security는 `Authentication` 객체에 `GrantedAuthority` 권한 목록을 저장하며 이를 통해 인증 주체에게 부여된 권한을 사용하도록 한다.
* `GrantedAuthority` 객체는 `AuthenticationManager`에 의해 `Authentication` 객체에 삽입되며 Spring Security는 인가 결정을 내릴 때, `AuthenticationManager`를 사용해 `Authentication` 즉, 인증 주체로부터 `GrantedAuthority` 객체를 읽어들여 처리한다.
* 구현체는 하나로 `SimpleGrantedAuthority`가 있다.

#### 사용자 정의 역할 접두사 - `ROLE_`

* 기본적으로 역할 기반 인가 규칙은 역할 앞에 `ROLE_`를 접두사로 사용한다. 즉, "USER" 역할을 가진 보안 컨텍스트가 필요한 인가 규칙이 있다면 Spring Security는 기본적으로 "ROLE_USER"를 반환하는 GrantedAuthority를 찾게 된다.
* `GrantedAuthorityDefaults`로 사용자 지정할 수 있으며 `GrantedAuthorityDefaults`는 역할 기반 인가 규칙에 사용할 접두사를 사용자 정의하는데 사용된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6a303fd9-281f-442f-8307-68da6358ab7e.png)

### AuthorizationManager

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/401e5240-40c6-4d09-a683-cc13c4de2a95.png)

* `AuthorizationManager`는 인증된 사용자가 요청 자원에 접근할 수 있는가를 결정할 수 있는 인터페이스로서 인증된 사용자의 권한 정보와 요청 자원 보안 요구사항 기반으로 권한 부여 결정을 내린다.
* `AuthorizationManager`는 Spring Security 요청 기반, 메서드 기반 인가 구성 요소에서 호출되며 최종 액세스 제어 결정을 수행한다.
* `AuthorizationManager`는 Spring Security 필수 구성 요소로 권한 부여 처리는 `AuthorizationFilter`를 통해 이루어지며 `AuthorizationFilter`는 `AuthorizationManager`를 호출해 권한 부여 결정을 내리게 된다.
  * `check()` : 권한 부여 결정을 내릴 때 필요한 모든 관련 정보가 전달된다. 액세스가 허용되면 `true`를 포함하는 `AuthorizationDecision`, 거부되면 `false`를 포함하는 `AuthorizationDecision`, 결정을 내릴 수 없는 경우에는 null을 반환한다.
  * `verify()` : check를 호출해서 반환된 값이 `false`를 가진 `AuthorizationDecision`인 경우 `AccessDeniedException`을 던진다.

### AuthorityAuthorizationManager 클래스

* Spring Security는 요청 기반의 인증된 사용자 및 특정 권한을 가진 사용자의 자원접근 허용 여부를 결정하는 인가 관리자 클래스들을 제공한다.
* 대표적으로 `AuthorityAuthorizationManager`, `AuthenticatedAuthorizationManager`, 그리고 요청을 위임하는 `RequestMatcherDelegatingAuthorizationManager`가 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a0560663-dac4-4e25-92f5-b2b73fcb0560.png)

#### [AuthenticatedAuthorizationManager](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#authz-authenticated-authorization-manager)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d57d7aab-e175-478a-a428-616fa7a7612f.png)

#### [AuthorityAuthorizationManager](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#authz-authority-authorization-manager)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0c66511c-e85c-4c38-83a5-f9db5d07f80a.png)

* `AuthorityAuthorizationManager`는 매핑된 요청 패턴과 권한 정보를 사용해 사용자 요청 정보와 `Authentication` 권한을 비교해서 서로 일치하는가를 검사한다.

### PreAuthorizeAuthorizationManager 클래스

* Spring Security는 메서드 기반 인증된 사용자 및 특정 권한을 가진 사용자의 자원 접근 허용 여부를 결정하는 인가 관리자 클래스들을 제공한다.
* `PreAuthorizationManager`, `PostAuthorizeAuthorizationManager`, `Jsr250AuthorizationManager`, `SecuredAuthorizationMaanger`가 있다.
* 메서드 기반 권한 부여는 내부적으로 AOP 방식에 의해 초기화 설정이 이루어지며 메서드 호출을 `MethodInterceptor`가 가로채어 처리하고 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cd3c3512-827c-4040-953c-ec2f22da3ecb.png)

* 스프링 컨테이너가 초기화 시, 생성되는 전체 빈을 검사하면서 빈이 가진 메서드 중에서 보안이 설정된 메서드가 있는지를 탐색한다.
* 보안이 설정된 메서드가 있다면 스프링은 그 빈의 프록시 객체를 자동으로 생성해준다.
* 보안이 설정된 메서드에는 인가 처리 기능을 하는 Advice를 등록한다.
* 스프링은 기본적으로 빈 참조시 실제 빈이 아닌 프록시 객체를 참조하도록 한다.
* 초기화 과정이 종료된다.
* 사용자는 프록시 객체를 통해 메서드를 호출하고 프록시 객체는 Advice로 등록된 메서드가 있다면 호출해 작동시킨다. 이 때, 호출 시점에 실제 빈 객체의 메서드가 호출된다.
* Advice는 메서드 진입 전 인가 처리를 하게 되고 인가 처리가 승인되면 실제 객체 메서드를 호출하게 되고 인가 처리가 거부되면 예외가 발생하고 메서드 진입에 실패하게 된다.

#### Method Interceptor 구조

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/635e8df9-5c21-415e-a1df-73618336c83c.png)

### @PreAuthorize 처리 구조

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bf6a3e4e-4816-4940-ab0d-d848f8665c67.png)

### @PostAuthorize 처리 구조

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ea5aa62f-8962-4315-9b2c-03df75e956ac.png)
