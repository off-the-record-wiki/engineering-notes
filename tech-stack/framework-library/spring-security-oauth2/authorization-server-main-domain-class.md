# Spring Security - Authorization Server Main Domain Class

#### RegisteredClient

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/71247985-b8c4-472b-a29c-c7944d5f4e81.png)

* 인가 서버에 등록된 클라이언트를 의미하며 클라이언트가 authorization_code 또는 client_credentials와 같은 권한 부여 흐름을 시작하려면 먼저 클라이언트를 권한 부여 서버에 등록해야 한다.
* 클라이언트 등록 시 클라이언트는 고유한 client_id, client_secret 및 고유한 클라이언트 식별자와 연결된 메타 데이터를 할당한다.
* 클라이언트 메타데이터는 클라이언트 이름부터 프로토콜 흐름과 관련된 클라이언트 식별자와 연결된 메타 데이터를 할당한다.
* 클라이언트 주요 목적은 보호된 리소스에 대한 액세스를 요청하는 것으로 클라이언트는 먼저 권한 부여 서버를 인증하고 액세스 토큰과 교환을 요청한다.
* 권한 부여 서버는 클라이언트 및 권한 부여를 인증하고 유효한 경우 액세스 토큰을 발급하고 클라이언트는 액세스 토큰을 표시하여 리소스 서버에소 보호된 리소스를 요청할 수 있다.

|필드|설명|
|:---:|:---:|
|`clientId`|클라이언트 식별자|
|`clientAuthenticationMethods`|클라이언트가 사용할 수 있는 인증 방법|
|`clientIdIssuedAt`|클라이언트 식별자가 발급된 시간|
|`clientSecret`|클라이언트 비밀값|
|`clientSecretExpiresAt`|클라이언트 암호 만료 시간|
|`authorizationGrantTypes`|클라이언트가 사용할 수 있는 권한 부여 유형|
|`tokenSettings`|클라이언트에 발급된 OAuth2 토큰에 대한 사용자 정의 설정|
|`clientSettings`|클라이언트에 대한 사용자 지정 설정|
|`redirectUris`|클라이언트가 리디렉션 기반 흐름에서 사용할 수 있는 등록된 리디렉션 URI|
|`clientName`|클라이언트명|
|`id`|등록된 클라이언트를 고유하게 식별하는 ID|
|`scopes`|클라이언트가 요청할 수 있는 범위|

#### RegisteredClientRepository

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/83a674f5-ab0e-4f5e-ab25-6190e2f13aa8.png)

* 새로운 클라이언트를 등록하고 기존 클라이언트를 조회할 수 있는 저장소 클래스이다.
* 클라이언트 인증, 권한 부여 처리, 토큰 자체 검사, 동적 클라이언트 등록과 같은 특정 프로토콜 흐름 시 다른 구성 요소에서 참조한다.
* 제공하는 구현체로 InMemoryRegisteredClientRepository 및 JdbcRegisteredClientRepository가 있다.

#### OAuth2AuthorizationService

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/463207a4-eb3f-4bf4-b35b-6493c0f80fb0.png)

* `OAuth2AuthorizationService`는 새로운 `OAuth2Authorization`을 저장하고 기존 `OAuth2Authorization`을 검색하는 구성요소이다.
* 특정 엔드포인트 프로토콜 흐름을 따를 때 다른 구성 요소에서 사용된다(예: 클라이언트 인증, 권한 부여 처리, 토큰 자체 검사, 토큰 취소, 동적 클라이언트 등록 등).
* 제공되는 기본 구현체는 `InMemoryOAuth2AuthorizationService` 및 `JdbcOAuth2AuthorizationService`이 있다.
* `InMemoryOAuth2AuthorizationService`는 개발 및 테스트에만 사용하는 것이 좋으며 기본값이다.
* `JdbcOAuth2AuthorizationService`는 `JdbcTemplate`를 사용하여 `OAuth2Authorization` 객체를 DB에 저장하여 상태를 계속 유지하도록 한다.

#### OAuth2Authorization

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/61051ca4-dba8-4d2d-b242-019549341e09.png)

* 리소스 소유자의 역할이 있는 권한 부여 방식인 경우 클라이언트에 부여된 권한 부여 즉 인가 상태를 유지하는 클래스이다.
* 권한 부여 흐름이 성공적으로 완료되면 `OAuth2Authorization`이 생성되고 Access Token이 저장되며 선택적으로 Refresh Token, ID Token이 저장된다.
* 생성된 `OAuth2Authorization`은 `OAuth2AuthorizationService`에 의해 메모리나 DB에 저장된다.
* `OAuth2Authorization`에 저장되는 OAuth2Token 타입들은 권한 부여 유형 및 scope 등에 따라 다르다.
