# Redis - Implementing Popular Searches

## 📚 Sorted Set 자료 구조 (ZADD, ZRANGE, ZINCRBY)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/320649a5-0af8-494e-b41e-67536413facf.png)

* Redis에서 Sorted Set(ZSet)은 score(점수)를 기준으로 자동 정렬되는 중복 없는 자료구조이다.
* 이런 특성 때문에 실제 서비스에서 랭킹 시스템, 실시간 인기 데이터 관리처럼 순서와 가중치가 중요한 경우에 주로 활용된다.

> [Redis sorted sets](https://redis.io/docs/latest/develop/data-types/sorted-sets/)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/12d3f8c9-62ec-48d5-88f7-ad7785fa54b8.png)

```java
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    public void searchWithRedis(String keyword) {
      // Redis 명령어에서 'ZINCRBY search_keyword_ranking 1 [keyword]'와 동일
      // ZINCRBY 명령어를 사용하여 해당 keyword의 score를 1 증가시킴
      // (만약 해당 키워드가 존재하지 않으면 새로 추가하고 score를 1로 설정함)
      redisTemplate.opsForZSet()
          .incrementScore("search_keyword_ranking", keyword, 1.0);
    }

    public List<String> getTop10KeywordsWithRedis() {
      // Redis 명령어에서 'ZRANGE [key] [start index] [end index] REV'와 동일
      // REV 옵션을 사용하여 Score가 높은 순(내림차순)으로 0~9번 인덱스 데이터를 조회
      Set<String> topKeywords = redisTemplate.opsForZSet()
          .reverseRange("search_keyword_ranking", 0, 9);
      return new ArrayList<>(topKeywords);
    }
}
```
