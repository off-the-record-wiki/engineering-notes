# Apache Kafka - Multi Node Kafka Cluster

#### 카프카 Replication과 Leader/Follower

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/136b1d33-da62-464d-8c97-530aa913cc53.png)

* Producer와 Consumer는 Leader 파티션을 통해서 쓰기와 읽기를 수행한다.
* 파티션의 Replication은 Leader에서 Follower로만 이루어지고 그 반대는 이루어지지 않는다.
* 파티션 리더를 관리하는 브로커는 Producer/Consumer의 읽기/쓰기를 관리함과 동시에 파티션 팔로우를 관리하는 브로커의 Replication도 관리한다.

#### 단일 파티션의 Replication 과정

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b297e313-4c3a-4cc7-9def-7b0803a23559.png)

* 파티션의 Replication은 Leader와 Follower로만 이루어진다.
* 파티션 리더를 관리하는 브로커는 Producer/Consumer의 읽기/쓰기를 관리함과 동시에 파티션 팔로우를 관리하는 브로커의 Replication도 관리한다.

#### 멀티 파티션의 Replication 과정

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3efb406f-8401-46cd-9942-7171c699f17f.png)

* 파티션의 Replication은 Leader와 Follower로만 이루어진다.
* 파티션 리더를 관리하는 브로커는 Producer/Consumer의 읽기/쓰기를 관리함과 동시에 파티션 팔로우를 관리하는 브로커의 Replication도 관리한다.

#### Kafka Replication

* 카프카는 개별 노드의 장애를 대비해 높은 가용성을 제공한다.
* 카프카 가용성의 핵심은 Replication(복제)에 있다.
* Replication은 토픽 생성 시 Replication factor 설정값을 통해 구성할 수 있다.
* Replication factor가 3이라면 원본 파티션과 복제 파티션을 포함해 모두 3개의 파티션을 가짐을 의미한다.
* Replication factor의 개수는 브로커의 개수보다 클 수 없다.
* Replication의 동작은 토픽 내의 개별 파티션들을 대상으로 적용된다.
* Replication factor의 대상인 파티션들은 1개의 Leader와 N개의 Follower들로 구성된다.

#### 멀티 브로커 환경에서 Producer의 bootstrap.servers 설정 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4d765c7e-0ef5-4d8f-8a71-3c9a8620364a.png)

* Producer는 `bootstrap.servers`에 기술되어 있는 브로커들의 List를 기반으로 접속한다.
* `bootstrap.servers`는 브로커들의 Listener들의 List이다.
* 개별 Broker들은 토픽 파티션의 Leader와 Follower들의 메타 정보를 서로 공유하며, Producer는 초기 접속 시 이 메타 정보를 가져와서 접속하려는 토픽의 파티션이 있는 브로커로 다시 접속한다.

#### Zookeeper

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/39d963f3-7a36-4653-938d-e20f1bafea2b.png)

* 분산 시스템 간의 정보를 신속하게 공유하기 위한 코디네이션 시스템을 말한다.
* 클러스터 내의 개별 노드의 중요한 상태 정보를 관리하며 분산 시스템에서 리더 노드를 선출하는 역할 등을 수행한다.
* 개별 노드 간의 상태 정보 동기화를 위한 복잡한 Lock 관리 기능을 제공한다.
* 간편한 디렉터리 구조 기반의 Z Node를 활용하며, Z Node는 개별 노드의 중요 정보를 담고 있다.
* 개별 노드들은 Zookeeper의 Z Node를 계속 모니터링하며, Z Node에 변경 발생 시 Watch Event가 트리거되어 변경 정보가 개별 노드들에게 통보된다.
* Zookeeper 자체의 클러스터링 기능을 제공한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/875f88f6-e34c-4514-83d9-d2cc48b130bb.png)

* 모든 카프카 브로커는 주기적으로 Zookeeper에 접속하면서 Session Heartbeat을 전송하여 자신의 상태를 보고한다.
* Zookeeper는 `zookeeper.session.timeout.ms` 내에 Heartbeat을 받지 못하면 해당 브로커의 노드 정보를 삭제하고 Controller 노드에게 변경 사실을 통보해야 한다.
* Controller의 노드는 다운된 브로커가 관리하는 파티션들에 대해서 새로운 파티션 Leader Election을 수행한다. 
* 만일 다운된 브로커가 Controller라면 모든 노드에게 해당 사실을 통보하고 가장 먼저 접속한 다른 브로커가 Controller가 된다.

