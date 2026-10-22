# Spring Security - OAuth 2.0 opaque()

#### Opaque 개념 및 프로세스 이해

* Opaque 토큰은 인가 서버에서 호스트하는 OAuth 2.0 Introspection 엔드포인트로 검증한다.
* Bearer 토큰은 리소스 서버에서 처리하는 자체 검증이라면 Opaque 토큰은 인가서버에서 처리하는 원격 검증이라고 볼 수 있다.

#### 토큰 검사 및 프로세스 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4e0543f7-064c-4d88-a91a-c9f65b0d0d9f.png)

#### OpaqueTokenIntrospector

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2324ffd1-0bce-42da-bc82-50786d25bfa0.png)

* 문자열 토큰을 RestTemplate를 사용해 인가 서버 엔드포인트로 요청한다.
* 토큰이 검증되면 최종 OAuth2AuthenticatedPrincipal 타입의 객체로 디코딩하여 반환한다.
* `OAuth2AuthenticationPrincipal`은 `BearerTokenAuthentication`의 Principal 속성에 저장된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/322e6ee9-ec01-4e50-a1f5-0a3ac0ca5c7a.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b88b4f9d-cbc1-4f55-a860-80f6cdfde5c5.png)

> `NimbusOpaqueTokenIntrospector`은 사장됨.

#### CustomOpaqueTokenIntrospector

* `OpaqueTokenIntrospector` 인터페이스를 구현하여 커스텀한 구현체를 만들어 재정의 할 수 있다.
* 검증 후 리턴 타입이 `OAuth2AuthenticatedPrincipal`이기 때문에 인가 서버에서 받아온 클레임 정보를 활용해서 여러가지 커스텀 작업이 가능하다.

```java
public class CustomOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private OpaqueTokenIntrospector delegate = new NimbusOpaqueTokenIntrospector("http://localhost:8080/realms/oauth2/protocol/openid-connect/introspect ", "client", "secret");
    
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        OAuth2AuthenticatedPrincipal principal = this.delegate.introspect(token); // 인가서버로 부터 받은 클레임 정보를 저장하고 있다
        return new DefaultOAuth2AuthenticatedPrincipal(principal.getName(), principal.getAttributes(), extractAuthorities(principal)); // 커스텀하게 권한 매핑 한다

    }

    private Collection<GrantedAuthority> extractAuthorities(OAuth2AuthenticatedPrincipal principal) {
        List<String> scopes = principal.getAttribute(OAuth2IntrospectionClaimNames.SCOPE);
        return scopes.stream().map(SimpleGrantedAuthority::new)
                              .collect(Collectors.toList());
    }
}
```
