# Redis - Solving Concurrency Issues (3)

## 📚 Redis를 활용해 선착순 쿠폰 발급 문제 해결

```java
@Repository
@RequiredArgsConstructor
public class CouponCountRepository {

	private final RedisTemplate<String, String> redisTemplate;

	public Long incrementCouponCount() {
		return redisTemplate
			.opsForValue()
			.increment("coupon_count");
	}
}
```

* Redis는 기본적으로 단일 쓰레드 기반으로 동작하기 때문에 멀티 쓰레드 환경에서의 동시성 이슈가 발생할 확률이 낮다.

#### 문제점 정리

* 분산 환경의 복잡성
* 락의 구현 복잡성
* 트랜잭션의 한계
* **잠재적인 병목 지점이 되는 리소스 = RDB**
  * Redis에서 카운트한 값을 RDS에 저장하는 과정에서 부하가 집중될 수 있다.
  * 이는 다른 서비스의 RDB 접근에도 영향을 줄 수 있기 때문에 잠재적 병목 지점이 된다.
