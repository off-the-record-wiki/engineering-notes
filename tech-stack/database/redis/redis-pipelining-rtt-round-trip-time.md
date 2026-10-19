# Redis - Redis Pipelining & RTT(Round Trip Time)

#### Pipelining - 일반 실행과 파이프라이닝

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d6b9737b-fc57-4aec-b553-1d2c3a25fb27.png)

**1. 일반 실행(Request-Response)**

* 동작 : 명령어 하나 보내고 서버가 처리해서 응답을 줄 때까지 기다렸다가 응답을 받으면 다음 명령어를 보낸다.
* 병목의 원인(RTT) : 서버 처리속도가 아니라 네트워크 왕복 시간(RTT, Round Trip Time)이 핵심이다. 명령어가 100개라면 네트워크를 타고 왔다 갔다 하는 시간만 100배가 소요된다.
* Redis 내부 처리는 마이크로초 단위인데 RTT는 보통 밀리초 단위다. **즉 총 소요 시간의 대부분은 Redis가 아니라 네트워크가 잡아먹는다.**

**2. 파이프라이닝(Pipelining)**

* 동작 : 클라이언트가 응답을 기다리지 않고 여러 개의 명령어를 한꺼번에 묶어서 전송한다.
* 네트워크 오버헤드 감소로 RTT를 단 1회로 줄여버린다. 그렇게 되면 명령어가 많을수록 일반 방식보다 수십배에서 수백배까지 빠른 성능을 낼 수 있다.
* **파이프라이닝은 트랜잭션이 아니다.** 명령을 묶어서 보낼 뿐이므로 **파이프라인 안의 명령 사이사이에 다른 클라이언트의 명령이 끼어들 수 있다.** 원자성이 필요하면 `MULTI`/`EXEC`나 Lua를 써야 한다.(가장 흔한 오해)
* 한 번에 너무 많이 묶으면 **서버와 클라이언트 양쪽의 응답 버퍼가 그만큼 커진다.** 수만 건을 한 파이프라인에 담지 말고 **수백~수천 건 단위로 끊어서** 보내는 것이 안전하다.
* 클러스터 모드에서는 키가 서로 다른 슬롯에 있으면 노드별로 파이프라인을 나눠 보내야 한다. 대부분의 클라이언트 라이브러리가 이를 자동 처리하지만, 그만큼 효율은 떨어진다.

#### Pipelining - 3단계 동작 흐름

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/453f4e54-b302-4054-99d0-eb267b6fe1a9.png)

**1. 전송 단계 : 일단 한 번에 다 보낸다**

* 동작 : 클라이언트가 명령어 1, 2, 3을 각각 따로 보내고 응답을 기다리는게 아니라, 하나의 패킷에 실어서 네트워크로 한꺼번에 쏜다.
* 해당 과정에서 RTT(왕복 시간)의 절반(Request Latency)이 딱 한 번만 발생한다.

**2. 서버 실행 단계 : 순차적으로, 하지만 쉼 없이**

* 동작 : Redis 서버는 도착한 명령 리스트를 풀어서 순서대로 실행한다.
* Redis는 싱글 쓰레드이기 때문에 한 번에 하나씩 처리한다. 하지만 명령어 사이사이에 네트워크 응답을 기다리는 대기 시간이 없기 때문에 CPU를 100% 효율적으로 사용하며 아주 빠르게 루프를 돈다.

**3. 응답 단계 : 결과도 묶어서 한 번에**

* 동작 : 모든 명령 실행이 끝나면 서버는 결괏값들을 하나로 묶어서 클라이언트에게 보낸다.
* 여기서도 RTT의 나머지 절반(Response Latency)이 한 번만 발생한다.
* 결과는 **보낸 명령 순서와 같은 순서의 배열**로 돌아온다. 중간에 실패한 명령이 있어도 그 자리에 에러가 담겨 올 뿐, 나머지는 정상 실행된다.

