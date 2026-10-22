# Redis - Redis Transaction ACID

#### Redis - Redis Transaction ACID

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/67f93e7b-e1ad-481e-a4e6-4bd89ae63217.png)

**1. 명령 일괄 실행 : MULTI와 EXEC**

* 동작 : `MULTI` 명령어를 날리면 트랜잭션이 시작된다. 이후 들어오는 명령어들은 즉시 실행되지 않고 큐에 차곡차곡 쌓인다. 마지막에 `EXEC`를 호출하는 순간, 쌓여있던 모든 명령어가 순차적으로 한꺼번에 실행된다.
* 격리성 : Redis는 기본적으로 싱글 쓰레드 기반으로 동작하기 때문에 `EXEC`가 실행되어 큐의 명령들이 처리되는 동안 다른 클라이언트의 명령은 중간에 끼어들 수 없다. 덕분에 작업의 독립성이 보장된다.
* **큐에 쌓는 동안에는 이전 명령의 결과를 볼 수 없다.** 명령을 보내면 실행 결과가 아니라 `QUEUED`라는 응답만 돌아오기 때문이다. 따라서 **"조회한 값에 따라 분기"하는 로직은 MULTI로 만들 수 없다.**

**2. 원자성(Atomicity)**

* Redis는 롤백(Rollback)을 지원하지 않는다.
* 명령어 구문 오류(Syntax Error)는 `EXEC`전에 걸러지지만, 실행 중 에러가 발생하면 에러가 난 명령어만 실패하고 나머지는 그대로 반영된다.
* Redis는 성능 극대화를 지향하며, 롤백을 지원하지 않음으로써 시스템을 훨씬 단순하고 빠르게 유지한다.
* 공식 문서의 논리는 이렇다 — **문법 오류나 타입 오류는 개발 단계에서 잡아야 할 프로그래밍 실수이지 운영 중 발생할 상황이 아니므로**, 그것을 위해 롤백이라는 복잡한 장치를 두어 전체 성능을 희생할 이유가 없다는 것이다.

**3. WATCH : 낙관적 잠금(Optimistic Locking)**

* 동작 : `MULTI`를 시작하기 전에 특정 키를 `WATCH`한다. 만약 `EXEC`를 호출하기 전까지 다른 클라이언트가 내가 감시하던 키를 수정했다면, 내 트랜잭션은 통째로 취소된다.
* 낙관적 잠금 : 작업하는 동안 아무도 수정하지 않았다는 것을 낙관적으로 가정하고 마지막에 검사하는 방식이다. 무거운 자원 잠금 방식을 사용하지 않아 성능상 이점이 크다.
* 알아둘 점
  * `WATCH`는 **반드시 `MULTI` 이전에** 실행해야 한다. 트랜잭션 큐 안에서는 사용할 수 없다.
  * 취소되면 `EXEC`가 에러가 아니라 **`nil`(빈 응답)을 반환**한다. 그래서 애플리케이션은 이를 감지해 **처음부터 다시 시도하는 재시도 루프**를 직접 작성해야 한다.
  * 값이 실제로 달라졌는지가 아니라 **"수정 명령이 실행되었는지"** 로 판단한다. 같은 값을 다시 `SET` 해도 수정으로 간주되어 취소된다.
  * `EXEC` 또는 `DISCARD`가 실행되면 감시는 자동으로 해제된다. 중간에 직접 풀려면 `UNWATCH`를 쓴다.

#### Redis Transaction - 트랜잭션의 생명 주기

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6f38e1f8-8204-43b7-9a3c-2503e1605648.png)

**1. 트랜잭션의 생명 주기**

* 시작 : MULTI - 클라이언트가 `MULTI` 명령을 보내면 Redis는 해당 연결을 트랜잭션 모드로 전환한다. 이제부터 들어오는 명령들은 즉시 실행되지 않는다.
* 대기 : 명령 큐잉(Queuing) - `MULTI` 이후 들어오는 모든 명령어는 서버 명령 큐에 순서대로 쌓이며, 각 명령에 대해 `QUEUED` 응답이 돌아온다.
* 실행 트리거 : EXEC - 모든 명령을 큐에 담은 후 클라이언트가 `EXEC` 명령을 보낸다. 이 명령이 도착하는 순간 실제 연산이 일어난다.
* 순차 실행 및 결과 반환 - Redis 서버는 큐에 쌓인 명령들을 하나씩 순서대로 실행해 격리성을 확보하고 모든 명령 실행이 완료되면 각 명령 결괏값들을 모아 하나의 배열 형태로 클라이언트에게 반환한다.
* 취소 : DISCARD - `EXEC` 대신 `DISCARD`를 보내면 큐에 쌓인 명령을 모두 버리고 트랜잭션 모드에서 빠져나온다. 감시 중이던 키도 함께 해제된다.

**2. 에러가 나는 두 시점과 그 결과 — 이 구분이 핵심이다**

