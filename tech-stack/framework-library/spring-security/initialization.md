# Spring Security - Initialization

#### 자동 설정에 의한 기본 보안 작동

* 서버가 기동되면 스프링 시큐리티 초기화 작업 및 보안 설정이 이루어진다.
* 별도의 설정이 코드를 작성하지 않아도 기본적인 웹 보안 기능이 현재 시스템에 연동되어 작동한다.

1) 기본적으로 모든 요청에 대해 인증 여부를 검증하고 인증이 승인되어야 자원에 접근이 가능하다.
2) 인증 방식은 폼 로그인 방식과 `httpBasic` 로그인 방식을 지원한다.
3) 인증을 시도할 수 있는 로그인 페이지가 자동적으로 생성되어 렌더링이 된다.
4) 인증 승인이 이루어질 수 있도록 한 개의 계정이 기본적으로 제공된다.

>❗허나, 기본적으로 작동하는 웹 보안으로는 실제 서비스에서 제공되기엔 한계가 있다.

#### SecurityBuilder / SecurityConfigurer

* `SecurityBuilder` 클래스는 빌더 클래스로서 웹 보안을 구성하는 빈 객체와 설정 클래스들을 생성하는 역할을 하며 대표적으로 `WebSecurity`, `HttpSecurity`가 있다.
* `SecurityConfigurer` 클래스는 Http 요청과 관련된 보안 처리를 담당하는 필터들을 생성하고 여러 초기화 설정에 관여한다.
* `SecurityBuilder` 클래스는 `SecurityConfigurer`를 참조하고 있으며, 인증 및 인가 초기화 작업은 `SecurityConfigurer`에 의해서 진행된다.

#### WebSecurity / HttpSecurity

* `HttpSecurityConfiguration`에서 `HttpSecurity`를 생성하고 초기화를 진행한다.
* `HttpSecurity`는 보안에 필요한 각 설정 클래스와 필터들을 생성하고 최종적으로 `SecurityFilterChain` 스프링 빈을 생성한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7659c68a-c662-4263-8480-b2c16357706c.png)

* `WebSecurityConfiguration`에서 `WebSecurity`를 생성하고 초기화를 진행한다.
* `WebSecurity`는 `HttpSecurity`에서 생성된 `SecurityFilterChain` 스프링 빈을 `SecurityBuilder`에 저장한다.
* `WebSecurity`가 `build()`를 실행하면 `SecurityBuilder`에서 `SecurityFilterChain`을 꺼내어 `FilterChainProxy` 생성자에 전달한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/16065845-e9d5-47ee-81c7-54307d29de0c.png)

#### SecurityFilterChain

```java
public interface SecurityFilterChain {

    /**
     * 현재 요청이 이 필터 체인의 보안 규칙과 일치하는지 여부를 확인합니다.
     */
    boolean matches(HttpServletRequest request);

    /**
     * 이 체인에 포함된 서블릿 필터(Filter) 목록을 반환합니다.
     */
    List<Filter> getFilters();

}
```
```java
public final class DefaultSecurityFilterChain implements SecurityFilterChain, BeanNameAware, BeanFactoryAware {

    private static final Log logger = LogFactory.getLog(DefaultSecurityFilterChain.class);

    private final RequestMatcher requestMatcher;

    private final List<Filter> filters;

    private @Nullable String beanName;

    private @Nullable ConfigurableListableBeanFactory beanFactory;

    // 가변 인자(Varargs)를 사용하는 생성자
    public DefaultSecurityFilterChain(RequestMatcher requestMatcher, Filter... filters) {
        this(requestMatcher, Arrays.asList(filters));
    }

    // 리스트를 사용하는 생성자 (핵심 로직)
    public DefaultSecurityFilterChain(RequestMatcher requestMatcher, List<Filter> filters) {
        if (filters.isEmpty()) {
            logger.debug(LogMessage.format("Will not secure %s", requestMatcher));
        } else {
            List<String> filterNames = new ArrayList<>();
            for (Filter filter : filters) {
                filterNames.add(filter.getClass().getSimpleName());
            }
            String names = StringUtils.collectionToDelimitedString(filterNames, ", ");
            logger.debug(LogMessage.format("Will secure %s with filters: %s", requestMatcher, names));
        }
        this.requestMatcher = requestMatcher;
        this.filters = new ArrayList<>(filters);
    }

    public RequestMatcher getRequestMatcher() {
        return this.requestMatcher;
    }

    @Override
    public List<Filter> getFilters() {
        return this.filters;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return this.requestMatcher.matches(request);
    }

    @Override
    public String toString() {
        List<String> filterNames = new ArrayList<>();
        for (Filter filter : this.filters) {
            String name = filter.getClass().getSimpleName();
            // 'Filter' 접미사를 제거하여 가독성 있는 로그 출력
            if (name.endsWith("Filter")) {
                name = name.substring(0, name.length() - "Filter".length());
            }
            filterNames.add(name);
        }

        String declaration = this.getClass().getSimpleName();
        if (this.beanName != null) {
            declaration += " defined as '" + this.beanName + "'";
            if (this.beanFactory != null) {
                BeanDefinition bd = this.beanFactory.getBeanDefinition(this.beanName);
                String description = bd.getResourceDescription();
                if (description != null) {
                    declaration += " in [" + description + "]";
                }
            }
        }
        return declaration + " matching [" + this.requestMatcher + "] and having filters " + filterNames;
    }

    @Override
    public void setBeanName(@NonNull String name) {
        this.beanName = name;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ConfigurableListableBeanFactory listable) {
            this.beanFactory = listable;
        }
    }

}
```

