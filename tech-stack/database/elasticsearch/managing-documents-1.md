# ElasticSearch - Managing Documents (1)

#### Elasticsearch란

* **오픈소스 검색·분석 엔진**이다. 수평 확장성(horizontal scalability), 안정성, 운영 편의성을 목표로 설계되었다.
* **Apache Lucene** 위에 구축되었고, **Elastic Stack**(Logstash, Kibana 포함)의 일부다.

#### 핵심 특성 4가지

| 특성 | 내용 |
|---|---|
| **분산 처리(Distributed)** | 클러스터의 가용 노드들에 데이터와 쿼리 부하를 자동 분산한다. 대용량 데이터를 준실시간으로 처리한다 |
| **전문 검색(Full-Text Search)** | 고급 전문 검색이 본질이다. HTTP 웹 인터페이스와 스키마 없는 JSON 문서를 사용한다 |
| **확장성(Scalability)** | 수백~수천 대 서버로 스케일 아웃하며, 페타바이트급 정형/비정형 데이터를 처리한다 |
| **유연성(Flexibility)** | 다양한 소스의 이기종 데이터 타입을 색인하고 복잡한 검색을 수행한다 |

#### 기본 데이터 모델: Document & Field

* **Document**는 색인 가능한 **정보의 기본 단위**이며, JSON으로 표현한다.
* **Field**는 Elasticsearch에서 **가장 작은 데이터 단위**로, 문서 안의 key-value 쌍이다.

```json
{
  "name": "John Doe",     ← Field
  "age": 30,
  "email": "john.doe@example.com"
}
```
↑ 이 JSON 객체 전체가 하나의 Document다

#### 주요 활용 사례

* **Enterprise Search** — 사내 전체 디지털 콘텐츠를 색인해 웹사이트나 내부망에 정교한 검색을 제공한다.
* **Logging & Log Analysis** — Logstash, Kibana와 함께 로그를 분석한다. IT 운영·보안·성능 모니터링에 필요한 인사이트를 얻는다.
* **SIEM(보안 정보·이벤트 관리)** — 보안 데이터를 실시간으로 분석·시각화해 위협 탐지와 컴플라이언스 관리를 돕는다.
* **Data Analysis** — 빅데이터 분석에 활용한다. 대용량 데이터를 빠르고 다양한 방식으로 탐색한다.
* **개인화·추천** — 이커머스에서 사용자 행동과 인터랙션을 분석해 맞춤 상품 추천과 동적 콘텐츠를 제공한다.

#### 데이터 분산과 저장

* **Indexing(색인)**
  * 데이터는 **index** 단위로 구성되며, index는 **데이터베이스에 해당**하는 개념이다.
  * index에 저장되는 문서는 JSON 형식이다.
  * 문서가 추가되면 Elasticsearch가 **검색에 최적화된 형태로 변환**해 색인한다.
* **Sharding(샤딩)**
  * 각 index는 **여러 shard로 분할**할 수 있으며, 이를 통해 클러스터의 여러 노드에 분산 배치된다.
  * 고가용성과 성능 확장성을 확보한다. 대용량 데이터셋과 높은 쿼리 부하를 처리하는 핵심이다.
* **Replicas(복제본)**
  * primary shard의 복사본인 **replica shard**를 생성할 수 있다.
  * 하드웨어 장애에 대비한 **데이터 이중화**를 제공하고, 어느 replica에서도 검색이 가능하므로 **쿼리 처리량이 증가**한다.

#### 검색 메커니즘

* **Query Processing(쿼리 처리)**
  * 쿼리가 들어오면 **파싱한 뒤 Lucene 인덱스 검색에 적합한 형태로 변환**한다.
  * 변환된 쿼리를 **관련된 모든 shard(primary 또는 replica)에 병렬로 실행**한다.
* **Relevance Scoring(연관도 점수)**
  * **TF-IDF**(Term Frequency–Inverse Document Frequency), **BM25**(Best Matching 25) 등의 알고리즘으로 문서가 쿼리에 얼마나 부합하는지 점수화한다.
  * 이 점수를 기준으로 검색 결과 랭킹을 결정한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7e935e25-017e-42e4-ade9-91e9baa71310.png)

