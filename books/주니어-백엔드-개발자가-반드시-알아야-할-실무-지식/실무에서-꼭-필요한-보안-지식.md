# 주니어 백엔드 개발자가 반드시 알아야 할 실무 지식 ‐ 실무에서 꼭 필요한 보안 지식

#### 정보 저장 수단 1 - 별도 저장소에 토큰과 사용자 식별 정보 저장하기(서버 별도 저장소)

* 서버는 토큰과 사용자 식별 정보를 DB나 레디스와 같은 별도 저장소에 보관할 수 있다.
* 로그인에 성공할 경우 서버는 임의 토큰 문자열을 만든 뒤 외부 저장소에 매핑 정보를 보관한다.
* 토큰 문자열을 생성할 때는 고유한 값을 생성해서 토큰 중복으로 인해 사용자 정보가 잘못 매칭되지 않도록 해야 한다.

#### 정보 저장 수단 2 - 토큰 자체에 사용자 식별 정보 저장하기(토큰)

* 토큰 자체에 사용자 식별 정보를 저장할 수도 있다.
* 사용자가 로그인에 성공하면 사용자 식별자를 값으로 갖는 JWT를 생성해서 클라이언트에 토큰으로 응답한다.
```java
String token = Jwts.builder()
    .subject("userid")
    .signWith(key)
    .compact();
```

* 서버는 클라이언트가 전송한 토큰으로부터 사용자 식별자를 구한다.
```java
try {
    Jws<Claims> jwt = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(jws);
    String userId = jwt.getPayload().getSubject();
} catch (JwtException e) {
    throw new AuthenticationException(e);
}
```
* 이 방식의 장점은 토큰만 있으면 사용자가 누구인지 확인할 수 있다는 점이다.
* 별도의 외부 DB에 토큰 데이터를 저장할 필요가 없고 메모리에 토큰 데이터를 저장할 필요가 없기에 서버를 수평 확장하기도 쉽다.
* 단점은 네트워크 트래픽이 증가한다는 것이다. 토큰 안에 데이터가 추가되므로 서버와 클라이언트가 주고받는 데이터의 크기가 증가한다.
* 트래픽 규모가 크면 데이터가 조금만 커져도 증가하는 데이터 양이 무시하기 힘들 정도로 커질 수 있다.
* 트래픽 양이 증가하면 비용도 같이 증가하므로 토큰에는 최소한의 필요한 데이터만 넣어야 한다.
* 토큰 데이터를 서버에서 제어할 수 없다는 것도 단점 중 하나이다.
* 서버에 토큰 데이터가 저장되면 쉽게 토큰 데이터를 삭제할 수 있으나 클라이언트에 전송된 토큰 데이터는 클라이언트에 저장되므로 서버에서 삭제하거나 변경할 수 없다.
* 이런 이유로 토큰을 클라이언트에 보관할 때는 토큰에 유효 시간을 설정하기도 한다.

#### 토큰 송수신 - 쿠키를 사용해서 토큰을 전송하기

* 웹 사이트는 주로 쿠키 방식을 사용한다.
* 서버 세션도 쿠키를 사용해서 세션 ID를 주고받는다.
* 서버는 사용자가 로그인에 성공하면 토큰 문자열을 값으로 갖는 쿠키를 웹 브라우저에 응답한다.
* 웹 브라우저는 서버가 전송한 쿠키를 모든 요청에 함께 전송하므로 토큰을 서버에 전송하기 위해 별도의 자바스크립트 코드를 작성할 필요가 없다.

#### 토큰 송수신 - 특정 이름을 갖는 헤더를 사용해서 토큰을 전송하기

* 쿠키도 헤더를 통해 전송되지만 여기선 쿠키를 제외한 다른 헤더를 말한다.
* 많은 앱이 서버와 통신할 때 헤더를 통해 토큰을 전송한다. 헤더 이름은 `Token`, `X-token`, `Auth` 등 알맞게 정하면 된다.
* OAuth 2.0처럼 Authorization 헤더를 사용하기도 한다.
* 클라이언트는 토큰을 로컬에 저장했다가 서버 API 요청을 호출할 때, 헤더를 이용해서 토큰을 전송한다.

