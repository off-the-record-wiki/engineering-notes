# Spring Security - Authentication Architecture

### 인증 아키텍처

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e8b494ea-79ee-40ee-b1b8-5d1963fd04ba.png)

#### [인증 - Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/index.html#page-title)

* `Authentication`은 특정 자원에 접근하려는 사람의 신원을 확인하는 방법을 의미한다.
* 사용자 인증의 일반적인 방법은 사용자 이름과 비밀번호를 입력하게 하는 것으로서 인증이 수행되면 신원을 알고 권한 부여를 할 수 있다.
* `Authentication`은 사용자 인증 정보를 저장하는 토큰 개념 객체로 활용되며 인증 이후 `SecurityContext`에 저장되어 전역적으로 참조가 가능하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fcf1cab7-6c52-440d-9a88-b424519a79a7.png)

* `getAuthorities()` : 인증 주체에게 부여된 권한
* `getCredentials()` : 인증 주체가 올바른 것을 증명하는 자격 증명으로서 대개 비밀번호를 의미
* `getDetails()` : 인증 요청에 대한 추가적인 세부 사항을 저장

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/783f29c6-8bd1-42ce-b7d9-3a5f561b6b7e.png)

* `getPrincipal()` : 인증 주체를 의미하며 인증 요청의 경우 사용자 이름을, 인증 후에는 UserDetails 타입의 객체가 된다.
* `isAuthenticated()` : 인증 상태 반환
* `setAuthenticated(boolean)` : 인증 상태를 설정

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fda3a10c-85f8-43e7-bbf7-dbd348306303.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a2e1c726-a000-4199-b099-06aba71e8611.png)

* 공식문서의 내용을 미루어 보았을 때, **ROLE Based Authority**에서는 접두사 `ROLE_`가 기본적으로 붙게 된다.

#### [인증 컨텍스트 - SecurityContextHolder](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-securitycontextholder)

* **SecurityContext**
  * `Authentication` 저장 - 현재 인증된 사용자의 `Authentication` 객체를 저장한다.
  * ThreadLocal 저장소 사용 - `SecurityContextHolder`를 통해 접근되며 ThreadLocal 저장소를 사용해 각 쓰레드가 자신만의 보안 컨텍스트를 유지할 수 있다.
  * 애플리케이션 전반에 걸친 접근성 - 애플리케이션 어느 곳에서나 접근 가능하며, 현재 사용자 인증 상태나 권한 상태를 확인하는 데 사용된다.

* **SecurityContextHolder**
  * `SecurityContext` 저장 - 현재 인증된 사용자의 `Authentication` 객체를 담고 있는 `SecurityContext` 객체를 저장한다.
  * 전략 패턴 사용 - 다양한 저장 전략을 지원하기 위해 `SecurityContextHolderStrategy` 인터페이스를 사용한다.
  * 기본 전략 - `MODE_THREADLOCAL`
  * 전략 모드 직접 지정도 가능 - `SecurityContextHolder.setStrategyName(String)`

* **SecurityContextHolder 저장 모드**
  * `MODE_THREADLOCAL` : 기본 모드로, 각 쓰레드가 독립적인 보안 컨텍스트를 가진다. 대부분의 서버 환경에 적합하다.
  * `MODE_INHERITABLETHREADLOCAL` : 부모 쓰레드로부터 자식 쓰레드로 보안 컨텍스트가 상속되며, 작업을 쓰레드 간 분산 실행하는 경우 유용할 수 있다.
  * `MODE_GLOBAL` : 전역적으로 단일 보안 컨텍스트를 사용하며 서버 환경에서는 부적합하며 간단한 애플리케이션에서나 적합하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d7c163c9-d720-485f-85b5-dc2f8c8af200.png)

* `SecurityContext`를 참조 -> `SecurityContextHolder.getContextHolderStrategy().getContext()`
* `SecurityContext`를 제거 -> `SecurityContextHolder.getContextHolderStrategy().clearContext()`
>❗쓰레드마다 할당되는 전용 저장소에 `SecurityContextHolder`를 저장하기 때문에 동시성 문제가 없다.<br>
>❗쓰레드 풀에서 운용되는 쓰레드일 경우 새로운 요청이더라도 기존의 쓰레드 풀에서 재사용될 수 있기 때문에 클라이언트로 응답 직전에 `SecurityContext`를 항상 삭제해주고 있다.<br>

#### [인증 관리자 - AuthenticationManager](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-authenticationmanager)

