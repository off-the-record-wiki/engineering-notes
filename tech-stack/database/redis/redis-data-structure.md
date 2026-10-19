# Redis - Redis Data Structure

#### Redis 자료구조

* Redis는 기본 설정상 0~15번까지 16개의 논리 데이터베이스로 구성된다(default는 0번 DB, databases 설정으로 변경 가능).
* 클러스터 모드에서는 0번 DB만 사용할 수 있어 `SELECT` 명령을 쓸 수 없다. 논리 DB로 데이터를 분리하는 대신 **키 접두사(`users:`, `orders:`)로 구분하는 것이 권장 방식**이다.

```
select DB 번호
```

#### String 자료구조

* 데이터를 String 형태의 value로 저장
* 가장 일반적인 "key - value" 구조 형태
* 바이너리 세이프(Binary Safe) : 텍스트 저장, 숫자 저장, 이진 데이터 저장, 직렬화된 객체 형태
* 최대 512MB 크기 제한 : 작은 값을 권장, 메모리 효율성 고려

#### String 자료구조 - SDS 구조와 3가지 인코딩

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-string-encoding.png)

* 값의 형태에 따라 3가지 인코딩 방식이 **자동으로 선택**된다 → 메모리 효율을 위해서
  * `int` : 값이 64비트 정수로 표현 가능할 때. Redis 객체 헤더(16바이트) + 정수값(8바이트) = 약 24바이트
  * `embstr` : **44바이트 이하**의 짧은 문자열. Redis 객체와 SDS를 한 번의 메모리 할당으로 붙여서 저장(약 20바이트 + 문자열 길이)
  * `raw` : **44바이트를 초과**하는 문자열. Redis 객체(16바이트) + SDS 헤더(8바이트) + 문자열 길이 + 여유 공간
  * 현재 인코딩은 `OBJECT ENCODING [key]`로 확인할 수 있다. embstr로 저장된 키도 한 번 수정(APPEND 등)되면 raw로 전환된다.
* 다음과 같이 `GET`, `SET`을 사용하며 `GET`은 읽기, `SET`은 쓰기를 위한 명령어이다.

#### String 자료구조 - SET / GET

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-set-get.png)

#### String 자료구조 - 카운터 명령어(INCR / DECR / INCRBY / DECRBY)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-counter-commands.png)

* `INCR`, `DECR`, `INCRBY`, `DECRBY`의 명령어에서 만약 키가 존재하지 않는 경우 카운터 초기화와 증감 연산을 모두 처리해준다.

#### String 자료구조 - 싱글 스레드와 원자적 연산

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-atomic-incr.png)

* Redis는 기본적으로 싱글 쓰레드(Single-Thread) 모델 위에서 동작한다. 즉, 여러 클라이언트가 동시에 `INCR` 요청을 보내더라도 Redis 서버 내부에서는 이 요청들이 순차적으로 하나씩 처리된다.
* 한 번에 하나의 명령어만 처리하기 때문에 한 명령어가 수행되는 동안 다른 명령어가 끼어들어 데이터(값)를 수정할 수 없다.
* 일반적인 멀티 쓰레드 환경에서 [값 읽기 → 1 더하기 → 값 저장하기] 이 3단계 과정을 수행하는 과정에서 다른 쓰레드가 개입하면 Race Condition이 발생한다.
* Redis의 경우 위의 3단계 과정을 하나의 덩어리로 묶어서 원자적으로 처리한다. 싱글 쓰레드가 이 덩어리를 통째로 처리하고 다음 요청으로 넘어가기 때문에 별도의 복잡한 잠금 메커니즘 없이도 동시성 이슈를 자연스럽게 해결할 수 있다.
* 엄밀히 말하자면 메인 루프가 명령어를 처리하는 동안 블로킹 상태가 되는 것이 맞다고 볼 수 있다.
* 하지만 Redis는 모든 데이터를 메모리 위에서 처리하기 때문에 개별 명령어의 실행 속도가 매우 빠르고 사용자 입장에서 거의 블로킹을 느끼지 못해 매우 높은 처리량(Throughput)을 보여준다.
* 참고 : Redis 6.0부터 `io-threads` 설정으로 **네트워크 I/O(송수신)만** 멀티 스레드로 처리할 수 있다. 명령 실행 자체는 여전히 싱글 스레드이므로 위의 원자성은 그대로 유지된다.

> ❗이 때, `KEYS *`와 같이 처리 시간이 오래 걸리는 명령어를 실행하게 되면 메인 쓰레드가 점유되어 뒤에 대기 중인 다른 명령어들이 정말로 블로킹되면서 서비스 장애가 발생할 수 있으니 `KEYS` 명령어는 운영 환경에서 절대로 사용하면 안 된다.(대안 : 커서 기반으로 나눠 순회하는 `SCAN`)