#### 토큰 보안

* 보안을 위해 토큰을 사용하는 만큼 자체 보안에도 신경을 써야 한다.
* 서버 보안을 철저히 해도 클라이언트가 보안에 취약하면 토큰이 탈취될 수 있기 때문이다.
* 토큰을 탈취한 클라이언트는 원래 토큰 소유자처럼 행세할 수 있다.
* 토큰 탈취에 따른 보안 문제를 완화하는 방법은 토큰 유효 시간에 제한을 두는 것이다.

#### 토큰 유효 시간

* 보안 사고 영향을 줄이려면 토큰을 무효화해서 강제로 로그아웃시키는 기능도 필요하다.
* 토큰 데이터를 DB나 레디스와 같은 외부 저장소에서 보관하면 토큰 데이터를 삭제하거나 유효하지 않은 상태로 변경해서 토큰을 무효화할 수 있다.
* 토큰 자체에 데이터를 저장하는 방식을 사용하면 클라이언트에 토큰 데이터가 저장되므로 서버에서 토큰을 무효화하기가 쉽지 않다.

#### 토큰 유효 시간 설정 1 - 토큰 생성 시점을 기준으로 제한 시간을 두는 방식

* 유효 시간을 지정하는 방식이다.
* 예를 들어, 9시에 유효 시간이 1시간인 토큰을 생성하면 해당 토큰은 10시에 만료된다.
* 10시가 지나면 토큰이 유효하지 않은 것으로 판단해서 사용자의 접근을 차단한다.

#### 토큰 유효 시간 설정 2 - 마지막 접근 시간을 기준으로 토큰 유효 시간 정하기

* 유효 시간이 10분일 때, 마지막 접근 시간이 11시면 만료 시간은 11시 10분이 된다.
* 다시 11시 5분에 접근하면 만료 시간이 11시 15분으로 바뀐다.

#### 토큰 재발급

* 인증과 인가에서 사용하는 토큰으로 액세스 토큰과 리프레시 토큰이 있다.
  * **액세스 토큰(Access Token)**
    * 보호된 리소스(API)에 접근할 때 요청마다 함께 전송되어, 인증·인가를 처리하는 토큰이다. 
    * 보통 HTTP 요청의 `Authorization: Bearer <token>` 헤더에 담아 보낸다.
    * 사용자 식별 정보(subject), 권한(role). 만료 시간(exp) 등을 담으며, 서버는 이 토큰만 검증하면 되므로 별도 세션 조회 없이 인증할 수 있다.
    * 만료 시간을 짧게 두는 이유는 토큰이 탈취되더라도 유효 기간이 짧아 피해를 줄일 수 있기 때문이다.
    * 그러나 매 요청에 노출되는 만큼 탈취 위험이 상대적으로 높다.
  * **리프레시 토큰(Refresh Token)**
    * 액세스 토큰이 만료됐을 때 새 액세스 토큰을 발급받기 위한 용도로만 사용되는 토큰이다.
    * 일반 API 요청에는 사용되지 않고 오직 토큰 재발급 엔드포인트에만 전달되므로 노출 빈도가 낮다.
    * 만료 시간을 상대적으로 길게 두어, 사용자가 자주 재로그인하지 않아도 되도록 한다.
    * 유효 시간이 길어 탈취 시 위험이 크므로 서버 측 저장소(DB, Redis 등)에 보관해 강제 무효화가 가능하도록 하거나, 재발급 시마다 새로운 리프레시 토큰으로 교체하는 회전 방식을 함께 쓰는 경우가 많다.
    * 클라이언트에서 노출을 최소화하기 위해 `HttpOnly` 쿠키 등 안전한 저장소에 보관하는 것이 권장된다.
