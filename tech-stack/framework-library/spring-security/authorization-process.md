# Spring Security - Authorization Process

### 요청 기반 권한 부여 - HttpSecurity.authorizeHttpRequests()

* Spring Security는 요청 기반 권한 부여와 메서드 기반 권한 부여를 통해 리소스에 대한 심층적인 방어를 제공한다.
* 요청 기반 권한 부여는 클라이언트 측 요청 즉, `HttpServletRequest`에 대한 권한 부여를 말한다.
* `authorizeHttpRequests()`는 사용자 자원 접근을 위한 요청 엔드포인트와 접근에 필요한 권한을 매핑시키기 위한 규칙이다. 엔드포인트에 접근하려면 `authorizeHttpRequests()`에 해당 규칙들이 모두 포함되어야 한다.
* `authorizeHttpRequests()`을 통해 요청과 권한 규칙이 설정되면 내부적으로 `AuthorizationFilter`가 요청에 대한 권한 작업을 수행한다.

#### [requestMatchers()](https://docs.spring.io/spring-security/reference/5.8/migration/servlet/config.html#use-new-requestmatchers)

* `requestMatchers` 메소드는 HTTP 요청의 URL 패턴, HTTP 메소드, 요청 파라미터 등을 기반으로 어떤 요청에 대해서는 특정 보안 설정을 적용하고 다른 요청에 대해서는 적용하지 않도록 세밀하게 제어할 수 있게 해준다.
* Spring Security는 클라이언트 요청에 대해 위에서부터 아래로 나열된 순서대로 처리하며 요청에 대해 첫 번째 일치만 적용된다.
* 예를 들어, `/admin/**`가 `/admin/db` 요청을 포함하므로 의도한 대로 권한 규칙이 올바르게 적용되지 않을 수 있다. 그렇기 때문에 엔드포인트를 정의할 때 좁은 범위의 경로를 먼저 정의하고 이후에 큰 범위 경로를 정의해야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0a94de46-2f8d-4053-a602-2c74a3fc2f00.png)

* 권한 규칙은 내부적으로 `AuthorizationManager` 클래스에 의해 재구성되며 모든 요청은 여러 종류의 `AuthorizationMaanger`에 의해 승인 혹은 거부된다.

### 표현식 및 커스텀 권한 구현 방법

* Spring Security는 표현식을 사용해서 권한 규칙을 설정하도록 `WebExpressionAuthorizationMaanger`를 제공한다.
* 표현식은 시큐리티가 제공하는 권한 규칙을 사용하거나 사용자가 표현식을 커스텀으로 구성할 수 있다.

```java
requestMatchers().access(new WebExpressionAuthorizationManager("expression"))
```

### 요청 기반 권한 부여 - HttpSecurity.securityMatcher()

* `securityMatcher`는 특정 패턴에 해당하는 요청에만 보안 규칙을 적용할 수 있도록 설정하는 것으로 중복해서 정의하면 마지막에 설정한 것으로 대체한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d52dc84a-11b8-400b-a04f-6a290cb4f3b1.png)

* `securityMatchers` 메소드는 특정 패턴에 해당하는 요청을 단일이 아닌 다중 설정으로 구성해서 보안 규칙을 적용할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bc728e2f-c984-4eb3-a90c-2d3451bcf95d.png)

### 메서드 기반 권한 부여 - @PreAuthorize, @PostAuthorize

* Spring Security는 요청 수준 권한뿐만 아니라 메서드 수준에서의 권한 부여도 지원한다.
* 메서드 수준 권한 부여를 활성화하려면 설정 클래스에 `@EnableMethodSecurity` 어노테이션을 추가해야 한다.
* SpEL(Spring Expression Language) 표현식을 사용해 다양한 보안 조건을 정의할 수 있다.

#### [@PreAuthorize](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-preauthorize)

* 메서드가 실행되기 전에 특정 보안 조건이 충족되는지 확인하는 데 사용되며 보통 서비스 또는 컨트롤러 메서드에 적용되어 해당 메서드가 호출되기 전에 사용자 인증 정보와 권한을 검사한다.

#### [@PostAuthorize](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-preauthorize)

* `@PreAuthorize` 어노테이션과 달리 메서드가 실행된 후 보안 검사를 수행하는데 사용된다.
* 메서드 실행 후 결과에 대한 보안 조건을 검사해 특정 조건을 만족하는 경우에만 사용자가 결과를 받을 수 있도록 한다.

### 메서드 기반 권한 부여 - @PreFilter, @PostFilter

#### [@PreFilter](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-prefilter)

