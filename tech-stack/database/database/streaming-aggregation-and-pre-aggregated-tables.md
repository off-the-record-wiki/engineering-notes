# Database - Streaming Aggregation and Pre-aggregated Tables

#### 인덱스 정렬 대신 스트리밍 집계는?

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/67885e23-db40-4712-82d6-58b4fa257ba7.png)

* MySQL이 `GROUP BY`를 처리하는 방식은 크게 3가지로 나뉜다.

| 방식 | 조건 | 메모리 | EXPLAIN 표시 |
| --- | --- | --- | --- |
| 임시 테이블 | 입력이 정렬돼 있지 않음 | 그룹 수에 비례 | `Aggregate using temporary table` |
| 스트리밍 | 입력이 그룹 키 순으로 정렬됨 | 그룹 하나 분량 | `Group aggregate: ...` |
| 느슨한 인덱스 스캔 | 인덱스 선두 컬럼 + MIN/MAX·DISTINCT류 | 거의 없음 | `Using index for group-by` |

* 입력이 그룹 키 순으로 들어오면 같은 그룹의 행에 반드시 연속한다. 지금 그룹의 누적값만 들고 있다가 키가 바뀌는 순간 결과를 흘려보내고 상태를 초기화한다.
* 반면 정렬이 안 돼 있으면 같은 값의 행이 마지막 행에도 나올 수 있어서 전부 다 읽을 때까지 모든 그룹의 상태를 들고 있어야 한다. 그래서 임시 테이블이 필요하다.