|시점|예시|결과|
|:---|:---|:---|
|**큐잉 단계 에러**|존재하지 않는 명령, 인자 개수 오류 등 서버가 큐에 담는 시점에 알 수 있는 오류|해당 명령이 큐잉되지 않고, **`EXEC` 시 `EXECABORT` 에러와 함께 트랜잭션 전체가 실행되지 않는다**(Redis 2.6.5+)|
|**실행 단계 에러**|String 키에 `LPUSH`를 하는 등 실제로 실행해봐야 아는 타입 오류|**그 명령만 에러로 처리되고 나머지 명령은 그대로 실행된다.** 이미 성공한 것도 되돌아가지 않는다|

* 즉 "구문 오류는 걸러진다"는 말은 **큐잉 시점에 알 수 있는 오류에 한정**된다. 실행해봐야 아는 오류는 막을 방법이 없고, 이것이 Redis에 롤백이 없다는 말의 실제 의미다.

```shell
MULTI
SET user:1 "kim"        # QUEUED
LPUSH user:1 "value"    # QUEUED (String 키인지 지금은 모른다)
INCR counter            # QUEUED
EXEC
# 1) OK
# 2) (error) WRONGTYPE Operation against a key holding the wrong kind of value
# 3) (integer) 1        ← 에러 이후의 명령도 그대로 실행된다
```

#### Redis Transaction - ACID 관점에서 본 Redis

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5e52fd0a-2286-42c1-99cd-2715bdc94005.png)

**1. Atomicity(원자성) : "부분적 지원"**

* 트랜잭션 내의 명령어가 전부 성공하거나 전부 실패해야 한다는 원칙이다.
* Redis의 현실 : `MULTI - EXEC`를 묶어서 실행하는 것까지 지원한다. 하지만 롤백이 없다. 중간에 특정 명령어가 실행 중 에러가 나더라도 이미 성공한 명령어는 그대로 남고 뒤의 명령어도 계속 실행된다.
* 정확히 말하면 Redis가 보장하는 것은 **"전부 아니면 전무"가 아니라 "중간에 다른 명령이 끼어들지 않는다"** 는 것이다. 원자성이라는 단어를 RDBMS와 같은 뜻으로 받아들이면 안 된다.

**2. Consistency(일관성) : "애플리케이션 책임"**

* 트랜잭션 전후 데이터 상태가 비즈니스 규칙에 맞게 유효해야 한다는 원칙이다.
* RDBMS처럼 강력한 스키마 제약 조건이 없고 Redis는 데이터 타입이 맞는지 정도만 체크해준다.
* 결국 데이터가 논리적으로 올바른가를 Redis가 아니라 애플리케이션에서 직접 검증하고 관리해야 한다.
* 외래키·유니크 제약·CHECK 같은 장치가 없으므로, "재고는 음수가 될 수 없다" 같은 규칙은 **애플리케이션이 검증하거나 Lua 스크립트 안에서 확인**해야 한다.

**3. Isolation(격리성) : "완전 지원"**

* 동시에 실행되는 트랜잭션들이 서로 간섭하지 못하게 격리되어야 한다는 원칙이다.
* Redis는 기본적으로 싱글 쓰레드 기반으로 동작하기 때문에 격리성이 완전 보장된다.
* `EXEC`가 실행되는 동안 다른 클라이언트 명령은 줄을 서서 기다려야 한다. 즉, 트랜잭션 도중에 다른 트랜잭션이 끼어들 여지가 물리적으로 차단되어 있어 완벽한 격리가 이루어지는 것이다.
* 단, 격리되는 구간은 **`EXEC` 실행 중**이다. `WATCH` 이후 `EXEC` 이전까지, 즉 **명령을 큐에 쌓고 있는 동안에는 다른 클라이언트가 얼마든지 값을 바꿀 수 있다.** 그 개입을 감지해 트랜잭션을 취소시키는 장치가 바로 `WATCH`다.

**4. Durability(지속성) : "설정 의존"**

* 성공적으로 완료된 트랜잭션의 결과는 시스템이 장애가 나더라도 영구적으로 보존되어야 한다는 점이다.
* Redis는 기본적으로 인메모리(In-Memory) 기반이기 때문에 설정을 건드리지 않으면 지속성을 보장하지 않는다.
* AOF/RDB 설정을 어떻게 하느냐에 따라 보존 수준이 달라진다.
* 가장 안전한 `appendfsync always`를 쓰더라도 **복제 환경에서는 Master가 응답한 뒤 Replica로 전파되기 전에 죽으면 유실될 수 있다.** Redis의 지속성은 설정으로 좁힐 수 있는 확률의 문제이지 RDBMS 같은 보장이 아니다.

#### Redis Transaction - WATCH를 이용한 CAS 패턴

* `WATCH`가 취소를 알려주기만 하므로, 실제 사용은 **성공할 때까지 반복하는 재시도 루프**가 된다.

