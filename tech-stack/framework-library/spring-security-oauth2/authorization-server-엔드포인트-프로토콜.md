# Spring Security - Authorization Server Endpoint Protocol

#### OAuth2AuthorizationEndpointConfigurer

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a780a96f-0a0b-4947-8030-2d577ab73907.png)

* OAuth2 권한 부여 엔드포인트에 대한 사용자 정의를 할 수 있는 기능을 제공한다.
* OAuth2 권한 부여 요청에 대한 전처리, 기본처리 및 후처리 로직을 커스텀하게 구성할 수 있도록 하는 API를 지원한다.
* `OAuth2AuthorizationEndpointFilter`를 구성하고 이를 OAuth2 인증 서버 SecurityFilterChain 빈에 등록한다.

#### OAuth2AuthorizationEndpointFilter

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4a2480d2-7a27-4b95-bb42-bb668e7cae4e.png)

* OAuth2 인증 요청을 처리하는 필터이며 다음과 같은 기본값으로 구성된다.
  * `OAuth2AuthorizationCodeRequestAuthenticationConverter` : 클라이언트 요청 파라미터를 `OAuth2AuthorizationCodeRequestAuthenticationToken`으로 변환하고 `AuthenticationProvider`에게 전달한다.
  * `OAuth2AuthorizationCodeRequestAuthenticationProvider` : Authorization Code 권한 부여 방식을 처리하는 OAuth 2.0 인증 요청 및 동의에 대한 `AuthenticationProvider` 구현체이다.

#### 
#### RequestMatcher

* Code 요청 패턴 : `/oauth2/authorize, GET`, `/oauth2/authorize, POST`
* Consent 요청 패턴 : `/oauth2/authorize, POST`

#### Code 요청과 응답 플로우

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/76452b3d-7478-42e8-9623-3d09275902d0.png)

* `response_type` : 필수(값은 code 고정)
* `client_id` : 필수(인가 서버에 등록된 클라이언트 식별자)
* `redirect_uri` : 선택(여러 개 등록 시 필수. 생략하면 등록된 값을 사용, OIDC에서는 필수)
* `scope` : 선택(OIDC로 동작시키려면 openid가 반드시 포함되어야 한다)
* `code_challenge`, `code_challenge_method` : RFC 7636(PKCE)
* `nonce` : OIDC(ID 토큰 재생 공격 방어 수단. OIDC 흐름에서는 권장한다.)

#### OAuth2AuthorizationConsent

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9cb121c6-e464-45d1-9c5a-a144c36055ea.png)

* `OAuth2AuthorizationConsent` 는 OAuth2 권한 부여 요청 흐름의 권한부여 "동의“(결정)를 나타낸다.
* 클라이언트에 대한 액세스를 승인할 때 리소스 소유자는 클라이언트가 요청한 권한의 하위 집합만 허용할 수 있다.
* OAuth2 인증 요청 흐름이 완료되면 OAuth2 Authorization Consent가 생성(또는 업데이트)되고 부여된 권한을 클라이언트 및 리소스 소유자와 연결한다.
* 클라이언트가 범위를 요청하고 리소스 소유자가 요청된 범위에 대한 액세스를 허용하거나 거부하는 `authorization_code grant` 흐름이다.

#### OAuth2AuthorizationConsentService

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/48d6caf3-f154-46db-8446-a2252feaa026.png)

* `OAuth2AuthorizationConsent` 저장되고 기존 `OAuth2AuthorizationConsent`를 조회하는 클래스로 주로 OAuth2 권한 부여 요청 흐름을 구현하는 구성 요소에 의해 사용된다.
* 기본 구현체는 `InMemoryOAuth2AuthorizationConsentService`와 `JdbcOAuth2AuthorizationConsentService`가 있다.

#### OAuth2ClientAuthenticationConfigurer

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9a752fa2-5c16-4c9b-946d-58555cf7a071.png)

* OAuth2 클라이언트 인증을 위한 사용자 정의하는 기능을 제공한다.
* 클라이언트 인증 요청에 대한 전처리, 기본 처리 및 후처리 로직을 커스텀하게 구현할 수 있도록 API를 지원한다.
* `OAuth2ClientAuthenticationFilter`를 구성하고 이를 OAuth2 인증 서버 `SecurityFilterChain` 빈에 등록한다.
* 지원되는 클라이언트 인증 방법은 `client_secret_basic`, `client_secret_post`, `private_key_jwt`, `client_secret_jwt`및 `none`(공개 클라이언트)이다.

#### OAuth2ClientAuthenticationFilter

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/44dc7e02-4c5f-4f98-9219-053bfa6075af.png)

