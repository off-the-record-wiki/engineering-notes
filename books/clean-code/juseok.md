# (Clean Code 4) Clean Code - 주석

#### 주석은 나쁜 코드를 보완하지 못한다.

* 코드에 주석을 추가하는 일반적인 이유는 코드 품질이 나쁘기 때문이다.
* 표현력이 풍부하고 깔끔하며 주석이 거의 없는 코드가 복잡하고 어수선하며 주석이 많이 달린 코드보다 훨씬 좋다.
* 자신이 저지른 난장판을 주석으로 설명하려 애쓰는 대신에 그 난장판을 깨끗이 치우는 데 시간을 보내야 한다.

#### 코드로 의도를 표현하라.

* 확실히 코드만으로 의도를 설명하기 어려운 경우가 존재한다.

```java
// ❌ Bad
if ((employee.flags & HOURLY_FLAG) && (employee.age > 65))
```
```java
// ⭕ Good
if (employee.isEligibleForFullBenefits())
```

#### 좋은 주석

**1. 법적인 주석**
* 때로는 회사가 정립한 구현 표준에 맞춰 법적인 이유로 특정 주석을 넣으라고 명시한다.

**2. 정보를 제공하는 주석**
* 때로는 기본적인 정보를 주석으로 제공하면 편리하다.
```java
// 테스트 중인 Responder 인스턴스를 반환한다.
protected abstract Responder responderInstance();
```

**3. 의도를 설명하는 주석**
```java
// ⭕ Good
@Test
public void testConcurrentAccess() {
    // 다수의 스레드가 동시에 접근할 때 데드락(Deadlock)이 발생하는지 
    // 검증하기 위해 의도적으로 100개의 래치를 걸고 동시에 출발시킴.
    CountDownLatch latch = new CountDownLatch(100);
    // ... 테스트 로직
}
```

**4. 의미를 명료하게 밝히는 주석**
* 모호한 인수나 반환값은 그 의미를 읽기 좋게 표현하면 이해하기 쉬워진다.
* 일반적으로 인수나 반환값 자체를 명확하게 만들면 더 좋겠지만, 인수나 반환값이 표준 라이브러리나 변경하지 못하는 코드에 속한다면 의미를 명료하게 밝히는 주석이 유용하다.

```java
// ⭕ Good
// s1이 s2보다 사전 편찬 순서상 앞서면 true를 반환한다. (s1 < s2)
if (s1.compareTo(s2) < 0) {
    return true;
}
```

**5. 결과를 경고하는 주석**
* 때로는 다른 프로그래머에게 결과를 경고할 목적으로 주석을 사용한다.
```java
@Test
public void testStockPriceCrawlerWithLargeData() {
    // ⚠️ 경고: 대량의 가상 금융 데이터를 생성하고 파싱하므로 
    // 실행하는 데 10분 이상 소요됩니다. 빌드 서버나 로컬에서 
    // 전체 테스트를 가볍게 돌릴 때는 가급적 @Disabled 처리해 주세요.
    
    // ... 대형 테스트 로직
}
```

**6. TODO 주석**
* 때로는 앞으로 할 일을 `//TODO` 주석으로 남겨두면 편한다.
```java
public void fetchMarketPrice() {
    // TODO: 2026년 3분기에 코스콤(KOSCOM) 실시간 소켓 API가 오픈되면
    // 현재의 1분 주기 HTTP 폴링 방식을 웹소켓 구독 구조로 리팩터링해야 함.
    String temporaryData = httpClient.get("/market-price/temporary");
    // ... 임시 파싱 로직
}
```
* TODO 주석은 프로그래머가 필요하다 여기지만 당장 구현하기 어려운 임무를 기술한다.
* 어떤 용도로 사용하든 시스템에 나쁜 코드를 남겨 놓는 핑게가 되어선 안 된다.
* TODO로 도배한 코드는 바람직하지 않다. 주기적으로 TODO 주석을 점검해 없애도 괜찮은 주석은 없애라고 권한다.

