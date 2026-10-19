# Spring Security - OAuth 2.0 Client Fundamentals

#### 클라이언트 권한 부여 요청 시작

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7eb29623-0143-4cc2-9b18-c06056d148e5.png)

```yml
# application.yml 코드 발췌(keycloak)
spring:
  security:
    oauth2:
      client:
        registration:                                                      # 클라이언트 설정
          keycloak:
            client-id: oauth2-client-app                                   # 서비스 공급자에 등록된 클라이언트 아이디
            client-secret: tynI8eYUw4H1fJYxwLQ36XhFC1Ge1w1x                # 서비스 공급자에 등록된 클라이언트 비밀번호
            client-name: oauth2-client-app                                 # 클라이언트 이름
            authorization-grant-type: authorization_code                   # OAuth 2.0 권한 부여 타입
            redirect-uri: http://localhost:8081/login/oauth2/code/keycloak # 인가 서버가 권한 코드 부여 후 리다이렉트하는 위치
            client-authentication-method: client_secret_post               # 클라이언트 자격증명 전송 방식
            scope: openid,email                                            # 리소스에 접근 제한 범위
        provider:                                                          # 공급자 설정
          keycloak:
            issuer-uri: http://localhost:8080/realms/oauth2                                     # 서비스 공급자 위치
            authorization-uri: http://localhost:8080/realms/oauth2/protocol/openid-connect/auth # OAuth 2.0 권한 코드 부여 엔드포인트
            token-uri: http://localhost:8080/realms/oauth2/protocol/openid-connect/token        # OAuth 2.0 토큰 엔드포인트
            jwk-set-uri: http://localhost:8080/realms/oauth2/protocol/openid-connect/certs      # OAuth 2.0 JwkSetUri 엔드포인트
            user-info-uri: http://localhost:8080/realms/oauth2/protocol/openid-connect/userinfo # OAuth 2.0 UserInfo 엔드포인트
            user-name-attribute: preferred_username                                             # 사용자명을 추출하는 클레임명
```

* 클라이언트가 인가 서버로 권한 부여 요청을 하거나 토큰 요청을 할 경우 클라이언트 정보 및 엔드포인트 정보를 참조해서 전달한다.
* `application.yml` 환경설정 파일에 클라이언트 설정과 인가서버 엔드포인트 설정을 한다.
* 초기화가 진행되면 `application.yml`에 있는 클라이언트 및 엔드포인트 정보가 `OAuth2ClientProperties`의 각 속성에 바인딩이 된다.
* `OAuth2ClientProperties`에 바인딩 되어 있는 속성의 값은 인가서버로 권한 부여 요청을 하기 위한 `ClientRegistration` 클래스의 필드에 저장된다.
* `OAuth2AuthorizationRequestRedirectFilter`가 `ClientRegistrationRepository`에서 `ClientRegistration`을 조회해 `OAuth2AuthorizationRequest`를 구성하고, 사용자 브라우저를 인가서버로 리다이렉트시킨다. 
* 이후 토큰 요청은 `RestClientAuthorizationCodeTokenResponseClient`가 인가서버로 직접 POST한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/35aa31f1-656d-47ce-bddb-3c79c28fa0e8.png)

* `Registration` : 인가 서버에 등록된 클라이언트 및 요청 파라미터 정보를 나타낸다.
* `Provider` : 공급자에서 제공하는 엔드포인트 등의 정보를 나타낸다.
* `OAuth2ClientProperties`에서 클라이언트 및 공급자 정보를 맵에 저장하고 인가서버와의 통신 시 각 항목을 참조하여 사용하는 구조가 된다.

#### ClientRegistration

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e622fc71-5683-4471-832c-be4ed1e81857.png)

* OAuth 2.0 또는 OpenID Connect 1.0 Provider에서 클라이언트 등록 정보를 나타낸다.
* `ClientRegistration`은 OpenID Connect Provider의 설정 엔드포인트나 인가 서버의 메타 데이터 엔드포인트를 찾아 초기화할 수 있다.
* `ClientRegistration`의 메서드를 사용하면 ClientRegistration을 쉽게 설정할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/cad66f1b-1bd3-4a33-9c35-d2be13acd871.png)

