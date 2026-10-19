# Spring Security - Open ID Connect

#### Open ID Connect

* OpenID Connect 1.0은 OAuth 2.0 프로토콜 위에 구축된 ID 계층으로 OAuth 2.0을 확장해 인증 방식을 표준화 한 OAuth 2.0 기반의 인증 프로토콜이다.
* scope 지정 시 `openid`를 포함하면 OpenID Connect 사용이 가능하며 인증에 대한 정보는 ID 토큰(ID Token)이라고 하는 JWT로 반환된다. 
* OpenID Connect는 클라이언트가 사용자 ID를 확인할 수 있게 하는 보안 토큰인 ID Token을 제공한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1d682e93-1e10-41ca-805f-2b4f4ce2191f.png)

#### ID Token과 scope

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3027d921-be12-4b10-a706-84a0f8ab36f1.png)

* ID Token은 사용자가 인증되었음을 증명하는 결과물로서 OIDC 요청 시 Access Token과 함께 클라이언트에게 전달되는 토큰이다.
* ID Token은 JWT으로 표현되며 헤더, 페이로드 및 서명으로 구성된다.
* ID Token은 개인 키로 발급자가 서명하는 것으로서 토큰의 출처를 보장하고 변조되지 않았음을 보장한다.
* 애플리케이션은 공개 키로 ID Token을 검증 및 유효성을 검사하고 만료여부 등 토큰의 클레임을 확인한다.
* 클라이언트는 클레임 정보에 포함되어 있는 사용자명, 이메일을 활용하여 인증 관리를 할 수 있다.

|scope|description|
|:--:|:--:|
|openid|필수. 클라이언트가 OpenID Connect 요청을 하고 있음을 서버에 알린다.|
|profile|기본 프로필 클레임에 대한 액세스 요청|
|email|이메일 및 `email_verified` 클레임에 대한 액세스 요청|
|address|주소 클레임에 대한 액세스 요청|
|phone|`phone_number` 및 `phone_number_verified` 클레임에 대한 액세스 요청|

#### OIDC 로그인 요청 흐름 정리

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/26348806-9d79-49a8-8fc5-89a97037b6bd.png)

* 매개변수 요청 및 응답
  * 요청시 `openid` 범위를 scope 매개변수에 포함해야 한다.
  * `response_type` 매개변수는 `id_token`을 포함한다.
  * 요청은 `nonce` 매개변수를 포함해야 한다.
    * 요청에 포함되는 값으로서 결과 `id_token`값에 클레임으로 포함되며 이것은 토큰의 재생 공격을 방지하고 요청의 출처를 식별하는 임의의 고유 문자열이다.
    * 해당 `nonce` 클레임에는 요청에서 전송된 것과 정확히 동일한 값이 포함되어야 한다. 그렇지 않은 경우 애플리케이션에서 인증을 거부해야 한다.
