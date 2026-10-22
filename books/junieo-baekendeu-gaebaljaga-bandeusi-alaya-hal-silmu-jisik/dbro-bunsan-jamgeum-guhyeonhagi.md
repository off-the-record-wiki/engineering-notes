# 주니어 백엔드 개발자가 반드시 알아야 할 실무 지식 ‐ DB로 분산 잠금 구현하기

#### 잠금 정보 저장 테이블 설계

```sql
CREATE TABLE distributed_lock (
    lock_key      VARCHAR(255) NOT NULL,           -- 잠글 대상 식별자 (예: "order:1234")
    owner_id      VARCHAR(128) NOT NULL,           -- 잠금 보유자 (인스턴스ID + 스레드ID 등)
    fence_token   BIGINT       NOT NULL,           -- 펜싱 토큰 (단조 증가, 좀비 방지)
    acquired_at   DATETIME(3)  NOT NULL,           -- 획득 시각
    expires_at    DATETIME(3)  NOT NULL,           -- 만료 시각 (TTL, 데드락 방지)
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (lock_key)
);
```

* `lock_key(PK)` : 잠금의 핵심. 유니크 제약이 곧 잠금 그 자체가 된다. 두 프로세스가 동시에 같은 키로 INSERT하면 하나만 성공하고 나머지는 Unique Constraint 제약으로 인해 실패한다.
* `owner_id` : 누가 잡고 있는지. 해제할 때 "내가 잡은 락만 내가 푼다"를 보장하는 데 필수이다. 만약 이게 없으면 A의 락이 만료된 뒤 B가 잡았는데 A가 늦게 DELETE를 하면 B의 락이 날아가는 사고가 발생한다.
* `expires_at(TTL)` : 락을 잡은 프로세스가 죽어버리면 락이 영원히 안 풀려 데드락이 된다. 만료 시각을 두고 지난 락은 다른 프로세스가 뺏을 수 있게 해야 한다.
* `fence_token` : 좀비 프로세스 방지. A가 락을 잡고 GC나 네트워크 지연으로 멈춘 사이 TTL이 만료돼 B가 락을 잡은 상태에서 A가 뒤늦게 구동되어 작업하면 둘 다 임계영역에 들어가버린다. 그래서 락을 걸 때마다 증가하는 토큰을 발급하고 실제 자원이 더 큰 토큰만 수용하게 하면 뒤늦게 구동된 A의 쓰기를 거부할 수 있다.

```sql
-- 획득: INSERT 성공하면 락 획득. 중복 키 에러면 실패.
--       단, 만료된 락은 뺏을 수 있어야 하므로 UPSERT로 처리
INSERT INTO distributed_lock (lock_key, owner_id, fence_token, acquired_at, expires_at)
VALUES ('order:1234', 'inst-A#t1', :token, NOW(3), NOW(3) + INTERVAL 30 SECOND)
ON DUPLICATE KEY UPDATE
    owner_id    = IF(expires_at < NOW(3), VALUES(owner_id), owner_id),
    fence_token = IF(expires_at < NOW(3), VALUES(fence_token), fence_token),
    acquired_at = IF(expires_at < NOW(3), VALUES(acquired_at), acquired_at),
    expires_at  = IF(expires_at < NOW(3), VALUES(expires_at), expires_at);
-- 이후 owner_id가 내 값인지 SELECT로 확인해서 획득 성공 여부 판정

-- 해제: 반드시 owner_id 조건을 걸어 "내 락만" 삭제
DELETE FROM distributed_lock WHERE lock_key = 'order:1234' AND owner_id = 'inst-A#t1';

-- 갱신(long task 중 TTL 연장):
UPDATE distributed_lock SET expires_at = NOW(3) + INTERVAL 30 SECOND
WHERE lock_key = 'order:1234' AND owner_id = 'inst-A#t1';
```

#### 분산 잠금 동작 구현

***

1. 트랜잭션 시작
2. 선점 잠금 쿼리를 이용해 해당 행을 점유한다
3. 행이 없으면 잠금 테이블에 새로운 데이터 추가
4. `owner`가 다른데 아직 `expiry`가 안 지났다면 잠금 획득 실패
5. `owner`가 다른데 `expiry`가 지났다면 `owner`와 `expiry` 값을 변경한 후 잠금 획득
6. `owner`가 같다면 `expiry`만 갱신한 후 잠금을 획득
7. 트랜잭션을 커밋하고 소유 결과 리턴
8. 트랜잭션 커밋에 실패하면 잠금 획득도 실패

