# Redis - Redis Streams

#### Redis Streams - Stream이란

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3c1e8207-39c2-4989-9b5d-5d93f29d81c3.png)

* **추가만 가능한(append-only) 로그** 자료구조다. 메시지를 넣으면 사라지지 않고 순서대로 쌓이며, 여러 소비자가 각자의 위치에서 읽어갈 수 있다.
* Pub/Sub이 "지금 듣고 있는 사람에게만 뿌리고 잊는" 방식이라면, Streams는 **저장하고 · 누가 어디까지 읽었는지 추적하고 · 실패하면 다시 처리**할 수 있다.
* 각 항목(Entry)은 `밀리초타임스탬프-시퀀스` 형태의 ID를 가진다. 같은 밀리초에 여러 건이 들어오면 시퀀스가 증가하므로 **ID는 항상 단조 증가**하고, 그래서 순서가 보장된다.

```shell
XADD orders * userId 100 amount 5000
# → "1735200000000-0"   ← * 를 주면 Redis가 ID를 자동 생성한다

XLEN orders                     # 항목 개수
XRANGE orders - +                # 전체 조회(- 최소 ID, + 최대 ID)
XRANGE orders - + COUNT 10       # 앞에서 10건만
```

* **Stream은 자동으로 지워지지 않는다.** 계속 쌓이면 메모리를 잠식하므로 길이 제한이 필수다.

```shell
XADD orders MAXLEN ~ 10000 * userId 100    # 대략 1만 건 유지
XTRIM orders MAXLEN ~ 10000                # 나중에 따로 정리
XTRIM orders MINID ~ 1735200000000         # 특정 시각 이전 삭제
```

* `~`(근사) 옵션을 붙이면 정확히 N개로 맞추는 대신 **노드 단위로 잘라내 훨씬 빠르다.** 정확한 개수가 중요하지 않다면 항상 `~`를 쓰는 편이 좋다.

#### Redis Streams - 두 가지 읽기 방식

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/28875920-fc32-4173-a4ec-fe7ed6d6cf95.png)

|방식|명령|특징|
|:---|:---|:---|
|**독립 소비**|`XREAD`|여러 소비자가 **같은 메시지를 각자 전부** 받는다. 위치를 스스로 관리해야 하고 PEL·ACK가 없다. 브로드캐스트 용도|
|**그룹 소비**|`XREADGROUP`|하나의 메시지를 **그룹 내 소비자 중 한 명만** 받는다. 작업 분배 용도이며 PEL·ACK·재처리를 지원한다|

```shell
# 컨슈머 그룹 생성 ($ = 지금 이후 새 메시지부터, 0 = 처음부터)
XGROUP CREATE orders order-workers $ MKSTREAM
#                                     └ 스트림이 없으면 함께 생성

# 그룹 소비 (BLOCK 0 = 새 메시지가 올 때까지 대기)
XREADGROUP GROUP order-workers worker-1 COUNT 10 BLOCK 0 STREAMS orders >
#                                                                       └ 특수 ID
```

* **`>` 는 "아직 아무에게도 전달되지 않은 새 메시지"** 를 뜻한다. 여기에 `0`이나 특정 ID를 주면 새 메시지가 아니라 **그 소비자의 PEL에 남아 있는(이미 받았지만 ACK 안 한) 메시지**를 다시 가져온다. 재시작 후 밀린 작업을 이어받을 때 쓰는 방식이다.

#### Redis Streams - 메시지 생명주기와 PEL

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a11ba738-0c31-43d3-9ca2-e4ecfab04d4b.png)

**1. 메시지 발행 및 전달(Production)**

