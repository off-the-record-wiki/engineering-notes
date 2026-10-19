# Spring - Bean LifeCycle Callback

#### 빈 생명주기 콜백

* 생성자 : 매개변수를 받아 필드를 채우고 객체를 메모리에 올리는 구조적 역할에 집중해야 한다.
* 초기화 메서드 : 생성된 값을 바탕으로 외부 리소스(DB, Network, File)와 연결하는 등 비즈니스 로직의 준비 작업을 수행한다.

#### 스프링 빈 생명주기 상세 흐름

1. 스프링 컨테이너 생성
2. 스프링 빈 생성(생성자 주입은 이 단계에서 의존관계 주입도 함께 일어남)
3. 의존관계 주입(수정자/필드 주입)
4. 초기화 콜백(Post-Construct / InitializingBean / initMethod)
5. 사용 (애플리케이션에서 빈 사용)
6. 소멸전 콜백(Pre-Destroy / DisposableBean / destroyMethod)
7. 스프링 종료

#### 빈 생명주기 콜백의 방법 - 인터페이스 InitializingBean, DisposableBean

```java
public class NetworkClient implements InitializingBean, DisposableBean {

    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 서비스 시작시 호출
    public void connect() {
        System.out.println("connect: " + url);
    }

    public void call(String message) {
        System.out.println("call: " + url + " message = " + message);
    }

    // 서비스 종료시 호출
    public void disconnect() {
        System.out.println("close: " + url);
    }

    /**
     * InitializingBean 인터페이스의 메서드
     * 의존관계 주입이 끝난 후 호출됩니다.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("NetworkClient.afterPropertiesSet");
        connect();
        call("초기화 연결 메시지");
    }

    /**
     * DisposableBean 인터페이스의 메서드
     * 빈 소멸 직전(컨테이너 종료 전)에 호출됩니다.
     */
    @Override
    public void destroy() throws Exception {
        System.out.println("NetworkClient.destroy");
        disconnect();
    }
}
```
```java
class NetworkClientMain {

    @Test
    @DisplayName("빈 생명주기 콜백 by InitializingBean, DisposableBean")
    void lifecycleTest() {
        // 1. 컨테이너 생성 및 빈 등록/의존관계 주입
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(NetworkClientConfig.class);
        
        // 2. 빈 조회 (이미 초기화 콜백이 완료된 상태)
        NetworkClient networkClient = ac.getBean("networkClient", NetworkClient.class);
        
        // 3. 컨테이너 종료 (소멸전 콜백 호출)
        ac.close(); 
    }

    @Configuration
    static class NetworkClientConfig {
        @Bean
        public NetworkClient networkClient() {
            NetworkClient networkClient = new NetworkClient();
            networkClient.setUrl("http://localhost:8080"); // 수정자 주입
            return networkClient;
        }
    }
}
```

> 테스트 코드 실행 결과

```
생성자 호출 url = null
Connecting.. http://localhost:8080
call = http://localhost:8080, message = 초기화 연결 메시지
close = http://localhost:8080
```

#### 빈 생명주기 콜백의 방법 2 - 빈 등록 초기화, 소멸 메서드

* 설정 정보에 `@Bean(initMethod = "init", destroyMethod = "close")`와 같이 초기화, 소멸 메서드를 지정할 수 있다.

```java
public class NetworkClient {

    private String url;

    public NetworkClient() {
        System.out.println("생성자 호출, url = " + url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // 서비스 시작 시 호출
    @PostConstruct
    public void init() {
        System.out.println("NetworkClient.init");
        connect();
        call("초기화 연결 메시지");
    }

    // 서비스 종료 시 호출
    @PreDestroy
    public void close() {
        System.out.println("NetworkClient.close");
        disconnect();
    }

    private void connect() {
        System.out.println("Connecting.. " + url);
    }

    private void call(String message) {
        System.out.println("call = " + url + ", message = " + message);
    }

    private void disconnect() {
        System.out.println("close = " + url);
    }
}
```
```java
class NetworkClientMain {

    @Test
    @DisplayName("애노테이션 @PostConstruct, @PreDestroy 사용")
    void lifecycleTest() {
        // 1. 컨테이너 생성 (이 과정에서 빈 생성 -> 의존관계 주입 -> @PostConstruct 초기화까지 완료)
        AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(NetworkClientConfig.class);
        
        // 2. 빈 조회 및 사용
        NetworkClient networkClient = ac.getBean("networkClient", NetworkClient.class);
        
        // 3. 컨테이너 종료 (이 시점에 @PreDestroy 소멸 콜백 호출)
        ac.close();
    }

    @Configuration
    static class NetworkClientConfig {

        @Bean
        public NetworkClient networkClient() {
            NetworkClient networkClient = new NetworkClient();
            networkClient.setUrl("http://localhost:8080");
            return networkClient;
        }
    }
}
```

* 최신 스프링에서 권장하는 방법이다.
* 외부 라이브러리에는 적용할 수 없다.

> 테스트 코드 실행 결과

```
생성자 호출 url = null
Connecting.. http://localhost:8080
call = http://localhost:8080, message = 초기화 연결 메시지
close = http://localhost:8080
```
