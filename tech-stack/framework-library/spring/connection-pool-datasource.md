# Spring - Connection Pool & DataSource

#### JDBC(Java Database Connectivity)

* 자바에서 데이터베이스에 접속할 수 있도록 하는 자바 API를 말한다.
* JDBC 기능
  * Connection : 연결
  * Statement : SQL 내용
  * ResultSet : SQL 요청에 대한 응답

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/15d92f53-d631-46b0-aaf3-62ea03cc3c34.png)

* 각 DB마다 다르기에 DB 엔진을 바꿀 때마다 코드를 바꿔야한다는 문제가 발생한다.

#### 커넥션 풀 - 커넥션 풀을 사용하지 않은 경우

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c5c127c3-617f-4d1b-9837-2ffd7f9a5805.png)

* 애플리케이션 로직은 DB 드라이버를 통해 커넥션을 조회한다.
* DB 드라이버는 DB와 TCP/IP 커넥션을 연결한다.
* DB 드라이버는 연결이 완료되면 ID와 PW, 기타 정보를 DB에 전달한다.
* DB는 ID, PW를 통해 인증을 완료하고 내부에 DB 세션을 생성한다.
* DB는 커넥션 생성이 완료되었다는 응답을 보낸다.
* DB 드라이버는 커넥션 객체를 생성해서 클라이언트에 반환한다.

#### 커넥션 풀 - 커넥션 풀을 사용하는 경우

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4ab8c49e-a8a3-47e7-baa3-92f8458eb7cc.png)

* 애플리케이션을 시작하는 시점에 커넥션 풀은 필요한 만큼 미리 커넥션을 확보해서 커넥션 풀에 보관한다.
* 커넥션 풀에 보관된 커넥션은 이미 DB와 TCP/IP로 커넥션이 연결된 상태이기 때문에 언제든지 즉시 SQL을 DB에 전달할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c4f3a9df-38ec-428c-a909-483a41ff9652.png)

<br>

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/71063bfa-c72b-46f1-92c5-c3d61f0636e9.png)

* 애플리케이션 로직에서는 DB 드라이버를 통한 커넥션 확보를 하지 않아도 커넥션 풀에 이미 생성된 커넥션을 그대로 가져다 쓰기만 하면 된다.
* 커넥션을 사용하고 나면 종료하는 것이 아니라 다음에 다시 사용할 수 있도록 해당 커넥션을 그대로 커넥션 풀에 반환한다. 이 때, 커넥션을 종료하는 것이 아니라 커넥션이 살아있는 상태로 커넥션 풀에 반환해야 한다는 것이다.

> [📚HikariCP](https://github.com/brettwooldridge/HikariCP)
>
> **1. 핵심 개념**
> * HikariCP는 Zero Overhead를 목표로 설계된 매우 빠르고 가벼운 JDBC 커넥션 풀이다.
> * 커넥션 풀이란 미리 데이터베이스 연결을 맺어놓고 필요할 때마다 빌려주고 반납받는 관리자를 말한다.
> * 바이트 코드 수준의 최적화, 마이크로 벤치마킹을 통한 오버헤드 제거, 효율적인 자료구조 사용 덕분에 빠르다.
>
> **2. 동작 원리 : 커넥션 획득 과정**
> * Thread 요청 : 애플리케이션 쓰레드가 `dataSource.getConnection()`을 호출한다.
> * Hand-off 큐 확인 : HikariCP는 이전에 사용했던 커넥션이 있는지 확인하고 없으면 공용 풀에서 유효한 커넥션을 찾는다.
> * Wait & Timeout : 만약 풀에 남은 커넥션이 없다면 쓰레드는 `connection-timeout` 설정 시간동안 대기한다. 만약 시간이 초과되면 `SQLException`을 던진다.
>
> **3. HikariCP 핵심 설정 파라미터**
> * `maximumPoolSize` : 기본값은 10, 풀에 유지할 수 있는 최대 커넥션 개수이다. 이 값은 무조건 크게 설정한다고 좋은 것이 아니다. 커넥션이 많아지면 DB 서버 메모리 사용량이 늘어나고 컨텍스트 스위칭 오버헤드로 인해 오히려 성능이 저하될 수 있다.
> * `minimumIdle` : 기본값은 maximumPoolSize와 동일, 풀에서 유지할 수 있는 최소 유휴 커넥션 수이다. 트래픽이 몰리는 경우 커넥션을 새로 생성하는 비용(TCP Handshake)을 없애기 위함이다. 고정된 크기의 풀을 유지하는 것이 응답 속도의 편차를 줄이는 방법이다.
> * `connectionTimeout` : 기본값은 30s, 쓰레드가 풀에서 커넥션을 받기 위해 대기하는 시간이다. 일반적인 웹 서비스에서 사용자가 30초동안 기다리게 하는 것보다 빨리 에러를 내고 재시도하는 것이 더 나을 수 있다. 그래서 서비스 특성에 따라 타이트하게 가져가는 경우가 많다.
> * `maxLifetime` : 기본값은 30m, 커넥션이 풀에서 머무를 수 있는 최대 수명이다. DB가 먼저 유휴 커넥션을 끊어버리면 애플리케이션은 끊긴 줄 모르고 커넥션을 가져오다가 에러가 발생하게 된다. HikariCP가 먼저 커넥션을 끊어 좀비 커넥션 문제를 방지한다. 그렇기 때문에 DB에서 관리하는 값보다도 작은 값을 주어야만 한다.
> * `idleTimeout` : 기본값은 10m, 커넥션이 유휴 상태일 때 풀에서 제거되기까지의 시간이다. `minimumIdle`이 `maximumPoolSize`보다 작게 설정된 경우에만 의미가 있다.

#### DataSource

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6f78ea26-1c6a-4530-ac5f-d304d548a046.png)

