# Redis - Redis Cluster Example

#### Redis Cluster 구성

* [참고1 - 블로그](https://hwasurr.com/blog/redis/cluster)<br>
* [참고2 - 공식문서](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)<br>
* Redis Cluster를 구성하는 과정에서 어려움을 겪었다. 호스트 컵퓨터와 내부의 도커 컨테이너에 대한 포트 포워딩을 잘 맞춰주었고 아래와 같이 구성했다.

```yml
services:

  // ...

  redis-node-1:
    image: redis
    container_name: redis-test1
    volumes:
      - ./conf/redis.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf
    ports:
      - 6380:6380
      - 6381:6381
      - 6379:6379
      - 7380:7380
      - 7381:7381
      - 7379:7379

  redis-node-2:
    network_mode: "service:redis-node-1"
    image: redis
    container_name: redis-test2
    volumes:
      - ./conf/redis1.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf

  redis-node-3:
    network_mode: "service:redis-node-1"
    image: redis
    container_name: redis-test3
    volumes:
      - ./conf/redis2.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf

  slave-node-1:
    network_mode: "service:redis-node-1"
    image: redis
    container_name: redis-slave-test1
    volumes:
      - ./conf/slave.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf

  slave-node-2:
    network_mode: "service:redis-node-1"
    image: redis
    container_name: redis-slave-test2
    volumes:
      - ./conf/slave1.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf

  slave-node-3:
    network_mode: "service:redis-node-1"
    image: redis
    container_name: redis-slave-test3
    volumes:
      - ./conf/slave2.conf:/usr/local/etc/redis/redis.conf
    command: redis-server /usr/local/etc/redis/redis.conf

  redis-cluster-entry:
    network_mode: "service:redis-node-1"
    image: redis
    container_name: redis-cluster-entry
#    command: >
#      redis-cli --cluster create
#      redis-test1:6379 redis-test2:6380 redis-test3:6381
#      redis-slave-test1:7379 redis-slave-test2:7380 redis-slave-test3:7381
#      --cluster-replicas 1 --cluster-yes
    command: >
      redis-cli --cluster create
      127.0.0.1:6379 127.0.0.1:6380 127.0.0.1:6381
      127.0.0.1:7379 127.0.0.1:7380 127.0.0.1:7381
      --cluster-replicas 1 --cluster-yes
    depends_on:
      - redis-node-1
      - redis-node-2
      - redis-node-3
      - slave-node-1
      - slave-node-2
      - slave-node-3
```

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/22050ab3-46ab-4375-a681-db9b66b02a3a.png)
