# PostgreSQL - Transactions, Concurrency & Why PostgreSQL in the AI Era

#### WAL(Write-Ahead Logging, 선행 기록 로그)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d9be4da2-76e7-4895-9ad5-441a3761af75.png)

* 데이터 파일을 고치기 전에 그 변경 내용을 로그에 먼저 기록하고, 이 로그를 디스크에 확실히 써야만 커밋을 완료로 인정하는 방식.
* 데이터 페이지가 디스크에 쓰이기 전에 그 페이지의 변경 기록이 반드시 먼저 디스크의 WAL에 쓰여 있어야 한다.
* 지속성(D) : 커밋할 때 WAL만 디스크에 강제로 기록한다. 서버가 죽어도 재시작할 때 마지막 체크포인트 이후 WAL을 재생해서 커밋된 변경을 되살린다.
* 성능 : WAL은 파일 끝에 이어 쓰기만 하는 순차 쓰기라 빠르다. 반면 데이터 파일은 흩어진 페이지를 고치는 랜덤 쓰기라 느리다. 그래서 데이터 파일 쓰기는 체크포인트 때 모아서 나중에 처리한다.

#### 이상 현상 - Dirty Read

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/77c7c62d-8083-45ec-bd7c-5e0e613ed73e.png)

* 다른 트랜잭션이 아직 커밋하지 않은 값을 읽는 현상. 그 트랜잭션이 롤백하면 존재한 적 없는 값을 근거로 삼은 것이다.
* PostgreSQL에서는 Read Uncommitted를 지정해도 Read Committed로 동작하므로 어떤 격리 수준에서도 Dirty Read가 발생하지 않는다.

#### 이상 현상 - Non-Repeatable Read

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3b8b714f-e010-4181-a0df-3f3e52062a04.png)

* 한 트랜잭션 안에서 같은 행을 2번 읽었는데 값이 다른 현상. 그 사이에 다른 트랜잭션이 그 행을 고치고 커밋했기 때문이다.
* PostgreSQL의 기본 격리 수준인 Read Committed는 쿼리마다 새 스냅샷을 찍기 때문에 이 현상이 발생하며, Repeatable Read부터는 트랜잭션 동안 같은 스냅샷을 유지해 발생하지 않는다.

#### 이상 현상 - Phantom Read

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/714075f2-a34e-4a30-bf63-2c1a8baa8c64.png)

* 한 트랜잭션 안에서 같은 조건으로 2번 조회했는데 행 수가 다른 현상. 기존 행이 바뀐 것이 아니라 조건에 맞는 새로운 행이 생겼기 때문이다.
* SQL 표준은 Serializable에서만 이 현상을 막도록 정하지만, PostgreSQL은 스냅샷 방식 덕분에 Repeatable Read에서부터 발생하지 않는다.

#### 격리 수준 - Read Committed(PostgreSQL 기준 기본)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/49ec9fad-9846-42ec-93bf-f2d3a8d61aeb.png)

* Read Committed는 쿼리를 실행할 때마다 스냅샷을 새로 찍는다. 그래서 각 쿼리는 그 쿼리가 시작되기 직전까지 커밋된 데이터만 본다.
* 동시 UPDATE : 두 트랜잭션이 같은 행을 UPDATE하면 나중에 온 쪽이 먼저 잠근 쪽이 끝날 때까지 기다린다. 그 뒤 최신 값을 기준으로 조건과 계산을 다시 적용한다. 그래서 `balance = balance - 100`처럼 SQL 식 안에서 계산하면 안전하다.

#### 격리 수준 - Repeatable Read

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/016e1c3b-be8b-472b-985a-f8b550d423d2.png)

* Repeatable Read는 트랜잭션 첫 쿼리에서 스냅샷을 한 번 찍고 트랜잭션이 끝날 때까지 그 스냅샷을 그대로 쓴다.
* 트랜잭션 A에서 스냅샷을 찍은 이후 만약 트랜잭션 B에서 고치고 커밋한 행을 수정하면 PostgreSQL은 `could not serialize access due to concurrent update(40001)`이 발생한다.
* 이 오류가 발생하면 트랜잭션 전체가 실패하므로 애플리케이션이 트랜잭션을 처음부터 다시 실행하는 재시도 로직을 갖춰야 한다.

#### 격리 수준 - Serializable

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/df470d52-b3bf-48b7-befa-90f7df6ac588.png)

* 동시에 실행된 트랜잭션들의 결과가 어떤 순서로 하나씩 실행한 결과와 같을 때만 커밋을 허용하는 실행 방식은 느슨한데 결과에 대해서는 엄격한 방식이다.
* 충돌이 생기면 기다리는 대신 `could not serialize access(40001)` 오류가 발생하므로, 애플리케이션이 트랜잭션을 처음부터 다시 실행하는 재시도 로직을 반드시 갖춰야 한다.

#### Database Lock 교착상태와 읽기/쓰기 성능을 보장하는 MVCC 모델

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e3fc3e52-0a1c-49ef-9d31-6571147cc1de.png)