#### String 자료구조 - 네트워크 RTT(Round Trip Time)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-rtt-comparison.png)

* Redis 자체의 처리 시간은 짧으나 네트워크 왕복 시간은 Redis가 제어를 할 수 없는 영역이다.

#### String 자료구조 - MSET / MGET

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-mset-mget.png)

* RTT(Round Trip Time) 자체를 줄이기 위해 여러 값을 읽고 넣을 수 있는 `MSET`, `MGET`을 제공한다.
* 서로 다른 종류의 명령을 한 번에 몰아 보내려면 **파이프라이닝(Pipelining)** 을 사용한다.(Redis Pipelining & RTT 문서 참고)
* 클러스터 모드에서는 `MSET`/`MGET`의 모든 키가 같은 슬롯에 있어야 한다. 해시 태그(`{user:100}`)로 슬롯을 맞춰야 사용할 수 있다.

#### String 자료구조 - TTL 관리(SETEX / SETNX / SET 옵션)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-set-options.png)

* 메모리는 제한적이기 때문에 만료일자를 지정해두고 사용해서 오래된 데이터를 제거해주어야 한다.
* `SETEX key seconds value` : 저장과 동시에 TTL을 부여한다.
* `SETNX key value`는 **TTL과 무관한 명령이다.** "키가 없을 때만 저장(SET if Not Exists)"할 뿐 만료시간을 설정하지 않는다. 분산 락 용도로 `SETNX`만 쓰면 락 해제에 실패했을 때 키가 영원히 남는 문제가 생긴다.
* 따라서 현재 권장 방식은 옵션을 조합한 **`SET key value NX EX seconds`** 한 줄이다. 저장과 TTL 부여가 하나의 원자적 명령으로 처리된다.

#### List 자료구조 - 전체 구조와 시간 복잡도

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-list-structure.png)

* Redis의 List 자료구조는 **논리적으로는** 양방향 연결 리스트(Doubly Linked List)처럼 동작한다.(실제 내부 구현은 아래의 quicklist)
* 어느 지점을 건드리는지에 따라 성능이 극명히 갈린다.

|작업 유형|명령어 예시|시간 복잡도|설명|
|:---:|:---:|:---:|:---:|
|양 끝 삽입/삭제|LPUSH, RPUSH, LPOP, RPOP|O(1)|양방향 포인터를 가지고 있어 즉시 접근이 가능하다.|
|인덱스 조회|LINDEX|O(N)|인덱스만큼 노드를 타고 이동해야 한다.|
|중간 삽입/삭제|LINSERT, LREM|O(N)|특정 위치나 값을 찾기 위한 탐색 과정(O(N))이 필요하다.|
|범위 조회|LRANGE|O(S + N)|시작 지점까지의 거리(S)와 가져올 개수(N)만큼에 비례한다.|
|길이 조회|LLEN|O(1)|내부에 유지하는 길이 필드를 그대로 반환한다.|

* 단순히 Linked List만 쓰자니 포인터가 차지하는 메모리가 너무 비효율적이고 데이터가 파편화되어 CPU 캐시 효율이 떨어지는 문제를 해결하기 위해 quicklist가 등장했다.(Redis 3.2부터 List의 유일한 내부 인코딩)
* 여러 개의 Listpack(구버전의 ziplist)을 Doubly Linked List 형태로 연결한 구조가 quicklist이다.
  * 메모리 절약 : 노드 간의 포인터 개수를 획기적으로 줄인다.
  * 성능 향상 : 연속된 메모리 공간(Listpack)에 데이터를 모아두어 CPU 캐시 적중률을 높인다.
  * 한 노드에 담을 요소 수는 `list-max-listpack-size`로 조정한다.

#### List 자료구조 - LPUSH / RPUSH

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-lpush-rpush.png)

* `LPUSH` : 가장 앞은 단 한 번에 찾을 수 있기 때문에 시간 복잡도는 O(1)이다.
* `RPUSH` : Redis의 List는 양방향 구조이므로 마지막 노드를 찾기 위해 전체를 순회할 필요가 없다. 그렇기 때문에 시간 복잡도는 O(1)이다.
* 여러 값을 한 번에 넣을 때 `LPUSH key A B C`는 하나씩 차례로 Head에 밀어 넣으므로 결과가 `[C, B, A]`로 **역순**이 된다. 입력 순서를 유지하려면 `RPUSH`를 쓴다.

#### List 자료구조 - LPOP / RPOP

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-lpop-rpop.png)

