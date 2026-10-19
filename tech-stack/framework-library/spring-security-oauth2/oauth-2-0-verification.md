# Spring Security - OAuth 2.0 Verification

#### JCA(Java Cryptography Architecture) & JCE(Java Cryptography Extention)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/56850edb-bea6-4e16-9767-7e82b7b065fe.png)

* 자바는 JCA & JCE 프레임워크를 통해 자체적인 보안 관련 기능을 제공하고 있으며 전자서명(Digital Signature), 메시지 다이제스트(Message Digest, hashs), 인증서와 인증
서 유효성 검사(Certificate Validation), 키 생성 및 관리 그리고 보안 랜덤 수(Secure Random Number) 생성 등 현대 정보 통신 암호 기술 중에서 필수적인 것은 모두 제공하
고 있다.
* JCA & JCE 프레임워크는 프로바이더 보안 구조를 사용하여 보안과 관련한 다양한 API를 제공하고 있다.
* 프로바이더는 `java.security.Provider` 클래스의 구현체로 보안 알고리즘 구현체 목록을 포함하고 있고 이를 통해 보안 서비스를 구현할 수 있다.

**1. MessageDigest - 입력 정보의 메시지 다이제스트(해시)를 계산하는 데 사용**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/029eda12-b98b-4b9c-aff5-21533356d6a8.png)

* 메시지 다이제스트의 목적은 원본 파일이 그대로인지 파악하는 무결성 검사이다.
* 메시지 다이제스트 알고리즘은 입력 값으로 전달된 다양한 길이의 원본 값을 고정 길이 해시 값으로 출력한다.
* 이 알고리즘은 단방향이기 때문에 해시 값에서 거꾸로 원본 값을 도출할 수 없다.

**2. Signature - 초기화 시 전달받은 키를 가지고 데이터를 서명하고 전자 서명 유효성을 판단하는데 사용**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/48959bd8-3e98-4a45-8c05-55e45ecaca51.png)

* `Signature` 객체는 개인키로 서명하기 위해 초기화되고 서명할 원본 데이터가 제공된다. `Signature.sign()`은 개인키로 원본 데이터를 서명하면 해시된 데이터를 암호화한 Signature Bytes를 반환한다.
* 검증이 필요한 경우 검증을 위해 `Signature` 객체를 생성 및 초기화하고 개인키와 쌍을 이루는 해당 공개 키를 제공한다. 원본 데이터와 Signature Bytes가 검증 `Signature` 객체에 전달되고 `verify()`를 실행하면 공개키로 Signature Bytes의 해시 데이터를 추출하고 원본 데이터를 해시한 값과 비교해서 일치하면 Signature 객체가 성공을 보고한다.
* 서명은 메시지 다이제스트와 비대칭키 암호화가 결합한 형태로서 "SHA256WithRSA” 처럼 메시지 다이제스트 알고리즘인 "SHA256"을 사용하여 초기에 대규모 데이터를 보다 관리하기 쉬운 고정길이의 형식으로 "압축"한 다음 비대칭키 암호화인 "RSA" 알고리즘으로 고정길이의 32바이트 메시지 다이제스트에 서명한다.

#### 대칭키와 비대칭키 개념

**1. 대칭키 암호(symmetric-key algorithm)**

* 암호화 알고리즘의 한 종류로 암호화와 복호화에 같은 암호키를 쓰는 알고리즘을 의미한다.
* 대칭키 암호에서는 암호화를 하는 측과 복호화를 하는 측이 같은 암호키를 공유한다.
* 비대칭키 암호에서 공개키와 비밀키를 별도로 가지는 것과 구별되며 대부분의 대칭키 암호는 비대칭키 암호와 비교하여 계산 속도가 빠르다는 장점을 가진다.

> **MAC(Message Authentication Code)**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9f873b14-7998-462f-b6a3-48d603893eab.png)

* 메시지 인증 코드는 데이터가 변조(수정, 삭제, 삽입 등) 되었는지를 검증할 수 있도록 데이터에 덧붙이는 코드이다.
* 해시 값을 생성한다는 점에서 메시지 다이제스트와 비슷하지만, 초기화 시 비밀키(SecretKey, 대칭키)를 요구한다는 점에서 다르다.
* 메시지 다이제스트는 받은 측이 누구든 무결성 검사가 가능하지만, MAC은 오직 동일한 비밀 키를 가진 쪽에서만 전달받은 메시지의 무결성을 검사 할 수 있다.
* 암호화 해시 함수(MD5, SHA256 등)를 기반으로 하는 MAC이 잘 알려진 HMAC이다.
* HMAC은 메시지 다이제스트 알고리즘과 공유된 비밀 키의 조합으로 가능하며 데이터의 무결성과 기밀성, 송.수신자간 인증을 보증하기 위한 암호화 기법이다.

