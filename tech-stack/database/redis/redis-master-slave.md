# Redis ‐ Redis Master-Slave

#### Master - Slave

* 이번 프로젝트에서 AWS Multipart Upload를 구현하는 과정에서 S3 - DB 간 데이터 정합성을 위해 2개의 버킷과 함께 Redis를 사용하게 되었다.
* 이전 프로젝트에서도 JWT의 리프레시 토큰을 보관하기 위한 용도로 사용을 했었는데 그 때는 Redis를 잘 알고 사용하지 못한 느낌이 들어 이번에 확실히 잡아보려고 한다.
* 이전 프로젝트의 경우 Redis를 단일 노드로 구성했었다. 이렇게 되면 Redis에 장애가 발생할 경우 Redis의 특성상(인메모리 DB) 데이터가 소실될 수 있다.
* 그래서 이런 문제를 해결하고자 Master - Slave 구조로 Redis를 운영하고자 한다. Master 노드에 장애가 발생할 경우 복제 노드인 Slave 노드를 통해 데이터를 읽을 수 있게 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/63aa540d-f0f4-40f9-9023-51c775b2a720.png)

* 위와 같이 구성을 하게 되면 Master Node는 쓰기 모드만 가능하고 Master Node의 값을 Slave Node 측에도 저장한다. 반면 Slave Node는 읽기 모드만 가능해지면서 서버 측으로 요청이 들어오게 되면 Master Node를 대신해서 읽기 작업을 수행하게 된다.
* Redis Master - Slave 구성을 위해 루트 프로젝트에 redis.conf를 작성한다.

```conf
################################# REPLICATION #################################

# Master-Replica replication. Use replicaof to make a Redis instance a copy of
# another Redis server. A few things to understand ASAP about Redis replication.
#
#   +------------------+      +---------------+
#   |      Master      | ---> |    Replica    |
#   | (receive writes) |      |  (exact copy) |
#   +------------------+      +---------------+
#
# 1) Redis replication is asynchronous, but you can configure a master to
#    stop accepting writes if it appears to be not connected with at least
#    a given number of replicas.
# 2) Redis replicas are able to perform a partial resynchronization with the
#    master if the replication link is lost for a relatively small amount of
#    time. You may want to configure the replication backlog size (see the next
#    sections of this file) with a sensible value depending on your needs.
# 3) Replication is automatic and does not need user intervention. After a
#    network partition replicas automatically try to reconnect to masters
#    and resynchronize with them.
#
replicaof master 6379
```

* 그 다음으로 Docker Compose 파일을 작성한다.

```yml
services:
  mysql:
    container_name: mysql
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: spoteditor
      MYSQL_DATABASE: spoteditor
    ports:
      - "3306:3306"
    volumes:
      - ./mysql_data:/var/lib/mysql

  master-redis:
    container_name: master-redis
    image: redis
    ports:
      - "6379:6379"
    healthcheck:
      test: [ "CMD", "redis-cli", "ping" ]
      interval: 5s
      retries: 10
    restart: unless-stopped

  slave-redis-a:
    container_name: slave-redis-a
    image: redis
    ports:
      - "7001:6379"
    command: redis-server --slaveof master-redis 6379
    depends_on:
      - master-redis

  slave-redis-b:
    container_name: slave-redis-b
    image: redis
    ports:
      - "7002:6379"
    command: redis-server --slaveof master-redis 6379
    depends_on:
      - master-redis

  redis-commander:
    platform: linux/amd64
    container_name: redis-commander
    hostname: redis-commander
    image: rediscommander/redis-commander:latest
    restart: always
    environment:
      - REDIS_HOSTS=master:master-redis:6379,slave-a:slave-redis-a:6379,slave-b:slave-redis-b:6379
    ports:
      - "8081:8081"
    depends_on:
      - master-redis
      - slave-redis-a
      - slave-redis-b
```

```shell
$ docker compose up -d --build
```

* 백그라운드에서 실행하도록 위와 같은 명령어를 입력해주고 Docker Desktop을 통해 결과를 조회하면 아래와 같이 컨테이너들이 잘 뜨게 되는 것을 볼 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3b681779-7222-4926-96c7-5d8746ba7336.png)

* Redis Commander를 들어가보면 다음과 같이 Redis 구성 현황을 알 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0c7d7e2b-c649-4965-83bf-6ec274f66e63.png)

* 시스템적인 설계는 위와 같고 이제 자바 코드로 Redis 관련 설정을 작성해주면 된다.

```yml
# application.yml
data:
  redis:
    master:
      host: localhost
      port: 6379
    slaves:
      - host: localhost
        port: 7001
      - host: localhost
        port: 7002
```
```java
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisProperties {

	private String host;
	private int port;
	private RedisProperties master;
	private List<RedisProperties> slaves;
}
```
```java
@Configuration
@RequiredArgsConstructor
public class RedissonConfiguration {

	private final RedisProperties redisProperties;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		config.useMasterSlaveServers()
				.setMasterAddress("redis://" + redisProperties.getMaster().getHost() + ":" + redisProperties.getMaster().getPort())
				.setReadMode(ReadMode.SLAVE)
				.setSubscriptionMode(SubscriptionMode.MASTER)
				.setMasterConnectionMinimumIdleSize(5)
				.setMasterConnectionPoolSize(250)
				.setSlaveConnectionMinimumIdleSize(5)
				.setSlaveConnectionPoolSize(250)
				.setTimeout(3000);

		redisProperties.getSlaves().forEach(slave ->
				config.useMasterSlaveServers()
						.addSlaveAddress("redis://" + slave.getHost() + ":" + slave.getPort())
		);

		return Redisson.create(config);
	}
}
```