* `LPOP` : `LPUSH`와 마찬가지로 O(1)이다.
* `RPOP` : `RPUSH`와 마찬가지로 O(1)이다.
* 꺼낸 값을 반환하며 조회와 삭제가 한 번에 원자적으로 처리된다. 비어 있거나 키가 없으면 `nil`을 반환하고(에러 아님), 마지막 요소를 꺼내면 키 자체가 자동 삭제된다.
* 이 O(1) 양 끝 연산 덕분에 List는 **큐(RPUSH + LPOP)** 나 **스택(LPUSH + LPOP)** 으로 활용된다. 큐가 빌 때까지 대기해야 한다면 폴링 대신 블로킹 명령인 `BLPOP`/`BRPOP`을 사용한다.

#### List 자료구조 - LRANGE

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-lrange.png)

* `LRANGE`는 리스트의 start 인덱스부터 stop 인덱스까지의 모든 요소를 가져온다.(start와 stop **모두 포함**)
  * 양수 인덱스(0부터 시작)
  * 음수 인덱스(-1부터 시작)
* Redis는 연결 리스트 기반이기 때문에 특정 위치를 찾으려면 포인터를 타고 이동해야 한다.
* `S(start offset)` : 시작 지점까지 찾아가는 거리
* `N(number of elements)` : 시작 지점부터 가져올 요소의 개수
* 즉, 리스트의 아주 깊숙한 지점부터 데이터를 가져오라고 한다면 데이터 양에 비례하기 때문에 시간 복잡도를 따진다면 `O(S + N)`이 된다.
* `LRANGE key 0 -1`(전체 조회)은 리스트가 커질수록 그대로 `O(N)` 블로킹 명령이 된다. 페이지 단위로 끊어서 조회하고, 리스트가 무한히 커지지 않도록 `LTRIM`으로 길이를 제한하는 것이 안전하다.

#### List 자료구조 - LLEN / LINDEX

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-llen-lindex.png)

* `LLEN`
  * Redis는 List 객체 내부에 `len` 필드를 두고 요소가 추가되거나 삭제될 때마다 이 값을 갱신한다.
  * 그렇기 때문에 `LLEN` 명령이 들어오면 리스트를 훑는 것이 아니라 메모리에 저장된 숫자 하나를 그냥 읽어서 반환한다. → `O(1)`
* `LINDEX`
  * 반면, 특정 위치의 데이터를 가져오는 LINDEX는 연결 리스트의 태생적 한계를 그대로 가진다.
  * 인덱스 번호가 커질수록 그에 비례해서 탐색 시간에 들어가는 비용이 증가하기 때문에 시간 복잡도는 `O(N)`이다.
  * 그나마 다행인 점은 Redis가 양방향(Doubly) 구조이기 때문에 무조건 앞에서부터 가는 것이 아니라, Head와 Tail 중 목표 인덱스에 가까운 쪽을 선택해서 탐색을 한다.

#### Set 자료구조 - 네 가지 성질

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-set-structure.png)

**1. 중복 없는 컬렉션**

* Redis Set은 수학의 '집합'과 동일하게 중복된 데이터를 허용하지 않는다.
* 만약 이미 Set에 존재하는 값을 다시 추가하려고 하면 무시된다. 따라서 데이터의 고유성을 보장해야 할 때 매우 유용하다.

**2. 순서 없음(Unordered)**

* 데이터가 입력된 순서를 기억하는 List와 달리, Set은 데이터의 순서를 유지하지 않는다.
* 데이터를 조회할 때 입력했던 순서대로 반환된다는 보장이 없으므로 순서가 중요한 데이터에는 적합하지 않다.

**3. 매우 빠른 검색**

* 특정 데이터가 Set 안에 포함되어 있는지(존재 여부)를 확인하는 속도가 데이터의 양과 관계없이 항상 일정하고 매우 빠르다.
* 해시 테이블 구조를 사용하기 때문에 엄청난 양의 데이터 속에서도 즉시 검색이 가능하다.
* 내부 인코딩은 상황에 따라 자동 선택된다. 모든 요소가 정수이고 개수가 적으면 `intset`, 짧은 문자열이 적게 들어 있으면 `listpack`(7.2+), 그 외에는 `hashtable`. 앞의 두 경우는 메모리를 크게 아끼는 대신 요소 탐색이 선형이지만, 요소 수가 적어 실질적인 성능 저하는 없다.

**4. 집합 연산 지원**

* 다수의 Set 간에 수학적인 집합 연산을 데이터베이스 레벨에서 강력하고 빠르게 지원한다.
  * `SINTER`(교집합) / `SUNION`(합집합) / `SDIFF`(차집합), 결과를 새 키에 저장하는 `...STORE` 계열도 있다.
  * 대용량 Set 간의 집합 연산은 O(N) 이상의 비용이 드는 블로킹 연산이다. 크기가 큰 Set들끼리는 신중히 사용하고, 클러스터 모드에서는 대상 키들이 같은 슬롯에 있어야 한다.