* DataSource는 커넥션을 획득하는 방법을 추상화한 인터페이스이다.
* 인터페이스 핵심 기능은 커넥션 조회이다.
* 대부분의 커넥션 풀은 DataSource 인터페이스를 이미 구현해두었으므로 DataSource에만 의존하도록 애플리케이션 로직을 구현하면 된다.

#### DriverManager로 커넥션 얻는 방법

```java
@Slf4j
public class ConnectionTest {

    @Test
    @DisplayName("DriverManager")
    void driverManager() throws SQLException {
        // 커넥션 획득 시마다 URL, ID, PW 파라미터를 매번 넘겨야 한다.
        // 설정과 사용이 섞여 있는 구조
        Connection con1 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        Connection con2 = DriverManager.getConnection(URL, USERNAME, PASSWORD);

        log.info("connection={}, class={}", con1, con1.getClass());
        log.info("connection={}, class={}", con2, con2.getClass());
    }

    @Test
    @DisplayName("DriverManagerDataSource")
    void dataSourceManagerDataSource() throws SQLException {
        // 1. 초기 세팅 시에만 설정값을 넘긴다. (설정 단계)
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);

        // 2. 실제 사용 시에는 설정값 없이 getConnection()만 호출한다. (사용 단계)
        useDataSource(dataSource);
    }

    private void useDataSource(DataSource dataSource) throws SQLException {
        // 외부에서 주입된 dataSource를 통해 커넥션을 획득한다.
        // 구현체가 바뀌어도(DriverManager -> HikariCP) 이 코드는 수정되지 않는다.
        Connection con1 = dataSource.getConnection();
        Connection con2 = dataSource.getConnection();
        
        log.info("connection={}, class={}", con1, con1.getClass());
        log.info("connection={}, class={}", con2, con2.getClass());
    }
}
```

#### HikariDataSource로 커넥션 얻는 방법

```java
@Slf4j
public class ConnectionTest {

    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection con1 = dataSource.getConnection();
        Connection con2 = dataSource.getConnection();
        
        log.info("connection={}, class={}", con1, con1.getClass());
        log.info("connection={}, class={}", con2, con2.getClass());
    }

    @Test
    @DisplayName("HikariDataSource 커넥션 풀 테스트")
    void dataSourceConnectionPool() throws SQLException, InterruptedException {

        // 1. 커넥션 풀 생성 및 설정
        // 인터페이스(DataSource)를 넘어 세부 설정(MaximumPoolSize 등)을 위해 구현체로 선언
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(URL);
        hikariDataSource.setUsername(USERNAME);
        hikariDataSource.setPassword(PASSWORD);
        hikariDataSource.setMaximumPoolSize(10);
        hikariDataSource.setPoolName("MyPool");

        // 2. 사용 단계: 인터페이스를 통한 커넥션 획득
        useDataSource(hikariDataSource);

        // 3. 풀이 채워지는 로그를 확인하기 위한 대기
        // HikariCP는 별도의 스레드(AddConnectionExecutor)를 통해 풀을 채우기 때문에 잠시 대기해야 로그가 보입니다.
        Thread.sleep(1000);
    }
}
```

