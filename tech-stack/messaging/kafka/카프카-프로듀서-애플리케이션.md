# Apache Kafka - Kafka Producer Application

#### 파티셔너

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7e8cfa46-de6d-4fa0-a73f-21da0026db16.png)

* 키가 있는 경우
  * 파티션이 명시되지 않았지만 키가 있을 경우 키의 해시를 기반으로 파티션을 선택한다.
  * `murmur2` 알고리즘으로 `hash(keyBytes) % numPartitions`를 계산허며, 이 방식은 동일한 키를 가진 모든 메시지가 항상 같은 파티션으로 라우팅되는 것을 보장한다.
  * 이 해시-파티션 매핑은 결정론적이지만 토픽에 파티션 수를 추가하면 무효화되어 같은 키의 메시지가 다른 파티션으로 라우팅될 수도 있어 순서 보장이 깨진다.
* 키가 없는 경우
  * 파티션도 키도 없는 경우 `batch.size` 바이트 이상이 해당 파티션에 프로듀스될 때 변경되는 sticky partition을 선택한다.
  * `Partitioner` 객체가 아닌 `RecordAccumulator` 내부로 이동했다.
  * 기존 `UniformStickyPartitioner`의 문제는 "스티키 시간"이 새 배치 생성 기준으로 결정되었기 때문에 발생했다. 느린 브로커일수록 배치를 더 느리게 소비하여 더 많은 "스티키 시간"을 점유하고, 결과적으로 느린 브로커에 더 많은 레코드가 쌓이는 런어웨이 현상이 생겼다.
  * 새 기본 동작은 배치 생성 시점이 아닌 파티션에 프로듀스된 바이트 수(batch.size) 기준으로 파티션을 전환한다. 이렇게 하면 배칭이나 기타 상황과 무관하게 균등한 분배가 가능하다.
  * 추가로 `partitioner.adaptive.partitioning.enable`(기본값 true)이 활성화되면 프로듀서가 브로커 성능에 적응하여 더 빠른 브로커가 리더인 파티션에 더 많은 메시지를 보내려 시도한다. `partitioner.availability.timeout.ms`가 0보다 크면, 해당 시간 동안 프로듀스 요청을 수락하지 않는 파티션은 비가용 상태로 마킹된다.