#### Set 자료구조 - SADD / SREM

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-sadd-srem.png)

**1. SADD(Set ADD) - 요소 추가**

* SADD는 Set에 새로운 데이터를 집어넣을 때 사용하는 명령어이다.
* 중복 무시, 유일성 보장 : 여러 개의 데이터를 한 번에 추가하려고 시도하더라도, 이미 Set 내부에 존재하는 값이라면 무시하고 넘어간다.
* 추가된 개수 반환 : 명령을 실행한 후, Redis는 실제로 새롭게 추가된 데이터의 개수를 숫자로 반환한다.
  * 이 반환값을 이용하면 "이번 요청이 최초인가"를 원자적으로 판별할 수 있다.(중복 참여 방지, 중복 이벤트 처리 방지 등)

**2. SREM(Set REMove) - 요소 제거**

* SREM은 Set에서 특정 데이터를 삭제할 때 사용하는 명령어이다.
* 존재하는 요소만 제거 : 지우고자 하는 데이터를 지정했을 때, 해당 데이터가 Set 안에 실제로 존재할 때만 삭제 작업을 수행한다. 만약 Set에 없는 엉뚱한 데이터를 지우라고 명령하더라도, 시스템 오류를 발생시키지 않고 조용히 무시하고 넘어간다.
* 제거된 개수 반환 : SADD와 마찬가지로, 명령 실행 후 실제로 삭제에 성공한 데이터의 개수를 반환한다.
  * 마지막 요소를 지우면 키 자체가 자동 삭제된다.

#### Set 자료구조 - SMEMBERS / SISMEMBER

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-smembers-sismember.png)

**1. SMEMBERS - 모든 요소 조회**

* 특정 Set 안에 들어있는 모든 데이터를 한 번에 가져오고 싶을 때 사용하는 명령어이다.
* 전체 요소 배열 반환 : Set 내부에 저장된 모든 고유한 값들을 모아 하나의 배열(Array) 형태로 반환한다.
* 시간 복잡도 `O(N)` : 이 명령어는 Set에 들어있는 데이터의 총 개수(N)에 비례하여 처리 시간이 늘어난다. 수십만 ~ 수백만 개의 데이터가 쌓인 Set에서 이 명령어를 실행하면 Redis 서버 전체에 심각한 성능 저하를 유발할 수 있으므로 주의해서 사용해야 한다.
  * 대안 : 커서 기반으로 나눠서 순회하는 `SSCAN`을 사용한다.(`KEYS` → `SCAN`과 같은 관계)
  * 개수만 필요하다면 `O(1)`인 `SCARD`를 쓴다.

**2. SISMEMBER - 존재 확인**

* 찾고자 하는 특정 값이 이 Set 안에 포함되어 있는지(멤버가 맞는지) 질문할 때 사용하는 명령어이다.
* 전체 데이터를 가져오는 것이 아니라, 오직 논리적인 참/거짓 결과만 숫자로 반환한다.
* 시간 복잡도 `O(1)` : Set 내부의 데이터가 10개이든 1,000만 개이든 전혀 상관없이, 항상 즉시(일정한 시간 내에) 결과를 반환한다.
* 여러 값을 한 번에 확인해야 한다면 `SMISMEMBER key m1 m2 m3`(Redis 6.2+)로 `[1, 0, 1]` 형태의 결과를 받을 수 있다.

#### Hash 자료구조 - 네 가지 성질

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-hash-structure.png)

**1. Field-Value 쌍 저장**

* Redis Hash는 하나의 거대한 Key 안에 또 다시 여러 개의 필드(Field)와 값(Value)이 짝을 지어 저장되는 구조이다.

**2. 객체 표현에 최적화**

* 관계형 데이터베이스(RDBMS)의 '행(Row)'이나 프로그래밍의 '객체(Object)' 데이터를 표현하는 데 유용하다.

**3. 메모리 효율적(압축 인코딩 지원)**

* 객체 데이터를 각각 개별적인 일반 Key-Value로 저장하는 것보다, 하나의 Hash 안에 여러 Field로 모아서 저장하는 것이 메모리 사용량 측면에서 훨씬 효율적이다.
* Redis는 Hash 내부의 필드 개수가 적고 값의 길이가 짧을 때, 내부적으로 압축 인코딩(Redis 7.0+ `listpack`, 그 이전은 `ziplist`)을 사용하여 메모리 공간을 극적으로 절약해준다.
  * 전환 기준은 `hash-max-listpack-entries`(기본 128개)와 `hash-max-listpack-value`(기본 64바이트)이며, 이를 넘어서면 일반 `hashtable` 인코딩으로 자동 전환된다. **한 번 전환되면 데이터가 줄어도 되돌아가지 않는다.**

