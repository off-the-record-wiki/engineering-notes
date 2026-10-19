# Apache Kafka - Kafka CLI

#### kafka.topics.sh

```shell
$ 01ea2e6ab911:/opt/kafka# bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic hello.kafka --partitions 1 --replication-factor 1
WARNING: Due to limitations in metric names, topics with a period ('.') or underscore ('_') could collide. To avoid issues it is best to use either, but not both.
Created topic hello.kafka.
$ 01ea2e6ab911:/opt/kafka# bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic hello.kafka --describe
Topic: hello.kafka	TopicId: 8sYA9kkHSjSTJet4yQhNoQ	PartitionCount: 1	ReplicationFactor: 1	Configs: segment.bytes=1073741824
	Topic: hello.kafka	Partition: 0	Leader: 1	Replicas: 1	Isr: 1	Elr: 	LastKnownElr: 
```

* 만들어진 토픽은 파티션 개수, 복제 개수 등과 같이 다양한 옵션이 포함되어 있지만 모두 브로커에 설정된 기본값으로 생성된다.
* 파티션 개수, 복제 개수, 토픽 데이터 유지 기간 옵션들을 지정하여 토픽을 생성하고 싶다면 다음과 같이 명령을 실행하면 된다.

```shell
$ 01ea2e6ab911:/opt/kafka# bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic hello.kafka --partitions 10 --replication-factor 1 --config retention.ms=172800000

$ 01ea2e6ab911:/opt/kafka# bin/kafka-topics.sh --bootstrap-server localhost:9092 --list # 생성된 토픽들의 이름을 조회
```

* 파티션 개수를 늘리기 위해서 --alter 옵션을 사용하면 된다.

```shell
$ 01ea2e6ab911:/opt/kafka# bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --alter --partitions 4
```

* 파티션 개수를 늘릴 순 있으나 줄일 순 없다.
* 다시 줄이는 명령을 내리게 되면 `InvalidPartitionsException` 예외가 발생한다.
* 분산 시스템에서 이미 분산된 데이터를 줄이는 방법은 매우 복잡하다. 삭제 대상 파티션을 지정해야할 뿐만 아니라 기존에 저장된 레코드를 분산해 저장하는 로직도 필요하기 때문이다.
* 이 때문에 카프카에서는 파티션을 줄이는 로직은 제공하지 않는다.
* 만약 피치못할 사정으로 파티션 개수를 줄여야 한다면 토픽을 제거하고 재생성하는 것이 좋다.

#### kafka.configs.sh

* 토픽의 일부 옵션을 설정하기 위해서 `kafka-configs.sh` 명령어를 사용해야 한다.
* `--alter`과 `--add-config` 옵션을 사용하여 `min.insync.replicas` 옵션을 토픽별로 설정할 수 있다.
* 브로커에 설정된 각종 기본값은 `--broker`, `--all`, `--describe` 옵션을 사용해 조회할 수 있다.

#### kafka-console-producer.sh

* 메시지 키와 메시지 값을 함께 전송한 레코드는 토픽의 파티션에 저장된다.
* 그러나 메시지 키가 `null`인 경우에는 프로듀서가 파티션으로 전송할 때 레코드 배치 단위로 라운드 로빈으로 전송한다.
* 메시지 키가 존재하는 경우에는 키의 해시값을 작성해 존재하는 파티션 중 한 개에 할당된다. 이로 인해 메시지 키가 동일한 경우에는 동일한 파티션으로 전송된다.

#### kafka-console-consumer.sh

* 토픽으로 전송한 데이터는 `kakfa-consolg-consumer.sh` 명령으로 확인할 수 있다.
* 이 때, 필수 옵션으로 `--bootstrap-server`에 카프카 클러스터 정보, `--topic`에 토픽 이름이 필요하다.
* 추가로 `--from-beginning` 옵션을 주면 토픽에 저장된 가장 처음 데이터부터 출력된다.
* 만약 레코드 메시지 키와 메시지 값을 확인하고 싶다면 `--property` 옵션을 사용하면 된다.
* `--max-messages` 옵션을 사용하면 최대 컨슘 메시지 개수를 설정할 수 있다.
* `--partition` 옵션을 사용하면 특정 파티션만 컨슘할 수 있다.
* `--group` 옵션을 사용하면 컨슈머 그룹을 기반으로 kafka-console-consumer가 동작한다. 컨슈머 그룹이란 특정 목적을 가진 컨슈머들을 묶음으로 사용하는 것을 말한다. 컨슈머 그룹으로 토픽의 레코드를 가져갈 경우 어느 레코드까지 읽었는지에 대한 데이터가 카프카 브로커에 저장된다.

#### kafka-console-groups.sh

* 컨슈머 그룹은 따로 생성하는 명령을 날리지 않고 컨슈머를 동작할 때 컨슈머 그룹 이름을 지정하면 새로 생성된다.
* 생성된 컨슈머 그룹 리스트는 kafka-consumer-groups.sh 명령으로 확인할 수 있다.
* `--describe` 옵션을 사용하면 해당 컨슈머 그룹이 어떤 토픽을 대상으로 레코드를 가져갔는지 상태를 확인할 수 있다. 파티션 번호, 현재까지 가져간 레코드의 오프셋, 파티션 마지막 레코드의 오프셋, 컨슈머 렉, 컨슈머 ID, 호스트를 알 수 있기 때문에 컨슈머 상태 조회 시 유용하다.

#### kafka-producer-perf-test.sh

* `kafka-producer-perf-test.sh`는 카프카 프로듀서로 퍼포먼스를 측정할 때 사용한다.
* 카프카 브로커와 컨슈머 간 네트워크를 체크할 때 사용할 수 있다.

#### kafka-reassign-partitions.sh

* `kafka-reassign-partitions.sh`를 사용하면 리더와 팔로워 위치를 변경할 수 있다.
* 카프카 브로커에는 `auto.leader.rebalance.enable` 옵션이 있는데 이 옵션 기본값은 `true`로 클러스터 단위에서 리더 파티션을 자동으로 리밸런싱하도록 도와준다.
* 브로커의 백그라운드 쓰레드가 일정한 간격으로 리더 위치를 파악해 필요시 리더 파티션의 핫스왑(몰리는 경우)을 방지하고자 리밸런싱을 통해 리더 위치가 알맞게 배분된다.
