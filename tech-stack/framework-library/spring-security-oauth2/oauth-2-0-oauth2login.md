# Spring Security - OAuth 2.0 oauth2Login

#### OAuth2LoginConfigurer 초기화 이해

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/527a77dd-eb72-493e-80c8-ca157c32e65e.png)

* `AuthorizationEndpointConfig` : 브라우저를 인가서버로 보낸다.(프론트 채널)
* `RedirectionEndpointConfig` : 코드를 들고 돌아오는 지점
* `TokenEndpointConfig` : 코드를 토큰으로 바꾼다.(백 채널)
* `UserInfoEndpointConfig` : 토큰으로 사용자를 가져와 권한화한다.

#### OAuth 2.0 Login Page 생성 방법

```java
public static final String DEFAULT_LOGIN_PAGE_URL = "/login";
public static final String ERROR_PARAMETER_NAME = "error";

public DefaultLoginPageGeneratingFilter(UsernamePasswordAuthenticationFilter authFilter) {
    this.loginPageUrl = DEFAULT_LOGIN_PAGE_URL;
    this.logoutSuccessUrl = DEFAULT_LOGIN_PAGE_URL + "?logout";
    this.failureUrl = DEFAULT_LOGIN_PAGE_URL + "?" + ERROR_PARAMETER_NAME;
    if (authFilter != null) {
        initAuthFilter(authFilter);
    }
}
```
* 기본적으로 OAuth 2.0 로그인 페이지는 `DefaultLoginPageGeneratingFilter`가 자동으로 생성해준다.
* 디폴트 로그인 페이지를 재정의하는 것도 가능하다.

#### Authorization Code 요청

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/dd737e84-2a61-4d5e-80ee-089e95271f9d.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e1255c06-23ad-4d42-bfc7-bfa1b1f54e58.png)

* 클라이언트는 사용자 브라우저를 통해 인가 서버의 권한 부여 엔드포인트로 리다이렉션하여 권한 코드 부여 흐름을 시작하게 된다.
* `DefaultOAuth2AuthorizationRequestResolver` : 웹 요청에 대해 `OAuth2AuthorizationRequest` 객체를 최종 완성한다. 여기서 `registrationId`를 추출하고 이를 사용해서 `ClientRegistration`을 가져와 `OAuth2AuthorizationRequest`를 빌드한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4f39033d-aa5c-49b9-b213-6139ea573ad4.png)

* 토큰 엔드포인트 요청 파라미터를 담는 객체로서 인가 응답을 연계하고 검증할 때 사용된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8e2817f9-aeb3-4f65-b54a-964fe15257df.png)

* `AuthorizationRequestRepository`는 인가 요청을 내보내는 시점부터 인가 응답을 받는 시점까지, 즉 리다이렉트로 브라우저가 인가서버에 다녀오는 구간 동안 `OAuth2AuthorizationRequest`를 보관한다.
* 리다이렉트 직전에 `saveAuthorizationRequest`로 저장하고 콜백에서 `removeAuthorizationRequest`로 꺼내면서 곧바로 삭제하므로 1회용이다. 이렇게 남겨둔 값으로 돌아온 응답의 `state`를 대조하고 PKCE `code_verifier`를 복원하기 때문에 단순한 임시 보관소 개념이 아니라 CSRF와 인가 코드 탈취를 막는 장치의 일부로 볼 수 있다.

#### Access Token 교환

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c048f394-a7e2-4225-a9f3-69a191a6d9fe.png)

* 인가서버로부터 리다이렉트되면서 전달된 code를 인가서버의 Access Token으로 교환하고 Access Token이 저장된 `OAuth2LoginAuthenticationToken`을 `AuthenticationManager`에 위임해 `UserInfo` 정보를 요청해서 최종 사용자로 로그인한다.
* `this.authorizedClientRepository.saveAuthorizedClient()`를 사용해 `OAuth2AuthorizedClient`를 저장한다.
* 인증에 성공하면 `OAuth2AuthenticationToken`이 생성되고 `SecurityContext`에 저장되어 인증 처리를 완료한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0deab0a2-6715-4555-b724-474aae74c719.png)