**4. 부분 업데이트 가능(필드별 개별 접근)**

* 하나의 거대한 텍스트나 JSON 형태로 전체 데이터를 저장하면 데이터 일부만 바꾸고 싶을 때도 전체를 다 불러와서 수정한 뒤 다시 덮어써야 한다.
* Hash를 사용하면 원하는 특정 필드에만 직접 접근하여 값을 읽거나 수정할 수 있다.
* 단, **TTL은 키 단위로만 걸린다.** 필드별 만료는 지원하지 않으므로(7.4부터 일부 지원) 필드마다 수명이 달라야 한다면 별도 키로 분리해야 한다.

#### Hash 자료구조 - HSET / HGET

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-hset-hget.png)

**1. HSET(Hash SET) - 필드 설정**

* HSET은 Hash 구조(Key) 안에 특정 필드(Field)와 그에 해당하는 값(Value)을 저장하거나 수정할 때 사용하는 명령어이다.
* 단일/다중 필드 설정 : 과거에는 하나의 필드만 설정하는 HSET과 여러 필드를 동시에 설정하는 HMSET이 구분되어 있었으나, 최신 Redis 버전부터는 HSET 명령어 하나로 통합되었다.
* 반환값은 **새로 생성된** 필드의 개수이며, 기존 필드를 덮어쓴 경우는 세지 않는다.

**2. HGET(Hash GET) - 필드 조회**

* HGET은 Hash 구조 안에 저장된 여러 정보 중 내가 딱 원하는 특정 필드의 값만 읽어올 때 사용하는 명령어이다.
* 단일 필드 값 반환 : 불필요한 데이터를 네트워크로 전송하지 않아도 되므로 트래픽을 아끼고 속도를 높일 수 있다.
* 없는 필드나 없는 키를 조회하면 `nil`을 반환한다.(에러 아님)

**3. 시간 복잡도 : `O(1)` per field**

* Hash 안에 필드가 10개가 있든 10만 개가 있든, 특정 필드 하나를 콕 집어서 값을 읽거나 쓰는 데 걸리는 시간은 데이터 양과 무관하게 항상 O(1)으로 일정하다.
* per field(필드 당) : 전체 데이터의 크기가 아니라 내가 한 번의 명령어로 조작하려는 필드의 개수에만 비례하여 정직하게 시간이 걸린다.
* 그래서 JSON 문자열로 통째 저장하는 방식과 차이가 크다. JSON은 필드 하나를 바꾸려 해도 [전체 조회 → 파싱 → 수정 → 직렬화 → 저장] 과정을 거쳐야 하고 그 사이 다른 요청과 덮어쓰기 충돌이 날 수 있지만, Hash는 `HSET key field value` 한 번으로 원자적으로 끝난다.

#### Hash 자료구조 - HMSET / HMGET

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-hmset-hmget.png)

**1. HMSET(Hash Multiple SET) - 여러 필드 설정**

* HMSET은 한 번의 명령으로 Hash 안에 여러 개의 필드와 값을 동시에 집어넣을 때 사용하던 명령어이다.
* HSET으로 대체 : 최신 버전의 Redis(4.0 이후)에서는 기존의 단일 필드용 명령어였던 HSET이 여러 필드를 동시에 설정할 수 있도록 기능이 변경되었다. 따라서 굳이 HMSET을 쓸 이유가 없다.(HMSET은 deprecated 상태)
* 하위 호환성 유지 : 과거에 HMSET을 사용해서 만들어진 수많은 기존 프로그램들이 갑자기 작동을 멈추면 안 되기 때문에, Redis 측에서 명령어 자체는 계속 정상적으로 동작하도록 하위 호환성을 보장해 주고 있다.

**2. HMGET(Hash Multiple GET) - 여러 필드 조회**

* HMGET은 Hash 안에 있는 수많은 정보 중, 내가 딱 필요한 '여러 개'의 필드 값만 쏙쏙 골라서 한 번에 가져오고 싶을 때 사용하는 매우 유용한 명령어이다.
* 배열로 값 반환 : 요청한 여러 필드에 해당하는 값들을 모아서 하나의 배열(Array) 리스트 형태로 묶어서 반환해준다.
* 필드 순서 보존 : 반환되는 배열 안의 데이터 순서는 내가 명령어를 입력할 때 요청한 필드의 순서와 정확히 일치한다. 존재하지 않는 필드 자리에는 `nil`이 들어와 **자리를 유지**하므로 인덱스가 밀리지 않는다.
* HGET을 여러 번 호출하는 것보다 네트워크 왕복(RTT)이 줄어든다.

