# JPA ‐ Solving Concurrency Issues with Pessimistic Locking

## 📚 Pessimistic Lock 소스 코드(비관적 락)

```java
@Service
@RequiredArgsConstructor
public class PessimisticLockStockService {

	private final StockRepository stockRepository;

	// 재고 감소 로직
	/*
	 * 비관적 락 적용
	 * 하나의 쓰레드가 락을 획득하고 작업을 수행하는 동안 다른 쓰레드는 락을 획득하려고 시도하나 락을 획득하지 못하고 대기 상태가 된다.
	 */
	@Transactional
	public void decrease(Long productId, Long quantity) {
		Stock stock = stockRepository.findByIdWithPessimisticLock(productId);
		stock.decrease(quantity);
		stockRepository.saveAndFlush(stock);
	}
}
```

```java
public interface StockRepository extends JpaRepository<Stock, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Stock s where s.id = :id")
	Stock findByIdWithPessimisticLock(Long id);
}
```

```java
@Test
@DisplayName("동시에 여러 요청을 통한 상품 주문시 요청만큼의 재고 감소가 이루어진다. - 비관적 락 도입")
void 동시에_여러_요청을_통한_상품_주문시_요청만큼의_재고_감소가_이루어진다() throws InterruptedException {
	int threadCount = 100;
	ExecutorService executorService = Executors.newFixedThreadPool(32);
	CountDownLatch latch = new CountDownLatch(threadCount);

	for (int i = 0; i < threadCount; i++) {
		executorService.submit(() -> {
			try {
				stockService.decrease(1L, 1L);
			} finally {
				latch.countDown();
			}
		});
	}

	latch.await();
	Stock stock = stockRepository.findById(1L).orElseThrow();

	/*
	 * 비관적 락을 사용하는 경우 -> @Lock(LockModeType.PESSIMISTIC_WRITE)
	 */
	assertThat(stock.getQuantity()).isEqualTo(0L);
}
```

## 📚 JPA 비관적 락

* JPA가 제공하는 비관적 락은 데이터베이스 트랜잭션 락 메커니즘에 의존하는 방법이다.
* 주로 SQL 쿼리에 `select for update` 구문을 사용하면서 시작하고 버전 정보는 사용하지 않는다.
* 비관적 락은 주로 `PESSIMISTIC_WRITE` 락 모드를 사용한다.
* 비관적 락은 다음과 같은 특징이 있다.

```
- 엔티티가 아닌 스칼라 타입을 조회할 때도 사용할 수 있다.
- 데이터를 수정하는 즉시 트랜잭션 충돌을 감지할 수 있다.
```

* 비관적 락에서 발생하는 예외는 다음과 같다.

```
- javax.persistence.PessimisticLockException(JPA 예외)
- org.springframework.dao.PessimisticLockingFailureException(스프링 예외 추상화)
```

### [ PESSIMISTIC_WRITE ]

* 비관적 락이라 하면 일반적으로 이 옵션을 뜻한다.
* 데이터베이스에 쓰기 락을 걸 때 사용한다.
* 용도 : 데이터베이스에 쓰기 락을 건다.
* 동작 : 데이터베이스 select for update문을 사용해서 락을 건다.
* 이점 : NON_REPEATABLE_READ를 방지한다. 락이 걸린 로우는 다른 트랜잭션이 수정할 수 없다.

### [ PESSIMISTIC_READ ]

* 데이터를 반복 읽기만 하고 수정하지 않는 용도로 락을 걸 때 사용한다.
* 일반적으로 잘 사용하지 않는다.
* 데이터베이스 대부분은 방언에 의해 PESSMISTIC_WRITE으로 동작한다.

### [ PESSIMISTIC_FORCE_INCREMENT ]

* 비관적 락 중에서 유일하게 버전 정보를 사용한다.
* 비관적 락이지만 버전 정보를 강제로 증가시킨다.
* 하이버네이트는 nowait를 지원하는 데이터베이스에 대해서 for update nowait 옵션을 적용한다.

> ❗비관적 락과 타임아웃

> 비관적 락을 사용하면 락을 획득할 때까지 트랜잭션이 대기한다. 무한정 기다릴 수 없으므로 타임아웃 시간을 줄 수 있다.<br>
