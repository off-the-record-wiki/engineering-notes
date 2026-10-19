# Redis ‐ Redis

#### Redis란?

* Redis는 Remote Dictionary Server의 약자로서, "키-값" 구조로 데이터를 저장하고 관리하는 오픈 소스 기반의 인메모리 비관계형 데이터베이스(NoSQL)이다.
* 값(Value)이 단순 문자열만 가능한 것이 아니라 String, List, Hash, Set, Sorted Set 등 **다양한 자료구조 자체를 값으로 저장**할 수 있다는 점이 다른 키-값 저장소와의 결정적 차이다.
* 쉽게 말하면 Redis는 데이터 처리 속도가 빠른 NoSQL 데이터베이스이다.

#### Redis가 빠른 이유

* 인메모리(In-Memory) 저장
  * MySQL과 같은 RDBMS의 데이터베이스는 대부분 디스크에 데이터를 저장한다.
  * 하지만 Redis는 메모리(RAM)에 데이터를 저장한다. 디스크보다 메모리에서의 데이터 처리 속도가 월등히 빠르다.
* 싱글 스레드 이벤트 루프
  * Redis는 명령을 단일 스레드로 순차 처리한다. 덕분에 락(Lock) 없이도 **모든 개별 명령이 원자적**으로 실행되며, 멀티 스레드의 컨텍스트 스위칭·경합 비용이 없다.
  * "싱글 스레드인데 빠르다"가 아니라, 병목이 CPU가 아닌 메모리/네트워크이기 때문에 싱글 스레드로도 초당 수십만 건을 처리할 수 있는 것이다.(Redis 6.0부터 네트워크 I/O만 멀티 스레드로 처리 가능 - 명령 실행은 여전히 싱글 스레드)
* 최적화된 자료구조 구현
  * 각 자료구조가 메모리 효율과 연산 속도에 최적화된 내부 인코딩(listpack, skiplist 등)으로 구현되어 있다.

#### Redis 단점 및 주의사항

* 휘발성 : 메모리 기반이므로 프로세스가 재시작되면 데이터가 손실된다.
  * RDB 스냅숏(주기적 백업)과 AOF(명령 로그 기록)로 영속성을 보완할 수 있다. 완전한 소멸을 막는 장치이지, RDBMS 수준의 내구성 보장은 아니다.
* 메모리 용량 제한과 비용 : RAM은 디스크보다 훨씬 비싸다. 전체 데이터가 아닌 자주 쓰는 데이터(Hot Data)만 올리는 것이 기본 전략이며, `maxmemory`와 축출(Eviction) 정책 설정이 필요하다.
* 싱글 스레드의 함정 : 명령이 순차 처리되므로 오래 걸리는 명령 하나가 서버 전체를 블로킹한다.
  * 운영 환경에서 `KEYS *`, 대형 컬렉션에 대한 O(N) 명령은 금물이다. `SCAN` 계열로 나눠서 순회해야 한다.
* 캐시로 쓸 때 원본 DB와의 정합성(캐시 무효화, TTL 전략)은 Redis가 해결해 주지 않는다. 별도 설계가 필요하다.

#### Redis 사용 사례 간략히 정리

* 캐싱(=데이터 조회 성능 향상) : DB 조회 결과를 String/Hash + TTL로 저장해 반복 조회를 메모리에서 처리
* 세션 관리 : 서버 여러 대가 세션 저장소를 공유 → 무상태(stateless) 서버 확장 가능
* 실시간 분석 및 통계 : Sorted Set으로 리더보드·인기 검색어, HyperLogLog로 DAU 카운팅
* 메시지 큐 : List(간단한 큐), Streams(컨슈머 그룹·재처리가 필요한 경우)
* 지리공간 인덱싱 : GEO 명령으로 반경 검색("내 주변 매장")
* 속도 제한 : `INCR` + `EXPIRE` 조합으로 API Rate Limiting
* 실시간 채팅 및 메시징 : Pub/Sub 브로드캐스트

#### Redis Usage

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1faaad3c-c373-41b7-be48-5b58ddbc0422.png)

#### Redis TradeOff

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e522bad7-af4b-450d-8a7d-5e5fc2fe18f5.png)
