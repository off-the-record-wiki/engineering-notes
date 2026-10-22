# (Clean Code 10) Clean Code - 클래스

#### 클래스 체계

* 변수와 유틸리티 함수는 가능한 공개하지 않는 편이 낫지만 반드시 숨겨야 한다는 법칙도 없다.
* 때로는 변수나 유틸리티 함수를 `protected`로 선언해 테스트 코드에 접근을 허용하기도 한다.
* 같은 패키지 안에서 테스트 코드가 함수를 호출하거나 변수를 사용해야 한다면 그 함수나 변수를 `protected`로 선언하거나 패키지 전체로 공개한다.
* 하지만 그 전에 비공개 상태를 유지할 온갖 방법을 강구해야한다.

#### 클래스는 작아야 한다

* 클래스는 작아야 한다.
```java
// ❌ Bad(너무 많은 책임을 진다)
public class SuperDashboard extends JFrame implements MetaDataUser {

    // 너무 많은 인스턴스 변수
    private String lastFocusedComponent;
    private int majorVersionNumber;
    private int minorVersionNumber;

    // 책임 1: UI 컴포넌트 관리
    public Component getLastFocusedComponent() { ... }
    public void setLastFocused(Component lastFocused) { ... }

    // 책임 2: 버전 관리
    public int getMajorVersionNumber() { ... }
    public int getMinorVersionNumber() { ... }
    public int getBuildNumber() { ... }
}
```

#### 단일 책임 원칙(SRP)

* SRP는 클래스나 모듈을 변경할 이유가 하나, 단 하나뿐이어야 한다는 원칙이다.
* SRP는 '책임'이라는 개념을 정의하며 적절한 클래스 크기를 제시한다. 클래스는 책임, 즉 변경할 이유가 하나여야 한다는 의미이다.
* 책임, 즉 변경할 이유를 파악하려고 노력해야한다.
* 큰 클래스 몇 개가 아니라 작은 클래스 여럿으로 이뤄진 시스템이 더 바람직하다. 작은 클래스는 각자 맡은 책임이 하나며, 변경할 이유가 하나며, 다른 작은 클래스와 협력해 시스템에 필요한 동작을 수행한다.

```java
// ⭕ Good(단일 책임 클래스)
public class Version {
    public int getMajorVersionNumber();
    public int getMinorVersionNumber();
    public int getBuildNumber();
}
```

#### 응집도

* 클래슨느 인스턴스 변수 수가 작아야 한다.
* 일반적으로 메서드가 변수를 더 많이 사용할수록 메서드와 클래스는 응집도가 더 높다.
* 모든 인스턴스 변수를 메서드마다 사용하는 클래스는 응집도가 가장 높다.

```java
// ⭕ Good(모든 메서드가 모든 변수를 골고루 사용)
public class Stack {

    private int topOfStack = 0;
    List<Integer> elements = new LinkedList<Integer>();

    public int size() {
        return topOfStack;
    }

    public void push(int element) {
        topOfStack++;
        elements.add(element);
    }

    public int pop() throws PoppedWhenEmpty {
        if (topOfStack == 0) {
            throw new PoppedWhenEmpty();
        }
        int element = elements.get(--topOfStack);
        elements.remove(topOfStack);
        return element;
    }
}
```

#### 응집도를 유지하면 작은 클래스 여럿이 나온다.

```java
// 주식 데이터를 집계하는 책임을 가진 작은 클래스
public class StockPerformanceAggregator {

    private final MarketDataClient marketDataClient;
    private final List<String> stockCodes;
    
    private BigDecimal totalPerformance;
    private int successCount;

    public StockPerformanceAggregator(MarketDataClient marketDataClient, List<String> stockCodes) {
        this.marketDataClient = marketDataClient;
        this.stockCodes = stockCodes;
    }

    public void calculateMetrics() {
        totalPerformance = BigDecimal.ZERO;
        for (String code : stockCodes) {
            BigDecimal price = marketDataClient.getClosedPrice(code);
            totalPerformance = totalPerformance.add(price);
            successCount++;
        }
    }

    public String getSummaryLog() {
        return "성공 종목 수: " + successCount + ", 총 성과: " + totalPerformance;
    }
}
```
```java
// 텍스트 포매팅 스타일을 관리하는 책임을 가진 작은 클래스
public class ReportRowFormatter {

    private String delimiter;
    private String dateFormat;

    public ReportRowFormatter() {
        this.delimiter = ",";
        this.dateFormat = "yyyy-MM-dd";
    }

    public String formatRow(String code, BigDecimal price) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat));
        return code + delimiter + price + delimiter + today;
    }

    public void changeFormat(String delimiter, String dateFormat) {
        this.delimiter = delimiter;
        this.dateFormat = dateFormat;
    }
}
```

#### 변경으로부터 격리

* 상세한 구현에 의존하는 클라이언트 클래스는 구현이 바뀌면 위험에 빠진다. 그래서 우리는 인터페이스와 추상 클래스를 사용해 구현이 미치는 영향을 격리한다.
* 상세한 구현에 의존하는 코드는 테스트가 어렵다.

```java
public interface StockExchange {
    Money currentPrice(String symbol);
}
```
```java
public Portfolio {
    private StockExchange exchange;
    public Portfolio(StockExchange exchange) {
        this.exchange = exchange;
    }
    // ...
}
```
