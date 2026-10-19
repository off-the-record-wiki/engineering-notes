# Apache Kafka - Differences Between RocksDB and In-Memory KeyValueStore in GlobalKTable

### RocksDB vs InMemoryKeyValueStore

#### In-Memory Store

* 인메모리 저장소는 모든 상태 데이터를 RAM에 저장하여 빠른 접근을 제공하지만 내구성은 부족하다.
* 인메모리 저장소를 사용하는 Kafka Streams 애플리케이션이 충돌하거나 재시작되면, 디스크에 기록되지 않으므로 모든 상태 정보가 사라진다.
* 인메모리 저장소는 지속성이 필요하지 않거나 내구성보다 성능을 우선시하는 경우에 이상적이다.

#### Persistent Store (e.g., RocksDB)

* 영구 저장소는 상태가 디스크에 쓰여지도록 보장하며, 일반적으로 RocksDB를 스토리지 엔진으로 사용한다.
* Kafka Streams는 스트림 처리에서 로컬 상태를 관리하는 기본 영구 상태 저장소로 RocksDB를 사용한다.
* 애플리케이션 충돌, 재시작, 노드 장애 등에도 견딜 수 있다.

#### RocksDB

* RocksDB는 내장형 키-값 저장소로, Kafka Streams 애플리케이션과 동일한 프로세스 내에서 작동하여 읽기와 쓰기 시 네트워크 호출이 필요 없다. 
* 이로 인해 지연 시간이 줄어들고 상태 기반 연산에서 높은 처리량을 보장하여 외부 저장 시스템이 초래할 수 있는 병목 현상을 방지한다.
* Kafka Streams에서 RocksDB 운영은 Kafka 변경 로그 주제에 의해 뒷받침된다. 
* Kafka 아키텍처와의 이러한 통합은 Kafka 주제에 상태 변화를 지속적으로 복제하여 내결함성 스트림 처리를 가능하게 하고 실패 시 변경 로그는 Kafka Streams가 애플리케이션 상태를 복원하여 데이터 손실이 없고 상태 일관성을 유지할 수 있도록 한다.

#### How RocksDB Handles Reads and Writes

Read Operations:

1. RocksDB는 먼저 요청된 데이터를 위해 활성 멤터블을 확인한다.
2. 데이터가 멤테이블에 없으면 RocksDB는 이전 읽기 전용 멤터블을 확인한다.
3. 데이터가 여전히 발견되지 않으면, 다음 단계는 최근 디스크에서 접근한 데이터를 저장하여 읽기 지연을 줄이기 위해 블록 캐시를 확인한다.
4. 마지막으로, 데이터가 메모리에 없으면 RocksDB는 디스크 내 SST 파일을 검색한다. 이렇게 되면, SST 파일은 변경 불가능하고 정렬되어 있기 때문에, RocksDB는 디스크에서 접근하더라도 요청된 데이터를 효율적으로 찾을 수 있다.

Write Operations:

1. 데이터는 먼저 메모리 내 버퍼인 멤터블에 기록된다. Memtable은 RocksDB의 주요 쓰기 경로이며, 모든 새 쓰기는 활성 Memtable로 가게 된다.
2. 멤테이블이 가득 차면 불변 상태로 변환되고, 추가 쓰기 작업을 처리할 새로운 멤테이블이 생성된다.
3. 전체 읽기 전용 멤터블은 백그라운드 프로세스에 의해 디스크에 플러시되어 SST(정렬된 문자열 테이블) 파일로 저장된다. 이 파일들은 효율적인 디스크 조회를 용이하게 하기 위해 압축되고 정렬된 형식으로 데이터를 저장한다.

### Fault Tolerance and State Recovery in RocksDB

* 위에 정리한 표준 쓰기 프로세스가 효율적이고 낮은 지연을 보장하는 반면, Kafka Streams는 내결함성과 상태 복구 가능성을 보장하기 위한 추가 메커니즘을 추가한다. 
* 이러한 메커니즘은 장애가 발생하더라도 상태를 복구하고 데이터 손실 없이 처리를 계속할 수 있도록 보장한다.
* 내결함성 쓰기 프로세스는 표준 RocksDB 쓰기 메커니즘 위에 구축되어 있다.
* Kafka Streams는 RocksDB의 로컬 저장소(memtable과 SST 파일을 통해)에 쓰는 것 외에도, 상태가 지속 가능하고 복구 가능하도록 다음과 같은 단계를 추가한다.

Fault-Tolerant Write Process:

1. 데이터는 RocksDB 상태 저장소에 기록된다. 

* 레코드가 처리될 때, 이는 RocksDB의 표준 쓰기 과정을 따른다.

2. 상태 변경은 changelog topic에 기록된다.

* Kafka Streams는 상태 변경 사항을 비동기적으로 Kafka changelog topic에 복제한다.
* 이렇게 하면 RocksDB가 손실되었을 때, Kafka에서 상태를 재구성할 수 있다.

3. 오프셋은 오프셋 토픽에 기록된다.

* Kafka Streams가 처리하는 레코드에 해당하는 오프셋은 Kafka의 Consumer Offset에 저장된다.
* 이를 통해 카프카는 어떤 기록이 성공적으로 처리되었는지 추적할 수 있다.

4. 오프셋은 체크포인트 파일에 기록된다.

* Kafka Streams는 또한 최신 처리된 오프셋을 디스크의 로컬 체크포인트 파일에 쓴다.
* 이 파일은 Kafka Streams가 실패 후 로컬 저장소(RocksDB)에서 상태를 빠르게 복구할 수 있도록 한다.