* 액세스 토큰은 인증된 사용자임을 식별하기 위한 목적으로 사용된다.
* 액세스 토큰의 만료 시간은 짧게 지정하는데 만료 시간이 짧으면 사용자 로그인이 풀려 불편을 줄 수 있다.
* 이 때, 다시 액세스 토큰을 발급받아 로그인을 다시 하지 않아도 인증 상태를 유지할 수 있도록 하는 것이 바로 리프레시 토큰이다.
* 액세스 토큰과 리프레시 토큰을 지원하는 시스템은 사용자가 로그인에 성공하면 만료 시간이 짧은 액세스 토큰과 만료 시간이 상대적으로 긴 리프레시 토큰을 함께 발급한다.
* 이후 액세스 토큰이 만료되면 리프레시 토큰을 이용해서 새로운 액세스 토큰을 발급해준다.
* 이를 통해 사용자는 리프레시 토큰이 만료될 떄까지 재로그인없이 인증 상태를 유지할 수 있다.

#### 인가와 접근 제어 모델

* 인증과 토큰은 사용자가 누구인지 그리고 정상적으로 접근하는지 확인하는 역할을 한다면 인가는 사용자가 요청한 기능을 실행할 권한이 있는지 확인하는 역할을 한다.
* 접근 제어의 기본은 접근한 사용자를 토큰이나 세션으로 식별하는 것이다.
* 사용자가 접근할 수 있는 기능을 관리하기 위한 모델을 접근 제어(Access Control) 모델이라고 한다.
* 대표적인 접근 제어 모델로는 역할 기반 접근 제어(Role-Based Access Control, RBAC)이 있다.
* RBAC는 역할별로 실행 가능한 기능 집합을 할당하고 사용자에게는 역할을 부여한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b669d62b-a885-4a37-b394-6efece111681.png)

* 그림과 같이 역할은 허용된 기능 집합을 갖는다.
* 역할별 권한 부여 방식과 사용자별 권한 부여 방식은 각각 장단점이 있기 때문에 단독으로 사용하기보다는 함께 사용하는 경우가 많다.
* 역할별 권한 부여 방식을 사용하면 권한을 체계적으로 관리할 수 있다.
* 사용자에게 권한을 일일이 부여할 필요 없이 역할만 부여하면 되므로 권한 관리가 쉬워진다.
* 사용자의 속성을 이용해서 접근을 제어하는 속성 기반 접근 제어(Attribute-Based Access Control, ABAC)도 있다.
* 사용자 IP 주소에 따라 특정 기능의 접근을 허용하거나 제한할 수 있다.
* 속성을 활용하면 정교한 접근 제어가 가능하나 그만큼 구현이 복잡하고 사용할 속성과 규칙을 정의하는 데도 많은 시간이 소요된다.

#### 데이터 암호화

* 데이터를 암호화하는 방식에는 크게 단방향 암호화와 양방향 암호화가 있다.

#### 단방향 암호화

* 단방향 암호화는 암호화한 데이터를 복호화할 수 없는 암호화 방식이다.
* 단방향 암호화는 해시 함수를 사용해서 데이터를 해시 값으로 변환한다.
* 해시 함수 알고리즘에는 SHA-256, MD5, BCrypt 등이 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/cee335a1-789d-4a0b-a6db-cff5de5b4c26.png)