```java
// Spring : executePipelined 로 묶어서 전송
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (int i = 0; i < 1000; i++) {
        connection.stringCommands().set(
            ("key:" + i).getBytes(), ("value:" + i).getBytes());
    }
    return null;   // 반환값은 무시된다. 결과는 executePipelined가 리스트로 돌려준다
});
```

* 콜백 안에서 각 명령의 반환값을 읽으려 해도 `null`이다. **아직 실행 전이기 때문**이며, 결과는 전부 끝난 뒤 리스트로 받는다. 이 점은 `MULTI` 큐잉과 같다.

#### Lua Script - 파이프라이닝을 넘어서

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2475bec4-1645-4d39-bf55-ae37c9ad8283.png)

**1. 원자적 실행 : 트랜잭션 그 이상의 보장**

* 동작 : Redis 서버는 Lua 스크립트를 실행하는 동안 다른 어떤 클라이언트 명령도 처리하지 않는다.
* 일반적인 `MULTI-EXEC` 트랜잭션은 명령어들을 묶어서 던지는 것뿐이지만 Lua 스크립트는 스크립트 전체가 하나의 거대한 원자적 명령처럼 동작한다.

**2. 복잡한 로직 : 서버에서 계산까지 끝낸다**

* 동작 : 클라이언트가 데이터를 가져와서 자바 코드에서 if문을 돌리고 다시 저장하는 개념이 아니라 if문 로직 자체를 Redis 서버로 보낸다.
* 이것이 `MULTI`와의 결정적 차이다. **`MULTI`는 큐에 쌓기만 해서 이전 명령의 결과를 볼 수 없지만, Lua는 값을 읽고 그에 따라 분기할 수 있다.**

**3. 네트워크 절감 : RTT의 최소화**

* 파이프라이닝과의 공통점 : 여러 명령을 한 번의 네트워크 왕복(RTT)으로 처리한다.
* 차이점 : 파이프라이닝은 그냥 명령 리스트를 쏘는 거지만 Lua 스크립트는 로직을 포함한 자체를 쏜다.
* 애플리케이션과 Redis 사이를 왔다갔다 할 필요가 없으므로 네트워크 지연이 심한 환경에서 성능을 비약적으로 높인다.

#### Lua Script - EVAL 실행 구조

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a9cec8a7-9553-4faa-b4c3-1f77307f7b17.png)

**1. EVAL 명령과 스크립트 전송**

* 동작 : 클라이언트가 `EVAL` 명령과 함께 Lua 소스 코드를 서버에 보낸다.
* 전송 : 스크립트 본체와 그 안에서 사용할 Key 개수, 실제 인자들이 네트워크를 타고 Redis 서버에 도착한다.

```shell
EVAL "return redis.call('GET', KEYS[1])" 1 mykey
#     └─ 스크립트 본문 ─────────────────┘ │  └ KEYS[1]
#                                        └ 키 개수
```

* **키는 반드시 `KEYS`로 넘기고 그 외 값만 `ARGV`로 넘겨야 한다.** 스크립트 안에 키 이름을 하드코딩하면 클러스터가 어느 슬롯을 다루는지 알 수 없어 동작이 깨진다.

**2. Lua 인터프리터의 역할**

* Redis 서버 내부는 Lua 5.1 인터프리터가 내장되어 있다.
* 서버에 전송된 스크립트는 이 인터프리터에 로드되어 해석된다. 이 때, Redis는 스크립트를 컴파일하여 실행 준비 상태로 만든다.

**3. 핵심: 원자적 실행과 다른 명령 차단**

