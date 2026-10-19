# Spring - Filter & Interceptor

#### 필터 / 인터셉터 기준에서의 흐름 정리

```
HTTP 요청 → 내장 톰켓 서버(WAS) → 필터 → 디스패처 서블릿 → 인터셉터 → 컨트롤러
```

#### 서블릿 필터(Filter)

* 필터를 적용하면 필터가 호출된 다음 서블릿이 호출되기 때문에 서블릿에 도달하기 전에 부가적인 작업을 먼저 처리할 수 있다.
* 스프링 하위 프레임워크인 스프링 시큐리티 역시 필터 체인으로 구성이 되어 있다.

```java
public interface Filter {

    /**
     * Called by the web container to indicate to a filter that it is being placed into service. The servlet container
     * calls the init method exactly once after instantiating the filter. The init method must complete successfully
     * before the filter is asked to do any filtering work.
     * <p>
     * The web container cannot place the filter into service if the init method either:
     * <ul>
     * <li>Throws a ServletException</li>
     * <li>Does not return within a time period defined by the web container</li>
     * </ul>
     * The default implementation is a NO-OP.
     *
     * @param filterConfig The configuration information associated with the filter instance being initialised
     *
     * @throws ServletException if the initialisation fails
     */
    default void init(FilterConfig filterConfig) throws ServletException {
        // 필터 초기화 메서드, 서블릿 컨테이너가 생성될 때 호출
    }

    /**
     * The <code>doFilter</code> method of the Filter is called by the container each time a request/response pair is
     * passed through the chain due to a client request for a resource at the end of the chain. The FilterChain passed
     * in to this method allows the Filter to pass on the request and response to the next entity in the chain.
     * <p>
     * A typical implementation of this method would follow the following pattern:- <br>
     * 1. Examine the request<br>
     * 2. Optionally wrap the request object with a custom implementation to filter content or headers for input
     * filtering <br>
     * 3. Optionally wrap the response object with a custom implementation to filter content or headers for output
     * filtering <br>
     * 4. a) <strong>Either</strong> invoke the next entity in the chain using the FilterChain object
     * (<code>chain.doFilter()</code>), <br>
     * 4. b) <strong>or</strong> not pass on the request/response pair to the next entity in the filter chain to block
     * the request processing<br>
     * 5. Directly set headers on the response after invocation of the next entity in the filter chain.
     *
     * @param request  The request to process
     * @param response The response associated with the request
     * @param chain    Provides access to the next filter in the chain for this filter to pass the request and response
     *                     to for further processing
     *
     * @throws IOException      if an I/O error occurs during this filter's processing of the request
     * @throws ServletException if the processing fails for any other reason
     */
    // 요청이 들어올 때마다 호출, 필터 로직을 구현하는 부분
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException;

    /**
     * Called by the web container to indicate to a filter that it is being taken out of service. This method is only
     * called once all threads within the filter's doFilter method have exited or after a timeout period has passed.
     * After the web container calls this method, it will not call the doFilter method again on this instance of the
     * filter. <br>
     * <br>
     * This method gives the filter an opportunity to clean up any resources that are being held (for example, memory,
     * file handles, threads) and make sure that any persistent state is synchronized with the filter's current state in
     * memory. The default implementation is a NO-OP.
     */
    default void destroy() {
        // 필터 종료 메서드, 서블릿 컨테이너가 종료될 때 호출
    }
}
```

#### 필터 작성

```java
@Slf4j
public class LoginCheckFilter implements Filter {

    private static final String[] whitelist = {"/", "/members/add", "/login", "/logout", "/css/*"};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            log.info("인증 체크 필터 시작 {}", requestURI);

            if (isLoginCheckPath(requestURI)) {
                log.info("인증 체크 로직 실행 {}", requestURI);
                HttpSession session = httpRequest.getSession(false);

                if (session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null) {
                    log.info("미인증 사용자 요청 {}", requestURI);
                    
                    // 로그인으로 redirect, 현재 페이지를 쿼리 파라미터로 전달하여 로그인 후 복귀 가능하게 함
                    httpResponse.sendRedirect("/login?redirectURL=" + requestURI);
                    return; // 미인증 사용자는 다음 단계로 진행하지 않고 여기서 종료
                }
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            throw e; // 예외 로깅 지점은 필요 시 추가 가능, 여기서는 WAS까지 예외를 던짐
        } finally {
            log.info("인증 체크 필터 종료 {} ", requestURI);
        }
    }

    /**
     * 화이트 리스트의 경우 인증 체크X
     */
    private boolean isLoginCheckPath(String requestURI) {
        return !PatternMatchUtils.simpleMatch(whitelist, requestURI);
    }
}
```