* SHA-256과 같은 해시 알고리즘은 원본 데이터를 유추하기 어렵게 하기 위해 원본 데이터가 조금만 달라도 완전히 다른 해시 값을 생성한다.
* 예를 들어, '가나다라'를 암호화하면 해시 값이 'df2ef824'로 시작하는데 '가나다마'를 암호화하면 해시 값이 'fa262235'로 시작하는 식이다.
* 단방향 암호화는 로그인 비밀번호 같은 문자열을 암호화하는 데 주로 사용되지만, 실제 암호화는 바이트 데이터를 기준으로 동작한다.
* 예를 들어, SHA-256 알고리즘을 이용해서 단방향 암호화하는 자바 코드는 다음과 같은데, 암호화 메서드의 입력 파라미터와 리턴 타입이 모두 바이트 배열이다.
```java
byte[] origin = input.getBytes("UTF-8");
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(origin); // byte 배열을 암호화
```
* 입력 파라미터가 바이트 배열이므로 문자열을 암호화할 때는 바이트 배열로 변환해서 전달한다.
* 바이트 배열로 변환할 때는 문자열에 알맞은 캐릭터셋을 이용한다.
* 암호화한 결과를 DB와 같은 저장소에 읽을 수 있는 형태로 저장하려면 바이트 배열을 문자열로 표현해야 한다.
* 이를 위해 바이트 배열을 16진수 표기법이나 Base64 표기법을 사용해서 문자열로 표현한다.
* 다음은 문자열을 암호화해서 16진수 문자열로 변환하는 기능을 구현하는 예제 코드이다.
```java
public static String encrypt(String input) {
    StringBuilder hexString = new StringBuilder();
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes("UTF-8"));
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            } else {
                hexString.append(hex);
            }
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    return hexString.toString();
}
```

#### 충돌 저향성(collision resistance)

* 해시 함수는 원본 데이터에 상관없이 일정한 길이의 해시 값을 생성한다.
* 길이가 제한되기 때문에 서로 다른 데이터가 동일한 해시 값을 가질 수 있다.
* 서로 다른 데이터에 대해 최대한 다른 해시 값을 생성하는 해시 알고리즘이 좋다고 할 수 있다.
* 동일한 해시 값을 갖는 서로 다른 데이터를 찾기 어려울 때, 해시 함수는 충돌 저향성을 갖는다.
* 해시 함수의 생성 결과가 길수록 충돌 날 가능성이 줄어든다.
* 예를 들어, SHA-256과 SHA-512의 해시 값은 각각 256비트와 512비트이므로 SHA-256 대비 SHA-512가 충돌 가능성이 더 낮다.

#### 값의 비교

* 단방향 암호화는 해시 함수로 생성한 해시 값이 같다면 두 데이터가 같다고 간주한다.
* 단방향 암호화는 원본 데이터로 복호화할 수 없기 때문에, 사용자가 비밀번호를 잊었을 때 기존 비밀번호를 알려주는 기능은 구현할 수 없다.
* 대신 시스템은 임의의 문자열로 비밀번호를 초기화하고 사용자는 등록된 이메일이나 문자 메시지를 통해 초기화된 비밀번호를 받아 로그인하도록 한다.

#### Salt로 보안 강화하기

* 같은 해시 알고리즘을 사용하면 동일한 원본 데이터에 대해 항상 동일한 해시 값이 생성된다.
* 이 특성은 해시 값이 유출됐을 때, 원본을 유추하기 쉽게 만든다.
* 같은 원본 데이터에 대해 항상 동일한 해시 값을 생성하는 것은 보안에 취약하다.
* 해시 알고리즘은 이 취약점을 보완하기 위해 솔트(Salt)를 사용한다.
* 솔트는 임의의 값이며, 암호화할 때, 솔트를 함께 사용하면 솔트 값에 따라 결과 해시 값이 달라진다.
```java
public static String encrypt(String input) {
    StringBuilder hexString = new StringBuilder();
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());  // salt 추가
        byte[] hash = digest.digest(input.getBytes("UTF-8"));
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            } else {
                hexString.append(hex);
            }
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    return hexString.toString();
}
```
* 솔트를 사용해서 암호화한 해시 값은 유출되더라도 미리 계산해둔 해시 표에서 일치하는 값을 찾기 어렵다.
* 사용자마다 서로 다른 솔트를 사용하면 보안 강도를 더욱 높일 수 있다.

#### 양방향 암호화