* 배타적 실행 : Redis는 싱글 쓰레드로 동작한다. Lua 스크립트가 실행되는 순간, Redis 서버는 해당 스크립트 처리에만 전념한다.
* 블로킹 처리 : 스크립트가 수행되는 동안 외부에서 들어오는 다른 모든 명령은 큐에서 대기해야 한다.
* 결과 : 조회하고 계산하고 저장하는 과정 사이클이 다른 클라이언트가 절대 비집고 들어올 수 없는 완벽한 격리성이 보장된다.
* **이 장점은 그대로 위험이기도 하다.** 스크립트가 오래 걸리면 그동안 서버 전체가 멈춘다.
  * `busy-reply-threshold`(구 `lua-time-limit`, 기본 5초)를 넘기면 Redis는 다른 클라이언트에 `BUSY` 에러를 반환하기 시작한다.
  * 이 때 `SCRIPT KILL`로 중단할 수 있지만, **이미 쓰기 명령을 실행한 스크립트는 중단할 수 없다.** 롤백이 없어 데이터가 반쯤 바뀐 상태가 되기 때문이며, 이 경우 `SHUTDOWN NOSAVE` 외에는 방법이 없다.
  * 따라서 **반복문이 큰 스크립트, 대량 키를 훑는 스크립트는 절대 금물**이다.

**4. 스크립트 내 Redis 명령 호출**

* 스크립트 내부에서 `redis.call()` 또는 `redis.pcall()` 함수를 통해 실제 Redis 명령을 호출한다.
* 해당 과정은 서버 내부 메모리 안에서 일어나므로 네트워크 지연(RTT)이 전혀 발생하지 않는다.
* 두 함수의 차이 : `redis.call()`은 에러 발생 시 **스크립트 전체를 즉시 중단**하고 클라이언트에 에러를 반환한다. `redis.pcall()`은 에러를 **값으로 돌려주어** 스크립트 안에서 직접 처리할 수 있게 한다.

#### Lua Script - SCRIPT LOAD와 EVALSHA

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b9162d0f-a2bd-4784-bb84-45f7bc5b970b.png)

**스크립트 등록(Registration)**

* SHA1 해시 : Redis는 입력받은 스크립트 코드 전체를 SHA1 알고리즘을 통해 고유한 40글자 해시값으로 변환한다.
* SCRIPT LOAD : 클라이언트가 SCRIPT LOAD 명령을 실행하면 Redis 서버는 코드를 실행하지 않고 내부 메모리 캐시에 저장한 뒤, 방금 만든 해시값을 반환한다.
* 서버에 캐싱 : 이제 Redis 서버는 해당 해시값은 해당 코드라는 매핑 정보를 기억하게 된다.

**해시로 실행(Execution via EVALSHA)**

* EVALSHA : 클라이언트는 이제 긴 스크립트 전체를 보낼 필요가 없고 서버로부터 받은 해시값만 `EVALSHA [해시값] 1 [키] [인자]` 형태로 보낸다.
* 해시로 실행 : 서버는 받은 해시값이 캐시에 있는지 확인하고 있다면 저장된 코드를 즉시 꺼내서 실행한다.
* 네트워크 절감 : 스크립트 코드가 10KB 정도로 길더라도, 네트워크로는 단 40글자의 해시만 전송하면 된다. 대역폭을 획기적으로 아낄 수 있다.

**NOSCRIPT — 반드시 대비해야 할 상황**

* 스크립트 캐시는 **메모리에만 존재한다.** 서버가 재시작되거나 `SCRIPT FLUSH`가 실행되면 전부 사라지고, `EVALSHA`는 `NOSCRIPT` 에러를 반환한다.
* 그래서 표준 패턴은 **"`EVALSHA` 시도 → `NOSCRIPT`면 `EVAL`로 전체 전송하며 재등록"** 이다. Spring Data Redis의 `DefaultRedisScript`나 Lettuce·Redisson 같은 라이브러리는 이 폴백을 내부적으로 처리해 준다.
* 클러스터에서는 **각 노드가 자기 캐시를 따로 갖는다.** 한 노드에 LOAD 했다고 다른 노드에서 EVALSHA가 되는 것이 아니므로, 라이브러리의 폴백에 의존하거나 전 노드에 배포해야 한다.