* 인가서버로부터 리다이렉트 된 이후의 프로세스를 처리하며 Access Token으로 교환하고 이 토큰을 사용해 `UserInfo` 처리를 담당한다.
* scope에 `openid`가 포함되어 있으면 `OidcAuthorizationCodeAuthenticationProvider`를 호출하고 아니면 `OAuth2AuthorizationCodeAuthenticationProvider`를 호출한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2e4b4d57-dc02-4922-b3b6-58d52d9803a0.png)

* 권한 코드 부여 흐름을 처리하는 `AuthenticationProvider`이다.
* 인가서버에 Authorization Code와 Access Token의 교환을 담당한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3c036cb7-ede8-4a4c-a552-199b28cf669b.png)

* OpenID Connect Core 1.0 권한 코드 부여 흐름을 처리하는 `AuthenticationProvider`이며 요청 scope에 `openid`가 존재할 경우 실행된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7460795b-9ed6-448f-aa82-9eefa8635c0a.png)

* 인가서버의 token 엔드포인트로 통신을 담당하며 Access Token을 받은 후 `OAuth2AccessTokenResponse`에 저장하고 반환한다.

> **1. OIDC 케이스에서 Access Token 요청 흐름 정리**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/74d661b0-9562-4cc2-9866-dd48cfb5bfb9.png)

> **2. OAuth2 케이스에서 Access Token 요청 흐름 정리**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/12b9f593-9080-4c96-b311-6bbba6772ca0.png)

#### OAuth 2.0 User 모델 1 - OAuth2UserService

* 액세스 토큰을 사용해서 `UserInfo` 엔드포인트 요청으로 최종 사용자의 속성을 가져오며 `OAuth2User` 타입의 객체를 리턴한다.
* 구현체로는 `DefaultOAuth2UserService`와 `OidcUserService`가 제공된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a0d33536-10f8-43d4-8262-e72cb0130fc0.png)

* 표준 OAuth 2.0 Provider를 지원하는 `OAuth2UserService` 구현체다.
* `OAuth2UserRequest`에 액세스 토큰을 담아 인가서버와 통신 후 사용자의 속성을 가지고 온다.
* 최종 `OAuth2User` 타입의 객체를 반환한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/80a2b8b2-0c43-4914-9b33-00a51d0abcb5.png)

* OpenID Connect 1.0 Provider를 지원하는 `OAuth2UserService` 구현체다.
* `OidcUserRequest`에 있는 ID Token을 통해 인증 처리를 하며 필요시 `DefaultOAuth2UserService`를 사용해서 `UserInfo` 엔드포인트의 사용자 속성을 요청한다.
* 최종 `OidcUser` 타입의 객체를 반환한다.

> **1. 일반 OAuth2**
> * scope에 `openid`가 없다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/14c8ccaa-1805-4e05-91fd-6282cbcb74d8.png)

> **2. OIDC - UserInfo 생략**
> * 인가서버와 추가 통신 없이 `id_token`만으로 끝낸다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9b9acb69-eb61-4125-87ed-caf277408206.png)

> **3. OIDC - UserInfo 호출**
> * 인가서버와 추가 통신을 거친다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/65816566-bfb1-4032-8411-91c94891a5dd.png)

#### OAuth 2.0 User 모델 2 - OAuth2User와 OidcUser

* 시큐리티는 `UserAttributes` 및 ID Token Claims을 집계 & 구성하여 `OAuth2User`와 `OidcUser` 타입의 클래스를 제공한다.

> **1. OAuth2User**
> * OAuth 2.0 Provider에 연결된 사용자 주체를 나타낸다.
> * 최종 사용자의 인증에 대한 정보인 Attribute를 포함하고 있으며, first_name, middle_name, last_name, email, phone_number, address 등으로 구성된다.
> * 기본 구현체는 `DefaultOAuth2User`이며 인증 이후 `Authentication`의 principal 속성에 저장된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7fa52f7a-0933-403a-a2a4-50e4f2fc1451.png)

> **2. OidcUser**
> * `OAuth2User`를 상속한 인터페이스로 OIDC Provider에 연결된 사용자 주체를 나타낸다.
> * 최종 사용자의 인증에 대한 정보인 Claims를 포함하고 있으며, `OidcIdToken` 및 `OidcUserInfo`에서 집계 및 구성된다.
> * 기본 구현체는 `DefaultOidcUser`이며, `DefaultOAuth2User`를 상속하고 있으며 인증 이후 `Authentication`의 principal 속성에 저장된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/144f3d09-98cb-492e-b4af-df4bf4a8578b.png)

