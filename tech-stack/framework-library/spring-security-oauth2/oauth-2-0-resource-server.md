# Spring Security - OAuth 2.0 Resource Server

#### OAuth2ResourceServerProperties & application.yml

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/673fc922-b747-468d-8352-c0276c6426c8.png)

* `jwkSetUri` : 인가 서버에서 서명한 토큰의 위치를 나타낸다.
* `issuerUri` : 인가 서버의 위치를 나타낸다.
* `publicKeyLocation` : 공개키를 가지고 있는 파일의 위치를 나타낸다.
* `jwsAlgorithm` : JWT 토큰을 서명하기 위한 알고리즘을 나타낸다.
* `introspectionUri` : 토큰을 검증하기 위한 `introspection` 엔드포인트를 나타낸다.

#### OAuth2 Resource Server 토큰 검증 플로우

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ce6a4757-3b6e-4890-851b-71455ecd126b.png)

* JWT는 로컬 검증이다.
* 기동 후 `jwk-set-uri`에서 공개키를 한 번 받아 캐시하면, 이후 요청에서는 네트워크를 전혀 필요로 하지 않는다. 서명만 맞으면 통과된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3233db69-41b2-4443-bf6a-9ba03c5b7f70.png)

* Opaque는 매 요청 원격 질의를 필요로 한다.
* 디스커버리로 `jwk-set-uri`를 알아내는 동시에, `iss` 클레임 검증 기준이 된다.
* `jws-algorithms`는 방어용이다. 기본이 RS256이고, 지정하지 않은 알고리즘으로 서명된 토큰은 거부한다.

```yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/oauth2                                 # 서비스 공급자 위치
          jwk-set-uri: http://localhost:8080/realms/oauth2/protocol/openid-connect/certs  # OAuth 2.0 JwkSetUri 엔드포인트
          jws-algorithms: RS256                                                           # OAuth 2.0 JWS 서명 알고리즘
          audiences: http://localhost:8081                                                # 수신자 위치
          public-key-location: classpath:certs/publicKey.txt                              # JWS 검증을 위한 PublicKey 파일 위치
```

#### AuthenticationEntryPoint

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d0390fe5-e37c-431c-9049-e6b61b28abc8.png)

* `OAuth2ResourceServerConfigurer`의 `AuthenticationEntryPoint`는 인증 실패를 어떤 응답으로 번역할지 정하는 전략이며, 리소스 서버는 기본값으로 `BearerTokenAuthenticationEntryPoint`를 써서 리다이렉트 대신 401과 `WWW-Authenticate: Bearer` 헤더를 내려준다.
* 핵심은 이것이 전역이 아니라 조건부로 등록된다는 점이다. `registerDefaultEntryPoint()`가 `defaultAuthenticationEntryPointFor(entryPoint, matcher)`로 걸어두어, `Bearer` 토큰이 실렸거나 브라우저 문서 요청이 아닌 경우에만 적용된다. 덕분에 같은 애플리케이션에서 브라우저 요청은 로그인 페이지로 리다이렉트되고 API 요청은 401을 받는 일이 동시에 성립한다.
