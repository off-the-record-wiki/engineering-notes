# Redis - Redis Cluster Mode

### Redis ‐ Redis Cluster Mode

#### Standalone

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9f10d2d2-9478-42c3-8c45-f16634f0b69f.png)

* 하나의 Redis로 서비스하는 모드이다.
* 매우 간단하고 설정이 쉬우나 고가용성(HA, High Availability)이 없고 단일 장애점(SPOF, Single Point of Failure)이 되는 문제가 있다. 이 인스턴스 하나가 죽으면 캐시/세션 전체가 사라진다.

#### Replica

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/385cdbda-89e6-48ff-9e53-bffc2a83fb52.png)

* Master Node는 쓰기 요청을 처리한다.
* Replica Node는 주로 읽기 요청을 처리한다.(기본 설정이 읽기 전용 `replica-read-only yes`)
* Replica Node가 Master Node에 처음 연결을 시도하면, 전체 데이터 동기화(Full Resynchronization)가 수행된다.
  * Master Node는 현재 메모리 데이터를 기반으로 RDB 스냅샷을 생성한다.
  * 이 과정에서 발생하는 쓰기 요청은 별도의 replication buffer에 저장된다.
  * 생성된 RDB 파일을 네트워크를 통해 Replica Node로 전송한다.
  * Replica Node는 해당 RDB 파일을 받아 메모리에 로드한다.
  * 이후 Master Node는 replication buffer에 저장된 쓰기 명령들을 Replica Node에 전달하고, Replica Node는 이를 순차적으로 적용하여 데이터 동기화를 완료한다.
* 동기화 이후에는 Master Node의 모든 쓰기 명령이 Replica Node로 실시간 전파된다.
* 부분 동기화(Partial Resynchronization) : 네트워크가 잠깐 끊겼다 붙는 경우에는 매번 전체 동기화를 하는 것이 아니라, Master가 유지하는 replication backlog에서 **끊긴 지점 이후의 명령만** 이어받는다(PSYNC). backlog 범위를 벗어날 만큼 오래 끊겼을 때만 전체 동기화로 넘어간다.
* 복제는 기본적으로 **비동기(Async)**다. Master는 Replica의 적용 완료를 기다리지 않고 클라이언트에 응답한다. 따라서 Master가 장애로 죽으면 아직 전파되지 않은 마지막 쓰기 일부가 유실될 수 있다.
* Replica에서 읽기를 분산하면 복제 지연(replication lag)만큼 과거 데이터를 읽을 수 있다.*
* 한계 : Master가 죽어도 **자동 승격이 없다.** 운영자가 수동으로 Replica를 Master로 올리고 클라이언트 설정을 바꿔야 한다. 이 문제를 해결하는 것이 Sentinel이다.

#### Sentinel

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ffa1d1ee-83bb-458b-9c2d-2adba6228f27.png)

* 모니터링(Monitoring) : 각 센티널은 마스터와 복제본 노드들에 주기적으로 PING 메시지를 보내서 살아있는지 확인한다.
* 다운 판정은 2단계로 이루어진다. 오탐(네트워크 순단)으로 인한 불필요한 failover를 막기 위해서다.
  * `SDOWN(주관적 다운)` : 센티널 "하나"가 응답 없음을 감지한 상태 - 아직 조치하지 않는다.
  * `ODOWN(객관적 다운)` : **쿼럼(quorum) 수 이상의 센티널이 동의**했을 때 비로소 마스터 다운으로 확정한다.
* 자동 장애 조치(Failover) : 마스터의 다운이 확정(ODOWN)되면 센티널들은 다음 과정을 수행한다.
  * 리더 선출 : 센티널 중 하나가 장애 조치를 주도할 리더가 된다.
  * 새 마스터 선정 : 살아있는 Replica들 중 데이터 복제 상태가 가장 최신인 노드를 골라 새로운 Master로 승격시킨다.
  * 복제 재설정 : 나머지 Replica들이 새로운 마스터를 바라보도록 설정을 변경한다.
  * 클라이언트 통보 및 재접속 : 센티널은 새로운 마스터의 정보를 업데이트한다. 클라이언트는 센티널을 통해 바뀐 마스터 정보를 받아와서 중단 없이 서비스를 계속 이어갈 수 있다.
