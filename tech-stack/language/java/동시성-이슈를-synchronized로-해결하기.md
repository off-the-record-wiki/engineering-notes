# Java ‐ Solving Concurrency Issues with Synchronized

#### 작성한 재고 감소 테스트 코드

```java
@SpringBootTest
class StockServiceTest {

	@Autowired private StockService stockService;
	@Autowired private StockRepository stockRepository;

	@BeforeEach
	public void before() {
		stockRepository.saveAndFlush(new Stock(1L, 100L));
	}

	@AfterEach
	public void after() {
		stockRepository.deleteAll();
	}

	@Test
	@DisplayName("상품 주문시 수량만큼의 재고 감소가 이루어져야 한다.")
	void 재고_감소() {
		stockService.decrease(1L, 1L);

		Stock stock = stockRepository.findById(1L).orElseThrow();

		assertThat(stock.getQuantity()).isEqualTo(99L);
	}

	@Test
	@DisplayName("동시에 여러 요청을 통한 상품 주문시 요청만큼의 재고 감소가 이루어진다.")
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
		 * @Transactional 어노테이션과 synchronized 키워드를 같이 사용하는 경우 : 동시성 문제 발생
		 * synchronized 키워드를 사용하는 이유 : 한 쓰레드에 대해서만 사용하기 위해서
		 * 하지만 여기에 @Transactional 어노테이션을 사용하는 경우 트랜잭션이 끝나기 전에 다른 쓰레드에서 접근할 수 있어 문제가 발생한다.
		 */
		assertThat(stock.getQuantity()).isEqualTo(0L);
	}
}
```

* [Java - synchronized 키워드 설명](동기화-synchronized.md) 