* 양방향 암호화는 암호화와 복호화가 모두 가능한 방식이다.
* 서버에 접속할 때 사용하는 SSH 프로토콜이나 API 호출 시 사용하는 HTTPS처럼 보안이 중요한 데이터 송수신 과정에서 주로 사용된다.
* 대표적인 양방향 암호화 알고리즘으로는 AES나 RSA가 있다.
* 양방향 암호화는 암호화와 복호화할 때, 키를 사용한다. 같은 알고리즘, 같은 원본 데이터라도 어떤 키를 사용하냐에 따라 결과가 달라진다.
* 양방향 암호화는 대칭키 방식과 비대칭키 방식으로 나뉜다.

#### 양방향 암호화 - 대칭키 방식

* 대칭키 암호화는 암호화와 복호화할 때 동일한 키를 사용한다.
* 즉, 암호화와 복호화를 수행하는 쌍이 같은 키를 공유해야 한다.
* 이 방식에서는 키가 유출되면 누구나 암호화된 데이터를 복호화할 수 있기 때문에 키의 보안이 매우 중요하다.

#### 양방향 암호화 - 비대칭키 방식

* 비대칭키 암호화는 암호화할 때와 복호화할 때 서로 다른 키를 사용한다.
* 비대칭키 암호화에서는 공개 키와 개인 키를 생성한다.
* 공개 키는 누구에게나 공개할 수 있는 키이나 반대로 개인 키는 키 소유자만 접근할 수 있어야 한다.
* 공개 키는 데이터를 암호화할 때 사용하며, 개인 키는 암호화된 데이터를 복호화할 때 사용된다.

#### 대칭 키 암호화

* 대표적인 대칭 키 암호화 알고리즘에는 AES가 있다.
* AES 알고리즘을 사용할 때는 다음의 두 값을 생성해서 공유한다.
  * 키(Key)
  * IV(Initialization Vector, 초기화 벡터)
* AES는 키 값으로 128비트, 192비트, 256비트 중 하나를 사용한다.
* 바이트로는 각각 16바이트, 24바이트, 32바이트가 된다.
* 키는 무작위로 생성해서 유추가 어려워야 한다.
```java
public static byte[] generateSecretKey() throws Exception {
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(256);  // 256비트 키 생성
    SecretKey secretKey = keyGenerator.generateKey();
    return secretKey.getEncoded();  // 길이 32바이트의 배열 반환
}
```
* 생성한 키는 바이너리 파일이나 문자열 형식으로 변환해서 보관하고 공유한다.
* 보관된 키를 이용해 암호화를 하려면 해당 키 데이터로부터 SecretKey 객체를 만들어야 한다.
* 다음은 바이트 배열로부터 SecretKey를 생성하는 코드다.
```java
SecretKey key = new SecretKeySpec(bytes, "AES");
```
* 같은 키를 사용해서 같은 데이터를 암호화하면 항상 같은 결과가 생성된다.
* 이처럼 반복되는 패턴은 공격자가 암호화된 데이터를 분석할 수 있는 단서를 줄 수 있는데 이런 패턴을 막기 위해 IV를 사용한다.
* IV는 임의의 바이트 배열로서 암호화할 때 함께 사용되면 같은 키를 쓰더라도 결과값이 매번 달라져 패턴이 드러나는 것을 방지할 수 있다.
* 복호화할 때는 키와 함께 IV도 필요하기 때문에 IV 역시 안전하게 전달하거나 저장해야 한다.
```java
public static String encrypt(String plain, SecretKey key, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // 알고리즘
    IvParameterSpec parameterSpec = new IvParameterSpec(iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
    byte[] encrypted = cipher.doFinal(plain.getBytes("UTF-8")); // 암호화 실행
    return Base64.getEncoder().encodeToString(encrypted);
}

public static String decrypt(String encrypted, SecretKey key, byte[] iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // 알고리즘
    IvParameterSpec parameterSpec = new IvParameterSpec(iv);
    cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
    byte[] decoded = Base64.getDecoder().decode(encrypted);
    byte[] decrypted = cipher.doFinal(decoded);                 // 복호화 실행
    return new String(decrypted, "UTF-8");
}
```