* 클라이언트 인증 요청을 처리하는 필터이며 다음과 같은 기본값으로 구성된다.
* `DelegatingAuthenticationConverter`
  * `ClientSecretBasicAuthenticationConverter` : 클라이언트 요청 방식이 HTTP Basic일 경우 처리
  * `ClientSecretPostAuthenticationConverter` : 클라이언트 요청 방식이 POST일 경우 처리
  * `JwtClientAssertionAuthenticationConverter` : 클라이언트 요청 방식이 JWT 토큰일 경우 처리
  * `PublicClientAuthenticationConverter` : 클라이언트 요청 방식이 PKCE일 경우 처리
* `DelegatingAuthenticationProvider`
  * `ClientSecretAuthenticationProvider`, `JwtClientAssertionAuthenticationProvider`, `PublicClientAuthenticationProvider`
  * 권한 부여 유형에 따라 토큰을 발행하는 `AuthenticationProvider` 구현체이다.
* `AuthenticationSuccessHandler` - 인증된 `OAuth2ClientAuthenticationToken`에 `SecurityContext`를 연결하는 내부 구현체
* `AuthenticationFailureHandler` – 연결된 `OAuth2AuthenticationException`를 사용하여 OAuth2 오류 응답을 반환하는 내부 구현체

#### OAuth2TokenEndpointConfigurer

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5cb330a5-d27a-4d3a-a58e-17ce4d7affad.png)

* OAuth2 토큰 엔드포인트에 대한 사용자 정의 할 수 있는 기능을 제공한다.
* OAuth2 토큰 요청에 대한 전처리, 기본 처리 및 후처리 로직을 커스텀하게 구현할 수 있도록 API를 지원한다.
* `OAuth2TokenEndpointFilter`를 구성하고 이를 OAuth2 인증 서버 `SecurityFilterChain`빈에 등록한다.
* 지원되는 권한 부여 유형은 `authorization_code`, refresh_token 및 client_credential이다.

#### OAuth2TokenEndpointFilter

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ea7a06a3-c5eb-4f46-a671-5de4c3d79060.png)

* 클라이언트의 토큰 요청을 처리하는 필터이며 다음과 같은 기본값으로 구성된다.
  * `DelegatingAuthenticationConverter` : 각 특정 유형의 AuthenticationConverter 를 호출해서 처리를 위임한다.
    * `OAuth2AuthorizationCodeAuthenticationConverter` : `HttpServletRequest` 정보를 `OAuth2AuthorizationCodeAuthenticationToken`로 변환하여 반환한다.
    * `OAuth2RefreshTokenAuthenticationConverter` : `HttpServletRequest` 정보를 `OAuth2RefreshTokenAuthenticationToken`로 변환하여 반환한다.
    * `OAuth2ClientCredentialsAuthenticationConverter` : `HttpServletRequest` 정보를 `OAuth2ClientCredentialsAuthenticationToken`로 변환하여 반환한다.
  * `OAuth2AuthorizationCodeAuthenticationProvider`, `OAuth2RefreshTokenAuthenticationProvider`, `OAuth2ClientCredentialsAuthenticationProvider` : 권한 부여 유형에 따라 토큰을 발행하는 `AuthenticationProvider` 구현체이다.
  * `AuthenticationSuccessHandler` : 인증된 `OAuth2AccessTokenAuthenticationToken`을 처리하는 내부 구현체로서 인증토큰을 사용하여 `OAuth2AccessTokenResponse`를 반환한다
  * `AuthenticationFailureHandler` : `OAuth2AuthenticationException`과 관련된 `OAuth2Error`를 사용하는 내부 구현 인증예외이며 `OAuth2Error`응답을 반환한다.

#### RequestMatcher

* Token 요청 패턴 : `/oauth2/token, POST`

#### OAuth 2.0 Token Endpoint - Authorization Code

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9f91556e-36e5-4ae4-9baa-3871e386712f.png)

**Access Token Response**

**1. Successful Response 정리**
* access_token(필수) : 권한 부여 서버에서 발급한 액세스 토큰 문자열
* token_type(필수) : 토큰 유형은 일반적으로 Bearer 문자열  
* expires_in(권장) : 토큰의 만료시간
* refresh_token(선택 사항) : 액세스 토큰이 만료되면 응용 프로그램이 다른 액세스 토큰을 얻는 데 사용할 수 있는 Refresh 토큰을 반환하는 것이 유용하다. 단, implicit 권한 부여로 발행된 토큰은 Refresh 토큰을 발행할 수 없다.
* scope(선택사항) : 사용자가 부여한 범위가 앱이 요청한 범위와 동일한 경우 이 매개변수는 선택사항이다.