> 쓰기 쪽은 HSET이 다중 설정 기능을 흡수해 HMSET이 불필요해졌지만, 읽기 쪽은 HGET이 여전히 단일 필드 전용이라 **HMGET은 계속 필요하다.** 이것이 둘 중 HMSET만 deprecated된 이유다.

#### Hash 자료구조 - HINCRBY / HINCRBYFLOAT

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-hincrby.png)

**1. HINCRBY - 정수 증가**

* HINCRBY는 필드에 저장된 값이 정수(Integer)일 때, 지정한 숫자만큼 값을 더해주는 명령어이다.
* 64비트 정수 범위 : 내부적으로 64비트 부호 있는 정수(Signed Integer)를 지원하여 약 `±922경`까지의 값을 다룰 수 있다.
  * 이 범위를 벗어나면 오버플로우가 조용히 발생하는 것이 아니라 **에러를 반환한다.** 필드 값이 숫자가 아닐 때도 마찬가지로 에러다.
* 증감 모두 가능 : 증가시킬 값으로 양수를 넣으면 값이 올라가고, 음수(예: -1)를 입력하면 더하기의 역연산으로 결과적으로 값을 감소시킨다.
* 필드가 없으면 0에서 시작해 연산하며, 반환값은 연산 후의 값이다.

**2. HINCRBYFLOAT - 실수 증가**

* HINCRBYFLOAT는 필드에 저장된 값이 실수(소수점이 있는 숫자)일 때, 지정한 수치만큼 더해주는 명령어이다.
* 배정밀도 부동소수점(Double-precision floating-point) : 컴퓨터가 소수를 표현하는 정교한 방식으로, 소수점 아래 계산 시 발생할 수 있는 미세한 오차를 최소화한다.
  * 다만 부동소수점의 특성상 오차가 완전히 사라지는 것은 아니므로, **금액처럼 정확도가 중요한 값은 정수(원 단위·최소 단위)로 저장하고 HINCRBY를 쓰는 것이 안전하다.**

**3. 원자적 연산(Atomic Operation)**

* 데이터 일관성 완벽 보장 : Redis는 이 명령들을 절대 쪼개거나 섞지 않고 한 번에 하나씩 온전하게(=원자적으로) 처리해낸다.
* 동시성 문제(Race Condition) 원천 차단 : 해당 명령어들을 사용하면 누락이나 충돌 없이 정확하게 세는 것을 100% 보장한다.(단, 애플리케이션에서 값을 읽어와 계산한 뒤 다시 쓰는 방식으로 바꾸는 순간 이 보장은 사라진다)

#### Hash 자료구조 - HEXISTS / HKEYS / HVALS

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-hash-info.png)

**1. HEXISTS - 필드 존재 확인**

* HEXISTS는 특정 Hash 안에 내가 찾고자 하는 필드가 실제로 존재하는지 여부만 빠르게 검사할 때 사용한다.
* O(1) 시간복잡도 및 빠른 체크 : 값을 굳이 네트워크로 가져올 필요 없이 로직상 '유무'만 판단해야 할 때 시스템 자원을 아끼며 아주 빠르게 사용할 수 있다.

**2. HKEYS - 모든 필드명 조회**

* HKEYS는 특정 Hash 안에 저장된 모든 필드(Field)의 이름들만 쏙쏙 뽑아서 배열 형태로 반환한다.

**3. HVALS - 모든 값 조회**

* HVALS는 Hash 안에 저장된 모든 값(Value)들만 모아서 배열로 반환한다.

**4. 전체 조회 계열의 주의사항**

* `HKEYS`, `HVALS`, `HGETALL`은 모두 필드 개수 N에 비례하는 **O(N) 블로킹 명령**이다. 필드가 수만 개인 Hash에 사용하면 `KEYS *`와 같은 장애로 이어질 수 있다.
* 필요한 필드만 `HMGET`으로 집어오거나, 전체를 훑어야 한다면 커서 기반의 `HSCAN`을 사용한다. 개수만 필요하면 O(1)인 `HLEN`을 쓴다.

#### Sorted Set 자료구조 - 네 가지 성질

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-zset-structure.png)

**1. 중복 없는 요소(Set의 유일성)**

* 이미 존재하는 Member를 다시 삽입하려고 하면, 새로운 Member가 추가되는 것이 아니라 기존 Member의 Score 값만 새로운 값으로 업데이트되며, 그에 맞게 정렬 위치가 재조정된다.
* Member는 중복될 수 없지만 **Score는 중복될 수 있다.** Score가 같은 요소들끼리는 Member의 사전순(lexicographical)으로 정렬된다.