#### 비대칭키 암호화

* 비대칭키 암호화는 공개 키/개인 키 쌍을 생성한 뒤에 공개 키를 공유한다.
```java
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
keyGen.initialize(2048);                           // 키 길이 2048비트로 설정
keyPair keyPair = keyGen.generateKeyPair();
PublicKey publicKey = keyPair.getPublic();         // 공개 키
PrivateKey privateKey = keyPair.getPrivate();      // 개인 키
byte[] publicKeyBytes = publicKey.getEncoded();    // 공개 키 바이트 배열
byte[] privateKeyBytes = privateKey.getEncoded();  // 개인 키 바이트 배열
```
* 공개 키는 바이트 배열을 Base64 형식으로 인코딩해서 문자열로 공유하거나 바이트 배열 자체를 파일로 저장해서 공유한다.
* 개인 키도 유사하게 Base64로 인코딩한 문자열을 저장하거나 바이트 배열 자체를 바이너리 형식으로 저장한다.
* 문자열이나 바이너리 형태로 저장한 공개 키와 개인 키는 다시 코드에서 사용할 수 있는 형태로 변환해야 한다.
```java
public static KeyPair getKeyPairIdFromBytes(byte[] publicKeyBytes, byte[] privateKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    PrivateKey privateKey = keyFactory.generatePublic(new PKCS8EncodedKeySpec(privateKeyBytes));
    return new KeyPair(publicKey, privateKey);
}
```
```java
public static String encrypt(String plain, PublicKey key) {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    byte[] encrypted = cipher.doFinal(plain.getBytes("UTF-8"));
    return Base64.getEncoder().encodeToString(encrypted);
}

public static String decrypt(String encrypted, PrivateKey key) {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, key);
    byte[] decoded = Base64.getDecoder().decode(encrypted);
    byte[] decrypted = cipher.doFinal(decoded);
    return new String(decrypted, "UTF-8");
}
```

#### HMAC을 이용한 데이터 검증

* HMAC는 Hash-based Message Authentication Code의 약자로, 메시지의 무결성과 인증을 보장하기 위해 사용하는 암호화 기술이다.
* HMAC은 해시 함수와 비밀 키를 이용해서 다음 2가지를 보장한다.
  * 메시지 무결성 - 메시지가 중간에 위변조가 되지 않았다.
  * 인증 - 메시지 발신자를 인증할 수 있다.
