# MySQL - DeadLock Case

### MySQL - DeadLock Case

#### DeadLock이란?

* 두 개 이상의 트랜잭션이 서로가 쥐고 있는 락(Lock)을 차지하기 위해 무한정 기다리는 상태를 말한다.
* MySQL(InnoDB)은 데드락을 감지하면, 서비스 전체가 멈추는 것을 막기 위해 얽혀있는 트랜잭션 중 희생자(Victim)를 하나 골라 강제로 롤백(Rollback)시켜 버리고 에러(Deadlock found when trying to get lock; try restarting transaction)를 뱉어낸다.

#### 서로 다른 자원을 교차하여 요청할 때

```sql
-- [트랜잭션 A] A의 계좌에서 출금 (id=1 레코드에 X-Lock 획득)
UPDATE accounts SET balance = balance - 100 WHERE id = 1;

-- [트랜잭션 B] B의 계좌에서 출금 (id=2 레코드에 X-Lock 획득)
UPDATE accounts SET balance = balance - 100 WHERE id = 2;

-- [트랜잭션 A] B의 계좌로 입금 시도 
-- (id=2 레코드가 필요하지만, 트랜잭션 B가 X-Lock을 쥐고 있어 대기 상태 진입)
UPDATE accounts SET balance = balance + 100 WHERE id = 2;

-- [트랜잭션 B] A의 계좌로 입금 시도
-- (id=1 레코드가 필요하지만, 트랜잭션 A가 X-Lock을 쥐고 있어 대기 상태 진입)
UPDATE accounts SET balance = balance + 100 WHERE id = 1; 

-- 💥 결과: DEADLOCK 발생! 서로 영원히 기다리게 됩니다.
```
* 두 트랜잭션이 여러 테이블(혹은 여러 행)을 업데이트할 때, 접근하는 순서가 엇갈리면 발생한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e0fd0e0c-b351-4a30-a920-68fe570e132d.png)

#### 읽기 락(S-Lock)에서 쓰기 락(X-Lock)으로 승급 시

* 상품의 재고를 차감하거나, 선착순 이벤트를 처리할 때 일단 재고를 읽어오고(SELECT), 남았으면 차감하라(UPDATE)는 비즈니스 로직을 짜면 100% 발생하는 데드락이다.

```sql
-- [트랜잭션 A] 재고 확인을 위해 S-Lock(공유 락) 획득
SELECT stock FROM products WHERE id = 99 FOR SHARE; 

-- [트랜잭션 B] B도 동시에 재고 확인. (S-Lock끼리는 호환되므로 B도 S-Lock 획득 성공)
SELECT stock FROM products WHERE id = 99 FOR SHARE;

-- [트랜잭션 A] 재고가 1개 있음을 확인하고, 차감(UPDATE)을 위해 X-Lock(배타 락) 요청
-- (X-Lock을 얻으려면 다른 모든 S-Lock이 풀려야 함. 트랜잭션 B의 S-Lock 때문에 대기 상태 진입)
UPDATE products SET stock = stock - 1 WHERE id = 99;

-- [트랜잭션 B] B도 재고를 차감하기 위해 X-Lock 요청
-- (트랜잭션 A가 쥐고 있는 S-Lock 때문에 대기 상태 진입)
UPDATE products SET stock = stock - 1 WHERE id = 99;

-- 💥 결과: DEADLOCK 발생! 둘 다 S-Lock을 쥔 채로 상대방이 놓아주기만을 기다립니다.
```
* 이후에 `UPDATE`를 할 목적이라면 처음부터 `SELECT`를 할 때 S-Lock을 쓰지 말고 `FOR UPDATE`를 사용한 X-Lock을 걸면 데드락이 발생하지 않는다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/551c036a-e25c-4f35-9c30-dc83dc0e718c.png)
