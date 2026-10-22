# Spring Security - Authorization Server

#### OAuth 2.0 Authorization Server

* Spring Authorization Server는 OAuth 2.1 및 OpenID Connect 1.0 사양 및 기타 관련 사양의 구현을 제곤하는 프레임워크이다.
* OpenID Connect 1.0 공급자 및 OAuth2 권한 부여 서버 제품을 구축하기 위한 안전하고 가볍고 사용자 지정 가능한 기반을 제공하기 위해 구축되었다.

#### OAuth2AuthorizationServerConfiguration

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f952c333-4ebb-4329-b2ee-08dd8f6dc503.png)

* OAuth2 인가 서버에 대한 최소 기본 구성을 제공하는 설정 클래스이다.
* `OAuth2AuthorizationServerConfigurer`를 사용하여 기본 구성을 적용하고 OAuth2 인가 서버를 지원하는 모든 인프라 구성 요소로 구성된 `SecurityFilterChain` 빈을 등록한다.
* OpenID Connect 1.0 UserInfo 엔드포인트 및 OpenID Connect 1.0 클라이언트 등록 엔드포인트를 사용하기 위해서는 필수요소인 `JwtDecoder`를 정의해야 한다.
* `OAuth2AuthorizationSererConfigurer`는 OAuth2 인증 서버의 보안 구성을 완전히 사용자 정의할 수 있는 기능을 제공한다.
* 이를 통해 `RegisteredClientRepository`, `OAuth2AuthorizationService`, `OAuth2TokenGenerator` 등과 같이 핵심 구성 요소를 지정할 수 있다.
* 또한 권한 부여 엔드포인트, 토큰 엔드포인트, 토큰 검사 엔드포인트 등과 같은 프로토콜 엔드포인트에 대한 요청 처리 논리를 지정할 수 있다.

#### OAuth2AuthorizationServerConfigurer

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d4ac0418-8d09-4b41-8027-b2b84929189b.png)

* `OAuth2ClientAuthenticationConfigurer` : OAuth 2.0 클라이언트 인증 엔드포인트 설정클래스
* `OAuth2AuthorizationEndpointConfigurer` : OAuth 2.0 권한부여 엔드포인트 설정클래스
* `OAuth2TokenEndpointConfigurer` : OAuth 2.0 토큰 엔드포인트 설정클래스
* `OAuth2TokenRevocationEndpointConfigurer` : OAuth 2.0 토큰 취소 엔드포인트 설정클래스
* `OidcConfigurer` : OpenID Connect 엔드포인트 설정클래스

#### ProviderContext(AuthorizationServerContext)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/98730f4e-c4b9-43ca-ba06-0115794e5f8f.png)

* 공급자에 대한 정보를 저장하는 컨텍스트 객체로서 공급자 설정 및 현재 issuer에 대한 액세스를 제공한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bc7c0c26-9194-49c9-be05-aaec979cec95.png)

* `ProviderContext`를 가지고 있으며 Thread Local을 사용하여 현재 요청 쓰레드와 연결되어 있는 `ProviderContext`에 접근할 수 있게 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/57a98555-a40b-4be7-9bb8-4924fb4b9a55.png)

* `ProviderContextHolder`와 `ProviderContext`를 연결한다.