**2. Unsuccessful Response 정리**
* invalid_request : 요청에 매개변수가 누락, 지원되지 않는 매개변수, 매개변수 반복되는 경우 서버가 요청을 진행할 수 없다.
* invalid_client : 청에 잘못된 클라이언트 ID 또는 암호가 포함된 경우 클라이언트 인증에 실패, HTTP 401을 응답한다.
* invalid_grant : 인증 코드가 유효하지 않거나 만료됨. 권한 부여에 제공된 리디렉션 URL이 액세스 토큰 요청에 제공된 URL과 일치하지 않는 경우 반환하는 오류이다.
* invalid_scope : 범위를 포함하는 액세스 토큰 요청의 경우 이 오류는 요청의 유효하지 않은 범위 값을 나타낸다.
* unauthorized_client : 이 클라이언트는 요청된 권한 부여 유형을 사용할 권한이 없다.(RegisteredClient에 정의하지 않은 권한 부여 유형을 요청한 경우)
* unsupported_grant_type : 권한 부여 서버가 인식하지 못하는 승인 유형을 요청하는 경우 이 코드를 사용한다.

#### OAuth 2.0 Token Endpoint - Client Credentials

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/714651da-f6fa-4da1-b9a4-820033c9e0e5.png)

* OAuth 전용 설계가 아니다. 폼 로그인과 동일한 Spring Security 골격(Filter → Converter → Token → Provider)을 그대로 쓴다.
* 토큰들은 하나의 OAuth2Authorization 안에 형제로 묶여 있다. 그래서 하나를 무효화하면 나머지까지 연쇄로 죽일 수 있다.
* 무효화는 삭제가 아니라 플래그다. 지우지 않고 남겨야 "없는 code"와 "이미 쓴 code"를 구분해 재사용을 탐지하고, 그 순간 발급된 토큰을 전부 폐기할 수 있다.

#### OAuth 2.0 Token Endpoint - Refresh Token

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/56b72661-456c-457d-a31e-2cf181d5cee3.png)

* 토큰을 발급받은 클라이언트와 지금 요청한 클라이언트가 같은지, 그 클라이언트가 refresh_token 그랜트를 쓸 수 있는지를 `isActive()`보다 먼저 본다.
* 최초 부여 범위를 넘는 scope는 거절해 리프레시로 권한을 넓히지 못하게 막는다. 반대로 scope를 생략하면 최초 부여분을 그대로 승계하는데, 같은 상황에서 빈 집합이 되는 client_credentials와 정반대이다.
* 회전은 무효화가 아니라 교체다. 새 refresh token이 저장소 값을 덮어쓰므로 옛 토큰은 "없는 토큰"이 되어 invalid_grant일 뿐, code 흐름처럼 재사용을 탈취 신호로 감지해 연쇄 폐기하지 않는다.

#### OAuth 2.0 Token Endpoint - Authorization Code with PKCE

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b02e6c40-8254-489e-a75e-47d40990b513.png)

* PKCE는 두 지점에 걸쳐 있다. authorize는 code_challenge를 저장만 하고, 실제 대조는 token 엔드포인트의 클라이언트 인증 단계에서 code_verifier와 일어난다. 원본은 앞쪽 절반만 그려서 왜 안전한지가 설명되지 않는다.
* code_challenge는 무조건 필수가 아니다. requireProofKey 기본값이 false라 이 설정이 꺼져 있고 code_verifier도 없으면 PKCE를 통째로 건너뛴다. confidential client는 기본적으로 PKCE 없이 동작한다.

#### OAuth2TokenIntrospectionEndpointConfigurer

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fb33594a-4d8c-41a1-826b-d4ce986d09a8.png)

* OAuth2 토큰 검사 엔드포인트에 대한 사용자 정의 할 수 있는 기능을 제공한다.
* `OAuth2TokenIntrospectionEndpointFilter`를 구성하고 이를 OAuth2 인증 서버 `SecurityFilterChain`빈에 등록한다.
* OAuth2 검사 요청에 대한 전처리, 기본 처리 및 후처리 로직을 커스텀하게 구현할 수 있도록 API를 지원한다.

#### OAuth2TokenIntrospectionEndpointFilter

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9dc8e1f2-b8b8-48cf-8b82-72b0e091f27f.png)

* OAuth2 검사 요청을 처리하는 필터이며 다음과 같은 기본값으로 구성된다.
* `OAuth2TokenIntrospectionAuthenticationProvider` : `OAuth2TokenIntrospectionAuthenticationToken`를 받아 인증 처리를 하는 `AuthenticationProvider` 구현체이다.
* `IntrospectionRequestConverter` : OAuth2 검사 요청을 추출하려고 할 때 사용되는 전처리기로서 `OAuth2TokenIntrospectionAuthenticationToken`을 반환한다.