* `chain.doFilter(request, response)`
  * 다음 필터가 있으면 다음 필터를 호출하고 다음 필터가 없다면 서블릿을 호출한다.
  * 해당 코드를 기준으로 앞쪽을 서블릿 호출 전, 뒤쪽을 서블릿 호출 후라고 볼 수 있다.
  * 해당 코드가 없으면 필터를 호출하지 않고, 서블릿도 호출하지 않게 된다. 

#### 필터 등록

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean logFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        
        // 필터 등록: 앞서 만든 LoginCheckFilter를 등록
        filterRegistrationBean.setFilter(new LoginCheckFilter());
        
        // 필터 순서: 숫자가 낮을수록 먼저 실행됨
        filterRegistrationBean.setOrder(1);
        
        // 적용 URL: 모든 요청(/*)에 대해 필터 적용
        filterRegistrationBean.addUrlPatterns("/*");
        
        return filterRegistrationBean;
    }
}
```

#### 스프링 인터셉터(Interceptor)

* Servlet이 자바 기반의 서버 사이드 컴포넌트라면 Interceptor는 스프링 프레임워크 기반의 기술이다.
* 필터와 마찬가지로 공통 관심 사항 및 부가 기능을 담당하지만 컨트롤러 앞에서 동작하고 필터보다 더 편리하고 정교하며 더 많은 기능을 제공한다.
* 필터보다는 인터셉터를 사용하는 것을 권장한다.
* 인터셉터 역시 필터와 마찬가지로 컨트롤러 호출 전에 동작해서 적절하지 않은 요청이라면 컨트롤러를 호출하지 않고 끝낼 수 있다.

```java
public interface HandlerInterceptor {

    /**
     * 컨트롤러(핸들러) 호출 전에 실행됩니다.
     * HandlerMapping이 적절한 핸들러 객체를 결정한 후, HandlerAdapter가 핸들러를 호출하기 직전에 호출됩니다.
     * * @return true이면 다음 인터셉터나 핸들러로 진행하고, false이면 진행을 중단합니다.
     */
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        // 컨트롤러 호출 전에 호출
        // 응답 값이 true이면 그대로 진행되고, false이면 더 이상 진행되지 않는다.
        return true;
    }

    /**
     * 컨트롤러(핸들러)가 정상적으로 실행된 후에 실행됩니다.
     * HandlerAdapter가 핸들러를 실제로 호출한 후, DispatcherServlet이 뷰를 렌더링하기 전에 호출됩니다.
     * 핸들러가 반환한 ModelAndView를 통해 추가적인 모델 작업을 할 수 있습니다.
     */
    default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                            @Nullable ModelAndView modelAndView) throws Exception {
        // 컨트롤러 호출 후에 호출
    }

    /**
     * 모든 요청 처리가 완료된 후(뷰 렌더링 이후)에 실행됩니다.
     * 핸들러 실행 결과와 상관없이 항상 호출되므로, 리소스 정리 등의 공통 처리에 적합합니다.
     * 단, preHandle이 true를 반환했을 때만 호출됩니다.
     */
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                 @Nullable Exception ex) throws Exception {
        // 뷰가 렌더링 된 이후에 호출
        // afterCompletion()은 예외가 발생하더라도 호출이 된다. 
        // 공통 처리를 하려면 이 메서드를 활용해야 한다.
    }

}
```

#### 인터셉터 작성

```java
@Slf4j
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {

        String requestURI = request.getRequestURI();

        log.info("인증 체크 인터셉터 실행 {}", requestURI);

        // 세션이 없으면 새로 생성하지 않도록 false가 아닌 기본 getSession()을 사용하거나 
        // 용도에 따라 request.getSession(false)를 검토할 수 있습니다.
        HttpSession session = request.getSession();

        if (session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null) {
            log.info("미인증 사용자 요청");
            
            // 로그인으로 redirect, 현재 페이지를 파라미터로 전달하여 로그인 후 복귀 가능하게 함
            response.sendRedirect("/login?redirectURL=" + requestURI);
            
            // false를 반환하면 다음 인터셉터나 컨트롤러가 호출되지 않고 여기서 끝남
            return false;
        }

        // true를 반환하면 다음 인터셉터나 컨트롤러 호출
        return true;
    }
}
```

#### 인터셉터 등록

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 인터셉터 등록 및 체인 설정
        registry.addInterceptor(new LoginCheckInterceptor())
                .order(1) // 인터셉터 실행 순서 (낮을수록 우선)
                .addPathPatterns("/**") // 모든 경로에 인터셉터 적용
                .excludePathPatterns(
                    "/", "/members/add", "/login", "/logout",
                    "/css/**", "/*.ico", "/error"
                ); // 위 경로는 인터셉터 호출을 제외 (화이트리스트)
    }
}
```