**7. 중요성을 강조하는 주석**
* 자칫 대수롭지 않다고 여겨질 뭔가의 중요성을 강조하기 위해서도 주석을 사용한다.
```java
public class StockPerformanceCache {
    private final Map<Integer, PeerGroupStockPerformanceRow> cacheMap;

    public StockPerformanceCache() {
        // ✨ 중요: 이 캐시 맵은 초당 수천 번의 주식 데이터 조회 요청을 처리하는 
        // 멀티스레드 환경에서 공유되므로, 일반 HashMap이 아닌 동시성이 보장되는 
        // ConcurrentHashMap 계열이나 대안 인프라를 무조건 유지해야 합니다.
        this.cacheMap = new ConcurrentHashMap<>();
    }
}
```

**8. 공개 API에서 Javadocs**
* 설명이 잘 된 공개 API는 참으로 유용하고 만족스럽다.
* 공개 API를 구현한다면 반드시 훌륭한 Javadocs를 작성한다.
* 여느 주석과 마찬가지로 Javadocs 역시 독자를 오도하거나 잘못 위치하거나 그릇된 정보를 전달할 가능성이 존재한다.

#### 나쁜 주석

**1. 주절거리는 주석**
* 특별한 이유 없이 의무감으로 혹은 프로세스에서 하라고 하니까 마지못해 주석을 단다면 전적으로 시간낭비이다.
* 주석을 달기로 결정했다면 충분한 시간을 들여 최고의 주석을 달도록 한다.

**2. 같은 이야기를 중복하는 주석**
* 헤더에 달린 주석이 같은 코드 내용을 그대로 중복하면 코드보다 주석을 읽는 시간이 더 오래 걸린다.
```java
public class StockProcessor {
    
    // 기본 생성자
    public StockProcessor() {
    }

    // 피어그룹 ID
    private int peerGroupId;

    // 피어그룹 이름을 설정한다.
    public void setPeerGroupName(String peerGroupName) {
        // 피어그룹 이름을 할당한다.
        this.peerGroupName = peerGroupName;
    }

    // 포트폴리오 리스트가 비어있는지 확인한다.
    public boolean isPortfolioEmpty(List<String> stocks) {
        // 리스트가 비어있으면 참을 반환한다.
        if (stocks.isEmpty()) {
            return true; // 참 반환
        } else {
            return false; // 거짓 반환
        }
    }
}
```

**3. 오해할 여지가 있는 주석**
* 코드가 실제로 작동하는 방식과 미세하게(혹은 완전히) 다르게 작성되어, 읽는 개발자를 잘못된 길로 인도하는 주석이다.

**4. 의무적으로 다는 주석**
* 모든 함수에 Javadocs를 달거나 모든 변수에 주석을 달아야 한다는 규칙은 어리석다.
* 이런 주석은 코드를 복잡하게 만들며, 거짓말을 퍼뜨리고 혼동과 무질서를 초래한다.

**5. 이력을 기록하는 주석**
* 아예 쓰지 말자.

**6. 있으나 마나 한 주석**
* 너무 당연한 사실을 언급하며 새로운 정보를 제공하지 못하는 주석이다.

**7. 함수나 변수로 표현할 수 있다면 주석을 달지 마라**
```java
// ❌ Bad
// 피어그룹 ID가 유효하고, 수익률이 마이너스가 아니며, 활성화된 그룹인지 확인
if (row.getPeerGroupId() > 0 && row.getPerformance() != null && row.getPerformance().compareTo(BigDecimal.ZERO) >= 0 && "ACTIVE".equals(row.getStatus())) {
    // ... 로직 수행
}
```
* 주석이 필요하지 않도록 코드를 개선하는 편이 더 좋다.

**8. 주석으로 처리한 코드**
* 주석으로 처리한 코드는 되게 밉살스러운 관행이다.
* 주석으로 처리된 코드는 다른 사람들이 지우기를 주저한다. 이유가 있어 남겨놓았으리라고, 중요하니까 지우면 안 된다고 생각한다.

**9. HTML 주석**
* 소스 코드에서 HTML 주석은 혐오 그 자체다.
