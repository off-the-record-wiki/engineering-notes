# Spring Security - OAuth 2.0 Authorization Type

#### OAuth 2.0 Grant Type

* 권한 부여란 클라이언트가 사용자를 대신해서 사용자의 승인 하에 인가서버로부터 권한을 부여받는 것을 말한다.
* OAuth 2.0 메커니즘은 아래와 같은 권한 부여 유형들을 지원하고 있으며 일부는 사장되었다.

#### Authorization Code Grant Type

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c90933d4-b44c-4c56-a3f2-a63a1021b3eb.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9174110d-8454-4491-9e0a-9c8c9d14caed.png)

* 사용자가 애플리케이션을 승인하면 인가서버는 Redirect URI로 임시 코드를 담아서 애플리케이션으로 다시 리다이렉션한다.
* 애플리케이션은 해당 임시 코드를 인가서버로 전달하고 액세스 토큰으로 교환한다.
* 애플리케이션이 액세스 토큰을 요청할 때 해당 요청을 클라이언트 암호로 인증할 수 있으므로 공격자가 인증 코드를 가로채서 스스로 사용할 위험이 줄어든다.
* 액세스 토큰이 사용자 또는 브라우저에 표시되지 않고 애플리케이션에 다시 전달되는 가장 안전한 방법이므로 토큰이 다름 사람에게 노출될 위험이 줄어든다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/035e7dec-b070-4c4c-9648-e3c9f68585b8.png)

* 인가서버에게 code를 요청한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/864252fe-dbb6-4022-944c-efa404d38bdb.png)

* 사용자의 승인 및 동의 하에 인가서버가 클라이언트에게 code를 발급한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ac2eba81-f6aa-4a65-9f34-51a4e906cae5.png)

* 클라이언트의 권한 부여가 승인되고 그 결과로 토큰을 획득한다.

> **❗매개변수 용어 정리**
>
> * `client_id` : 인가서버에 등록된 클라이언트에 대해 생성된 고유 키
> * `client_secret` : 인가서버에 등록된 특정 클라이언트의 `client_id`에 대해 생성된 비밀 값
> * `response_type` : 애플리케이션이 권한 부여 코드 흐름을 시작하고 있음을 인증 서버에 알린다. `code`, `token`, `id_token`이 있으며, `token`, `id_token`은 `implict` 권한 부여 유형에서 지원해야 한다. 서버가 쿼리 문자열에 인증 코드(code), 토큰(id_token)을 반환한다.
> * `grant_type` : 권한 부여 타입 지정 - `authorization_code`, `password`, `client_credentials`, `refresh_token`
> * `redirect_uri` : 사용자가 응용 프로그램을 성공적으로 승인하면 권한 부여 서버가 사용자를 다시 응용 프로그램으로 리디렉션한다. `redirect_uri`가 초기 권한 부여 요청에 포함된 경우 서비스는 토큰 요청에서도 이를 요구해야 한다. 토큰 요청의 `redirect_uri`는 인증 코드를 생성할 때 사용된 `redirect_uri`와 정확히 일치해야 한다.
> * `scope` : 애플리케이션이 사용자 데이터에 접근하는 것을 제한하기 위해 사용된다. 사용자에 의해 특정 스코프로 제한된 권한 인가권을 발행함으로써 데이터 접근을 제한한다.
> * `state` : 응용 프로그램은 임의의 문자열을 생성하고 요청에 포함하고 사용자가 앱을 승인한 후 서버로부터 동일한 값이 반환되는지 확인해야 한다. CSRF 공격을 방지하는 데 사용된다.

#### Client Credentials Grant Type

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ed53da34-d481-4c48-b97c-acbcccfb727e.png)

* 애플리케이션이 리소스 소유자인 동시에 클라이언트 역할을 한다.
* 리소스 소유자에게 권한을 위임받아 리소스에 접근하는 것이 아니라 직접 애플리케이션을 사용할 목적으로 사용하는 것이다.
* 서버 - 서버 간 통신에서 사용할 수 있으며 IoT와 같은 장비 애플리케이션과의 통신을 위한 인증으로도 사용 가능하다.
* `client_id`와 `client_secret`을 통해 액세스 토큰을 바로 발급받을 수 있기 때문에 Refresh Token을 사용하지 않는다.
* Client 정보를 기반으로 하기 때문에 사용자 정보를 제공하지 않는다.

#### Refresh Token Grant Type

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5d86a08a-84cf-442e-b35d-b2623fc8f0c7.png)

* 액세스 토큰이 발급될 때 함께 제공되는 토큰으로서 액세스 토큰이 만료되더라도 함께 발급받았던 리프레시 토큰이 유효하다면 인증 과정을 처음부터 반복되지 않아도 액세스 토큰을 재발급 받을 수 있다.
* 한 번 사용된 리프레시 토큰은 폐기되거나 재사용이 가능하다.

#### [PKCE](https://datatracker.ietf.org/doc/html/rfc7636)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/abbc4dd5-b521-47dc-bb13-4ec6faea9220.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1abdef95-5d77-4c6d-a257-e52f3536f4bf.png)

* 코드 교환을 위한 증명 키로서 CSRF 및 권한 부여 코드 삽입 공격 방지를 위한 Authorization Code Grant Flow의 확장 버전이다.
* 권한 부여 코드 요청시 `Code Verifier`와 `Code Challenge`를 추가해 만약 Authorization Code Grant Flow에서 `authorization code`가 탈취되었을 때, `access token`을 발급하지 못하도록 차단한다.
  * `Code Verifier` : 권한부여코드 요청 전에 앱이 생성한 PKCE 요청에 대한 코드 검증기.
  * `Code Challenge` : 선택한 Hash 알고리즘으로 Code Verifier를 해싱한 후 Base64 인코딩을 한 값.
  * `Code Challenge Method` : 종류에 따라 2가지로 나뉜다. `plain`은 Code Verifier가 특정한 알고리즘을 사용하지 않도록 설정하고 `S256`은 Code Verifier가 해시 알고리즘을 사용하도록 설정한다.
* PKCE는 모바일 앱에서 Authorization Code Grant Flow를 보호하도록 설계되었으며 단일 페이지 앱에서도 사용하도록 권장하며 모든 유형의 OAuth2 클라이언트, 심지어 클라이언트 암호를 사용하는 웹 서버에서 실행되는 앱도 유용하다.
* `code_challenge_method` 검증
  * 권한부여코드 흐름에 있어 인가서버는 Code Verifier를 검증하기 위해 `code_challenge_method`를 이미 알고 있어야 한다.
  * 토큰 교환시 `code_challenge_method`가 `plain`이면 인가 서버는 전달된 Code Verifier와 보관하고 있는 `Code Challenge` 문자열과 일치하는지 확인만 하면 된다.
  * `code_challenge_method`가 `S256`이면 인가 서버는 전달된 Code Verifier를 가져와서 동일한 `S256` 해시 메서드를 사용해 변환한 다음 보관된 `Code Challenge` 문자열과 비교해서 일치 여부를 판단한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fa3f5dbc-c0c2-4b78-924c-078535599c01.png)
