# Apache Kafka - Topic Segment Management

#### 메시지 로그 세그먼트의 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/be72cb14-1392-4d58-b4ac-383ee503d7e9.png)

* 카프카의 로그 메시지는 실제로 Segment로 저장된다.
* 파티션은 단순히 파일 디렉터리로만 되어 있고, 해당 파티션 디렉터리에 메시지 저장 Segment를 File로 가지고 있다.
* 파티션은 여러 개의 Segment로 구성되며 개별 Segment는 데이터 용량이 차거나 일정 시간이 경과하면 close되고 새로운 Segment를 생성해 데이터를 연속적으로 저장한다.
* Segment는 close되면 더 이상 브로커가 write하지 않으며 read-only가 된다. 
* 브로커는 여러 개의 Segment 중에 단 하나의 `Active Segment`에만 write와 read를 수행한다. 하나의 파티션은 단 하나의 `Active segment`를 가진다.

#### log.segment.bytes

* 개별 Segment의 최대 크기이며, 기본값은 1GB이다.
* 지정된 크기를 넘기면 해당 Segment는 더 이상 `Active Segment`가 아니고 close된다.(write는 안되고 read만 됨)
* Topic Config는 segment.bytes이며, 기본값은 `log.segments.bytes` 설정을 따른다.

#### log.roll.hours(ms)

* 개별 Segment가 유지되는 최대 시간이며 기본값은 7일이다.
* 지정된 시간을 넘기면 해당 Segment는 더 이상 `Active Segment`가 아니고 close된다.(write는 안되고 read만 됨)
* `log.segments.bytes`에 지정된 크기만큼 차지 않아도 `log.roll.ms`만큼의 시간이 지나면 해당 segment를 close한다.
* Topic Config는 segment.ms이며 기본값은 `log.roll.hours` 설정을 따른다.

#### 인덱스(Index)와 타임인덱스(TimeIndex) 세그먼트의 이해

* Topic을 생성하면 파티션 디렉터리 내에 메시지 내용을 가지는 `Segment`와 offset 위치 byte 정보를 가지는 `Index`, record 생성 시간에 따른 위치 byte 정보를 가지는 `TimeIndex` 파일로 구성된다.
* `Index` : offset별로 byte position 정보를 가지고 있다. 메시지 `Segment`는 File 기반이므로 특정 offset의 데이터를 파일에서 읽기 위해서 시작 File Pointer에서 얼마만큼의 byte에 위치해있는지를 알아야 한다.
* `TimeIndex` : 메시지의 생성 Unix 시간을 ms 단위로 가지고 있고 해당 생성 시간에 해당하는 offset 정보를 가진다.

#### 세그먼트의 생명 주기 관리 및 log.cleanup.policy의 삭제 설정 이해

* Segment 파일은 `Active` -> `Closed` -> `Deleted` 또는 `Compacted` 상태로 관리한다.
* Segment 파일은 Log Cleanup 정책에 따라 지정된 특정 시간이나 파일 크기에 따라 삭제되거나 `Compact`형태로 저장된다.
* 하나의 파티션에는 단 하나의 `Active Segment`만 존재한다.

#### Log Cleanup Policy

* 카프카 브로커는 오래된 메시지를 관리하기 위한 정책을 `log.cleanup.policy`로 설정한다.(Topic 레벨은 `cleanup.policy`)
* `log.cleanup.policy = delete`로 설정하면 segment를 `log.retention.hours`나 `log.retention.bytes` 설정 값에 따라 삭제된다.
* `log.cleanup.policy = compact`로 설정하면 segment를 key 값 레벨로 가장 최신의 메시지만 유지하도록 segment를 재구성한다.
* `log.cleanup.policy = [delete, compact]`로 설정하면 compact와 delete를 함께 적용한다.

#### log.retention.hours(ms)

* 개별 Segment가 삭제되기전 유지하는 시간으로 기본은 1주일이다.
* 크게 설정하면 오래된 Segment를 그대로 유지하므로 디스크 공간이 더 필요하다. 작게 설정하면 오래된 Segment를 조회할 수 없다.
* Topic Config는 `retention.ms`이며, 기본값은 `log.retention.ms`를 따른다.

#### log.retention.bytes

* Segment 삭제 조건이 되는 파티션 단위의 전체 파일의 크기를 설정한다. 기본값은 -1로 무한대이다.
* 적정한 디스크 공간 사용량을 제약하기 위해 보통 설정한다.
* Topic Config는 `retention.bytes`이며, 기본값은 `log.retention.bytes`를 따른다. 

#### log.retention.check.interval.ms

* 브로커가 background로 Segment 삭제 대상을 찾기 위한 ms 단위의 주기를 말한다.

#### Log Compaction

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fd99fcdc-6ce5-414a-acc3-37424aa0005e.png)

* `log.cleanup.policy=compact`로 설정 시 Segment의 Key 값에 따라 가장 최신의 메시지로만 compact하게 Segment를 재구성한다.
* Key 값이 null인 메시지에는 적용할 수 없다.
* 백그라운드 쓰레드 방식으로 별도의 I/O 작업을 수행하므로 추가적인 I/O 부하가 소모된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/41d6f97f-2473-488c-a1ca-518b63808ab5.png)

* `Active Segment`는 Compact 대상에서 제외된다.
* Compaction은 파티션 레벨에서 수행 결정이 되며, 개별 세그먼트들을 새로운 세그먼트들로 재생성한다.
* Log Compaction을 수행하면 메시지의 순서는 여전히 유지되며 offset은 변하지 않는다.
* Consumer는 메시지의 가장 최신값을 읽는다.(단, Compact 기반으로 로그가 재생될 시 Key 값은 있으나 Value 값을 가지는 메시지는 일정 시간 이후에 삭제되기 때문에 읽지 못할 수 있다.)