```java
// Spring : DefaultRedisScript 는 EVALSHA → NOSCRIPT 시 EVAL 폴백을 알아서 처리한다
DefaultRedisScript<Long> script = new DefaultRedisScript<>();
script.setScriptText(
    "local stock = tonumber(redis.call('GET', KEYS[1])) " +
    "if stock and stock > 0 then return redis.call('DECR', KEYS[1]) else return -1 end");
script.setResultType(Long.class);

Long result = redisTemplate.execute(script, List.of("stock:100"));
```

* 참고 : Redis 7.0부터는 스크립트를 라이브러리 단위로 서버에 등록해두고 이름으로 호출하는 **Redis Functions**(`FUNCTION LOAD` / `FCALL`)가 추가되었다. EVALSHA와 달리 영속화되어 재시작 후에도 남는다.

#### Redis 성능 최적화 원칙

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2f37cae9-18ee-4677-8e31-ee605205eb12.png)

**1. 명령 최소화(Minimize Commands)**

* Redis 호출 횟수 자체가 성능 결정타. `MSET`으로 묶거나 파이프라이닝을 활용한다.
* 우선순위는 이렇다 — 같은 종류의 다중 키는 `MGET`/`MSET`, 서로 다른 명령의 나열은 **파이프라이닝**, 읽고 판단해서 쓰는 로직은 **Lua**.

**2. 메모리 효율(Memory Efficiency)**

* Redis는 메모리 기반. 메모리는 비싼 자원임을 인지해야 한다.
* 데이터 구조를 짤 때 무조건 `String`보다 필드가 많다면 `Hash`를 쓰는 등 메모리 사용량을 최적화하는 설계도 필요하다.
* 키 이름을 너무 길게 짓지 않는 것도 작은 시작이 된다.

**3. 조기 반환(Early Return)**

* Redis는 싱글 쓰레드. CPU를 오래 붙잡고 있게 되면 서비스 전체가 느려진다.
* Lua 스크립트를 짤 때 중요하다. 처리할 조건이 안 맞으면 즉시 Return해서 Redis 서버가 불필요한 연산을 하지 않고 바로 다음 명령을 처리할 수 있게 비켜줘야 한다.

**4. 캐시 활용(Cache Utilization - EVALSHA)**

* 네트워크 대역폭도 자원이다.
* 무거운 스크립트 코드를 매번 전송하지 않고, 서버에 캐싱된 해시값만 보내서 네트워크 부하와 스크립트 해석 시간을 최소화하는 EVALSHA를 사용한다.

#### 세 가지 방식 비교 - 언제 무엇을 쓰는가

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b5a1305a-8b37-4964-8ce6-98403bce6fa7.png)

|구분|Pipelining|MULTI / EXEC|Lua Script|
|:---|:---|:---|:---|
|목적|**RTT 절감**|**끼어듦 없는 실행**|**RTT 절감 + 원자성 + 로직**|
|원자성|❌ 중간에 다른 명령이 끼어들 수 있다|✅ EXEC 구간 보장|✅ 스크립트 전체 보장|
|중간 결과 참조|❌|❌ (QUEUED만 반환)|✅ 읽고 분기·반복 가능|
|롤백|❌|❌|❌ (조건 검사로 사전 차단)|
|주의점|버퍼 크기·배치 단위|경합 시 WATCH 재시도|**긴 스크립트는 서버 전체 블로킹**|

* 정리하면 — **네트워크 왕복만 줄이면 되면 Pipelining, 끼어듦만 막으면 되면 MULTI/EXEC, 읽고 판단해서 써야 하면 Lua**다.
* 셋은 배타적이지 않다. 예를 들어 파이프라인 안에 `MULTI`/`EXEC`를 담아 보낼 수도 있고, 여러 `EVALSHA` 호출을 파이프라인으로 묶을 수도 있다.