* 메시지 발신자와 수신자는 둘만 알고 있는 비밀 키를 공유한다. 이 비밀 키는 외부에 절대로 노출되면 안 된다.
* 메시지 발신자는 메시지를 비밀 키로 해싱해서 생성한 MAC를 원본 메시지와 함께 수신자에게 보낸다.
* 수신자는 수신한 메시지와 비밀 키를 이용해서 MAC를 다시 생성한 뒤, 발신자가 보낸 MAC과 비교한다.
* 두 값이 같으면 메시지가 변경되지 않았음을 보장할 수 있다. 반대로 두 값이 다르면, 메시지는 유효하지 않은 것으로 판단한다.
```java
public static class HMAC {
    private String secretKey;

    public HMAC(String secretKey) {
        this.secretKey = secretKey;
    }

    public String hmac(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(message.getBytes("UTF-8");
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

#### 방화벽으로 필요한 트래픽만 허용하기

* 방화벽은 물리적인 장비로 존재하기도 하고 가상 방화벽으로 존재하기도 한다.
* 방화벽은 네트워크 통신을 두 방향으로 제어한다.
  * 인바운드 트래픽 : 외부에서 내부로 유입되는 것
  * 아웃바운드 트래픽 : 내부에서 외부로 유출되는 것
* 웹 방화벽(WAF)을 사용하면 HTTP/HTTPS 수준에서 발생하는 공격도 방어할 수 있다.
* 웹 방화벽은 SQL 인젝션, XSS같은 웹 기반 위협을 감지하고 차단한다.

#### 감사 로그 남기기

* 감사 로그는 데이터 조회 및 변경 이력, 작업자, 시점 등의 정보를 기록해 활동을 입증하는 증거로 사용된다.
* 이런 로그는 보안이 중요한 시스템에서 사고가 발생했을 때 문제 해결에 큰 도움이 된다.

#### 데이터 노출 줄이기

* 서비스 운영자는 백오피스에서 다양한 고객 정보를 조회할 수 있다.
* 다수의 고객 정보를 쉽게 획득하지 못하게 만드는 방법으로 마스킹이 있다.
* API 응답은 원본 데이터가 보이고 프론트 코드에서만 마스킹 처리를 하면 안 된다. 이렇게 되면 웹 브라우저 개발자 도구로 손쉽게 여러 고객 정보를 조회할 수 있게 되기 때문이다.
* 로그 메시지도 신경을 써야 한다.

#### 비정상 접근 처리

* 사용자가 평소와 다른 행동 패턴을 보이면 비정상 접근으로 판단하고 사용자에게 해당 내용을 알려주는 서비스가 있다.
  * 평소와 다른 장소에서 로그인함
  * 평소와 다른 기기에서 로그인함
  * 로그인에 여러 차례 실패함
* 사용자에게 알리는 것을 넘어 계정 사용 중지와 같은 정책을 적용할 수도 있다.
* 예를 들어, 연속적으로 로그인에 실패하면 일시적으로 계정을 잠그기도 한다. 이를 통해 브루트 포스 공격에 대응한다.

#### 브루트 포스(brute force) 공격

* 특정한 암호를 풀기 위해 가능한 모든 값을 대입하는 것을 말한다.
* 대부분의 암호화 방식은 이론적으로 무차별 대입 공격에 대해 안전하지 못하며, 충분한 시간이 존재한다면 암호화된 정보를 해독할 수 있다.

#### 시큐어 코딩(Secure Coding)

* SQL 인젝션 공격은 코드의 취약점을 이용해서 SQL 쿼리에 코드를 삽입하는 방식으로 공격하는 해킹 수법이다.
* SQL 인젝션을 막는 가장 쉬운 방법은 Prepared Statement를 사용하는 것이다.
* Prepared Statement를 사용하면 값에 포함된 특수 문자를 알맞게 변환해서 SQL을 만들어주기 때문에 SQL 인젝션 공격을 피할 수 있다.
* SQL 인젝션 외에도 서버 프로그램을 개발할 때는 아래 항목에 신경 써야 한다.
  * 입력 값 검증 : 클라이언트가 전송한 값이 올바르다고 가정하지 말고 모든 값을 검증해야 한다. 검사 항목으로는 필수 여부, 길이 제한, 미허용 값 등이 있다.
  * 개인 정보/민감 정보 암호화 : 로그인 암호와 바이오 정보처럼 인증에 사용되는 정보뿐만 아니라 주민 번호, 운전 면허 번호 같은 고유 식별 정보도 암호화해야 한다.
  * 에러 메시지에 시스템 정보 미노출 : 에러 메시지에 내부 IP나 DB IP와 같은 시스템 정보가 노출되지 않도록 한다.
  * 보안 통신 : HTTPS처럼 데이터를 암호화해서 데이터 유출을 방지한다.
  * CORS(Cross Origin Resource Sharing) : 허용된 도메인만 서버 자원에 접근할 수 있도록 제한한다.
  * CSRF(Cross-Site Request Forgery) : 주요 기능은 타 사이트에서 위조 공격이 들어오는 것을 방지하기 위해 CSRF 토큰, SameSite 쿠키, 캡차 등을 사용한다.
