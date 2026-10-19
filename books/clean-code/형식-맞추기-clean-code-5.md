# (Clean Code 5) Clean Code - 형식 맞추기

#### 적절한 행 길이를 유지하라

* 반드시 지킬 엄격한 규칙은 아니지만 바람직한 규칙으로 삼으면 좋다.
* 일반적으로 큰 파일보다 작은 파일이 이해하기 쉽다.

#### 신문 기사처럼 작성하라

* 이름은 간단하면서도 설명이 가능하게 짓는다.
* 이름만 보고도 올바른 모듈을 살펴보고 있는지 아닌지를 판단할 정도로 신경써서 짓는다.

#### 개념은 빈 행으로 분리하라

* 각 행은 수식이나 절을 나타내고 일련의 행 묶음은 완결된 생각 하나를 표현한다.
* 생각 사이는 빈 행을 넣어 분리해야 마땅하다.

```java
package clean.code.format;

import java.math.BigDecimal;
import java.util.List;

public class PeerGroupService {

    private final PeerGroupRepository repository;
    private final MarketDataClient marketDataClient;

    public PeerGroupService(PeerGroupRepository repository, MarketDataClient marketDataClient) {
        this.repository = repository;
        this.marketDataClient = marketDataClient;
    }

    public void updatePerformance(int peerGroupId) {
        // 1. 데이터 조회 및 유효성 검증 (하나의 개념)
        PeerGroup group = repository.findById(peerGroupId);
        if (group == null) {
            throw new IllegalArgumentException("그룹이 존재하지 않습니다.");
        }

        // 2. 외부 연동 및 핵심 비즈니스 계산 알고리즘 (하나의 개념)
        List<String> stockCodes = group.getStockCodes();
        BigDecimal totalPerformance = BigDecimal.ZERO;
        for (String code : stockCodes) {
            BigDecimal price = marketDataClient.getClosedPrice(code);
            totalPerformance = totalPerformance.add(price);
        }

        // 3. 상태 변경 및 영속화 (하나의 개념)
        group.setPerformance(totalPerformance);
        repository.save(group);
    }

    public List<PeerGroup> getActiveGroups() {
        return repository.findAllByStatus("ACTIVE");
    }
}
```

#### 세로 밀집도

* 줄바꿈이 개념을 분리한다면 세로 밀집도는 연관성을 의미한다.
* 즉, 서로 밀접한 코드 행은 세로로 가까이 놓여야 한다는 뜻이다.

#### 수직 거리

* 서로 밀접한 개념은 세로로 가까이 둬야 한다. 물론 두 개념이 서로 다른 파일에 속한다면 규칙이 통하지 않는다.
* 하지만 타당한 근거가 없다면 서로 밀접한 개념은 한 파일에 속해야 마땅하다.
* 이게 바로 `protected` 변수를 피해야 하는 이유 중 하나이다.
* 같은 파일에 속할 정도로 밀접한 두 개념은 세로 거리로 연관성을 표현한다.
* 여기서 연관성이란 한 개념을 이해하는 데 다른 개념이 중요한 정도다.
* 연관성이 깊은 두 개념이 멀리 떨어져 있으면 코드를 읽는 사람이 소스 파일과 클래스를 여기저기 뒤지게 된다.

#### 변수 선언

* 변수는 사용하는 위치에 최대한 가까이 선언한다.
```java
public void processStockBatch(int peerGroupId) {
    PeerGroup group = repository.findById(peerGroupId);
    if (group == null) {
        return;
    }

    // ⭕ stockCodes를 사용하는 반복문 바로 직전에 선언
    List<String> stockCodes = group.getStockCodes();
    
    // 반복문 내부에서 끈끈하게 함께 쓰일 변수들을 루프 바로 위에 집약시킴 (세로 밀집도 향상)
    BigDecimal totalPerformance = BigDecimal.ZERO;
    int successCount = 0;
    int failCount = 0;

    for (String code : stockCodes) {
        try {
            BigDecimal price = marketDataClient.getClosedPrice(code);
            totalPerformance = totalPerformance.add(price);
            successCount++;
        } catch (Exception e) {
            failCount++;
        }
    }
    
    log.info("성공: {}, 실패: {}", successCount, failCount);
}
```