* 운영 요건
  * 센티널 자체가 죽으면 안 되므로 **3대 이상의 홀수**로 구성한다. 과반 투표가 가능해야 리더 선출과 ODOWN 판정이 성립하고, 짝수 구성은 표가 갈리는 스플릿 브레인에 취약하다.
  * 클라이언트는 Redis 주소가 아니라 **센티널 주소 목록**을 바라보고, 센티널에게 "현재 마스터가 누구인지" 물어본 뒤 접속한다. (클라이언트 라이브러리의 Sentinel 지원 필요)
* Sentinel은 가용성(HA)만 해결한다. 데이터는 여전히 Master 한 대에 전부 들어가므로 **용량·쓰기 처리량의 확장은 불가능**하다. 이를 해결하는 것이 Cluster다.

#### Cluster

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e84e7a50-5424-42d5-a7e5-6a048768dc71.png)

* 16384개의 슬롯 : Redis 클러스터는 전체 데이터를 총 16,384개의 가상 슬롯으로 나눈다.
* 데이터 분산 : 사용자가 데이터를 저장할 때, `CRC16(key) mod 16384` 해싱 결과값에 따라 특정 슬롯에 할당되고, 각 Master가 슬롯 구간을 나눠 담당한다.
* 수평 확장 : 데이터가 너무 많아지면 Master 4를 추가하고 슬롯을 쪼개서 나눠주면 된다.
* Gossip 프로토콜(노드 간 통신) : 별도의 관리자(Sentinel) 없이 Master Node들끼리 클러스터 전체 상태 파악과 장애 감지를 스스로 수행한다.
* 클라이언트 접근
  * Sentinel 방식과 달리 클라이언트는 어느 Master Node에든 직접 붙을 수 있다.
  * 요청한 키가 다른 노드의 슬롯이면 해당 노드는 `MOVED [슬롯] [주소]` 응답으로 올바른 노드를 알려준다. 클러스터 인식 클라이언트(Lettuce 등)는 슬롯 → 노드 맵을 캐싱해서 처음부터 올바른 노드로 요청을 보낸다.
* 자체 장애 조치(Failover)
  * Master 1이 죽으면 이를 감시하던 다른 Master Node들이 투표를 시작해 Master 1의 짝꿍인 Replica 1을 새로운 마스터로 승격시킨다.
  * 이 과정이 노드들 간의 합의로 이루어지기 때문에 별도의 감시 서버를 필요로 하지 않는다.
  * 과반 합의가 성립해야 하므로 **최소 Master 3대**(+ 각각의 Replica) 구성이 권장 최소 단위다.
* 클러스터 모드의 제약
  * 여러 키를 다루는 연산(MSET, SINTER, 트랜잭션, Lua 스크립트 등)은 **모든 키가 같은 슬롯에 있을 때만** 가능하다. 함께 다뤄야 하는 키들은 해시 태그(`user:{100}:profile`, `user:{100}:cart`처럼 `{}` 부분만 해싱)로 같은 슬롯에 모은다.
  * 논리 DB는 0번 하나만 사용할 수 있다(`SELECT` 명령 불가).

#### 모드 선택 요약

|모드|해결하는 문제|자동 Failover|데이터 분산(샤딩)|적합한 상황|
|:---:|:---:|:---:|:---:|:---|
|Standalone|-|❌|❌|로컬 개발, 잃어도 되는 캐시|
|Replica|읽기 분산, 백업|❌ (수동)|❌|읽기 부하 분산이 필요하지만 장애 대응은 수동으로 가능한 경우|
|Sentinel|고가용성(HA)|✅|❌|데이터가 한 대에 다 들어가고, 무중단이 중요한 경우|
|Cluster|고가용성 + 수평 확장|✅ (자체 합의)|✅ (16384 슬롯)|데이터·쓰기 처리량이 한 대 용량을 넘는 경우|