```shell
# "재고가 있으면 1 줄인다"를 안전하게 처리하기
WATCH stock:100          # 감시 시작
GET stock:100            # → "5"  (여기서 값을 보고 판단할 수 있다)

# 애플리케이션이 5 > 0 임을 확인한 뒤
MULTI
DECR stock:100
EXEC
# → 성공하면 결과 배열 반환
# → 그 사이 누군가 stock:100을 건드렸다면 nil 반환 → 처음부터 다시 시도
```

* 이 구조가 성립하는 이유는 **`GET`이 `MULTI` 바깥에 있기 때문**이다. 트랜잭션 큐 안에서는 값을 읽어 판단할 수 없다.
* 경합이 심한 키에서는 재시도가 반복되어 오히려 느려질 수 있다. 그런 경우에는 아래 Lua 방식이 낫다.

#### Redis Transaction - Lua 스크립트와의 비교

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8d56a01a-0cbc-4aa5-863e-7d52993a362a.png)

* 실무에서 "여러 명령을 원자적으로"가 필요할 때는 트랜잭션보다 **Lua 스크립트(`EVAL`)** 를 쓰는 경우가 더 많다.

|구분|MULTI / EXEC|Lua 스크립트(EVAL)|
|:---|:---|:---|
|중간 결과 참조|❌ 큐에 쌓기만 하므로 불가|✅ 값을 읽고 **조건 분기·반복** 가능|
|원자성|명령들이 끼어듦 없이 실행|스크립트 전체가 하나의 명령처럼 실행|
|에러 시 롤백|❌ 없음|❌ 없음(단, 조건 검사로 사전 차단 가능)|
|경합 처리|WATCH + 재시도 루프 필요|재시도 없이 한 번에 처리|
|주의점|비교적 단순|⚠️ 스크립트가 길면 그동안 **서버 전체가 블로킹**된다|

```lua
-- "재고가 있을 때만 차감" — 조회와 판단과 갱신이 하나의 원자적 단위
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock and stock > 0 then
    return redis.call('DECR', KEYS[1])
else
    return -1
end
```

* 조건 검사를 스크립트 안에서 하므로 **애초에 잘못된 상태가 만들어지지 않는다.** 롤백이 없는 Redis에서 정합성을 지키는 현실적인 방법이다.

#### Redis Transaction - Spring에서의 사용

```java
// MULTI/EXEC는 같은 커넥션에서 실행되어야 하므로 SessionCallback으로 묶는다
List<Object> results = redisTemplate.execute(new SessionCallback<List<Object>>() {
    @Override
    public List<Object> execute(RedisOperations operations) {
        operations.watch("stock:100");                  // 감시 시작

        String stock = (String) operations.opsForValue().get("stock:100");
        if (stock == null || Integer.parseInt(stock) <= 0) {
            operations.unwatch();                        // 감시 해제 후 중단
            return null;
        }

        operations.multi();
        operations.opsForValue().decrement("stock:100");
        return operations.exec();                        // 취소되었으면 빈 리스트 반환
    }
});
// results가 비어 있으면 WATCH에 걸린 것 → 재시도 필요
```

* `SessionCallback` 없이 `redisTemplate`을 그냥 호출하면 **명령마다 커넥션 풀에서 다른 커넥션을 가져올 수 있어** `MULTI`와 `EXEC`가 서로 다른 연결에서 실행될 수 있다. 트랜잭션이 성립하지 않으므로 반드시 하나의 세션으로 묶어야 한다.
* `@Transactional`과 함께 쓰려면 `redisTemplate.setEnableTransactionSupport(true)` 설정이 필요하지만, 동작 방식이 직관적이지 않아 위처럼 `SessionCallback`을 쓰는 편이 명확하다.

#### Redis Transaction - 정리

|원칙|Redis|RDBMS|
|:---:|:---|:---|
|Atomicity|⚠️ 부분 — 끼어듦은 없지만 **롤백이 없다**|✅ 전부 아니면 전무|
|Consistency|⚠️ 애플리케이션 책임 — 제약 조건이 없다|✅ 스키마·제약으로 강제|
|Isolation|✅ 싱글 스레드로 완전 보장(단 EXEC 구간)|✅ 격리 수준으로 조절|
|Durability|⚠️ 설정 의존 — AOF/RDB 구성에 따라|✅ 커밋 = 영속 보장|

* 한 줄로 정리하면 — **Redis의 트랜잭션은 "여러 명령을 끼어듦 없이 순서대로 실행하는 도구"이지, RDBMS 트랜잭션처럼 실패를 되돌려주는 안전장치가 아니다.**
* 그래서 선택 기준은 이렇게 된다.
  * 단순히 여러 쓰기를 묶고 싶다 → **MULTI / EXEC**
  * 읽은 값에 따라 분기해야 한다 → **Lua 스크립트**
  * 경합이 드물고 재시도가 부담 없다 → **WATCH + 재시도**
  * 서로 다른 키·시스템에 걸친 정합성이 필요하다 → **Redis 트랜잭션으로는 부족하다.** 원본은 RDBMS에 두고 Redis는 보조로 쓰거나, 분산 락·이벤트 기반 설계를 검토해야 한다.
