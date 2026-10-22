# MicroService Architecture - Scalability & Caching Patterns

#### Scalability

| 동시접속자 수 | 초당 요청 수 (RPS) | 권장 시스템 설계 및 고려사항 |
|---|---|---|
| 500 | 50 RPS | 단일 서버로 처리 가능하나, 향후 확장을 대비한 설계 필요 |
| 1K | 100 RPS | 로드 밸런서를 도입하여 두 대의 서버로 부하 분산 고려 |
| 2K | 200 RPS | 세션 상태 관리를 위한 분산 캐시 도입 검토 |
| 5K | 500 RPS | 데이터베이스 샤딩 또는 리플리케이션 고려 |
| 10K | 1,000 RPS | 콘텐츠 전송 네트워크(CDN) 활용하여 정적 자원 제공 최적화 |
| 20K | 2,000 RPS | 마이크로서비스 아키텍처 도입 검토 |
| 50K | 5,000 RPS | 서비스 메시 도입 및 오케스트레이션 도구 활용 고려 |
| 100K | 10,000 RPS | 글로벌 로드 밸런싱 및 멀티 리전 배포 검토 |
| 500K | 50,000 RPS | 대규모 분산 시스템 설계 및 데이터 일관성 유지 전략 수립 필요 |

* 가동 중지 시간을 방지하고 대기 시간을 줄이려면 확장이 필요하다.
* 수평/수직 확장 ⭢ 컴퓨팅 인프라에 리소스를 추가하는 것으로 수평 확장은 더 많은 머신 추가를, 수직 확장은 더 많은 리소스 파워를 추가하는 것을 말한다.

#### Vertical Scaling – Scale up

* 기본적으로 노드를 더 강하게 구성한다.
  * 하드웨어를 더 추가해 서버를 더 강하게 구성한다.
  * 단일 노드에 더 많은 리소스를 추가한다.
* 기존 인프라를 유지하면서 더 많은 컴퓨팅 파워를 추가한다.
  * 최적화를 통해 더 많은 요청을 처리한다.
  * 기존 코드에 대한 변경은 없다.
  * 증가하는 작업 부하 ⭢ CPU, RAM 및 DISK 추가
* 확장성 제한
  * 하드웨어에도 최대 용량 제한이 있다.
  * 수백만 개의 요청 처리 ⭢ 수평적 확장 또는 더 많은 컴퓨팅 확장이 필요하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6c39e468-3e5c-45d1-a763-3a06746d04dc.png)

#### Horizontal Scaling – Scale out

* 서로 다른 서버 간에 부하 분산
  * 기존 사양을 변경하지 않고 더 많은 머신 인스턴스를 추가
  * 여러 머신에서 처리 능력과 부하 분산을 공유
  * 리소스 풀에 더 많은 머신을 추가
* 확장성과 안정성을 모두 만족
  * 분산 아키텍처의 확장
* 여러 서버로 분할할 때의 고려사항
  * 상태가 있는지 여부
  * 데이터베이스 서버 ⭢ CAP 정리 및 관리 필요

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d48ba12f-654d-4fdf-978e-ee08fd2f11a1.png)

#### Load Balancer

* 모든 애플리케이션 노드에 걸쳐 트래픽을 분산
  * 응답성과 가용성을 개선
  * 다양한 알고리즘을 사용하여 여러 백엔드 서버에 트래픽 분산 ⭢ 해싱 알고리즘 적용
* 내결함성이 있어야 하며 가용성을 개선
* 수평적 확장성 문제를 해결. 모든 키를 다시 정렬할 필요가 없다.  

#### MSA Consistency Level

* Strict Consistency : 데이터를 저장할 때 데이터는 모든 클라이언트에 즉시 적용(은행 계좌에서 직불 또는 인출 등)
* Eventual Consistency : 데이터를 업데이트 시 시간이 소요(유튜브 좋아요, 스트리밍 조회수 등)

#### Distributed Caching

* 자주 액세스하는 데이터 : 메모리에서 빠르게 액세스할 수 있는 캐시에 저장. 시스템 성능 개선
* 캐시로 대기 시간을 줄이고 애플리케이션을 더 빠르게 만듦 : 마이크로서비스 성능, 확장성, 가용성을 높임. 요청 수가 증가 ⭢ 캐싱은 고가용성으로 요청 처리
* 애플리케이션 요청이 자주 변경되지 않는 데이터 : 캐싱이 매우 효율적
* 여러 종류의 타입이 존재한다.
  * In-memory Cache : 컴퓨터 주 메모리에 데이터 저장. 가장 빠른 유형의 캐시. 캐시를 다시 시작하거나 컴퓨터를 종료하면 데이터가 손실
  * Disk Cache : HDD나 SDD에 데이터 저장. 디스크 캐시는 메모리 내 캐시보다 느리지만 데이터 유지 가능
  * 분산 캐시 : 여러 컴퓨터에 분산. MSA와 같은 분산 시스템에서 사용. 데이터를 여러 위치에서 저장하고 액세스. 시스템의 성능과 확장성 개선 가능

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/24bf2755-8844-4466-a915-85629e0fa564.png)

#### Distributed Caching – Hit and Miss

* Cache Hit : 요청된 데이터가 캐시에서 발견될 때. 적중은 요청 수를 줄여 시스템 성능 개선 가능
* Cache Miss : 요청된 데이터가 캐시에서 발견되지 않을 때. 더 느린 스토리지 DB에서 검색해야 할 때. 요청된 데이터를 검색하는 데 추가 시간과 리소스가 필요 ⭢ 성능에 부정적인 영향
* Cache Hit Rates : 캐시가 자체 저장소에서 요청을 얼마나 자주 찾을 수 있는지를 측정. 높을수록 캐시가 자주 액세스하는 데이터를 저장하는 데 효과적이고 낮으면 효과가 크지 않다.

#### Caching Consistency

* TTL(Time-Ti-Live) : Cache 항목에 대한 유효한 시간을 정의. 설정된 시간이 지나면 항목은 자동으로 만료 ⭢ 삭제. 가장 단순하고 광범위하게 사용
* Event Based Invalidation : 데이터가 변경될 때 이벤트를 발생 ⭢ 캐시 즉시 무효. 실시간 일관성이 중요한 상황에서 유용
* Strict Consistency : 모든 읽기 작업이 가장 최신 데이터 반환 보장. Write-Though Cahce + 동기식 무효화
* Eventual Consistency : 일정 시간 후에 모든 읽기 작업이 최신 데이터 반환. Write-Behind Cache + 비동기 무효화 또는 TTL
* Cache Stampede 방지 : 캐시 항목 만료 ⭢ 여러 요청이 동시에 DB 접근해 과부하 발생하는 문제. Locking / Semaphore Pattern / Background refresh / 확률적 조기 만료 / Grace Period 설정 추가 필요

#### 데이터베이스 확장과 캐싱

* 읽기 확장을 위한 Caching : 읽기 전용 DB Replica에 읽기 작업 분산. 핫스팟 데이터 식별 및 우선 Caching(TTL 차별화)
* CQRS와 Caching : 명령과 쓰기를 분리해 각각 최적화. Query Model은 Caching에 매우 적합
* 데이터베이스 샤딩과 캐싱 : 데이터를 여러 DB 노드에 분산 저장(수평적 확장성). 샤드 키에 따라 데이터 분할. 샤드 키와 캐시 키 정렬, 효율적인 캐시 무효화
* Polyglot Persistence 환경에서의 Caching 방법

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cdcf91fb-416d-4ae1-a15f-55250555d5c1.png)