* Producer → Stream(XADD) : 생산자가 데이터를 스트림에 넣는다.
* Consumer → Stream(XREADGROUP) : 소비자가 Consumer Group에 온 새로운 메시지를 소비한다.
* 메시지 전달 & PEL 기록 : 메시지를 전달함과 동시에 PEL(Pending Entries List)이라는 대기 목록에 해당 메시지 ID를 기록한다. 이 PEL의 역할은 해당 메시지가 어떤 소비자에게 전달되었고 아직 다 읽었다는 확인을 못 받았다는 상태를 저장한다.
* PEL에는 메시지 ID뿐 아니라 **소유한 소비자 이름 · 마지막 전달 시각 · 전달 횟수**가 함께 기록된다. 뒤의 재처리와 데드레터 판단이 모두 이 정보를 근거로 이루어진다.

**2. 메시지 처리(Processing)**

* 소비자가 전달받은 메시지를 가지고 실제 비즈니스 로직을 수행한다.
* 처리 성공 케이스 로직
  * Consumer → Stream(XACK) : 소비자가 처리를 완벽히 끝내고 메시지를 확인했다고 알린다.
  * PEL에서 제거 : 레디스는 그제야 PEL에서 해당 메시지 ID를 지운다. 이로써 이 메시지의 생명주기는 완벽히 종료된다.
  * **ACK는 메시지를 지우지 않는다.** PEL에서만 빠질 뿐 Stream에는 그대로 남아 있으므로, 별도로 `XTRIM`을 해줘야 메모리가 회수된다.
* 처리 실패 또는 타임아웃 케이스 로직
  * 메시지가 PEL에 남음 : 시간이 지나도 XACK가 오지 않으면 해당 메시지는 계속 PEL에 머물러 있다.
  * 다른 Consumer → Stream(XCLAIM) : 다른 소비자가 소유권을 이전해서 다시 처리한다고 선언한다.
  * 재처리 : 새로운 소비자가 메시지를 다시 처리하고 최종적으로 XACK을 보내 생명주기를 끝낸다.
* **재분배는 자동이 아니다.** Redis가 알아서 다른 소비자에게 넘겨주지 않으므로, **애플리케이션이 주기적으로 PEL을 확인하고 `XCLAIM`/`XAUTOCLAIM`을 호출하는 로직을 직접 만들어야 한다.**

#### Redis Streams - 밀린 메시지 조회와 회수

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/57ee086e-0a97-45ef-819f-28d4ea31610a.png)

```shell
# PEL 요약 : 대기 건수, 최소/최대 ID, 소비자별 건수
XPENDING orders order-workers

# 상세 조회 : 각 메시지의 소유자 · 마지막 전달 후 경과 시간 · 전달 횟수
XPENDING orders order-workers - + 10
# 1) 1) "1735200000000-0"
#    2) "worker-1"
#    3) (integer) 60000        ← 60초째 ACK 없음
#    4) (integer) 3            ← 3번 전달됨

# 60초 이상 방치된 메시지를 worker-2가 가져오기 (Redis 6.2+)
XAUTOCLAIM orders order-workers worker-2 60000 0 COUNT 10
```

* `XCLAIM`은 회수할 메시지 ID를 일일이 지정해야 하지만, **`XAUTOCLAIM`(6.2+)은 조건에 맞는 것을 알아서 스캔해 넘겨준다.** 실무에서는 이쪽이 훨씬 편하다.
* **데드레터(Dead Letter) 처리** : `XPENDING`의 **전달 횟수(delivery count)** 를 보고 임계값(예: 5회)을 넘으면 재처리를 포기해야 한다. 그렇지 않으면 처리 불가능한 메시지가 영원히 재분배를 반복한다.
  * Redis에는 데드레터 큐가 내장되어 있지 않으므로, 별도 Stream(`orders:dlq`)에 옮겨 넣고 원본은 `XACK`으로 정리하는 식으로 직접 구현한다.

#### Redis Streams - Pub/Sub과의 비교

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/550d324b-c637-4c51-9184-745154246e86.png)

