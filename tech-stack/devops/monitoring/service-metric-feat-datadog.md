# Monitoring - Service Metric(feat. DataDog)

#### 에러율(Error Rate)

> **❗에러율 = 에러로 판정된 요청 수 ÷ 전체 요청 수   (같은 시간 구간 안에서)**

* **에러율** = 같은 구간의 (5xx + 처리되지 않은 예외) 합계 ÷ 요청 합계.

#### 응답 시간(Latency)

> **❗지연시간 = 요청이 도착한 시점부터 응답이 끝난 시점까지 걸린 시간**

* **avg(평균)** : 전체 요청의 산술 평균. 극단값에 왜곡되기 쉬움. "전반적 상태"를 대략적으로 파악하는 용도.
* **P50(중앙값)** : 요청의 절반이 이 시간 이내에 완료. "일반적 사용자 경험".
* **P95/P99** : 상위 5%/1%를 제외한 최대 응답 시간. "최악에 가까운 사용자 경험". SLO의 핵심 지표.

#### 처리량(Throughput/TPS)

> **❗처리량 = 단위 시간 동안 "완료된" 작업의 수**

* **TPS(Transactions Per Second)** : 초당 처리 요청 수.
* **RPM(Requests Per Minute)** : 분당 요청 수. TPS보다 큰 단위로 트래픽 볼륨 추세를 파악하는 용도.
* **전주 동기 대비** : 같은 요일, 같은 시간대의 트래픽과 비교. 계절성을 반영한 가장 신뢰할 수 있는 비교 기준.
* **피크/비피크 패턴** : 서비스마다 고유한 트래픽 패턴이 있음. 이 패턴 자체의 변화가 이상 신호.

#### 리소스 사용률(Resource Utilization)

> **❗사용률 = 자원을 쓰고 있는 양(또는 시간) ÷ 그 자원의 전체 용량**

* 애플리케이션 지표에서 이상을 발견했을 때, 각 계층별로 다음과 같이 해석할 수 있어야 한다.
* **JVM** : Heap Used/Max, GC Pause Time, GC Count, Thread Count. Heap 부족 → OOM, GC 폭주 → Latency 급증.
* **컨테이너/Pod** : CPU Usage vs Request/Limit, Memory Usage vs Limit, Restart Count. CPU throttling → Latency 상승, OOMKill → 재시작 → 에러 스파이크.
* **노드** : CPU/Memory/Disk 사용률, Network I/O. 노드 과부하 → 모든 Pod에 영향.
* **DB** : Query Duration, Connection Pool Usage, Lock Wait, Disk I/O. 대부분의 Latency 문제의 근원지.
* **캐시** : Hit Rate, Memory Usage, Eviction Rate, Connection Count. Cache Miss Rate 증가 → DB 부하 증가 → Latency 상승의 연쇄.