* `boolean matches(HttpServletRequest request)`
  * 해당 메서드는 현재 요청이 처리될 수 있는지 여부를 결정한다. 
  * `true`인 경우 `FilterChain`에 의해 처리될 수 있음을 의미하며 `false`인 경우 현재 `FilterChain`으로 처리할 수 없음을 의미한다.
  * 특정 요청에 특화된 Filter에 의한 필터링 로직이 적용될 수 있도록 하는 `boolean` 메서드이다.
* `List<Filter> getFilters()`
  * 해당 메서드는 `SecurityFilterChain`을 구성하는 여러 필터 컬렉션을 반환한다.
  * 각 필터는 요청 처리 과정에서 필요한 특정 작업들을 수행한다.
  * 스프링 시큐리티는 여러 필터에 의해서 동작하는 메커니즘을 가지고 있다.

#### Filter

* 서블릿 필터는 웹 애플리케이션에서 클라이언트 요청과 서버 응답을 가공하거나 검사하는데 사용되는 구성 요소이다.
* 서블릿 필터는 클라이언트 요청이 서블릿에 도달하기 전이나 서블릿이 응답을 클라이언트에게 보내기 전에 특정 작업을 수행할 수 있다.
* 서블릿 필터는 Tomcat(WAS)에서 생성되고 실행되며 종료된다.

> [Spring - Filter와 Interceptor](../spring/filter-interceptor.md)

#### DelegatingFilterProxy

* `DelegatingFilterProxy`는 스프링에서 사용되는 특별한 서블릿 필터로, 서블릿 컨테이너와 스프링 애플리케이션 컨텍스트 간 연결고리 역할을 하는 필터이다.
* `DelegatingFilterProxy`는 서블릿 필터 기능을 수행하는 동시에 스프링 의존성 주입 및 빈 관리 기능과 연동되도록 설계된 필터이다.
* `DelegatingFilterProxy`는 "springSecurityFilterChain" 이름으로 생성된 빈을 ApplicationContext에서 찾아 요청을 위임한다.
* 실제 보안 처리를 수행하지 않는다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7795ff28-35e6-4284-aae0-c9314708fb97.png)

#### FilterChainProxy

* 앞서 `DelegatingFilterProxy`가 "springSecurityFilterChain"라는 이름을 가지는 스프링 빈을 ApplicationContext(=IOC Container)에서 찾아 요청을 위임한다.
* 이 요청을 위임받아 보안 처리 역할을 하는 것이 바로 `FilterChainProxy`이다.
* 내부적으로 하나 이상의 `SecurityFilterChain`을 가지고 있으며 요청 URL 정보를 기준으로 적절한 필터를 호출하게 된다.
* `HttpSecurity`를 통해 API 추가 시 관련 필터들이 추가된다.
* 사용자 요청들을 필터 순서대로 호출함으로 보안 기능을 동작시키고 필요 시 커스텀으로 필터를 개발 및 등록해 기존의 필터 전후로 순서까지 지정해 추가할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7552c2ff-f7d2-4fae-b6fd-9fbb22ef78c1.png)

```java
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
class ServletManagementChildContextConfiguration {

    @Bean
    ServletManagementWebServerFactoryCustomizer servletManagementWebServerFactoryCustomizer(
            ListableBeanFactory beanFactory) {
        return new ServletManagementWebServerFactoryCustomizer(beanFactory);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
    @ConditionalOnBean(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN, search = SearchStrategy.ANCESTORS)
    static class ServletManagementContextSecurityConfiguration {

        @Bean
        Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
            BeanFactory parent = beanFactory.getParentBeanFactory();
            Assert.state(parent != null, "'parent' must not be null");
            // 부모 컨텍스트(ANCESTORS)에서 기존 보안 필터 체인 빈을 찾아 가져옴
            return parent.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN, Filter.class);
        }

        @Bean
        @ConditionalOnBean(name = "securityFilterChainRegistration", search = SearchStrategy.ANCESTORS)
        DelegatingFilterProxyRegistrationBean securityFilterChainRegistration(HierarchicalBeanFactory beanFactory) {
            BeanFactory parent = beanFactory.getParentBeanFactory();
            Assert.state(parent != null, "'parent' must not be null");
            // 부모 컨텍스트에서 필터 등록 빈을 찾아 공유함
            return parent.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class);
        }

    }

}
```

#### 사용자 정의 보안 기능 구성

* `SecurityFilterChain` 클래스 빈을 등록한 후 인증 API 및 인가 API를 설정한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c6ac2f4c-b6e2-4e5d-a8fd-e0c8b459a1c3.png)

* `@EnableWebSecurity` 어노테이션을 필수로 작성해야 한다.
* 모든 설정 코드는 반드시 람다 형식으로 작성해야 한다.