* **AuthenticationManager**
  * 인증 필터로부터 `Authentication` 객체를 전달받아 인증을 시도해 인증에 성공할 경우 사용자 정보, 권한 등을 포함한 완전히 채워진 `Authentication` 객체를 반환한다.
  * `AuthenticationManager`는 여러 `AuthenticationProvider`들을 관리하며 `AuthenticationProvider` 목록을 순차적으로 순회하며 인증 요청을 한다.
  * `AuthenticationProvider` 목록 중에서 인증 처리 요건에 맞는 적절한 `AuthenticationProvider`를 찾아 인증 처리를 위임한다.
  * `AuthenticationManagerBuilder`에 의해 객체가 생성되며 주로 사용하는 구현체는 `ProviderManager`가 있다.

* **AuthenticationManagerBuilder**
  * `AuthenticationManager` 객체를 생성하며, `UserDetailsService` 및 `AuthenticationProvider`를 추가할 수 있다.
  * `HttpSecurity.getSharedObject(AuthenticationManagerBuilder.class)` 를 통해 객체를 참조할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/87c1e491-aa78-488b-a5db-45d3dd98c124.png)

* AuthenticationManager가 여러 AuthenticationProvider 목록들을 가지고 적합한 곳에 요청 처리를 위임한다.
* 만약 요청 처리를 위임할 수 있는 provider가 없다면 ProviderNotFoundException 예외가 발생하면서 인증에 실패한다.
>❗`HttpSecurity`를 사용할 경우 `build()`는 최초 한 번만 호출되어야 하고 그 이후에는 `getObject()`로 참조해야 한다. 그 이유는 `build()`가 호출되면 내부적으로 여러 Security Filter들이 중복 등록되거나 설정이 개발자가 의도한대로 동작하지 않을 수 있기 때문이다.

#### [인증 제공자 - AuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-authenticationprovider)

* 사용자 자격증명을 확인하고 인증 과정을 관리하는 클래스로서 사용자가 시스템에 액세스하기 위해 제공한 정보가 유효한가를 판단한다.
* 다양한 유형의 인증 메커니즘을 지원할 수 있는데 예를 들어 표준 사용자 이름과 비밀번호를 기반으로 하는 인증, 토큰 기반 인증, 지문 인식 등을 처리할 수 있다.
* 성공적인 인증 후에는 `Authentication` 객체를 반환하며 이 객체는 사용자 신원 정보와 인증된 자격증명을 포함한다.
* 인증 과정 중에 문제가 발생하면 `AuthenticationException` 예외를 발생시켜 문제를 알리는 역할을 수행한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ed18b614-b3ad-4b8c-ad31-fc5578f86e9e.png)

#### [사용자 상세 - UserDetails](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details.html#page-title)

* 사용자 기본 정보를 저장하는 인터페이스로서 `Spring Security`에서 사용하는 사용자 타입이다.
* 저장된 사용자 정보는 추후에 인증 절차에서 사용되기 위해 `Authentication` 객체에 포함되며 구현체로서 `User` 클래스가 제공된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6b9f96a8-20dc-47ec-9f5e-38f47a6b7ed9.png)

* `isCredentialsNonExpired()` : 사용자 비밀번호가 유효 기간이 지났는지를 확인하며 유효 기간이 지난 비밀번호는 인증할 수 없다.
* `isAccountNonExpired()` : 사용자 계정이 유효 기간이 지났는지를 나타내며 기간이 만료되었다면 인증할 수 없다.
* `getUsername()` : 사용자 인증에 사용된 사용자 이름을 반환하며 null을 반환할 수 없다.
* `isAccountNonLocked()` : 사용자가 잠겨있는지 아닌지를 나타내며 잠긴 사용자는 인증이 불가능하다.
* `getPassword()` : 사용자 인증에 사용된 비밀번호를 반환한다.
* `isEnabled()` : 사용자가 활성화되었는지를 나타내며 비활성화된 사용자는 인증할 수 없다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/979953c4-e879-490a-9c14-08ce4522cd76.png)

#### [사용자 상세 서비스 - UserDetailsService](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details-service.html#page-title)

* `UserDetailsService`의 주요 기능은 사용자와 관련된 상세 데이터를 로드하는 것이며, 사용자의 신원, 권한, 자격 증명 등과 같은 정보를 포함할 수 있다.
* 이 인터페이스를 사용하는 클래스는 주로 `AuthenticationProvider`이며, 사용자가 시스템에 존재하는지 여부와 사용자 데이터를 검색하고 인증 과정을 수행한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/88a3198f-038c-482f-a20e-213e34ed78aa.png)
