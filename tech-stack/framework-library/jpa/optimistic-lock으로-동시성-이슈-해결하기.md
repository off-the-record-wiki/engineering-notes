# JPA ‐ Solving Concurrency Issues with Optimistic Locking

## 📚 Optimistic Lock 소스 코드(낙관적 락)

```java
@Getter
@Entity
@NoArgsConstructor
public class Stock {

        // ...

	// 낙관적 락을 사용하기 위한 버전 필드 추가
	@Version
	private Long version;

        // ...
}
```

```java
public interface StockRepository extends JpaRepository<Stock, Long> {

	@Lock(LockModeType.OPTIMISTIC)
	@Query("select s from Stock s where s.id = :id")
	Stock findByIdWithOptimisticLock(Long id);
}

```

```java
@Component
@RequiredArgsConstructor
public class OptimisticLockStockFacade {

	private final OptimisticLockStockService optimisticLockStockService;

	public void decrease(Long productId, Long quantity) throws InterruptedException {
		while (true) {
			try {
				// 재고 감소 로직을 실행
				optimisticLockStockService.decrease(productId, quantity);
				break;
			} catch (Exception e) {
				// Version 필드의 값이 일치하지 않는 경우 50ms 뒤에 다시 재요청을 보낸다.
				Thread.sleep(50);
			}
		}
	}
}
```
```java
@Test
@DisplayName("동시에 여러 요청을 통한 상품 주문시 요청만큼의 재고 감소가 이루어진다. - 낙관적 락 도입")
void 동시에_여러_요청을_통한_상품_주문시_요청만큼의_재고_감소가_이루어진다() throws InterruptedException {
	int threadCount = 100;
	ExecutorService executorService = Executors.newFixedThreadPool(32);
	CountDownLatch latch = new CountDownLatch(threadCount);

	for (int i = 0; i < threadCount; i++) {
		executorService.submit(() -> {
			try {
				stockFacade.decrease(1L, 1L);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				latch.countDown();
			}
		});
	}

	latch.await();
	Stock stock = stockRepository.findById(1L).orElseThrow();

	/*
	 * 낙관적 락을 사용하는 경우 -> @Lock(LockModeType.OPTIMISTIC) & @Version 사용
	 */
	assertThat(stock.getQuantity()).isEqualTo(0L);
}
```

## 📚 JPA 낙관적 락

* JPA가 제공하는 낙관적 락은 버전을 사용한다.
* 따라서 낙관적 락을 사용하려면 버전이 있어야 한다.
* 낙관적 락은 트랜잭션을 커밋하는 시점에 충돌을 알 수 있다는 특징이 있다.
* 낙관적 락에서 발생할 수 있는 예외는 다음과 같다.

```
- javax.persistence.OptimisticLockException(JPA 예외)
- org.hibernate.StaleObjectStateException(하이버네이트 예외)
- org.springframework.orm.ObjectOptimisticLockFailureException(스프링 예외 추상화)
```

### [ NONE ]

* 락 옵션을 적용하지 않아도 엔티티에 `@Version` 어노테이션이 적용된 필드만 있으면 낙관적 락이 적용된다.
* 용도 : 조회한 엔티티를 수정할 때, 다른 트랜잭션에 의해 변경되지 않아야 한다. 조회 시점부터 수정 시점까지를 보장한다.
* 동작 : 엔티티를 수정할 때, 버전을 체크하면서 버전을 증가한다. 이 때, 데이터베이스의 버전 값이 현재 버전이 아니면 예외가 발생한다.
* 이점 : 두 번의 갱신 분실 문제를 예방한다.

### [ OPTIMISTIC ]

* `@Version`만 적용했을 떄는 엔티티를 수정해야 버전을 체크하지만 이 옵션은 엔티티를 조회만 해도 버전을 체크한다.
* 한 번 조회한 엔티티는 트랜잭션을 종료할 때까지 다른 트랜잭션에서 변경되지 않음을 보장한다.
* 용도 : 조회한 엔티티는 트랜잭션이 끝날 때까지 다른 트랜잭션에 의해 변경되지 않아야 한다. 조회 시점부터 트랜잭션이 끝날 때까지 조회한 엔티티가 변경되지 않음을 보장한다.
* 동작 : 트랜잭션을 커밋할 때 버전 정보를 조회해서 현재 엔티티의 버전과 같은지 검증한다. 만약 같지 않다면 예외가 발생한다.
* 이점 : OPTIMISTIC 옵션은 DIRTY READ와 NON-REPEATABLE READ를 방지한다.

### [ OPTIMISTIC_FORCE_INCREMENT ]

* 낙관적 락을 사용하면서 버전 정보를 강제로 증가시킨다.
* 용도 : 논리적인 단위의 엔티티 묶음을 관리할 수 있다.(일대다, 다대일의 양방향 연관관계)
* 동작 : 엔티티를 수정하지 않아도 트랜잭션을 커밋할 때, UPDATE 쿼리를 사용해서 버전 정보를 강제로 증가시킨다. 이 때, 데이터베이스 버전이 엔티티 버전과 다르면 예외가 발생한다. 추가로 엔티티를 수정하면 수정 시 버전 UPDATE가 발생한다. 따라서 총 2번의 버전 증가가 나타날 수 있다.
* 이점 : 강제로 버전을 관리해서 논리적 단위의 엔티티 묶음을 버전 관리할 수 있다.