***

#### 분산 잠금 구현 예제(feat. Java)

```java
/**
 * 분산 잠금 정보를 저장하는 {@code distributed_lock} 테이블에 대한 접근을 담당한다.
 *
 * <p>이 저장소는 잠금의 <b>상호배제</b>를 두 가지 DB 성질에 의존해 구현한다.
 * <ul>
 *   <li>{@code lock_key} 컬럼의 <b>유니크 제약</b> — 동시에 INSERT해도 하나만 성공한다.</li>
 *   <li>{@code SELECT ... FOR UPDATE}의 <b>행 단위 선점 잠금</b> — 같은 키에 대한
 *       동시 획득 시도를 직렬화하여 조건 분기를 원자적으로 수행한다.</li>
 * </ul>
 *
 * <p>모든 메서드는 호출하는 서비스의 트랜잭션 경계 안에서 실행되어야 한다.
 */
@Repository
@RequiredArgsConstructor
public class DistributedLockRepository {

    private final JdbcTemplate jdbc;

    /**
     * 주어진 키의 잠금 행을 <b>선점 잠금(FOR UPDATE)</b>으로 조회한다.
     *
     * <p>행이 존재하면 이 트랜잭션이 커밋/롤백될 때까지 다른 트랜잭션의 동일 행
     * 접근을 차단한다. 이 차단 덕분에 이후의 owner/expiry 판단이 경쟁 없이
     * 원자적으로 이뤄진다.
     *
     * @param lockKey 잠글 대상 식별자 (예: {@code "order:1234"})
     * @return 잠금 행. 아직 아무도 만든 적 없으면 {@code null}
     */
    public LockRow selectForUpdate(String lockKey) {
        List<LockRow> rows = jdbc.query(
            "SELECT lock_key, owner_id, fence_token, expires_at " +
            "FROM distributed_lock WHERE lock_key = ? FOR UPDATE",
            (rs, i) -> new LockRow(
                rs.getString("lock_key"),
                rs.getString("owner_id"),
                rs.getLong("fence_token"),
                rs.getTimestamp("expires_at").toLocalDateTime()),
            lockKey);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 잠금 행이 없을 때 새 행을 삽입하여 잠금을 신규 획득한다.
     *
     * <p>선점 잠금은 <b>존재하지 않는 행</b>을 잠글 수 없으므로, 두 트랜잭션이
     * 동시에 "행 없음"으로 판단하고 함께 INSERT를 시도할 수 있다. 이때
     * 유니크 제약이 최종 안전망 역할을 하며, 진 쪽은 {@link DuplicateKeyException}을
     * 받아 {@code false}로 처리된다.
     *
     * @param key    잠금 키
     * @param owner  잠금 보유자 식별자 (인스턴스ID + 스레드ID 등)
     * @param token  이번에 발급한 펜싱 토큰
     * @param expiry 만료 시각(TTL)
     * @return 삽입 성공 시 {@code true}, 유니크 충돌로 경쟁에서 패배하면 {@code false}
     */
    public boolean insert(String key, String owner, long token, LocalDateTime expiry) {
        try {
            jdbc.update(
                "INSERT INTO distributed_lock(lock_key, owner_id, fence_token, expires_at) " +
                "VALUES (?, ?, ?, ?)", key, owner, token, expiry);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /**
     * 기존 잠금 행의 소유자·펜싱 토큰·만료 시각을 갱신한다.
     *
     * <p>두 상황에서 호출된다.
     * <ul>
     *   <li>만료된 남의 락을 <b>뺏을 때</b> — owner와 token을 새 값으로 교체</li>
     *   <li>내 락의 TTL을 <b>연장할 때</b> — 기존 token을 유지한 채 expiry만 갱신</li>
     * </ul>
     *
     * @param key    잠금 키
     * @param owner  갱신 후 소유자
     * @param token  갱신 후 펜싱 토큰
     * @param expiry 새 만료 시각
     */
    public void update(String key, String owner, long token, LocalDateTime expiry) {
        jdbc.update(
            "UPDATE distributed_lock SET owner_id = ?, fence_token = ?, expires_at = ? " +
            "WHERE lock_key = ?", owner, token, expiry, key);
    }

    /**
     * 잠금을 해제한다. <b>반드시 {@code owner} 조건을 함께 걸어</b> 자신이 보유한
     * 락만 삭제한다.
     *
     * <p>owner 조건이 없으면, 내 락이 TTL 만료로 이미 다른 프로세스에게 넘어간 뒤
     * 뒤늦게 해제를 호출했을 때 <b>남의 락을 삭제</b>하는 사고가 발생한다.
     *
     * @param key   잠금 키
     * @param owner 해제를 시도하는 소유자 식별자
     * @return 삭제된 행 수. {@code 0}이면 이미 내 락이 아니었음을 의미
     */
    public int delete(String key, String owner) {
        return jdbc.update(
            "DELETE FROM distributed_lock WHERE lock_key = ? AND owner_id = ?", key, owner);
    }
}
```
```java
/**
 * DB 기반 분산 잠금의 획득·해제를 담당하는 서비스.
 *
 * <p>{@link #tryAcquire(String, String)}는 다음 알고리즘을 하나의 트랜잭션 안에서
 * 수행한다.
 * <ol>
 *   <li>트랜잭션 시작 ({@link Transactional})</li>
 *   <li>선점 잠금 쿼리로 대상 행 점유</li>
 *   <li>행이 없으면 INSERT로 신규 획득</li>
 *   <li>owner가 다르고 아직 만료 전이면 획득 실패</li>
 *   <li>owner가 다르지만 만료됐으면 소유자·만료를 교체하고 획득(뺏기)</li>
 *   <li>owner가 같으면 만료만 갱신하고 획득(재진입/heartbeat)</li>
 *   <li>정상 종료 시 커밋되어 결과가 확정</li>
 *   <li>커밋 실패(롤백) 시 획득도 실패로 간주</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final DistributedLockRepository repo;

    /** 잠금의 기본 유효 기간(TTL). 보유자가 죽어도 이 시간이 지나면 회수 가능하다. */
    private static final Duration TTL = Duration.ofSeconds(30);

    /**
     * 잠금 획득을 시도한다.
     *
     * <p>성공하면 이번에 유효한 <b>펜싱 토큰</b>을 반환한다. 호출자는 이 토큰을
     * 임계 영역 내 실제 자원 쓰기에 함께 전달하여, TTL 만료 후 뒤늦게 깨어난
     * 좀비 프로세스의 쓰기를 자원 쪽에서 거부하도록 해야 한다.
     *
     * @param lockKey 잠글 대상 식별자
     * @param owner   호출자 식별자. 인스턴스ID와 스레드ID를 조합해 전역적으로
     *                유일해야 한다. 재진입 판단의 기준이 된다.
     * @return 획득 성공 시 펜싱 토큰을 담은 {@link Optional}, 실패 시 빈 값
     */
    @Transactional
    public Optional<Long> tryAcquire(String lockKey, String owner) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiry = now.plus(TTL);

        LockRow row = repo.selectForUpdate(lockKey);          // 2

        if (row == null) {                                    // 3: 행 없음 → INSERT
            long token = nextToken();
            boolean ok = repo.insert(lockKey, owner, token, newExpiry);
            return ok ? Optional.of(token) : Optional.empty();
        }

        if (!row.owner().equals(owner)) {                     // owner 다름
            if (row.expiresAt().isAfter(now)) {
                return Optional.empty();                       // 4: 미만료 → 실패
            }
            long token = nextToken();                          // 5: 만료 → 뺏기
            repo.update(lockKey, owner, token, newExpiry);
            return Optional.of(token);
        }

        repo.update(lockKey, owner, row.fenceToken(), newExpiry); // 6: 재진입/heartbeat
        return Optional.of(row.fenceToken());
    }

    /**
     * 자신이 보유한 잠금을 해제한다. 이미 다른 소유자에게 넘어간 경우 아무 일도
     * 일어나지 않는다({@code owner} 조건으로 보호).
     *
     * @param lockKey 잠금 키
     * @param owner   해제를 시도하는 소유자 식별자
     */
    @Transactional
    public void release(String lockKey, String owner) {
        repo.delete(lockKey, owner);
    }

    /**
     * 새 펜싱 토큰을 발급한다.
     *
     * <p><b>단조 증가</b>가 핵심이다. 예시로 현재 밀리초를 쓰지만, 엄밀한 보장이
     * 필요하면 DB 시퀀스나 전용 카운터를 사용해야 한다.
     *
     * @return 이전에 발급한 어떤 토큰보다도 큰 값
     */
    private long nextToken() {
        return System.currentTimeMillis();
    }
}
```
