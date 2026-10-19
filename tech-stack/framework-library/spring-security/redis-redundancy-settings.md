# Spring Security - Redis Redundancy Settings

### Redis를 활용한 이중화 설정

* 이중화는 시스템 부하를 분산하고 단일 실패 지점없이 서비스를 지속적으로 제공하는 아키텍처를 구현하는 것을 목표로 하며 스프링 시큐리티에서는 이러한 이중화 환경에서 인증, 권한 부여, 세션 관리 등의 보안 기능을 제공한다.
* 스프링 시큐리티는 사용자 세션을 안전하게 관리하며 이중화된 환경에서 세션 정보를 공유할 수 있는 메커니즘을 제공하며 대표적으로 레디스와 같은 분산 캐시를 사용해 세션 정보를 여러 서버 간에 공유할 수 있다.
* `@EnableRedisHttpSession` 어노테이션을 달아두어야 한다.
>[참고](https://docs.spring.io/spring-session/reference/guides/boot-redis.html)
