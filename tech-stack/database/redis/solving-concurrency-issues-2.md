# Redis ‐ Solving Concurrency Issues (2)

## 📚 Redis 라이브러리

> ❗Lettuce

> * 구현이 간단하다.<br>
> * Spring Data Redis를 사용하면 기본적으로 Lettuce가 기본이 되므로 별도의 라이브러리를 사용하지 않아도 된다.<br>
> * spin lock 방식이기 때문에 많은 쓰레드가 lock 획득 대기 상태라면 Redis에 부하가 갈 수 있다.<br>

> ❗Redisson 

> * 락 획득 재시도를 기본으로 제공한다.<br>
> * pub-sub 방식으로 구현이 되어있기 때문에 Lettuce와 비교했을 때, Redis에 부하가 덜 간다.<br>
> * 별도의 라이브러리를 사용해야 한다.<br>
> * Lock을 라이브러리 차원에서 제공하기 때문에 사용법을 공부해야한다.<br>

## 📚 Lettuce

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

```java
@Component
@RequiredArgsConstructor
public class RedisLockRepository {

	private final RedisTemplate<String, String> redisTemplate;

        // 락을 획득했는지?
	public Boolean lock(Long key) {
		return redisTemplate
				.opsForValue()
				.setIfAbsent(generateKey(key), "lock", Duration.ofMillis(3000));
	}

        // 락을 해제했는지?
	public Boolean unlock(Long key) {
		return redisTemplate.delete(generateKey(key));
	}

	private String generateKey(Long key) {
		return key.toString();
	}
}
```

```java
@Component
@RequiredArgsConstructor
public class LettuceLockStockFacade {

	private final RedisLockRepository redisLockRepository;
	private final StockService stockService;

	public void decrease(Long productId, Long quantity) throws InterruptedException {
                // 락을 획득했는지 무한 반복
		while (!redisLockRepository.lock(productId)) {
                        // 락을 획득하지 못한 경우 락 획득 재시도
			Thread.sleep(100);
		}

                // 락을 획득했다면 로직 실행
		try {
			stockService.decrease(productId, quantity);
                // 수행했다면 finally문으로 이동해 락을 해제 
		} finally {
			redisLockRepository.unlock(productId);
		}
	}
}
```

## 📚 Redisson

```gradle
implementation 'org.redisson:redisson-spring-boot-starter:3.22.1'
```

```java
@Component
public class RedissonLockStockFacade {

	private RedissonClient redissonClient;
	private StockService stockService;

	public RedissonLockStockFacade(RedissonClient redissonClient, StockService stockService) {
		this.redissonClient = redissonClient;
		this.stockService = stockService;
	}

	public void decrease(Long productId, Long quantity) {
		RLock lock = redissonClient.getLock(productId.toString());

		try {
			boolean b = lock.tryLock(10, 1, TimeUnit.SECONDS);

			if (!b) {
				System.out.println("lock 획득 실패");
				return;
			}
			stockService.decrease(productId, quantity);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}
}
```


- [참고 자료 - Jedis보다 Lettuce를 쓰자](https://jojoldu.tistory.com/418)