#### 실시간 처리

* **Near Real-Time(NRT) 검색**
  * 데이터 색인과 조회가 거의 동시에 이루어진다.
  * **인메모리 버퍼**에 들어오는 문서를 모으고, 이 버퍼를 **주기적으로 flush**해 인덱스의 **새 세그먼트**를 생성하는 방식으로 구현한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/857cb65e-0fa3-4aa9-ac2f-527ccd00b272.png)

#### 클러스터 관리

* **Nodes & Cluster**
  * 클러스터는 하나 이상의 노드(서버) 집합이다. 전체 데이터를 보유하고 모든 노드에 걸쳐 색인·검색 기능을 제공한다.
  * 클러스터는 고유 이름으로 식별하며 기본값은 `elasticsearch`다.
* **Master Node**
  * 클러스터의 **health와 metadata를 관리**한다.
  * 새 문서 추가를 추적하고 노드에 대한 **shard 할당**을 담당한다.
* **Node Communication**
  * 노드 간 통신은 **JSON over HTTP의 RESTful API**나 내부 **transport 프로토콜**을 사용한다.

#### 분석과 집계

* **Analysis(분석)**
  * **색인 시점에 텍스트 분석**을 수행하며, 텍스트를 토큰이나 텀 단위로 분해한다.
  * 커스텀 **tokenizer, filter, analyzer**로 고도로 커스터마이징할 수 있다.
* **Aggregation(집계)**
  * 데이터로부터 복합적인 요약과 인사이트를 도출한다.
  * **metric**(합계·평균 등 계산), **bucketing**(데이터 그룹화), **nested aggregation**(계층적 탐색)으로 나뉜다.

#### Elastic Stack 구성요소

| 구성요소 | 역할 |
|---|---|
| **Elasticsearch** | 저장(Store), 검색(Searching), 분석(Analyzing)을 담당한다 |
| **Logstash** | 서버 사이드 데이터 처리를 담당한다 |
| **Kibana** | 데이터 시각화 대시보드를 제공한다 |
| **Beats** | 데이터를 수집해 전송한다(shipper). 예: Filebeat, Metricbeat |
| **X-Pack** | Security(RBAC, 암호화, Audit, IP Filtering), Alerting, Monitoring, Reporting, Machine Learning(이상 탐지), Graph Analytics, SQL을 제공한다 |

#### 데이터 파이프라인