**2. 내부 자료구조: Skip List + Hash Table**

* Hash Table : Member → Score를 O(1)로 찾기 위한 인덱스 역할
* Skip List(스킵 리스트)
  * 일반적인 연결 리스트와 달리, 중간 노드들을 건너뛸 수 있는(Skip) 여러 계층의 포인터를 가진다.
  * 이로 인해 이진 탐색 트리(Balanced Tree)와 유사하게 검색, 삽입, 삭제 시 시간 복잡도 O(log N)을 가진다.
  * 트리 구조에 비해 구현이 단순하고, 범위 탐색(Range Query) 시 노드를 순차적으로 따라가기만 하면 되므로 성능상 훨씬 유리하다.
* 참고 : 요소 수가 적고(`zset-max-listpack-entries`, 기본 128) 값이 짧을 때는 두 구조 대신 `listpack` 하나로 저장해 메모리를 아낀다. 기준을 넘으면 위의 Skip List + Hash Table 조합으로 전환된다.

**3. 범위 조회 지원**

* 정렬된 상태를 유지하므로, 특정 구간에 대한 조회가 매우 빠르다.
* 시간 복잡도는 `O(log N + M)(N은 전체 요소 수, M은 반환되는 요소 수)`으로 대규모 트래픽에서도 안정적으로 동작한다.
* 이 특성 덕분에 **실시간 랭킹(리더보드), 인기 검색어, 우선순위 큐, 시간 기반 정렬(Score에 타임스탬프)** 등에 널리 쓰인다.

#### Sorted Set 자료구조 - ZADD

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-zadd.png)

**1. 요소 존재 여부 확인(추가 또는 업데이트 분기)**

* Redis는 내부적으로 유지하고 있는 Hash Table을 활용하여 데이터가 이미 존재하는지 O(1)의 속도로 빠르게 확인한다.
  * 새 요소 삽입 : Hash Table에 데이터가 없다면 완전히 새로운 데이터로 간주하고 Skip List에 새로운 노드를 생성하여 배치한다.
  * 기존 요소 변경 : Hash Table에 데이터가 있다면 Score 값을 갱신하고, 정렬 순서가 달라졌다면 Skip List 상의 위치를 새 Score에 맞게 재배치한다.
* `ZADD`는 옵션으로 동작을 제어할 수 있다 - `NX`(없을 때만 추가), `XX`(있을 때만 수정), `GT`/`LT`(기존보다 크거나 작을 때만 갱신). 랭킹에서 "최고 점수만 유지"하려면 `GT`가 유용하다.

**2. 시간 복잡도 O(log N) 보장**

* 새로운 요소를 삽입할 위치를 찾거나 변경된 기존 요소를 새로운 위치로 옮길 때 Redis는 전체 데이터를 순차 탐색하지 않는다.
* 다층 연결 리스트 구조인 Skip List를 타며 탐색하기 때문에 이진 탐색 트리와 동일한 O(log N)의 시간 복잡도로 빠르게 정렬 위치를 찾아낸다.

**3. 자동 정렬 위치 조정**

* 적절한 위치를 찾았다면 노드들의 포인터를 수정하여 데이터를 끼워 넣거나 이동시킨다.
* 이 과정이 끝나면 데이터는 Score 오름차순으로 완벽하게 정렬된 상태를 유지하게 된다.

#### Sorted Set 자료구조 - ZRANGE / ZREVRANGE

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-zrange.png)

* ZRANGE(오름차순 조회)
* ZREVRANGE(내림차순 조회) - Redis 6.2부터는 `ZRANGE key start stop REV` 형태로 통합되었다.
* Redis Sorted Set에서는 `O(log N + M)`의 시간 복잡도가 나오게 된다. 이 때, 전체 요소의 수는 `N`, 반환할 요소의 수를 `M`이라고 한다.
  * 시작점 찾기 : `O(log N)`, Skip List 구조를 통해 이진 탐색 수준의 속도를 가지게 된다.
  * 순차적 탐색 : `O(M)`, 그 노드부터 메모리 상에 연결된 포인터를 따라 지정한 개수만큼 옆으로 이동하며 데이터를 쭉 읽어오기만 하면 된다.
* `ZRANGE key 0 -1`처럼 M이 전체가 되는 조회는 결국 O(N)이다. 랭킹은 상위 N건만 끊어서 조회해야 한다.
* 점수 구간으로 자를 때는 `ZRANGEBYSCORE`, 오래된 데이터를 정리할 때는 `ZREMRANGEBYSCORE`/`ZREMRANGEBYRANK`를 사용한다.