**2. 비대칭키 암호(asymmetric-key algorithm)**

* 암호화 알고리즘의 한 종류로 암호화와 복호화에 다른 암호 키를 쓰는 알고리즘을 의미한다.
* 외부에 절대 노출되어서는 안되는 개인키(Private key)와 공개적으로 개방되어 있는 공개키(Private key)를 쌍으로 이룬 형태이다.
* 비대칭키를 사용하여 두가지 암호학적 문제를 해결할 수 있다.
  * 데이터 보안 : 송신자 공개키로 암호화 ⭢ 송신자 개인키로 복호화를 통해 데이터를 안전하게 전송할 수 있는 보안 관점
  * 인증 : 송신자 개인키로 암호화 ⭢ 송신자 공개키로 복호화를 통해 메시지를 인증(부인방지)하는 것이 목적

> **RSA(Ron Rivest, Adi Shamir, Leonard Adleman)**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2aae7295-9035-4ed5-8f25-97e3da551b12.png)

* 현재 SSL/TLS에 가장 많이 사용되는 공개키 암호화 알고리즘으로 전세계 대부분의 인터넷 뱅킹(대한민국 포함)이 이 RSA-2048 암호화를 사용한다.

#### Key 생성 모델 - Key

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/690e6c9f-6fd7-4b67-a69a-b160b878814a.png)

* JCA에서 지원하는 모든 종류의 키에 대한 최상위 인터페이스이다.
  * `getAlgorithm()` : 키 알고리즘은 보통 대칭키 암호 방식(AES,DSA 등) 또는 비대칭키 연산 알고리즘(RSA)이다.
  * `getEncoded()` : 기본 인코딩된 형식의 키를 반환한다.
  * `getFormat()` : 이 키의 기본 인코딩 형식의 이름을 반환(표준형식인 X509 또는 PKCS8)한다.

#### Key 생성 모델 - KeyPair

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c0602d9e-1fe6-4d79-bd04-a8377275f317.png)

* 키 쌍(공개 키와 개인 키)을 보관하고 접근할 수 있는 기능만 제공한다.

#### Key 생성 모델 - KeyPairGenerator

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6bf4f144-9d63-4f93-829c-4b37799797f3.png)

* 공개 및 개인 키 쌍을 생성하는 데 완전히 새로운 객체를 생성한다.

#### Key 생성 모델 - KeyFactory

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/de99c48e-094d-4ba0-ae08-17d61ea83e27.png)

* 어떤 키 데이터 객체를 다른 타입의 키 데이터 객체로 전환하는데 사용한다.

#### JWT

**1. JOSE(JSON Object Signing and Encryption)**

* JSON 데이터의 컨텐츠를 암호화 또는 서명의 형태로 나타내기 위해 IETF에서 표준화 한 소프트웨어 기술 세트이다.
* 기술에는 다음 사양이 포함된다.
  * JWT(JSON Web Token, RFC7519)
    * 클레임 기반 보안 값을 나타내는 방법으로 두 당사자 간에 안전하게 전달되는 클레임을 표현하기 위한 개방형 표준이다.
    * JWT는 인증, 권한 부여 및 정보 교환에 사용된다.
    * JWS 또는 JWE 방식으로 구현된다.
  * JWS(JSON WEB SIGNITURE, RFC 7515)
    * JSON을 사용하여 디지털 서명 또는 MAC으로 보안된 콘텐츠를 표현하는 방법이다.
  * JWE(JSON WEB ENCRYPTION, RFC 7516)
    * JSON을 사용하여 의도한 수신자만 읽을 수 있도록 암호화된 데이터(토큰)를 나타내는 형식이다.
  * JWK(JSON WEB KEY, RFC 7517)
    * HMAC 이나 타원 곡선 또는 RSA 알고리즘을 사용하여 공개 키 세트를 JSON 객체로 나타내는 JSON 구조다.
  * JWA(JSON WEB ALGORITHM, RFC 7518)
    * JWS, JWK 및 JWE에 필요한 알고리즘 목록으로 JWS 헤더 및 JWS 페이로드의 내용을 서명하는 데 사용된다.

**2. JWS 구조**

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ca36ddce-2ede-4131-b2e6-0cc3729eb866.png)

* JOSE Header
  * 일반적으로 JWT인 토큰 유형과 HMAC SHA256 또는 RSA와 같은 서명 알고리즘의 두 부분으로 구성된다.
  * Base64Url로 인코딩되어 JSON 웹 토큰의 첫 번째 부분을 형성한다.
* Payload(JWT Claim Set)
  * 토큰에 포함할 내용인 클레임을 포함하는 페이로드로서 표준 필드인 7개의 등록 클레임 이름(Registered Claim Names) 및 사용자 지정 클레임 등으로 구성한다.
  * Base64Url로 인코딩되어 JSON 웹 토큰의 두 번째 부분을 형성한다.
