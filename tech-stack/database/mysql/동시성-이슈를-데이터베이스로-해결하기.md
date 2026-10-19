# MySQL ‐ Solving Concurrency Problems using Database-Level Locking

### Pessimistic Lock(비관적 락)

* 실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법이다.
* Exclusive Lock을 걸게 되면 다른 트랜잭션에서는 Lock이 해제되기전에 데이터를 가져갈 수 없게 된다.
* 데드락이 걸릴 수 있기 때문에 주의해서 사용해야 합니다.

### Optimistic Lock(낙관적 락)

* 실제로 Lock을 이용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법이다.
* 먼저 데이터를 읽은 후에 update를 수행할 때, 현재 내가 읽은 버전이 맞는지 확인하여 업데이트 한다.
* 내가 읽은 버전에서 수정사항이 생겼을 경우에는 application에서 다시 읽은 후에 작업을 수행해야 한다.

### Named Lock(네임드 락)

* 이름을 가진 Metadata Locking 이다.
* 이름을 가진 lock을 획득한 후 해제할 때까지 다른 세션은 이 lock을 획득할 수 없도록 한다.
* 주의할 점으로는 Transaction이 종료될 때, lock이 자동으로 해제되지 않는다.
* 별도의 명령어로 해제를 수행해주거나 선점시간이 끝나야 해제가 된다.

### 기타

* [MySQL - locking functions](https://dev.mysql.com/doc/refman/8.0/en/locking-functions.html)
* [MySQL - metadata locking](https://dev.mysql.com/doc/refman/8.0/en/metadata-locking.html)

### 사용 기준에 대한 질의 응답 정리

* 낙관적 락과 비관적 락을 사용하는 기준은 "동시에 수정을 하는 일이 빈번하게 일어나는가?" 이다.
* 동시에 수정을 하는 일이 적다면 낙관적 락을, 동시에 수정하는 상황이 빈번하게 발생한다면 비관적 락을 사용한다.
* 예를 들어, 1000명의 사용자가 동시에 A라는 상품을 구매하려고 할 때, 충돌이 빈번하게 일어난다고 할 수 있다.
* 반면 1000명의 사용자가 A라는 상품을 구매하지만 구매 시간은 다르다고 할 때, 충돌이 비교적 적게 일어난다고 할 수 있다.