#### Sorted Set 자료구조 - ZRANK / ZREVRANK

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-zrank.png)

* ZRANK(오름차순 순위)
* ZREVRANK(내림차순 순위)
* Skip List는, 노드와 노드를 연결하는 포인터에 단순히 다음 노드의 주소만 저장하는 것이 아니라 몇 개의 노드를 건너뛰고 있는지에 대한 숫자 정보(Span)도 함께 저장한다.
* Redis는 Skip List의 최상단부터 해당 요소를 찾아 내려가면서 거쳐온 경로의 Span 값들을 단순히 더하기만 한다.
* 데이터를 일일이 세지 않고 건너뛴 덩어리의 개수만 합산하여 타겟에 도달하기 때문에, 탐색 시간(O(log N))만으로 정확한 전체 순위를 계산할 수 있게 된다.
* 순위는 0부터 시작하므로 사용자에게 보여줄 때는 +1이 필요하고, 요소가 없으면 `nil`이 반환된다.

#### Sorted Set 자료구조 - ZINCRBY

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-zincrby.png)

* ZINCRBY(점수 증감 명령어)
* 양수/음수 모두 가능(증가 및 감소) : 덧셈, 뺄셈과 같은 기본 연산이 가능하며 만약 키나 요소가 존재하지 않는다면 초기값을 0으로 간주하고 입력한 값으로 새롭게 요소를 생성한 뒤 정렬 위치를 맞춘다.
* `INCR`과 마찬가지로 원자적이므로, 조회수·좋아요 집계와 랭킹 갱신을 한 번의 명령으로 동시에 처리할 수 있다.(인기 검색어 구현의 핵심 명령)

#### Sorted Set 자료구조 - ZREM

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/redis-zrem.png)

* ZREM(요소 삭제 명령어)
* 지정 요소 삭제 및 다중 삭제(Multiple Deletion) : 특정 요소 1개만 지울 수도 있지만, 여러 개의 요소를 한 번의 명령어로 동시에 지울 수도 있다.
* 시간 복잡도 O(M log N) : 현재 Sorted Set에 존재하는 전체 요소의 개수를 N, 이번 명령어로 삭제하려고 요청한 요소의 개수를 M이라고 할 때, Skip List를 타며 이진 탐색 수준인 O(log N)이 걸리고 여러 개를 지우라고 명령을 내리게 되면 O(log N)의 탐색 및 삭제 과정을 M번 반복하므로 최종적으로 O(M log N)이 된다.
* 삭제된 개수 반환(멱등성 보장) : 실제로 지워진 요소의 개수를 정수형(Integer)으로 반환한다. 없는 요소를 지우라고 해도 에러 없이 0을 반환한다.
* 하나씩 지정하지 않고 조건으로 정리하려면 `ZREMRANGEBYSCORE`(점수 구간)나 `ZREMRANGEBYRANK`(순위 구간)를 쓴다. 예를 들어 `ZREMRANGEBYRANK ranking 0 -101`은 상위 100개만 남기고 나머지를 삭제한다.

#### 자료구조 선택 요약

|자료구조|핵심 성질|대표 O(1) 연산|주의할 O(N) 연산|대표 활용|
|:---:|:---|:---|:---|:---|
|String|단일 값(최대 512MB), 원자적 카운터|GET/SET, INCR|-|캐시, 카운터, 분산 락|
|List|삽입 순서 유지, 양 끝 O(1)|LPUSH/RPUSH/LPOP/RPOP, LLEN|LINDEX, LRANGE 전체 조회|큐·스택, 최근 목록|
|Set|중복 없음, 순서 없음|SADD, SISMEMBER, SCARD|SMEMBERS, 대용량 집합 연산|태그, 중복 방지, 좋아요 목록|
|Hash|필드-값 묶음, 부분 수정|HGET/HSET, HEXISTS, HLEN|HGETALL, HKEYS, HVALS|객체·세션 저장|
|Sorted Set|Score 기준 자동 정렬|ZCARD (조회·삽입은 O(log N))|ZRANGE 전체 조회|랭킹, 인기 검색어, 우선순위 큐|

* 공통 원칙 : **전체를 한 번에 가져오는 명령(`KEYS`, `SMEMBERS`, `HGETALL`, `LRANGE 0 -1`, `ZRANGE 0 -1`)은 컬렉션이 커지는 순간 서버를 멈추게 하는 블로킹 명령이 된다.** 커서 계열(`SCAN`/`SSCAN`/`HSCAN`/`ZSCAN`)이나 범위 제한 조회로 대체하는 습관이 필요하다.