* SpringBoot 2.x부터 HikariCP가 기본 커넥션 풀로 채택되어 사용되고 있다.

실행 결과

```
19:50:58.837 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- MyPool - configuration:
19:50:58.841 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- allowPoolSuspension.............false
19:50:58.841 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- autoCommit......................true
19:50:58.841 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- catalog.........................none
19:50:58.841 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- connectionInitSql...............none
19:50:58.841 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- connectionTestQuery.............none
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- connectionTimeout...............30000
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- dataSource......................none
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- dataSourceClassName.............none
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- dataSourceJNDI..................none
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- dataSourceProperties............{password=<masked>}
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- driverClassName.................none
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- exceptionOverrideClassName......none
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- healthCheckProperties...........{}
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- healthCheckRegistry.............none
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- idleTimeout.....................600000
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- initializationFailTimeout.......1
19:50:58.842 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- isolateInternalQueries..........false
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- jdbcUrl.........................jdbc:h2:tcp://localhost/~/db1
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- keepaliveTime...................0
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- leakDetectionThreshold..........0
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- maxLifetime.....................1800000
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- maximumPoolSize.................10
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- metricRegistry..................none
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- metricsTrackerFactory...........none
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- minimumIdle.....................10
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- password........................<masked>
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- poolName........................"MyPool"
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- readOnly........................false
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- registerMbeans..................false
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- scheduledExecutor...............none
19:50:58.843 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- schema..........................none
19:50:58.844 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- threadFactory...................internal
19:50:58.844 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- transactionIsolation............default
19:50:58.844 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- username........................"sa"
19:50:58.844 [Test worker] DEBUG com.zaxxer.hikari.HikariConfig -- validationTimeout...............5000
19:50:58.844 [Test worker] INFO  com.zaxxer.hikari.HikariDataSource -- MyPool - Starting...
19:50:58.846 [Test worker] DEBUG c.z.hikari.util.DriverDataSource -- Loaded driver with class name org.h2.Driver for jdbcUrl=jdbc:h2:tcp://localhost/~/db1
19:50:58.884 [Test worker] INFO  com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn0: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:58.886 [Test worker] INFO  com.zaxxer.hikari.HikariDataSource -- MyPool - Start completed.
19:50:58.890 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn1: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:58.890 [Test worker] INFO  c.j.d.jdbc.connection.ConnectionTest -- connection=HikariProxyConnection@2061440682 wrapping conn0: url=jdbc:h2:tcp://localhost/~/db1 user=SA, class=class com.zaxxer.hikari.pool.HikariProxyConnection
19:50:58.891 [Test worker] INFO  c.j.d.jdbc.connection.ConnectionTest -- connection=HikariProxyConnection@1482748887 wrapping conn1: url=jdbc:h2:tcp://localhost/~/db1 user=SA, class=class com.zaxxer.hikari.pool.HikariProxyConnection
19:50:58.925 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Connection not added, stats (total=2, active=2, idle=0, waiting=0)
19:50:58.991 [MyPool housekeeper] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Pool stats (total=2, active=2, idle=0, waiting=0)
19:50:58.995 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn2: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:59.027 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - After adding stats (total=3, active=2, idle=1, waiting=0)
19:50:59.030 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn3: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:59.064 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - After adding stats (total=4, active=2, idle=2, waiting=0)
19:50:59.067 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn4: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:59.101 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - After adding stats (total=5, active=2, idle=3, waiting=0)
19:50:59.104 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn5: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:59.140 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - After adding stats (total=6, active=2, idle=4, waiting=0)
19:50:59.143 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn6: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:59.177 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - After adding stats (total=7, active=2, idle=5, waiting=0)
19:50:59.180 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn7: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:59.215 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - After adding stats (total=8, active=2, idle=6, waiting=0)
19:50:59.220 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn8: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:59.251 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - After adding stats (total=9, active=2, idle=7, waiting=0)
19:50:59.256 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Added connection conn9: url=jdbc:h2:tcp://localhost/~/db1 user=SA
19:50:59.291 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - After adding stats (total=10, active=2, idle=8, waiting=0)
19:50:59.291 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Connection not added, stats (total=10, active=2, idle=8, waiting=0)
19:50:59.291 [MyPool connection adder] DEBUG com.zaxxer.hikari.pool.HikariPool -- MyPool - Connection not added, stats (total=10, active=2, idle=8, waiting=0)
```