```
LOGS → [beats(Data Collection)] → [logstash(Data Processing)] → [elasticsearch(Storage)] → [kibana(Visualize)]
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fe6e3ec4-1dc6-4736-a537-304cf47f332a.png)

**확장 구성**: 수집(beats)과 처리(logstash) 사이에 **Kafka나 Redis**를 버퍼로 두어 유입량 급증을 흡수하고 처리 단계를 분리할 수 있다.

```
LOGS → beats → [Kafka / Redis] → logstash → elasticsearch → kibana
```

#### Cluster와 Node

* **Cluster**는 함께 동작하는 **하나 이상의 노드 집합**이다. 고유한 이름으로 자신을 식별하며, 데이터를 분산하고 집합적으로 관리하는 데 쓰인다.
* **Node**는 Elasticsearch의 **각 인스턴스**를 가리킨다. 노드는 데이터를 저장하고 클러스터의 색인·검색 기능에 참여한다.

#### 노드의 종류

| 노드 | 역할 |
|---|---|
| **Master Node** | 인덱스 생성·삭제, 노드 상태 관리 등 클러스터 전역 작업을 관리한다 |
| **Data Node** | 데이터를 저장하고 CRUD, 검색, 집계를 처리한다 |
| **Ingest Node** | 색인 전에 필터링이나 보강 같은 데이터 변환을 처리한다 |
| **Coordinating Node** | 클라이언트 요청을 라우팅하고 노드 간 검색 부하를 분산한다 |

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/25502a23-d375-41c2-bbf4-5d14903f062f.png)

#### Index, Document, Shard

* **Index**는 문서를 조직하는 **논리적 네임스페이스**다. 관계형 데이터베이스의 **테이블에 해당**하는 개념이다.
* **Document**는 색인 가능한 정보의 단위다. 데이터 속성을 나타내는 필드들로 구성된 JSON 객체다.
* **Shard**는 index를 잘게 나눈 조각이다. 데이터를 클러스터의 여러 노드에 분산할 수 있게 해주며, **각 shard는 본질적으로 하나의 작은 Lucene 인스턴스**다.

#### Shard의 두 종류

* **Primary Shard** — 인덱스 생성을 담당하고 CRUD 연산을 직접 처리한다.
* **Replica Shard** — primary shard의 복사본으로, 이중화와 고가용성을 제공한다.

#### 클러스터를 지탱하는 요소

* **Replica** — 각 primary shard는 장애 내성을 위해 **0개 이상의 replica**를 가질 수 있다. replica는 읽기 요청도 처리하므로 검색 성능을 높인다.
* **Routing Table** — 어느 노드가 어느 shard를 가지고 있는지 결정한다. 이 정보는 master node가 갱신하고, coordinating node가 요청을 적절한 shard로 보내는 데 사용한다.
* **Gateway** — Elasticsearch가 **클러스터와 인덱스의 상태를 영속화**하는 메커니즘이다. 메타데이터를 유지하며, 노드 장애 시 이 정보를 복구할 수 있다.
* **Discovery Module** — 새 노드가 기존 클러스터에 매끄럽게 합류하도록 보장하고, 노드 상태 변화를 관리한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/523fbb1d-526b-497d-9223-935013964f14.png)

#### 전체 구조

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5e9e571b-4716-4cf6-9653-54bae5c5ca2c.png)

#### Replication이란

* Elasticsearch의 replication은 **데이터의 중복 사본을 만드는 과정**이다. 데이터 이중화, 고가용성, 장애 내성을 확보하는 것이 목적이다.

#### Shard와 Replica

* **Primary Shard** — 각 인덱스는 primary shard로 분할된다. 원본 데이터의 색인과 저장을 담당한다.
* **Replica Shard** — primary shard의 복사본이다. **백업 역할**을 하면서 동시에 **쿼리 부하를 분담**한다.

#### Replication 동작 방식

* **Primary–Replica 관계** — 각 primary shard는 **0개 이상의** replica shard를 가질 수 있다.
* **쓰기(Write)** — 문서가 primary shard에 색인되면 **해당 primary의 모든 replica shard에도 복제**된다. 이를 통해 일관성을 보장한다.
* **읽기(Read)** — 검색 쿼리는 **primary와 replica 어느 쪽에서도** 처리할 수 있다. 읽기 부하를 분산해 성능을 높인다.

#### Replication의 이점

* **High Availability(고가용성)** — primary shard를 가진 노드가 다운되면 그 **replica 중 하나가 primary로 승격**되어 데이터에 계속 접근할 수 있다.
* **Load Balancing(부하 분산)** — 검색 요청을 primary와 replica 양쪽으로 라우팅해 검색 쿼리 성능을 개선한다.
* **Data Redundancy(데이터 이중화)** — 여러 노드에 걸쳐 복수의 사본을 유지하므로, 노드 장애로 인한 데이터 유실을 막는다.

#### 고려사항

* **노드 수** — replication 설정을 감당할 만큼 노드가 충분해야 한다. **동일한 shard의 primary와 replica는 같은 노드에 배치될 수 없기** 때문이다.
* **네트워크·스토리지 오버헤드** — 데이터가 중복되므로 추가적인 네트워크 트래픽과 저장 공간을 소비한다.
* **트레이드오프** — **데이터 안전성**(replica를 늘림)과 **자원 사용량**(디스크와 네트워크) 사이에 트레이드오프가 존재한다.