> * [Changed Default Configuration](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=191336857#KIP794:StrictlyUniformStickyPartitioner-ChangedDefaultConfiguration)
> * [New Configuration](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=191336857#KIP794:StrictlyUniformStickyPartitioner-NewConfiguration)
> * [Uniform Sticky Batch Size](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=191336857#KIP794:StrictlyUniformStickyPartitioner-UniformStickyBatchSize)
> * [Adaptive Partition Switching](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=191336857#KIP794:StrictlyUniformStickyPartitioner-AdaptivePartitionSwitching)
> * [KIP-794: Strictly Uniform Sticky Partitioner](https://cwiki.apache.org/confluence/display/KAFKA/KIP-794%3A+Strictly+Uniform+Sticky+Partitioner)

#### 프로듀서 주요 옵션(필수)

* **`bootstrap.servers`**
  * 프로듀서가 데이터를 전송할 대상 카프카 클러스터에 속한 브로커의 호스트 이름:포트를 1개 이상 작성한다.
  * 2개 이상 브로커 정보를 입력해 일부 브로커에서 이슈가 발생하더라도 접속하는 데 이슈가 없도록 설정 가능하다.
* **`key.serializer`**
  * 레코드의 메시지 키를 직렬화하는 클래스를 지정한다.
* **`value.serializer`**
  * 레코드의 메시지 값을 직렬화하는 클래스를 지정한다.

#### 프로듀서 주요 옵션(선택)

* **`acks`**
  * 프로듀서가 전송한 데이터가 브로커들에게 정상적으로 저장되었는지 전송 성공 여부를 확인하는데 사용하는 옵션으로 0, 1, -1(all) 중 하나로 설정할 수 있다. 기본 값은 1이다.
* **`linger.ms`**
  * 배치를 전송하기 전까지 기다리는 최소 시간이다. 기본 값은 0이다.
* **`max.in.flight.requests.per.connection`**
  * 한 번에 요청하는 최대 커넥션 개수. 설정된 값만큼 동시에 전달 요청을 수행한다. 기본 값은 5이다.
* **`retries`**
  * 브로커로부터 에러를 받고 난 뒤 재전송을 시도하는 횟수를 지정한다. 기본 값은 2147483647이다.
* **`partitioner.class`**
  * 레코드를 파티션에 전송할 때 적용하는 파티셔너 클래스를 지정한다. 기본 값은 `org.apache.kafka.clients.producer.internals.DefaultPartitioner`이다.
* **`enable.idempotence`**
  * 멱등성 프로듀서로 동작할지 여부를 지정한다. 기본 값은 false이다.
* **`transactional.id`**
  * 프로듀서가 레코드를 전송할 때 레코드를 트랜잭션 단위로 묶을지 여부를 설정한다. 기본 값은 null이다.

#### ISR와 acks 옵션

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3cb4eba6-0b33-4bc9-90fe-5e36624538c7.png)

* ISR은 리더 파티션의 상태와 팔로워 파티션의 상태가 모두 싱크가 된 상태를 말한다.
* 복제 개수가 2인 토픽을 가정하면 위와 같이 리더 파티션 1개, 팔로워 파티션 1개가 존재한다.
* 리더 파티션에 0부터 3까지의 오프셋이 있다고 가정할 때, 팔로워 파티션에 데이터 동기화가 완료되려면 0부터 3까지의 오프셋이 존재해야 한다.
* 동기화가 완료되었다는 의미는 리더 파티션의 모든 데이터가 팔로워 파티션에 복제된 상태를 말하기 때문이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/443c5da3-0794-4112-8bfe-60af99e34440.png)

* ISR이라는 용어가 나온 이유는 팔로워 파티션이 리더 파티션으로부터 데이터를 복제하는 데 걸리는 시간이 있기 때문이다.
* 프로듀서가 특정 파티션에 데이터를 저장하는 작업은 리더 파티션을 통해 처리한다.
* 이 때, 리더 파티션에 새로운 레코드가 추가되어 오프셋이 증가하면 팔로워 파티션이 위치한 브로커는 리더 파티션의 데이터를 복제한다.
* 리더 파티션에 데이터가 적재된 이후 팔로워 파티션이 복제하는 시간 차 때문에 리더와 팔로워 간 오프셋 차이가 발생한다.

#### acks

* 카프카 프로듀서의 acks 옵션은 0, 1, all(-1) 값을 가질 수 있다.
* **이 옵션을 통해 프로듀서가 전송한 데이터가 카프카 클러스터에 얼마나 신뢰성높게 저장할지 지정할 수 있다.**
* acks 옵션에 따라 성능이 달라질 수 있으므로 acks 옵션에 따라 카프카의 동작 방식을 상세히 알고 설정해야 한다.
* 복제 개수가 1 즉, 리더 파티션 1개만 있는 경우 acks 옵션에 따른 성능 변화는 크지 않다.
* 그러나 안정적으로 데이터를 운영하기 위해서는 복제 개수가 2이상으로 운영하는 경우가 대부분이다.

#### acks=0

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/649821d1-3950-476f-a3d8-2be9f38fdaab.png)

* acks를 0으로 설정하는 것은 프로듀서가 리더 파티션으로 데이터를 전송했을 때 리더 파티션으로 데이터가 저장되었는지 확인하지 않는다는 뜻이다.
* 리더 파티션은 데이터가 저장된 이후에 데이터가 몇 번째 오프셋에 저장되었는지를 리턴하는데 acks가 0으로 설정되어 있다면 프로듀서는 리더 파티션에 데이터가 저장되었는지 여부에 대한 응답 값을 받지 않는다.
* 데이터 전송 속도는 acks를 1 또는 all로 했을 경우보다 훨씬 빠르다. 데이터가 일부 유실이 발생하더라도 전송 속도가 중요한 경우라면 이 옵션값을 사용하는 것이 좋다.

#### acks=1

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1f3b5783-1f1d-4a72-b25a-7b56fdb7dc55.png)

* acks를 1로 설정한 경우 프로듀서는 보낸 데이터가 리더 파티션에만 정상적으로 적재되었는가를 확인한다.
* 만약 리더 파티션에 데이터가 정상적으로 적재되지 않았다면 리더 파티션에 적재될 때까지 재시도할 수 있다.
* 그러나 리더 파티션에 적재되었음을 보장하더라도 데이터는 유실될 수 있다. 왜냐하면 복제 개수를 2이상으로 운영할 경우 리더 파티션에 적재가 완료되더라도 팔로워 파티션에는 아직 동기화되지 않을 수 있기 때문이다.
* 팔로워 파티션이 데이터를 복제하기 직전에 리더 파티션이 있는 브로커에 장애가 발생하면 동기화되지 못한 데이터가 유실될 수 있다.

#### acks=-1(all)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9ee402ca-55cb-4c02-97c2-cf153e5e6c4d.png)

* acks를 -1 혹은 all로 설정할 경우 프로듀서는 보낸 데이터가 리더 파티션과 팔로워 파티션에 모두 적재되었는지를 확인한다.
* 리더 파티션뿐만 아니라 팔로워 파티션까지 데이터가 적재되었는지를 확인하기 때문에 0 또는 1 옵션에 비하면 상대적으로 속도가 느리다.
* 그럼에도 불구하고 팔로우 파티션에 데이터가 정상 적재되었는지 기다리기 때문에 일부 브로커에서 장애가 발생하더라도 프로듀서는 안전하게 데이터를 전송하고 저장할 수 있음을 보장할 수 있다.
* acks를 all로 설정할 경우에는 토픽 단위로 설정 가능한 `min.insync.replicas` 옵션값에 따라 데이터 안정성이 달라진다.

#### min.insync.replicas

* `min.insync.replicas` 옵션은 프로듀서가 리더 파티션과 팔로워 파티션에 데이터가 적재되었는가를 확인하기 위한 최소 ISR 그룹의 파티션 개수이다.
* 만약 이 값이 1이라면 ISR 중 최소 1개 이상의 파티션에 데이터가 적재되었음을 확인하는 것이다.
* 값이 1인 상태의 경우 acks를 1로 설정했을 때와 동일한 동작을 수행한다. 왜냐하면 값이 1이라면 팔로워 파티션이 없이 리더 파티션만 존재하기 때문에 ISR 중 처음 적재가 완료되는 파티션은 리더 파티션이기 때문이다.

#### min.insync.replicas=2, acks=-1(all)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/61a941f6-e04f-46d8-9f45-4da3fa08a809.png)

* `min.insync.replicas=2`로 설정했을 때부터 acks를 all로 설정하는 의미가 있다.
* 이 경우 ISR의 2개 이상의 파티션에 데이터가 적재되었음을 확인했다는 뜻이기 때문이다.
* ISR의 2개 이상의 파티션에 데이터가 적재되었다는 의미는 리더 파티션 1개와 팔로워 파티션 1개에 정상 적재되었음을 보장하기 떄문이다.
* 실제 카프카 클러스터를 운영하면서 브로커가 동시에 2개가 중단되는 일은 극히 드물기 때문에 리더 파티션과 팔로워 파티션 중 1개에 데이터가 적재 완료되었다면 데이터는 유실되지 않는다고 볼 수 있다.
