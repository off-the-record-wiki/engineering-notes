# Monitoring - Redis Metric(feat. AWS Cloudwatch)

#### ElastiCache(Redis OSS / Valkey) 핵심 지표 — 정의와 해석

> **공통 해석 규칙**
* 지표는 노드 단위다. 클러스터 모드면 샤드별 프라이머리·레플리카를 각각 본다. 평균만 보면 핫 샤드가 가려진다.
* 통계(Statistic) 선택이 해석을 바꾼다.
  * 사용률·게이지(CPU, 메모리, 연결 수) → Average로 추세, Maximum으로 순간 피크
  * 카운트(Evictions, NewConnections, CacheHits) → Sum (기간당 발생 횟수)
  * 지연(Latency) → Average 기본, 스파이크 확인은 Maximum
* 기간(Period)은 1분(60초)이 기본 해상도다. 5분 평균으로 보면 1분짜리 스파이크가 묻힌다.
* 절대값보다 "평소 대비"가 중요한 지표가 있다. 연결 수, 키 개수, 히트율은 서비스마다 정상값이 달라서 기준선(baseline)을 먼저 잡아야 한다.

#### EngineCPUUtilization

> **❗Redis 엔진 쓰레드(명령 처리 메인 쓰레드)의 CPU 사용률**

* 단위/통계 : Percent / Average, Maximum
* 경고 기준 : 지속 `80 ~ 90%` 이상
* Redis는 명령을 싱글 스레드로 처리한다. 4 vCPU 노드에서 CPUUtilization이 25%로 보여도 엔진 스레드는 100%일 수 있다. 그래서 이 지표가 진짜 포화도다.
* 높아지는 전형적 원인은 2가지가 있다.
  * 무거운 명령 : `KEYS *`, 큰 컬렉션에 대한 `HGETALL`·`SMEMBERS`·`LRANGE 0 -1`, 대량 DEL, Lua 스크립트
  * 단순히 요청량(throughput)이 많은 경우
* 대응 방안 : 무거운 명령 제거(SCAN으로 대체, 컬렉션 분할) → 읽기를 레플리카로 분산 → 샤드 추가(스케일 아웃).

#### CPUUtilization

> **❗호스트 전체 CPU 사용률 (엔진 + OS + 백업·복제 등 백그라운드 작업)**

* 단위/통계 : Percent / Average, Maximum
* 경고 기준 : `90 ÷ vCPU 수 %` (예: 2 vCPU → 45%)
* 2 vCPU 이하 노드에서는 엔진과 OS가 코어를 나눠 쓰므로 이 지표가 더 정확하다. 그보다 큰 노드에서는 EngineCPUUtilization을 우선한다.
* EngineCPUUtilization은 낮은데 이 값만 높다면 백그라운드 작업(스냅샷 fork, 복제 동기화, TLS 암복호화)을 의심한다.

#### DatabaseMemoryUsagePercentage

> **❗used_memory ÷ maxmemory × 100. Redis가 쓸 수 있도록 허용된 메모리 중 실제 사용 비율**

* 단위/통계 : Percent / Maximum
* 경고 기준 : 80% 이상
* `maxmemory`는 노드 물리 메모리 전체가 아니다. 백업·복제용 버퍼로 `reserved-memory-percent(기본 25%)`를 떼고 남은 값이다.
* `100%`에 닿으면 `maxmemory-policy`에 따라 동작이 갈린다.
  * `volatile-lru(ElastiCache 기본값)` : TTL이 있는 키만 삭제 대상이다. TTL 없는 키로 메모리가 차면 지울 키가 없어 쓰기가 OOM command not allowed 에러로 거부된다.
  * `allkeys-lru` : 모든 키가 삭제 대상이다.
  * `noeviction` : 삭제 없이 쓰기를 거부한다.
* 우상향 추세가 계속되면 TTL이 누락된 키를 의심한다. CurrItems가 함께 느는지 확인한다.
* 대응 방안 : TTL 누락 점검 → 큰 키 찾기(redis-cli --bigkeys, MEMORY USAGE) → 노드 스케일 업 또는 샤드 추가.

#### Evictions

> **❗maxmemory 한도 때문에 Redis가 강제로 삭제한 키 수(TTL 만료로 사라진 키는 포함하지 않는다. 그건 Reclaimed)**

* 단위/통계 : Count / Sum
* 경고 기준 : 0보다 큰 값이 지속될 때
* 캐시 용도라면 가끔 발생하는 eviction은 정상이다(LRU가 제 일을 하는 것). 하지만 계속 발생하면 working set이 메모리보다 크다는 뜻이고, 곧 CacheHitRate 하락으로 이어진다.
* 세션·락·큐처럼 잃으면 안 되는 데이터가 섞여 있다면 1건이라도 사고다. 이런 데이터와 캐시는 클러스터를 분리하는 것이 원칙이다.
* DatabaseMemoryUsagePercentage가 100% 근처인데 Evictions가 0이고 에러가 난다면 → 삭제 가능한 키(TTL 있는 키)가 없는 상태다.