* Signiture
  * 서명은 Base64url 인코딩을 이용하여 헤더와 페이로드를 인코딩하고 이 둘을 점(.) 구분자로 함께 연결시킴으로써 계산되어 토큰을 안전하게 확인한다.

**3. Claims**
* 클레임(claim)은 주장하고자 하는 정보를 나타내는 것으로 이 정보를 모두 가지고 있는 바디 부분을 Claim Set 이라고 부른다.
* Claim Set은 키 부분인 Claim Name과 값 부분인 Claim Value의 여러 쌍으로 이루어져 있다. 
* JWT 에는 여러개의 클레임들을 넣을 수 있다.
  * Registered Claims(등록된 클레임) : IANA 에 이미 등록된 표준 클레임이며 필수가 아닌 선택적으로 작성 가능하다. 공통으로 사용하기 위한 기본값이 정해져 있다.
  * Public Claims(공개 클레임) : 공개된 이름으로 충돌을 방지된 이름을 가지고 있어야 한다. 클레임 이름을 URI 형식으로 짓는다.
  * Private Claims(비공개 클레임) : 등록된 클레임이나 공개된 클레임이 아닌 당사자 간에 협의하에 사용되는 클레임 이름들이다. 공개 클레임과는 달리 이름이 중복되어 충돌이 될 수 있으니 사용할때에 유의해야 한다.
* `iss` : 토큰을 발급한 발급자(issuer)
* `sub` : 토큰의 주제 혹은 제목(subject)
* `aud` : 토큰을 사용할 수신자(audience)
* `exp` : 토큰 만료시간(expiration Time), 언제나 현재 시간보다 이후로 설정되어있어야 한다
* `nbf` : not before의 의미로 이 시간 이전에는 토큰을 처리하지 않아야 함을 의미한다.
* `iat` : 토큰이 발급된 시간(issued at)
* `jti` : JWT의 고유 식별자로서 중복 방지를 위해 사용하며, 일회용 토큰(Access Token 등)에 사용한다.

**4. JWT - 데이터 무결성과 취약점**
* Payload의 클레임 값을 변조하여 토큰을 생성한 후 전달하더라도 서명에서 해시된 값과 변조된 값의 해시된 값이 서로 일치하지 않기 때문에 검증이 실패하여 데이터의 안전성을 보장한다.
* SecretKey 를 탈취당했을 경우에는 중요한 정보가 도난당할 수 있는 취약점이 발생하기 때문에 SecretKey를 주기적으로 변경하도록 하는 key rotation(=key rolling) 정책이 필요할 수 있다.

#### JWK

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5d2401e7-52da-4b65-84bd-f5b0cda8a4df.png)

* 암호화 키를 저장하는 방식으로 인가서버에서 발행하는 JWT 토큰의 암호화 및 서명에 필요한 암호화 키의 다양한 정보를 담은 JSON 객체 표준이다.
* JwkSetUri 정보를 설정하면 인가서버로부터 JWK 형태의 정보를 다운로드할 수 있고 JWT 를 검증할 수 있다.

**1. JWK 확장**
* 자바 표준 보안 클래스를 사용하여 대칭키, 비대칭키 방식의 JWT 의 암호화 및 전자서명, 이후 검증을 위한 키 생성, 변환 등을 지원한다.
* 구현체로서 `RSAKey`, `OctetSequenceKey`, `ECKey`, `OctetKeyPair`가 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/de9c8474-4d9d-4940-839c-dc5b21e55a6f.png)

* 공개키 기반 암호화 방식인 RSA 의 JSON Web Key 구현체로서 RSA JWK 의 가져오기와 내보내기를 지원한다.
* PrivateKey와 PublicKey를 전달하여 새로운 RSAKey 를 생성할 수 있다.
* JWT Claims을 PrivateKey로 서명하고 PublicKey로 검증할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/10f1945b-5d8b-4505-a6c8-8b5acdfde8f3.png)

* 대칭키 기반 암호와 방식의 JSON Web Key 구현체이다.
* SecretKey를 전달하여 새로운 OctetSequenceKey를 생성할 수 있다.
*  JWT Claims을 SecretKey로 서명하고 검증할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/08093850-700e-4724-a1c6-33988cf4d473.png)

* 암호화 알고리즘 방식에 따라 JWK의 구현체가 있고 각 구현체를 편리하게 생성할 수 있는 제너레이터 클래스이다.
  * `RSAKeyGenerator` – 비대칭 암호화 알고리즘 키를 포함하는 JWK 생성기
  * `OctetSequenceKeyGenerator` - 대칭 암호화 알고리즘 키를 포함하는 JWK 생성기
  * `EcKeyGenerator` - 타원곡선 암호화 알고리즘 키 포함하는 JWK 생성기