#### 인스턴스 변수

* 인스턴스 변수는 클래스 맨 처음에 선언한다.
* 변수 간에 세로로 거리를 두지 않는다.

```java
public class PeerGroupPerformanceTracker {

    // ⭕ 클래스 최상단에 인스턴스 변수를 밀집시켜 선언
    private final PeerGroupRepository repository;
    private final MarketDataClient marketDataClient;
    private final CacheManager cacheManager;

    // 생성자가 그 바로 아래에 위치하여 변수들을 초기화함
    public PeerGroupPerformanceTracker(
        PeerGroupRepository repository, 
        MarketDataClient marketDataClient, 
        CacheManager cacheManager
    ) {
        this.repository = repository;
        this.marketDataClient = marketDataClient;
        this.cacheManager = cacheManager;
    }

    // 비즈니스 메서드들은 상태 선언이 끝난 아래쪽에 깔끔하게 배치
    public void trackPerformance(int peerGroupId) {
        PeerGroup group = repository.findById(peerGroupId);
        // ... 비즈니스 로직
    }

    public void clearCache() {
        cacheManager.clear();
    }
}
```

#### 종속 함수

* 한 함수가 다른 함수를 호출한다면 두 함수는 세로로 가까이 배치한다.

```java
public class StockAnalysisService {

    // 1. 가장 고차원의 메인 비즈니스 흐름이 맨 위에 위치 (마치 헤드라인 기사처럼)
    public void analyzePeerGroup(int peerGroupId) {
        PeerGroup group = findActiveGroup(peerGroupId);
        
        BigDecimal ratio = calculateRatio(group.getPerformance(), group.getTotalMarketCap());
        
        printResult(group.getPeerGroupName(), ratio);
    }

    // 2. analyzePeerGroup에서 첫 번째로 호출한 함수가 바로 밑에 등장
    private PeerGroup findActiveGroup(int peerGroupId) {
        return repository.findByIdAndStatus(peerGroupId, "ACTIVE");
    }

    // 3. analyzePeerGroup에서 두 번째로 호출한 함수가 그 다음에 등장
    private BigDecimal calculateRatio(BigDecimal performance, BigDecimal total) {
        return performance.divide(total, 4, RoundingMode.HALF_UP);
    }

    // 4. analyzePeerGroup에서 마지막으로 호출한 함수가 최하단에 배치
    private void printResult(String name, BigDecimal ratio) {
        log.info("그룹명: {}, 성과 비율: {}", name, ratio);
    }
}
```

#### 개념적 유사성

* 친화도가 높을수록 코드를 가까이 배치한다.

```java
public class MarketDataCrawler {

    // ⭕ 개념적 유사성이 높은 크롤링 함수들을 세로로 밀집시킴
    // 이름 선언 형식과 마주하는 맥락이 완벽히 유사하여 리듬감이 생김
    
    public void crawlDaytimeMarket() {
        // ... 코스콤 주간 데이터 크롤링 로직
    }

    public void crawlNighttimeMarket() {
        // ... 미국 시장 야간 데이터 크롤링 로직
    }

    public void crawlHolidayMarket() {
        // ... 휴일 특수 크롤링 로직
    }

    // 인프라 제어나 헬스체크 같은 '다른 개념'의 함수들은 아래쪽으로 격리
    public void clearConnectionPool() {
        connectionProvider.closeAll();
    }

    public boolean isServerAlive() {
        return pingClient.ping();
    }
}
```

#### 세로 순서

* 다시 말해, 호출되는 함수를 호출하는 함수보다 나중에 배치한다.