|구분|Pub/Sub|Streams|
|:---|:---|:---|
|메시지 저장|❌ 전달 즉시 소멸|✅ 스트림에 남아 나중에 조회 가능|
|오프라인 구독자|메시지 유실|재접속 후 이어서 처리 가능|
|전달 보장|At-Most-Once|**At-Least-Once**(ACK·재처리)|
|여러 소비자|모두 같은 메시지를 받음|`XREAD` 모두 / `XREADGROUP` 한 명만(작업 분배)|
|이력 조회|❌|✅ `XRANGE`로 과거 조회|
|메모리|누적되지 않음|**계속 쌓이므로 트리밍 필수**|

* 즉 **"놓쳐도 되는 실시간 알림"이면 Pub/Sub, "반드시 처리되어야 하는 작업"이면 Streams**다.

#### Redis Streams - Kafka와의 비교

* 개념은 닮았다 — append-only 로그, 컨슈머 그룹, 오프셋 기반 소비.
* 다른 점
  * **파티션이 없다.** Kafka는 파티션 단위로 병렬 처리하지만 Streams는 하나의 스트림에 소비자들이 붙는 구조다. 순서는 스트림 전체에서 보장된다.
  * **보관 기간이 아니라 길이로 관리**한다. Kafka는 디스크에 장기 보관하지만 Streams는 메모리에 있으므로 `MAXLEN`으로 잘라내야 한다.
  * **처리량과 내구성**에서 Kafka가 앞선다. 대용량 이벤트 파이프라인이나 장기 보관이 필요하면 Kafka가 맞다.
* 판단 기준 : **이미 Redis를 쓰고 있고 중간 규모의 작업 큐가 필요하다면 Streams로 충분하다.** 별도 브로커를 운영할 필요가 없다는 것이 가장 큰 장점이다.

#### Redis Streams - 운영 시 주의사항

* **트리밍을 잊지 말 것** : ACK를 해도 Stream은 줄지 않는다. `XADD ... MAXLEN ~ N` 또는 주기적 `XTRIM`이 없으면 메모리가 계속 증가한다.
* **소비자 이름은 안정적으로** : `XREADGROUP`의 소비자 이름이 재시작마다 바뀌면 이전 이름의 PEL이 고아로 남는다. 파드명 등 고정된 값을 쓰고, 사라진 소비자는 `XGROUP DELCONSUMER`로 정리한다.
* **재시작 후 PEL 확인** : 재시작한 소비자는 `>` 대신 `0`으로 한 번 읽어 **자신의 밀린 메시지를 먼저 처리**한 뒤 새 메시지로 넘어가는 것이 안전하다.
* **At-Least-Once이므로 멱등성 필요** : 재처리 시 같은 메시지를 두 번 받을 수 있다. 소비자는 같은 메시지를 여러 번 처리해도 결과가 같도록 설계해야 한다.
* **복제 지연 주의** : Master에 `XADD` 된 직후 장애가 나면 Replica로 전파되기 전의 메시지는 유실될 수 있다. Streams도 Redis의 지속성 한계를 그대로 갖는다.

```java
// Spring : 컨슈머 그룹으로 소비
StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
    StreamMessageListenerContainer.create(connectionFactory);

container.receive(
    Consumer.from("order-workers", "worker-1"),
    StreamOffset.create("orders", ReadOffset.lastConsumed()),   // '>' 에 해당
    message -> {
        try {
            process(message.getValue());
            redisTemplate.opsForStream().acknowledge("order-workers", message);
        } catch (Exception e) {
            // ACK 하지 않으면 PEL에 남아 나중에 XAUTOCLAIM으로 회수된다
            log.error("처리 실패, 재처리 대상: {}", message.getId(), e);
        }
    });

container.start();
```

* `receive()`(수동 ACK) 대신 `receiveAutoAck()`를 쓰면 전달 즉시 ACK되어 **실패해도 재처리가 불가능**하다. 재처리가 필요하다면 반드시 수동 ACK를 써야 한다.
