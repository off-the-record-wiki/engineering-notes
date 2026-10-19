# Spring Security - OAuth 2.0 Resource Server Permission Implementation

#### Scope 기반 권한 매핑 구현

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/29917011-d73c-4eda-9ccb-60623769a496.png)

* 클라이언트가 인가 서버로 OAuth 2.0 권한 부여 요청을 할 때 사용자 리소스에 대한 접근 범위를 제한하기 위해 마련한 장치이다.
* 클라이언트는 하나 이상의 scope를 요청할 수 있으며 동의 화면에서 사용자가 scope를 지정하게 되면 scope 범위에 제한된 토큰이 발행된다.

#### Scope로 리소스 접근 제어

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/bb7cfd71-4653-4544-a416-761a33370696.png)

* 권한 부여 요청시 인가서버에 지정했던 Scope가 리소스 서버의 권한 범위에 포함되지 않으면 접근이 거부되고 포함되면 접근이 허용된다.

#### 권한 구성 커스터마이징 - JwtAuthenticationConverter

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e0d2f999-0801-41d0-97af-7a59094422ae.png)

* 인가 서버가 Scope 속성 대신 자체 커스텀 속성을 사용하거나 리소스 서버에서 속성을 내부 권한에 맞게 조정해야 할 경우 사용한다.
* `JwtAuthenticationConverter`는 JWT 객체를 `Authentication`으로 변환하는 클래스이며 권한을 변경하는 `JwtGrantedAuthoritiesConverter`를 가지고 있다.

```java
@Component
@RequiredArgsConstructor
public class ExampleAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AccountMapper accountMapper;
    private final ObjectMapper objectMapper;

    private static final String ACCESS_TOKEN_TYPE = "access";

    private static final String TOKEN_USE_CLAIM = "token_use";
    private static final String USERNAME_CLAIM = "username";
    private static final String PERMISSION_CLAIM = "permission";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String tokenUse = jwt.getClaimAsString(TOKEN_USE_CLAIM);

        if (tokenUse.isEmpty()) {
            throw new OAuth2AuthenticationException("invalid token");
        }

        if (!tokenUse.equals(ACCESS_TOKEN_TYPE)) {
            throw new OAuth2AuthenticationException("invalid token");
        }
        
        String accountId = jwt.getClaimAsString(USERNAME_CLAIM);
        VaonAccount vaonAccount = accountMapper.findVaonAccount(accountId);
        List<GrantedAuthority> authorities = assignAuthorities(jwt);
        return new VaonAuthenticatedToken(vaonAccount, null, authorities);
    }

    private List<GrantedAuthority> assignAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        String permissionsFromClaim = jwt.getClaim(PERMISSION_CLAIM);

        if (permissionsFromClaim == null) {
            throw new VaonRuntimeException(INVALID_TOKEN);
        }
        String permissions = GzipUtils.decompressFromGzipBase64(permissionsFromClaim);

        try {
            String[] permissionArray = objectMapper.readValue(permissions, String[].class);
            for (String permission : permissionArray) {
                authorities.add(new SimpleGrantedAuthority(permission.trim()));
            }
        } catch (JsonProcessingException e) {
            throw new OAuth2AuthenticationException("Failed to parse permissions");
        }
        return Collections.unmodifiableList(authorities);
    }
}
```
