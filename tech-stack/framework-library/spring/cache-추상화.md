# Spring - Cache Annotation

#### 캐시의 사용

* 캐시는 서버 부담을 줄이고 성능을 높이기 위해 사용되는 기술이다.
* 어떤 요청을 처리하는데 계산이 복잡하거나 혹은 DB에서 조회되는데 오랜 시간이 걸리는 경우 결과를 저장해두고 가져옴으로써 빠르게 처리할 수 있다.
* 캐시는 값을 저장해두고 불러오기 때문에 데이터의 변동성이 적은 경우에 용이하다.
* 만약 매번 다른 결과를 돌려줘야 하는 상황에 캐시를 적용하게 되면 캐시 업데이트도 해줘야 하고 잘못하면 데이터의 일관성이 낮아지게 되는 문제가 발생할 수 있다.

#### Spring Cache 추상화

* 스프링은 AOP 방식으로 편리하게 메서드에 캐시 서비스를 적용할 수 있는 기능을 제공한다.
* 캐시 서비스는 트랜잭션과 마찬가지로 AOP를 이용해 메서드 실행 과정에 적용된다.
* 이를 통해 캐시 관련 로직을 핵심 비즈니스 로직과 분리할 수 있고 스프링 캐시 구현 기술에 종속되지 않도록 추상화된 서비스를 제공해 환경이 바뀌거나 적용할 캐시 기술을 변경하더라도 애플리케이션 코드에 영향을 주지 않는다.

#### Spring Cache 어노테이션 사용법(@Cacheable, @CachePut, @CacheEvict)

* Spring에서 `@Cacheable`과 같은 어노테이션을 사용하려면 별도의 선언이 필요하다.
* 이 때, `@EnableCaching` 어노테이션을 추가해야한다.

```java
@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 세부 구현 ....
    }
}
```

* [캐시를 저장/조회하기 위한 @Cacheable](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html#cache-annotations-cacheable)
* 클래스나 인터페이스에서 캐시를 지정할 순 있지만 상당히 드물고, 보통 메서드 단위로 적용한다.
* 캐시를 적용할 메서드에 `@Cacheable` 어노테이션을 붙여주면 캐시에 데이터가 없을 경우에는 기존 로직(DB 조회나 등등)을 실행한 후에 캐시에 데이터를 추가하고, 만약 캐시에 데이터가 있다면 캐시의 데이터를 반환하게 된다.

```java
@Cacheable(cacheManager = "cacheManager", cacheNames = MARKET_TOP_NET_TRADE_VALUE, keyGenerator = "marketCacheKeyGenerator")
public Aclass getAclass() {
    // 세부 구현
}
```

* Spring Framework의 @Cacheable 어노테이션은 **AOP(Aspect Oriented Programming, 관점 지향 프로그래밍)**를 기반으로 동작하며, 반복되는 무거운 연산이나 DB 조회를 최적화하는 데 사용된다.

**1. 기본 동작 흐름(The Logic)**

* 캐시 조회 : 메서드를 실행하기 전, 파라미터를 기반으로 생성된 Key를 가지고 지정된 **Cache(저장소)**를 뒤진다.
* **캐시 히트(Hit)** : 만약 캐시에 값이 있다면, 메서드를 아예 실행하지 않고 저장된 값을 즉시 반환한다.
* **캐시 미스(Miss)** : 캐시에 값이 없다면, 실제 메서드를 실행한다.
* 결과 저장 : 메서드가 반환한 값을 캐시에 저장하고, 호출자에게 결과를 전달한다.

**2. 내부 기술적 원리(AOP & Proxy)**

* Proxy 생성 : 스프링 빈으로 등록된 객체에 @Cacheable이 있으면, 스프링은 해당 빈을 감싸는 프록시 객체를 만든다.
* 인터셉터 : 사용자가 메서드를 호출하면 프록시가 이를 가로채고 CacheInterceptor에게 처리를 맡긴다.
* 결과 반환 : 인터셉터는 캐시 매니저를 통해 데이터를 확인한 뒤, 필요할 때만 실제 타겟 객체의 메서드를 호출한다.

**3. 주요 속성별 상세 동작**

* **Key 생성(key, keyGenerator)**
  * 기본적으로 모든 메서드 파라미터를 조합해 키를 만든다.
  * key 속성에 **SpEL(Spring Expression Language)**을 사용하여 특정 값만 키로 쓸 수 있다.
  * 복잡한 로직이 필요하면 keyGenerator를 커스텀하게 구현할 수 있다.
* **조건부 캐싱(condition, unless)**
  * condition : 메서드 실행 전에 검사합니다. 참일 때만 캐시를 확인/저장한다.
  * unless : 메서드 실행 후에 결과를 보고 결정한다.
* **동기화(sync)**
  * 기본값은 false
  * 만약 여러 스레드가 동시에 같은 키로 접근할 때, 실제 메서드가 딱 한 번만 호출되도록 보장하고 싶다면 sync = true로 설정한다.
* **Optional 처리**
  * 코드 주석에도 명시되어 있듯, Optional 반환 타입은 자동으로 언래핑(Unwrap)되어 처리됩니다. 값이 비어있으면 null로 저장된다.
* **주의해야하는점(Self-Invocation)**
  * `@Cacheable`은 프록시 기반이기 때문에, 같은 클래스 내부에서 메서드를 호출할 때는 동작하지 않는다.

#### 캐시 제거를 위한 @CacheEvict

* 캐시는 적절한 시점에 제거되어야 하는데, 만약 값이 달라진다면 캐시를 제거해야 할 것이다.
* 그렇지 않으면 잘못된 결과를 반환하는데 캐시를 제거하는 방법은 다음과 같이 2가지가 있다.
```
1. 일정한 주기로 캐시를 제거
2. 값이 변할 때 캐시를 제거
```
* 스프링은 캐시 제거에도 AOP 기반으로 메서드에 적용할 수 있는 `@CacheEvict` 어노테이션을 제공하고 있다.
* `@CacheEvict`에 캐시 이름을 넣어주면 메서드가 실행될 때 캐시 내용이 제거된다.

#### 캐시 저장만을 위한 @CachePut

* 캐시에 값을 저장하는 용도로만 사용되는 `@CachePut`도 존재한다.
* `@CachePut`은 `@Cacheable`과 유사하게 실행 결과를 캐시에 저장하지만 조회 시에 저장된 캐시 내용을 사용하지 않고 항상 메서드 로직을 실행한다는 점에서 다르다.
