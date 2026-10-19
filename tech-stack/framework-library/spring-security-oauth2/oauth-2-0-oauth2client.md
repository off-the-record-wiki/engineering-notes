# Spring Security - OAuth 2.0 oauth2Client

#### OAuth2ClientConfigurer

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/366cf8c3-6f14-4079-9778-2177bee24519.png)

* `oauth2Client()` DSL의 구현체. `AbstractHttpConfigurer`를 확장한 설정 클래스로 `http.oauth2Client(...)`를 호출했을 때 필터와 Provider를 조립해 넣는 역할을 한다.

#### OAuth2AuthorizedClient

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2d1cb409-fb10-4620-847d-3f20f7a13bc5.png)

* `OAuth2AuthorizedClient`는 인가받은 클라이언트를 의미하는 클래스다.
* 최종 사용자가 클라이언트에게 리소스에 접근할 수 있는 권한을 부여하면, 클라이언트를 인가된 클라이언트라고 간주한다.
* `OAuth2AuthorizedClient`는 Access Token과 Refresh Token을 ClientRegistration와 권한을 부여한 최종 사용자인 Principal과 함께 묶어 준다.
* `OAuth2AuthorizedClient`의 Access Token을 사용해서 리소스 서버의 자원에 접근할 수 있으며 인가서버와의 통신으로 토큰을 검증할 수 있다.
* `OAuth2AuthorizedClient`의 ClientRegistration과 Access Token을 사용해서 UserInfo 엔드포인트로 요청할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/782a4b42-8f9b-41fa-97df-d8fea321332b.png)

* `OAuth2AuthorizedClientRepository`는 다른 웹 요청이 와도 동일한 `OAuth2AuthorizedClient`를 유지하는 역할을 담당한다.
* `OAuth2AuthorizedClientService`에게 `OAuth2AuthorizedClient`의 저장, 조회, 삭제 처리를 위임한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ff791191-f296-43a9-b676-c94e86178897.png)

* `OAuth2AuthorizedClientService`는 어플리케이션 레벨에서 `OAuth2AuthorizedClient`를 저장, 조회, 삭제하는 일을 한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fb355539-45cd-46d3-a04a-e25ea56d1e40.png)

* Authorization Code Grant 방식으로 권한 부여 요청을 지원하는 필터이다.
* 인가서버로부터 리다이렉트되면서 전달된 code를 인가서버의 Access Token으로 교환한다.
* `OAuth2AuthorizedClientRepository`를 사용해 `OAuth2AuthorizedClient`를 저장한 후 클라이언트의 Redirect URI로 이동한다.
* 실행 조건
  * `OAuth2AuthorizationRequest` 객체가 존재하는지 확인
  * Redirect URI가 완전 일치하는지 확인
  * 요청 파라미터에 code + state가 둘 다 존재하는지 확인

#### DefaultOAuth2AuthorizedClientManager

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/08bacabb-addd-4ef7-a4de-f965761ea063.png)

* `OAuth2AuthorizedClient`를 전반적으로 관리하는 인터페이스
* `OAuth2AuthorizedClientProvider`로 OAuth 2.0 클라이언트에 권한 부여
  * Client Credentials Flow
  * Refresh Token Flow
  * Resource Owner Password Flow
* `OAuth2AuthorizedClientService`나 `OAuth2AuthorizedClientRepository`에 `OAuth2AuthorizedClient` 저장을 위임한 후 `OAuth2AuthorizedClient`를 최종 반환한다.
* 사용자 정의 `OAuth2AuthorizationSuccessHandler` 및 `OAuth2AuthorizationFailureHandler`를 구성하여 성공/실패 처리를 변경할 수 있다.
* invalid_grant 오류로 인해 권한 부여 시도가 실패하면 이전에 저장된 `OAuth2AuthorizedClient`가 `OAuth2AuthorizedClientRepository`에서 제거된다.