#### Controller Broker

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ce2789e7-af54-4826-824f-499c242e9b8a.png)

* Zookeeper에 가장 처음 접속을 요청한 Broker가 Controller가 된다.
* Controller는 파티션에 대한 Leader Election을 수행한다.
* Controller는 Zookeeper로부터 Broker 추가/다운 등의 정보를 받으면 해당 Broker로 인해 영향을 받는 파티션들에 대해서 새로운 Leader Election을 수행한다.

#### Controller의 Leader Election(리더 선출) 프로세스

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ce64ce60-3ed6-4a9d-a973-e8948f2121dd.png)

* Broker #3이 다운되고 Zookeeper는 session 기간 동안 Heartbeat가 오지 않으므로 해당 브로커 노드의 정보를 갱신한다.
* Controller는 Zookeeper를 모니터링하던 도증 Watch Event로 Broker #3이 다운되었다는 정보를 받는다.
* Controller는 다운된 브로커가 관리하던 파티션들에 대해 새로운 Leader/Follower를 결정한다.
* 결정된 새로운 Leader/Follower 정보를 Zookeeper에 저장하고 해당 파티션을 복제하는 모든 브로커들에 새로운 Leader/Follower 정보를 전달하고 새로운 Leader로부터 복제 수행할 것을 요청한다.
* Controller는 모든 브로커가 MetaData Cache를 새로운 Leader/Follower 정보로 갱신할 것을 요청한다.

#### ISR(In-Sync-Replicas)의 이해

* Follower들은 누구라도 Leader가 될 수 있지만 단, ISR 내에 있는 Follower들만 가능하다.
* 파티션의 Leader 브로커는 Follower 파티션의 브로커들이 Leader가 될 수 있는지 지속적으로 모니터링을 수행하여 ISR을 관리한다.
* Leader 파티션의 메시지를 Follower가 빠르게 복제하지 못하고 뒤쳐질 경우 ISR에서 해당 Follower는 제거되며 Leader가 문제가 생겨 차기 Leader의 후보 자격도 상실된다.

#### ISR의 조건

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9a790833-bf4f-48c2-854f-60221209c8ee.png)

* 브로커가 Zookeeper에 연결되어있어야 한다.
* `zookeeper.session.timeout.ms`로 지정된 기간 내에 Heartbeat을 지속적으로 Zookeeper로 보낸다.
* `replica.time.max.ms`로 지정된 기간 내에 Leader의 메시지를 지속적으로 가져가야 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/8611a14d-cd3e-47b2-a399-e807aa7af640.png)

* Follower는 Leader에게 fetch 요청을 수행한다. Fetch 요청에는 Follower가 다음에 읽을 메시지의 offset 번호를 포함한다.
* Leader는 Follower가 요청한 offset 번호와 현재 Leader Partition의 가장 최신 offset 번호를 비교해 Follower가 얼마나 Leader의 데이터 복제를 잘 수행하고 있는지를 판단한다.

#### min.insync.replicas 설정

* `min.insync.replicas`는 브로커의 설정값으로 Producer가 `acks=all`로 성공적으로 메시지를 보낼 수 있는 최소한의 ISR 브로커 개수를 의미한다.
* 이해하기 쉽게 한다면 `데이터를 최소한 몇 군데에 성공적으로 저장해야 '안전하게 저장됨'으로 인정할 것인가?`로 생각하면 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/de73eac9-953b-4c2d-a626-a24e55aa80fc.png)

* 프로듀서(Producer)가 `acks=all`로 설정하고 메시지를 보낼 때 해당 옵션이 작동한다.
* Leader Partition(리더): 모든 읽기와 쓰기 작업을 담당하는 원본 파티션이다.(무조건 1개)
* Follower Partition(팔로워): 리더의 데이터를 그대로 복제하여 들고 있는 파티션이다.
* Replication Factor(RF): 리더와 팔로워를 모두 합친 전체 개수이다.
* 만약 `min.insync.replicas=2`로 했는데 브로커가 2개가 다운되었다면 `min.insync.replicas=2`이 충족이 안되기 때문에 예외가 발생하는 것이다.