```
A: 계좌 1 잠금 → 계좌 2 요청 (B가 쥐고 있어 대기)
B: 계좌 2 잠금 → 계좌 1 요청 (A가 쥐고 있어 대기)
→ 서로가 서로를 기다리는 순환이라 영원히 끝나지 않음
```
* 두 건이 동시에 들어오는 경우이다.
* PostgreSQL의 처리 : 잠금을 `deadlock_timeout(기본 1초)` 이상 기다리면 순환이 있는지 검사한다. 순환을 찾으면 한쪽 트랜잭션을 `deadlock detected(40P01)`로 롤백한다.
* 항상 같은 순서로 잠근다. 혹은 트랜잭션을 짧게 유지해 잠금을 오래 쥐지 않도록 한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c50f1bb4-dd8d-491f-9659-c79053aafdb9.png)

* UPDATE는 행을 덮어쓰지 않고 새로운 버전을 만든다. 기존 T1 버전은 그대로 두고 T2 버전을 새로 만든다.
* 읽기는 잠금 없이 자기 스냅샷에 맞는 버전을 고른다. 누군가 그 행을 고치더라도 기다리지 않고 T1 버전을 읽는다.
  * Read Committed : T2가 커밋된 뒤 새로 실행한 쿼리부터 T2를 바라본다.
  * Repeatable Read : 트랜잭션이 끝날 때까지 계속 T1 스냅샷을 바라본다.
* 결국 오래 열려 있는 트랜잭션이 있으면 그 트랜잭션이 위와 같이 이전 버전을 볼 수도 있어서 VACUUM이 치우지 못하고 테이블이 계속 커지게 된다. 그래서 여러 행을 잠그는 순서를 통일해야 하고, 다른 하나는 트랜잭션을 오래 열어두지 않는 것이다.

#### pgvector와 벡터저장, 유사도 검색 패턴

![](https://github.com/user-attachments/assets/08ff00da-f94b-4f87-8ae9-a77f07d91dda)

* 임베딩 모델은 텍스트틀 고정 길이 숫자 벡터로 바꾼다.
* 뜻이 비슷하면 벡터도 가깝다. 예를 들어, "카드 결제"와 "신용카드 납부"는 겹치는 글자가 "카드"뿐인데도 값이 거의 같다. "부동산 매매"와는 값이 전혀 다르다.
* 그래서 문자열 일치(LIKE, n-gram)로는 못 잡는 "같은 뜻, 다른 표현"을 코사인 유사도 같은 거리 계산으로 찾을 수 있다.
* 벡터를 별도 벡터 DB에 두지 않고 PostgreSQL에 pgvector 확장을 붙여 원본 행과 같은 테이블에 저장한다. 그러면 "의미가 비슷한 것"과 조건을 결합해 쿼리 하나로 처리한다.(벡터 검색 + JOIN + JSONB)
* 두 저장소 사이의 동기화 파이프라인이 필요 없고 트랜잭션 정합성이 보장되며 운영할 시스템이 하나 줄어든다.
* 그러나 ANN 인덱스(HNSW, IVFFlat)에 필터를 같이 걸게 되면 후필터링 때문에 결과가 덜 나올 수 있고 행이 쓰일 때마다 임베딩을 생성하는 비용을 감수해야 한다. 데이터가 커지면 OLTP DB가 검색 부하까지 떠안는다.

```sql
CREATE TABLE doc_embeddings (
    id          SERIAL PRIMARY KEY,
    content     TEXT,
    embedding   VECTOR(8) -- 8차원 벡터를 저장
);

-- 1. 코사인 유사도 : 두 벡터가 같은 방향을 가리키는지를 측정

SELECT
    content,
    embedding <=> '[0.82, 0.14, 0.63, 0.45, 0.71, 0.28, 0.56, 0.39]' AS 코사인거리
FROM doc_embeddings
ORDER BY 코사인거리
LIMIT 5;

-- 2. L2 : 방향이 아니라 직선 거리로 유사도를 측정
-- → 두 벡터 사이의 유클리드 거리 → 벡터 공간에서의 직선 거리 계산

SELECT
    content,
    embedding <-> '[0.55, 0.72, 0.38, 0.61, 0.29, 0.84, 0.17, 0.66]' AS L2거리
FROM doc_embeddings
ORDER BY L2거리
LIMIT 5;

-- 3. 내적도 : 벡터의 방향과 크기 모두 반영하는 형태

SELECT
    content,
    embedding <#> '[0.31, 0.89, 0.12, 0.77, 0.08, 0.95, 0.24, 0.51]' AS 내적거리
FROM doc_embeddings
ORDER BY 내적거리
LIMIT 5;
```

#### 벡터

![](https://github.com/user-attachments/assets/af7f7736-2337-4f31-9293-6e6b8b990a86)

* 벡터를 대상의 특징을 정해진 개수의 숫자로 나열해 공간 위의 한 점(방향)으로 나타낸 것이고 그래서 두 대상이 얼마나 비슷한지를 거리로 잴 수 있게 한다. 임베딩 벡터는 임베딩 모델이 문장의 뜻을 고쳐 고정 길이 숫자 배열로 옮긴 좌표이고 뜻이 가까운 문장일수록 좌표도 가깝다.