```java
public class StockReportExporter {

    // 1. 태생적 상태 (인스턴스 변수)를 맨 위에 배치
    private final MarketDataClient marketDataClient;

    public StockReportExporter(MarketDataClient marketDataClient) {
        this.marketDataClient = marketDataClient;
    }

    // 2. [고차원 핵심 로직] 이 클래스의 존재 이유이자 전체 비즈니스 흐름 제어 (헤드라인 기사)
    public List<String> exportReport(List<String> stockCodes) {
        List<String> reportRows = new ArrayList<>();
        
        for (String code : stockCodes) {
            BigDecimal yield = calculateYield(code);
            reportRows.add(formatRow(code, yield));
        }
        
        return reportRows;
    }

    // 3. [중간차원 도우미 메서드] 비즈니스 흐름을 받쳐주는 구체적인 도메인 계산식
    private BigDecimal calculateYield(String stockCode) {
        BigDecimal currentPrice = marketDataClient.getClosedPrice(stockCode);
        BigDecimal purchasePrice = marketDataClient.getPurchasePrice(stockCode);
        
        return currentPrice.subtract(purchasePrice)
            .divide(purchasePrice, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    // 4. [최저차원 세부 구현] 비즈니스와 무관한 단순 유틸리티성 문자열 가공 (기사 맨 아래의 각주)
    private String formatRow(String code, BigDecimal yield) {
        return String.format("[%s] 당기수익률: %.2f%%", code, yield);
    }
}
```

#### 가로 공백과 밀집도

* 가로로는 공백을 사용해 밀접한 개념과 느슨한 개념을 표현한다.

```java
public class FinancialCalculator {

    // 1. 메서드 이름과 열림 괄호 '(' 사이는 공백 없이 붙입니다. 
    // -> 메서드와 매개변수는 서로 '긴밀하게 결합된 하나의 개념'이기 때문입니다.
    // 2. 반면 인수 구분 기호인 콤마 ',' 뒤에는 무조건 공백을 둡니다. (인수 분리)
    public BigDecimal calculateTotalValue(BigDecimal price, int quantity, double taxRate) {
        
        // 3. 할당 연산자 '=' 앞뒤로는 명확하게 공백을 두어 왼쪽과 오른쪽이 별개임을 강조합니다.
        BigDecimal rawAmount = price.multiply(BigDecimal.valueOf(quantity));
        
        // 4. 연산자 우선순위를 강조하기 위해 항 사이의 공백을 조절합니다.
        // 곱셈/나눗셈은 결합도가 높으므로 공백을 좁게, 덧셈/뺄셈은 공백을 넓게 두면 수식이 한눈에 읽힙니다.
        double netRate = 1.0 + taxRate; 
        
        return rawAmount.multiply(BigDecimal.valueOf(netRate))
            .setScale(2, RoundingMode.HALF_UP); // 콤마 뒤 공백으로 가독성 확보
    }

    public void printReport(String stockCode) {
        // 인자 내부의 괄호 바로 안쪽에는 공백을 두지 않아 밀집도를 높입니다.
        log.info("종목코드: {}", stockCode);
    }
}
```

#### 들여쓰기

* 범위로 이뤄진 계층을 표현하기 위해 코드를 들여쓴다.
* 들여쓰는 정도는 계층에서 코드가 자리잡은 수준에 비례한다.
* 클래스 정의처럼 파일 수준인 문장은 들여쓰지 않는다.
* 클래스 내 메서드는 클래스보다 한 수준 들여쓴다.
* 블록 코드는 블록을 포함하는 코드보다 한 수준 들여쓴다.

```java
public class CrawlerScanner {

    // ⭕ 메인 메서드는 Depth 1단계만 유지하여 전체 흐름이 한눈에 들어옵니다.
    public void scanMarketStatus(List<String> codes) {
        for (String code : codes) {
            scanSingleStock(code); // 하위 계층(도우미 메서드)으로 책임을 위임
        }
    }

    // ⭕ 각 스레드나 루프의 세부 실행 단락도 계층별로 깔끔하게 들여쓰기 처리
    private void scanSingleStock(String code) {
        try {
            processMarketClosure(code);
        } catch (Exception e) {
            log.error("에러 발생: {}", code, e);
        }
    }

    private void processMarketClosure(String code) {
        if (client.isMarketClosed(code)) {
            log.info("마감된 종목: {}", code);
            saveStatus(code, "CLOSED");
            return; // Guard Clause(보호 구문) 패턴을 활용해 else문의 추가 들여쓰기를 제거
        }
        
        log.info("진행 중인 종목: {}", code);
    }

    private void saveStatus(String code, String status) {
        // 데이터 저장 로직
    }
}
```
