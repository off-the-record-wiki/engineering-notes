# Apache Kafka - Kafka Topic/Producer/Consumer

#### Topic과 Partition 그리고 카프카 병렬 분산 처리 개요

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cf52a955-d0fc-40e6-99c2-1fae3bfc8e70.png)

* Topic은 Partition으로 구성된 일련의 로그 파일이다.
* Topic은 Key-Value기반의 메시지 구조이며, Value로 어떤 타입의 메시지도 가능하다.(문자열, 숫자값, 객체, Json, Avro, Protobuf 등)
* 로그 파일과 같이 연속적으로 추가되어 발생하는 데이터를 저장하는 구조를 가진다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/384c58fc-e3ef-4c7e-8167-f10daec781bf.png)

* Topic은 1개 이상의 파티션을 가질 수 있다.
* Topic의 Partition은 Kafka의 병렬 성능과 가용성 기능의 핵심 요소이며, 메시지는 병렬 성능과 가용성을 고려한 개별 파티션에 분산 저장된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f2c885fe-19cc-4962-b5d8-e58c175fe099.png)

* 개별 파티션은 정렬되고, 변경할 수 없는 일련의 레코드로 구성된 로그 메시지이다.
* 개별 레코드는 `offset`이라고 불리는 일련 번호를 할당받는다.
* 개별 파티션은 다른 파티션과 완전 독립적이다.
* 개별 파티션 내에서 정렬되고 `offset`이 할당된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/05919d5d-5bf2-4963-bcc5-08b82f62379e.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d2e04970-db61-4315-bb51-fba044a9de8b.png)

* 메시지는 병렬 성능과 가용성을 고려한 방식으로 토픽 내의 개별 파티션들에 분산 저장되는 형식이다.
* 또한 토픽의 파티션들은 단일 카프카 브로커뿐만 아니라 여러 개의 카프카 브로커들에 분산 저장된다.

#### Producer와 Consumer 개요

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/990d864f-277a-4325-bea3-dd3a2a0b2d11.png)

* Producer는 Topic에 메시지를 보낸다.
* Producer는 성능/로드밸런싱/가용성/업무 정합성 등을 고려해 어떤 브로커의 파티션으로 메시지를 보내야할지 전략적으로 결정된다.
* Consumer는 Topic에서 메시지를 읽어들인다.
* 여러 개의 Consumer들로 구성될 경우 어떤 브로커의 파티션에서 메시지를 읽을 것인지 전략적으로 결정한다.

#### Consumer의 auto.offset.reset

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cd513ad1-10b5-447b-9b90-55bf83ef1987.png)

* Consumer가 Topic에 처음 접속해 Message를 가져올 때 가장 초래된 처음 offset부터 가져올 것인지, 가장 최근인 마지막 offset부터 가져올 것인지를 설정하는 파라미터이다.

#### Producer 객체 직렬화 전송의 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5ee12cd9-a436-46c4-84fe-ed2095dfdb36.png)

* 객체를 객체 유형, 데이터 포맷, 적용 시스템에 상관없이 이동/저장/복원을 자유롭게 하기 위해 바이트 배열(바이트 스트림, ByteStream) 형태로 저장하는 것을 말한다.
* 객체는 `Serialization`과 `Deserialization`을 통해서 System to System 또는 서로 다른 저장 영역에 이동/저장/복원을 자유롭게 수행한다.
* `Serialization`을 통해서 객체가 바이트 스트림으로 변환되어 네트워크를 통해 손쉽게 데이터를 전송할 수 있으며 `Deserialization`을 통해서 바이트 스트림은 다시 원본 객체로 변환되어 자유로운 객체 데이터 이동을 가능케 한다.

#### Key값을 가지는 메시지 전송

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8e8f65eb-4fca-4b12-8248-2a64600cc249.png)

* 메시지 Key는 업무 로직이나 메시지 Producer/Consumer 분산 성능 영향을 고려하여 생성한다.
* 한 가지 주의할 점은 이 메시지 Key가 고유하게 식별하는 용도는 아니다.
* 특정 Key값을 가지는 메시지는 특정 파티션으로 고정되어 전송된다.
* 특정 Key값을 가지는 메시지는 단일 파티션 내에서 전송 순서가 보장되어 Consumer에서 읽혀진다.

#### Key가 없는 메시지 전송

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cb7f335c-8bf4-41b2-8dd4-1a6508d102ea.png)

* 메시지는 Producer를 통해 전송 시 Partitioner를 통해 토픽의 어떤 파티션으로 전송되어야할지 미리 결정된다.
* Key값을 가지지 않는 경우 Round-Robin, Sticky Partition 등의 파티션 전략 등이 선택되어 파티션별로 메시지가 전송될 수 있다.
* Topic이 여러 파티션을 가질 때 메시지 전송 순서가 보장되지 않은 채로 Consumer에서 읽혀질 수 있다.

#### 라운드 로빈 전략

* Kafka 2.4 버전 이전 기본 파티션 분배 전략

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d1ed61f1-becf-45a5-b24d-7a83a48786ff.png)

* 라운드 로빈 전략은 최대한 메시지를 파티션에 균일하게 분배하는 전략으로 메시지 배치를 순차적으로 다른 파티션으로 전송한다.
* 메시지가 배치 데이터를 빨리 채우지 못하면서 전송이 늦어지거나 배치를 다 채우지 못하고 전송되면서 전송 성능이 저하되는 문제가 발생한다.

#### 스티키 파티셔닝 전략

* Kafka 2.4 버전부터 기본 파티션 분배 전략

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/13b57782-de1a-4b77-b3d6-eb4c14d0fa9b.png)

* 라운드 로빈의 성능을 개선하고자 특정 파티션으로 전송되는 하나의 배치에 메시지를 빠르게 먼저 채워서 보내는 방식이다.
* 배치를 채우지 못하고 전송되거나 배치를 채우는데 너무 오래 걸리는 문제를 개선한 방식이다.

#### Consumer Group 내에 1개의 Consumer만 있을 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1cf839c8-ac89-4557-8ffa-fd02fd26e623.png)

#### Consumer 내에 2개의 Consumer가 있지만 토픽 파티션 개수보다 작을 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/399a09db-2120-4223-9f62-2760e0b93e31.png)

#### Consumer Group 내에 파티션 개수와 동일한 Consumer가 있는 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/001496c9-5c4a-4da1-ad14-89f6af530a80.png)

#### Consumer Group 내에 파티션 개수보다 많은 Consumer가 있는 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fcdb42b0-036a-4176-bb9b-84378b6ad7ea.png)

#### 하나의 토픽을 여러 Consumer Group에서 Subscribe하는 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b54a45bb-cefd-4d60-abe1-b69109f81cdf.png)

* 동일한 Consumer Group 내의 Consumer들은 작업량을 최대한 균등하게 분배한다.
* 서로 다른 Consumer Group의 Consumer들은 분리되어 독립적으로 동작한다.