#### OAuth2TokenRevocationEndpointConfigurer

* OAuth2 토큰 취소 엔드포인트에 대한 사용자 정의 할 수 있는 기능을 제공한다.
* `OAuth2TokenRevocationEndpointFilter`를 구성하고 이를 OAuth2 인증 서버 `SecurityFilterChain`빈에 등록한다.
* OAuth2 토큰 취소에 대한 전처리, 기본 처리 및 후처리 로직을 커스텀하게 구현할 수 있도록 API를 지원한다.

#### OAuth2TokenRevocationEndpointFilter

* OAuth2 토큰 취소를 처리하는 필터이며 다음과 같은 기본값으로 구성된다.
* `OAuth2TokenRevocationAuthenticationProvider` : `OAuth2TokenRevocationAuthenticationToken`을 전달받아 인증처리를 하는 `AuthenticationProvider` 구현체이다.
* `DefaultTokenRevocationAuthenticationConverter` : OAuth2 토큰 취소를 추출하려고 할 때 사용되는 전처리기로서 `OAuth2TokenRevocationAuthenticationToken` 을 반환한다.

#### RequestMatcher 

* 토큰 취소 요청 패턴 : `/oauth2/revoke, POST`

#### OAuth2AuthorizationServerConfigurer

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/550dcdc8-fcb1-4540-a71d-01166da4c058.png)

* OAuth2 Authorization Server 메타데이터 엔드포인트 대한 지원을 제공한다.
* `OAuth2AuthorizationServerMetadataEndpointFilter`를 구성하고 이를 OAuth2 인증 서버 `SecurityFilterChain`빈에 등록한다.
* OAuth2 Authorization Server 메타데이터 요청을 처리하고 OAuth2 Authorization Server 메타데이터 응답을 반환한다.
* JWK Set 엔드포인트 대한 지원을 제공한다.
* `NimbusJwkSetEndpointFilter`를 구성하고 이를 `SecurityFilterChain`빈에 등록한다.
* JWK Set 엔드포인트는 `JWKSource<SecurityContext>` 빈이 등록된 경우에만 구성된다.
* `NimbusJwkSetEndpointFilter`는 JWK Set 을 반환하는 필터이다.

#### RequestMatcher

* 토큰 검사 요청 패턴 : `/.well-known/oauth-authorization-server, GET`

#### OpenID Connect 1.0 Provider Configuration Endpoint

* `OidcConfigurer`는 OpenID Connect 1.0 Provider Configuration 엔드포인트 대한 지원을 제공한다.
* `OidcConfigurer` 는 `OidcProviderConfigurationEndpointFilter` 를 구성하고 이를 OAuth2 인증 서버 `SecurityFilterChain` 빈에 등록한다.
* `OidcProviderConfigurationEndpointFilter`는 `OidcProviderConfiguration` 응답을 처리한다.

#### OpenID Connect 1.0 UserInfo Endpoint

* `OidcUserInfoEndpointConfigurer` 는 OpenID Connect 1.0 UserInfo 엔드포인트 사용자 정의하는 기능을 제공한다.
* `OidcUserInfoEndpointFilter`를 구성하고 OAuth2 인증 서버 `SecurityFilterChain` 빈에 등록한다.

#### OidcUserInfoEndpointFilter

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/bfb9e897-175c-40fe-a9fd-85161984d1b2.png)

* UserInfo 요청을 처리하고 OidcUserInfo 응답을 반환하는 필터이며 다음과 같은 기본값으로 구성된다.
* `OidcUserInfoAuthenticationProvider` : 요청된 scope 를 기준으로 ID 토큰에서 표준 클레임을 추출하는 userInfoMapper을 가지고 있다.
* 이 때, UserInfo 엔드포인트 프로토콜은 기본적으로 인증을 받은 상태에서만 접근이 가능하다. 왜냐하면 FilterSecurityInteceptor 클래스 이후에 위치하고 있기 때문이다.
* UserInfo 엔드포인트 요청시 일반적으로 로그인 과정을 거치기 때문에 정상적으로 access token 발급이 가능하다.
* `/userinfo` 엔드포인트는 권한부여흐름 요청에서 받은 access token 을 가지고 인가서버로 요청하기 때문에 별도의 인증과정을 거치도록 구성되어져야 한다.

#### RequestMatcher

* 토큰 검사 요청 패턴 : `/userinfo, POST`, `/userinfo, GET`
