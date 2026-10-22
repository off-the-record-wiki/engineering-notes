# MySQL ‐ Skip Locked for Session

#### SKIP LOCKED

* 잠긴 행을 마주쳤을 때 3가지 선택지가 있다.
* `SELECT ... FOR UPDATE`로 행을 잠그려는데 그 행이 이미 다른 트랜잭션에 잠겨 있다면, 어떻게 반응할지 정할 수 있다.

| 옵션 | 잠긴 행을 만나면 |
|------|----------------|
| (기본) | 락이 풀릴 때까지 무한정 대기 (`innodb_lock_wait_timeout`까지) |
| `NOWAIT` | 기다리지 않고 즉시 에러 반환 |
| `SKIP LOCKED` | 잠긴 행은 조용히 건너뛰고, 안 잠긴 행만 반환 |

> **왜 필요한가? - 작업 큐 / 워커 풀**
> * 여러 워커(스레드·서버)가 하나의 테이블을 큐처럼 쓰면서 각자 다른 작업을 하나씩 집어가야 하는 상황이 대표적이다.

#### 기본 동작(SKIP LOCKED 없이) — 병렬성이 무너진다

* 워커 여러 개가 전부 "가장 오래된 작업 1건"을 FOR UPDATE로 잠그려 한다.
* 1개만 잠금에 성공하고 나머지는 그 락이 풀릴 때까지 줄 서서 대기한다.
* 결과적으로 워커를 늘려도 사실상 순차 처리가 되어 버린다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b01a8dab-efed-4b83-a0d4-a1ceba226581.png)

#### SKIP LOCKED 사용 — 경합 없이 병렬 처리

* 워커 A가 1번을 잠그면, 워커 B는 1번을 건너뛰고 2번을 잠근다. 대기가 없다.
* 워커 수만큼 서로 다른 작업을 동시에 처리할 수 있다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c50bd608-1c06-448d-9646-bdd373f3329e.png)

```sql
-- 사용 예시 : 워커 한 사이클
START TRANSACTION;

-- 1) 처리할 작업 1건을 집으면서 잠금 (남이 잠근 건 건너뜀)
SELECT id FROM jobs
WHERE status = 'ready'
ORDER BY id
LIMIT 1
FOR UPDATE SKIP LOCKED;

-- 2) 애플리케이션에서 실제 작업 수행 후 상태 변경
UPDATE jobs SET status = 'done' WHERE id = :picked_id;

COMMIT; -- 여기서 락 해제
```

#### 주의할 점

* 정확한 조회가 아니다 : 잠긴 행을 빼고 주므로 그 순간 결과는 "전체"가 아니다. 재고 합계·리포트처럼 정합성이 중요한 조회에는 쓰면 안 되고, "큐에서 하나 집어가기" 용도 전용이다.
* 반드시 잠금 읽기와 함께 : `FOR UPDATE` 또는 `FOR SHARE`에만 붙는다. 일반 SELECT에는 의미가 없다.
* 트랜잭션 안에서 : 집은 행의 락을 유지하려면 트랜잭션으로 감싸고, 작업 완료 후 상태를 바꾸고 COMMIT 해야 한다.
* 순서 보장 주의 : `ORDER BY`로 정렬해도 앞 행이 잠겨 있으면 건너뛰므로 "무조건 가장 오래된 것부터"가 항상 지켜지지는 않는다.