* OAuth 2.0 공급자를 제공하는 클래스로서 글로벌 서비스 제공자 일부는 기본으로 제공되어진다.
* `client_id`와 `client_secret`는 별도로 작성을 해야 한다. 국내 공급자 정보는 위의 모든 항목들을 수동으로 작성해야 한다.
* 클라이언트 기준인 `Registration` 항목과 서비스 제공자 기준인 `Provider` 항목으로 구분해 설정한다.
* `yml`이나 `properties` 항목이 아닌 Java Config 방식으로 `ClientRegistration`을 작성할 수도 있다.
* `ClientRegistration` 객체를 생성할 수 있는 빌더 클래스를 반환한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/89573cb7-114d-4ba6-b8be-220377cb9493.png)

* 내부 정적 클래스 중 하나인 `ClientRegistrationRepositoryConfiguration`에 선언된 어노테이션을 보게 되면 `@ConditionalOnMissingBean(ClientRegistrationRepository.class)`을 볼 수 있는데 이 어노테이션이 의미하는 바는 `ClientRegistrationRepository` 빈이 존재하지 않을 경우 실행된다는 것을 의미한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c2b518ee-506e-4b9d-8cab-f23a3a0534c9.png)

* `getBuilderFromIssuerIfPossible` 메서드에서는 provider 설정에 issuer-uri가 있는지만 확인하고, 있으면 `ClientRegistrations.fromIssuerLocation`으로 디스커버리를 시도하며 없으면 null을 반환한다.
* `fromIssuerLocation` 이 단계에서 OIDC 문서를 읽을지 아니면 RFC 8414 문서를 읽을지 결정한다.

#### ClientRegistrationRepository

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/19766eb9-df03-4528-916b-36a8a1a0cc76.png)

* `ClientRegistrationRepository`는 OAuth 2.0과 OpenID Connect 1.0의 `ClientRegistration` 저장소 역할을 한다.
* 클라이언트 등록 정보는 궁극적으로 인가 서버가 저장하고 관리하는데 이 Repository는 인가 서버에 일차적으로 저장된 클라이언트 등록 정보의 일부를 검색하는 기능을 제공한다.
* `ClientRegistrationRepository`의 구현체는 위와 같이 `InMemoryClientRegistrationRepository`이다.
* 자동 설정을 사용하면 `ClientRegistrationRepository`도 `ApplicationContext` 내의 `@Bean`으로 등록하므로 필요하다면 원하는 곳에 디펜던시를 주입할 수 있다.

#### 자동설정에 의한 초기화 플로우

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/320df1c3-bbc1-4ec2-85d8-212902683296.png)

* `OAuth2ClientWebSecurityAutoConfiguration`은 두 가지를 등록한다. `oauth2SecurityFilterChain(HttpSecurity)`은 `@ConditionalOnDefaultWebSecurity`가 붙어 있어 사용자가 `SecurityFilterChain` 빈을 직접 정의하지 않았을 때만 들어오는 기본 필터체인이고, `authorizedClientRepository(OAuth2AuthorizedClientService)`는 발급받은 토큰을 담아둘 저장소다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f19aba3f-1a24-487a-a1cd-a105b18c6132.png)

* `authorizedClientRepository()`가 실제로 반환하는 구현체다. 이름이 동작을 그대로 말해준다. 인증된 principal이냐를 기준으로 갈린다.
* `saveAuthorizedClient`가 호출되면 `isPrincipalAuthenticated(Authentication)`로 판정해서, 인증된 사용자면 `authorizedClientService`로 넘기고 익명 사용자면 `anonymousAuthorizedClientRepository`(기본값 HttpSessionOAuth2AuthorizedClientRepository)로 보내 세션에 담는다.