* 메서드가 실행되기 전에 메서드에 전달된 컬렉션 타입의 파라미터에 대한 필터링을 수행하는데 사용된다.
* 주로 사용자가 보낸 컬렉션 내의 객체들을 특정 기준에 따라 필터링하고 그 중 보안 조건을 만족하는 객체들에 대해서만 메서드가 처리하도록 할 때 사용한다.

#### [@PostFilter](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-postfilter)

* 메서드가 반환하는 컬렉션 타입의 결과에 대해 필터링을 수행하는 데 사용된다.
* 메서드가 컬렉션을 반환할 때 반환되는 각 객체가 특정 보안 조건을 충족하는지 확인하고 조건을 만족하지 않는 객체들을 결과에서 제거한다.

### 메서드 기반 권한 부여 - @Secured, JSR-250 부가 기능

#### [@Secured](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-secured)

* `@Secured` 어노테이션을 메서드에 적용하면 지정된 권한을 가진 사용자만 해당 메서드를 호출할 수 있으며 더 풍부한 형식을 지원한다.
* `@Secured` 어노테이션을 사용하려면 Spring Security 설정에서 `@EnableMethodSecurity(securedEnabled=true)` 설정을 활성화해야한다.

#### [JSR-250](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-jsr250)

* `JSR-250` 기능을 적용하면 `@RolesAllowed`, `@PermitAll` 및 `@DenyAll` 어노테이션 보안 기능이 활성화된다.
* `JSR-250` 어노테이션을 사용하려면 Spring Security 설정에서 `@EnableMethodSecurity(jsr250Enabled=true)` 설정을 활성화해야한다.
* 기본적으로 모든 메서드는 클래스 수준의 권한 처리 동작을 상속받는다.
* 메서드에 어노테이션을 선언한 메서드는 클래스 수준의 어노테이션을 덮어쓰게 된다.
* 인터페이스에 동일한 규칙이 적용되지만 클래스가 2개의 다른 인터페이스로부터 동일한 메서드의 어노테이션을 상속받는 경우에는 시작할 때 실패한다. 그래서 구체적인 메서드에 어노테이션을 추가해 모호함을 해결할 수 있다.

### 정적 자원 관리 방법

* Spring Security에서 `RequestMatcher` 인스턴스를 등록해서 무시해야 할 요청을 지정할 수 있다.
* 주로 정적 자원에 대한 요청이나 특정 엔드포인트가 보안 필터를 거치지 않도록 설정해야할 때 사용된다.
* [Ignoring보다 permitAll을 권장한다.](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#favor-permitall)
* 이전에는 모든 요청마다 세션을 확인해야해서 성능 저하가 수반되었으나 Spring Security 6.x부터 권한 부여 규칙에서 필요한 경우를 제외하고는 세션을 확인하지 않는다.
* 성능 문제가 해결되었기 때문에 모든 요청에 대해서 `permitAll`을 사용할 것을 권장하며 정적 자원에 대한 요청일지라도 안전한 헤더를 작성할 수 있어 더 안전하다.

### 계층적 권한 - RoleHireachy

* 기본적으로 Spring Security에서 권한과 역할은 계층적이거나 상하 관계로 구분되지 않는다. 그래서 인증 주체가 다양한 역할과 권한을 부여 받아야만 한다.
* RoleHireachy는 역할 간 계층 구조를 정의하고 관리하는 데 사용되며 보다 간편하게 역할 간 계층 구조를 설정하고 이를 기반으로 사용자의 액세스 규칙을 정의할 수 있다.
* 계층적 역할을 사용하면 액세스 규칙이 크게 줄어들 뿐만 아니라 더 간결하고 우아한 형태로 규칙을 표현할 수 있다.
* 허나 만약 세분화된 권한 배분이 필요하다면 해당 방식은 적합하지 않을 수 있다.

```java
@Bean
static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > permission:read");
}
```

* `setHirerachy` : 역할 계층을 설정하고 각 역할에 대해 해당 역할의 하위 계층에 속하는 모든 역할 집합을 미리 정해놓는다.
  * 역할 계층 예시 : ROLE_A > ROLE_B > ROLE_C
* `getReachableGrantedAuthorities`
  * 모든 도달 가능한 권한의 배열을 반환한다.
  * 도달 가능한 권한은 직접 할당된 권한에 더해 역할 계층에서 이들로부터 도달 가능한 모든 권한을 의미한다.
  * 예시 : 직접 할당된 권한이 ROLE_B라면 도달 가능한 권한은 ROLE_B, ROLE_C이다.