* OAuth 2.0 로그인을 통해 인증받은 최종 사용자의 `Principal`에는 `OAuth2User` 혹은 `OidcUser` 타입의 객체가 저장된다.
* 권한 부여 요청 시 scope 파라미터에 `openid`를 포함했다면 `OidcUser` 타입의 객체가 생성되며 `OidcUser`는 `OidcUserInfo`와 `OidcIdToken`을 가지고 있으며 최종 사용자에 대한 Claims 정보를 포함하고 있다.
* `OAuth2UserAuthority`는 인가서버로부터 수신한 scope 정보를 집계해서 권한 정보를 구성한다.
* `OidcUser` 객체를 생성할 때 ID 토큰이 필요한데 이 때, JWT로 서명된 Id Token은 JWS(Json Web Signature)로 서명되어 있기 때문에 반드시 정해진 알고리즘에 의한 검증이 성공하면 `OidcUser` 객체를 생성해야 한다.

#### OAuth 2.0 Provider UserInfo 엔드포인트

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/db2c79d7-53c3-4203-851d-8b1332585405.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/20b69a8a-7f75-4b76-ad8e-3a251f2577d7.png)

* `loadUser()` : 액세스 토큰을 사용자 객체로 교환하는 것이다. 

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c89e69fa-5ddc-4dd3-b764-fc7e7fec86fa.png)

* `OAuth2UserRequest`를 `RequestEntity`로 컨버터한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/00fe8d6a-d7db-48d4-9b1a-bfd4f13890b1.png)

* `RequestEntity`로 인가서버에 요청하고 `ResponseEntity`로 응답받는다.
* `OAuth2User` 타입의 객체를 반환한다.

#### OpenID Connect Provider UserInfo 엔드포인트

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f87240ac-d7bb-4c90-87b7-116837d4413e.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9b85a821-3769-44e7-9b72-bc9193552a8e.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a39571b9-440c-4371-acb9-2f53286aff87.png)

* 내부에 `DefaultOAuth2UserService`를 가지고 있으며 OIDC 사양에 부합할 경우 `OidcUserRequest`를 넘겨 주어 인가서버와 통신한다.
* `OidcUser` 타입의 객체를 반환한다.

#### OAuth 2.0 OpenID Connect 로그아웃

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f8ace272-f1c8-41ef-a45f-0b88e99b7887.png)

* 클라이언트는 로그아웃 엔드포인트를 사용해 웹 브라우저에 대한 세션과 쿠키를 지운다.
* 클라이언트 로그아웃 성공 후 `OidcClientInitiatedLogoutSuccessHandler`를 호출해 OpenID Provider 세션 로그아웃을 요청한다.
* OpenID Provider 로그아웃이 성공하면 지정된 위치로 리다이렉트된다.
* 인가서버 메타데이터 사양에 있는 로그아웃 엔드포인트는 `end_session_endpoint`로 정의되어 있다.

#### Spring MVC 인증 객체 참조

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4ef71767-a4b6-4107-af8f-ed681510b104.png)

* `oauth2Login()`으로 인증을 받게 되면 `Authentication`은 `OAuth2LoginAuthenticationToken` 타입 객체로 바인딩된다.
* principal에는 `OAuth2User` 타입 혹은 `OidcUser` 타입의 구현체가 저장된다.

> [**추가 : @AuthenticationPrincipal**](https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html#mvc-authentication-principal)
> * Spring Security는 `AuthenticationPrincipalArgumentResolver`를 제공한다. 이 리졸버는 현재 `Authentication.getPrincipal()`을 Spring MVC 메서드 인자로 자동 주입해준다.
> * `@EnableWebSecurity`를 사용하면 이 리졸버가 Spring MVC 설정에 자동으로 추가된다.
> * `AuthenticationPrincipalArgumentResolver`를 제대로 설정하고 나면 Spring MVC 계층을 Spring Security로부터 완전히 분리할 수 있다.