#### DefaultOAuth2AuthorizedClientManager - Client Credentials

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a0993a94-a274-4965-a690-08771e8e78e2.png)

* `DefaultOAuth2AuthorizedClientManager.authorize()`가 `OAuth2AuthorizedClientRepository`에서 기존 `OAuth2AuthorizedClient`를 꺼내고, `ClientRegistration`, `principal`, `contextAttributesMapper` 결과를 `OAuth2AuthorizationContext`로 묶어 Provider에 위임한다.
* `ClientCredentialsOAuth2AuthorizedClientProvider`가 `clock`과 `clockSkew`로 만료를 판정해 유효하면 `null`을 반환한다.
* 부재·만료일 때만 `RestClientClientCredentialsTokenResponseClient`가 grant_type=client_credentials로 토큰 엔드포인트에 POST하고, 응답의 access_token만으로 3인자 `OAuth2AuthorizedClient`를 생성해(refresh token 없음, 리다이렉트·인가 코드 없음) `OAuth2AuthorizationSuccessHandler`가 저장소에 반영한다.
* 조회 → 판정 → 조건부 발급 → 저장 4단계로 분리된 토큰 캐시이며, `null` 계약이 그랜트별 유효성 기준을 Provider 안에 가둬 매니저를 그랜트 무관하게 유지시키는 축이다.

#### DefaultOAuth2AuthorizedClientManager - Refresh Token

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/120f84e4-7fcb-40d1-a230-b83d92d8e88e.png)

* 매니저가 저장소에서 꺼낸 기존 `OAuth2AuthorizedClient`를 `OAuth2AuthorizationContext`로 묶어 `RefreshTokenOAuth2AuthorizedClientProvider`에 위임한다.
* Provider는 클라이언트가 존재하는지와 `refreshToken != null`, accessToken 만료 세 조건이 모두 참일 때만 움직이고, 하나라도 빠지면 `null`을 반환해 기존 토큰을 그대로 쓰게 한다.
* 갱신 시 `RestClientRefreshTokenTokenResponseClient`가 grant_type=refresh_token으로 POST하고, 응답에 새 refresh token이 오면 교체·없으면 기존 것 유지해 4인자 `OAuth2AuthorizedClient`를 만든 뒤 `OAuth2AuthorizedClientRefreshedEvent`를 발행한다
* 스스로 최초 발급을 못 하는 연장 전용 Provider다. Authorization Code가 발급한 토큰을 `DelegatingOAuth2AuthorizedClientProvider` 체인 안에서 이어받아 수명만 늘린다.

#### DefaultOAuth2AuthorizedClientManager - Based Filter

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7b6ab2e4-9c35-4123-a989-385a5fb516d8.png)

* 애플리케이션이 WebClient(또는 RestClient)로 외부 API를 호출하면 필터/인터셉터가 요청을 가로채, attributes → defaultClientRegistrationId → `SecurityContext`의 `OAuth2AuthenticationToken` 순으로 registrationId와 principal을 결정한다.
* 결정된 값으로 `OAuth2AuthorizedClientManager.authorize()`를 호출한다. 앞서 본 Client Credentials·Refresh Token 흐름이 바로 이 안에서 돈다.
* 돌려받은 `OAuth2AuthorizedClient`의 액세스 토큰을 `Authorization: Bearer` 헤더로 붙여 실제 요청을 내보내고, 응답이 401·403이면 authorizationFailureHandler로 저장된 토큰을 지워 다음 요청에 재발급을 유도한다.

#### @RegisteredOAuth2AuthorizedClient

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/cd5216f7-d9c4-4a4c-9333-8f82971d5569.png)

* 파라미터를 `OAuth2AuthorizedClient` 타입 인자로 리졸브해준다.
* `OAuth2AuthorizedClientArgumentResolver`에서 요청을 가로채 유형별로 권한 부여 흐름을 실행하도록 한다.
* `OAuth2AuthorizedClientManager`나 `OAuth2AuthorizedClientService`로 접근하는 것보다 편하다.