#### ArgumentResolver

* [적용 사례 - JWT 토큰 기반 인증 방식에서 현재 유저를 식별할 수 있도록 하는 어노테이션 개발 경험](https://github.com/Project-aniwhere/Aniwhere-BE/wiki/%EC%BB%A4%EC%8A%A4%ED%85%80-%EC%96%B4%EB%85%B8%ED%85%8C%EC%9D%B4%EC%85%98%EA%B3%BC-ArgumentResolver%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%9C-%EC%9D%B8%EC%A6%9D-%EC%B2%B4%ED%81%AC-%EA%B0%9C%EC%84%A0)
* 인증 / 인가를 필요로 하는 API를 요청할 경우 기존에는 서블릿에서 지원해주는 HttpSession을 찾아와 Member를 조회했으나 이 코드가 인증 / 인가를 필요로 하는 API를 요청하는 컨트롤러마다 추가해주어야 하기 때문에 코드의 중복이 발생한다. 

```java
@Target(ElementType.PARAMETER) // 파라미터에만 적용
@Retention(RetentionPolicy.RUNTIME) // 리플렉션 등을 활용할 수 있도록 런타임까지 정보가 남음
public @interface Login {
}
```

* 이전에도 공부를 했었지만 우리가 편리하게 사용하는 어노테이션인 `@RequestBody`, `@RequestMapping` 등등은 모두 ArgumentResolver에 의해 동작이 된다.
* 우리가 개발한 커스텀 어노테이션을 사용하려면 ArgumentResolver를 만들어서 추가해주어야 한다.

#### ArgumentResolver 작성

```java
@Slf4j
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        log.info("supportsParameter 실행");

        // @Login 애노테이션이 붙어 있는지 확인
        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);
        // 파라미터 타입이 Member 클래스이거나 그 자식 타입인지 확인
        boolean hasMemberType = Member.class.isAssignableFrom(parameter.getParameterType());

        return hasLoginAnnotation && hasMemberType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        log.info("resolveArgument 실행");

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);
        
        // 세션이 없으면 null 반환
        if (session == null) {
            return null;
        }

        // 세션에 보관된 회원 객체를 찾아서 반환
        return session.getAttribute(SessionConst.LOGIN_MEMBER);
    }
}
```

* `supportsParameter()`
  * LoginMemberArgumentResolver가 어떤 파라미터를 지원하는가?
  * `parameter.hasParameterAnnotation` : 파라미터에 해당 어노테이션이 붙어있는가?
  * `isAssignableFrom` : 파라미터 타입을 체크
* `resolveArgument()`
  * 구현하고자하는 실질적인 로직을 구현하는 곳으로 여기선 서버 측 세션 저장소에 있는 로그인 멤버를 가져와서 반환한다.
  * `supportsParameter`가 true가 나온 경우 resolveArgument가 동작하게 된다.

#### 커스텀 ArgumentResolver 등록

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // ... 기존 필터나 인터셉터 설정들

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 직접 구현한 LoginMemberArgumentResolver를 리스트에 추가
        // 이제 스프링 MVC는 컨트롤러 파라미터를 결정할 때 이 리졸버를 거치게 됩니다.
        resolvers.add(new LoginMemberArgumentResolver());
    }
}
```
