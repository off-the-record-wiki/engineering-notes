# Spring Security - OAuth 2.0 Resource Server API

#### JWT API

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/785e4685-111c-4e2a-bb73-cb56d011aecb.png)

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

* `spring.security.oauth2.resourceserver.jwt.issuer-uri` : 프로퍼티를 설정하면 JWT로 인코딩한 Bearer 토큰을 검증하는 리소스 서버가 자동으로 설정된다.
* Open ID Connect Provider 설정 엔드포인트 또는 인가 서버 메타데이터 엔드포인트를 검색해서 `jwk-set-url` 엔드포인트를 찾아 검증을 진행한다.
* 2가지 검증 전략을 설정한다.
  * 리소스 서버는 인가서버의 `jwk-set-uri` 엔드포인트로 유효한 공개키를 질의하기 위한 검증 전략을 설정한다.
  * `issuer-uri`에 대한 각 JWT 클레임을 검증할 전략을 설정한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d1e259ad-bc42-47d6-8dbd-21629d058d9a.png)

#### JwtDecoder 세부 흐름

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8b336413-4416-4716-b11b-fda7030011ef.png)

* `JwtDecoder`는 문자열로 된 JWT(Json Web Token)를 컴팩트 클레임 표현 형식에서 Jwt 인스턴스로 디코딩하는 역할을 한다.
* `JwtDecoder` 는 JWT 가 JSON 웹 서명(JWS) 구조로 생성된 경우 JWS 서명에 대한 검증의 책임이 있다.
* 기본 구현체로 `NimbusJwtDecoder`가 있다.
* `JwkSetUriJwtDecoderBuilder` : 원격 JWK Set URL에서 공개키를 받아 검증하는 디코더를 만들며, 넷 중 유일하게 키 회전을 지원해 `jwk-set-uri`와 `issuer-uri` 설정이 모두 여기로 합류한다.
* `PublicKeyJwtDecoderBuilder` : 로컬 `RSAPublicKey` 하나로 검증하는 디코더를 만들며 네트워크가 없는 대신 키 회전이 불가능하다.
* `SecretKeyJwtDecoderBuilder` : 발급자와 공유한 대칭키로 HMAC(HS256·HS384·HS512) 서명을 검증하는 디코더를 만든다.
* `JwkSourceJwtDecoderBuilder` : `Nimbus JWKSource`를 통째로 주입받는 탈출구로 다중 인가서버나 커스텀 캐시처럼 위 셋으로 표현되지 않는 키 조달 방식을 직접 구현할 때 쓴다.
* `JwkSetUriJwtDecoderBuilder$SpringJWKSource` : `JwkSetUriJwtDecoderBuilder` 내부에서 실제로 JWK Set을 가져오고 캐싱하는 JWKSource 구현체다.
* `RestTemplateWithNimbusDefaultTimeouts` : 조회에 쓰이는 기본 `RestTemplate`으로, 인가서버 지연이 리소스 서버 전체로 번지지 않도록 연결·읽기 타임아웃을 각각 500ms로 못 박아둔다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/64dba558-fee7-47ca-89cf-e659e6b9b1e7.png)

#### JwtDecoder 생성

```java
// JwtDecoderConfiguration이 @ConditionalOnMissingBean(JwtDecoder.class)로 Decoder를 자동 구성한다.
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
    return http.build();
}
```

```java
// 검증기를 추가하려면 @Bean으로 직접 선언한다.
private static final String ISSUER = "http://localhost:8080/realms/oauth2";

@Bean
JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withIssuerLocation(ISSUER)        // 디스커버리로 jwk-set-uri 획득
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .validateType(true)                // typ 헤더 검사, ID 토큰 오용 차단
            .build();

    decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
            new JwtIssuerValidator(ISSUER),                 // iss
            new JwtAudienceValidator("oauth2-client-app")   // aud
    ));
    return decoder;
}
```

```java
// 권한을 매핑하길 원한다면?
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthorityPrefix("ROLE_");            // 기본 SCOPE_
    authorities.setAuthoritiesClaimName("roles");       // 기본 scope 또는 scp
    authorities.setAuthoritiesClaimDelimiter(" ");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    converter.setPrincipalClaimName("preferred_username");  // 기본 sub
    return converter;
}
```
