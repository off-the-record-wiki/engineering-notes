# Spring Security - OAuth 2.0

#### [OAuth 2.0](https://oauth.net/2/)

* OAuth 2.0 인가 프레임워크는 애플리케이션이 사용자 대신하여 사용자의 자원에 대한 제한된 액세스를 얻기 위해 승인 상호 작용을 함으로써 애플리케이션이 자체적으로 액세스 권한을 얻도록 한다.
* 즉, 사용자가 속한 사이트의 보호된 자원에 대해 애플리케이션의 접근을 허용하도록 승인하는 것을 의미한다.
* Delegated Authorization Framework : 애플리케이션이 사용자의 데이터에 접근하도록 권한을 부여한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/43d4d534-6699-4d69-9be5-ee376b896420.png)

#### OAuth 2.0 Roles

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f8cdbf88-b99e-4731-b512-bc0577e30b67.png)

**1. Resource Owner(자원 소유자)** 
* 보호된 자원에 대한 접근 권한을 부여할 수 있는 주체를 말한다. 사용자로서 계정의 일부에 대한 접근 권한을 부여하는 사람이다.
* 사용자를 대신하여 작동하려는 모든 클라이언트는 먼저 사용자의 허가를 받아야 한다.

**2. Resource Server(보호 자원 서버)**
* 타사 어플리케이션에서 접근하는 사용자의 자원이 포함된 서버를 의미한다.
* 액세스 토큰을 수락 및 검증할 수 있어야 하며 권한 체계에 따라 요청을 승인할 수 있어야 한다.

**3. Authorization Server(인가 서버)**
* 클라이언트가 사용자 계정에 대한 동의 및 접근을 요청할 때 상호 작용하는 서버로서 클라이언트의 권한 부여 요청을 승인하거나 거부하는 서버이다.
* 사용자가 클라이언트에게 권한 부여 요청을 승인한 후 액세스 토큰을 클라이언트에게 부여하는 역할을 한다.

**4. Client(클라이언트)**
* 사용자를 대신해 권한을 부여받아 사용자 리소스에 접근하려는 애플리케이션
* 사용자를 권한 부여 서버로 안내하거나 사용자의 상호작용 없이 권한 부여 서버로부터 직접 권한을 얻을 수 있다.

#### OAuth 2.0 Client Types

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d21cf08b-1a08-43ce-b669-32d6a75e1c93.png)

**1. 개요**
* [RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
* 인증 서버에 클라이언트를 등록할 때 클라이언트 자격 증명인 클라이언트 아이디와 클라이언틍 암호를 받는다.
* 클라이언트 암호는 비밀이고 그대로 유지되어야 하는 반면 클라이언트 ID는 공개값이다.
* 이 자격증명은 인증 서버에 대한 클라이언트 ID를 증명한다.

**2. 기밀 클라이언트(Confidential Clients)**
* 기밀 클라이언트는 `client_secret`의 기밀성을 유지할 수 있는 클라이언트를 의미한다.
* 일반적으로 사용자가 소스코드에 액세스할 수 없는 서버에서 실행되는 응용 프로그램으로 NET, Java, PHP 및 Node JS와 같은 서버 측 언어로 작성된다.
* 이런 유형의 애플리케이션은 대부분 웹 서버에서 실행되기 때문에 일반적으로 웹앱이라고 부른다.

**3. 공개 클라이언트(Public Clients)**
* 공개 클라이언트는 `client_secret`의 기밀을 유지할 수 없으므로 이런 앱에는 `secret`이 사용되지 않는다.
* 브라우저(SPA)에서 실행되는 JS 애플리케이션, Android 또는 iOS 모바일 앱, 데스크톱에서 실행되는 기본 앱뿐만 아니라 IoT/임베디드 장치에서 실행되는 애플리케이션 등이 있다.
* Chrome 개발자 콘솔이나 디스어셈블러와 같은 디버깅 도구를 사용해 바이너리/실행 코드에서 기밀 정보를 추출할 수 있기 때문에 공개로 간주된다.
* 서버 측이 아닌 리소스 소유자가 사용하는 장치에서 실행되는 모든 클라이언트는 공개 클라이언트로 간주되어야 한다.

#### OAuth 2.0 Token Types

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d470c529-d3c3-4af6-8aef-4d5e2b4253a0.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f959fe1d-87f4-4eeb-9a05-e89a07f72455.png)

**1. Access Token**
* 클라이언트에서 사용자의 보호된 리소스에 접근하기 위해 사용하는 일종의 자격 증명으로서 역할을 하며 리소스 소유자가 클라이언트에게 부여한 권한 부여의 표현이다.
* 일반적으로 JWT(Json Web Token) 형식을 취하지만 사양에 따라 그럴 필요는 없다.
* 토큰에는 해당 액세스 기간, 범위 및 서버에 필요한 기타 정보가 존재한다.
* 타입에는 식별자 타입(Identifier Type)과 자체 포함타입(Self-contained Type)이 있다.

**2. Refresh Token**
* 액세스 토큰이 만료된 후 새로운 액세스 토큰을 얻기 위해 클라이언트 응용 프로그램에서 사용하는 자격 증명이다.
* 액세스 토큰이 만료된 경우 클라이언트는 권한 부여 서버로 인증하고 Refresh Token을 전달한다.
* 인증 서버는 Refresh Token의 유효성을 검사하고 새로운 액세스 토큰을 발급한다.
* Refresh Token은 액세스 토큰과 달리 권한 서버 토큰 엔드포인트에만 보내지고 리소스 서버에는 보내지 않는다.

**3. Authorization Code**
* 권한 부여 코드 흐름에서 사용되며 이 코드는 클라이언트가 액세스 토큰과 교환할 임시 코드이다.
* 사용자가 클라이언트가 요청하는 정보를 확인하고 인가 서버로부터 리다이렉트되어 받아온다.

#### 식별자 타입(Identifier Type)과 자체 포함 타입(Self-contained Type)의 차이점 정리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8e2c4393-e58a-4ab0-8137-f3c7ff7ded18.png)

* 식별자 타입
  * 인가서버는 데이터 저장소에 토큰의 내용을 저장하고 이 토큰에 대한 고유 식별자만 클라이언트에 다시 발행한다. 
  * 이 토큰을 수신하는 API는 토큰의 유효성을 검사하기 위해 인가서버에 백채널 통신을 열고 DB를 조회해야 한다.
* 자체 포함 타입
  * JWT 토큰 형식으로 발급되며 클레임 및 만료가 있는 보호된 데이터 구조이다.
  * 리소스 서버 API가 검증 키 등의 핵심 자료에 대해 알게 되면 발급자와 통신할 필요없이 자체 포함된 토큰의 유효성을 검사할 수 있다.
  * 특정한 암호화 알고리즘에 의해 개인키로 서명되고 공개키로 검증할 수 있으며 만료될 때까지 유효하다.
