# (Clean Code 3) Clean Code - 함수

```java
// ❌ Bad
public class BeforeHtmlUtils {

    public static String testableHtml(PageData pageData, boolean includeSuiteSetup) throws Exception {
        WikiPage wikiPage = pageData.getWikiPage();
        StringBuffer buffer = new StringBuffer();

        if (pageData.hasAttribute("Test")) {
            if (includeSuiteSetup) {
                WikiPage suiteSetup = PageCrawlerImpl.getInheritedPage(SuiteResponder.SUITE_SETUP_NAME, wikiPage);
                if (suiteSetup != null) {
                    WikiPagePath pagePath = wikiPage.getPageCrawler().getFullPath(suiteSetup);
                    String pagePathName = PathParser.render(pagePath);
                    buffer.append("!include -setup .").append(pagePathName).append("\n");
                }
            }
            WikiPage setup = PageCrawlerImpl.getInheritedPage("SetUp", wikiPage);
            if (setup != null) {
                WikiPagePath setupPath = wikiPage.getPageCrawler().getFullPath(setup);
                String setupPathName = PathParser.render(setupPath);
                buffer.append("!include -setup .").append(setupPathName).append("\n");
            }
        }

        buffer.append(pageData.getContent());
        if (pageData.hasAttribute("Test")) {
            WikiPage teardown = PageCrawlerImpl.getInheritedPage("TearDown", wikiPage);
            if (teardown != null) {
                WikiPagePath tearDownPath = wikiPage.getPageCrawler().getFullPath(teardown);
                String tearDownPathName = PathParser.render(tearDownPath);
                buffer.append("!include -teardown .").append(tearDownPathName).append("\n");
            }
            if (includeSuiteSetup) {
                WikiPage suiteTeardown = PageCrawlerImpl.getInheritedPage(SuiteResponder.SUITE_TEARDOWN_NAME, wikiPage);
                if (suiteTeardown != null) {
                    WikiPagePath pagePath = wikiPage.getPageCrawler().getFullPath(suiteTeardown);
                    String pagePathName = PathParser.render(pagePath);
                    buffer.append("!include -teardown .").append(pagePathName).append("\n");
                }
            }
        }

        pageData.setContent(buffer.toString());
        return pageData.getHtml();
    }

}
```

#### 작게 만들어라

* 함수를 만드는 첫째 규칙은 '작게'이다.
* 함수를 만드는 둘째 규칙은 더 '작게'이다.

```java
public static String renderPageWithSetupsAndTeardowns(PageData pageData, boolean isSuite) throws Exception {
    boolean isTestPage = pageData.hasAttribute("Test");
    if (isTestPage) {
        AfterHtmlUtils includer = new AfterHtmlUtils(pageData, isSuite);
        includer.render();
    }
    return pageData.getHtml();
}
```

* if문/else문/while문 등에 들어가는 블록은 한 줄이어야 한다는 의미이다.
* 중첩 구조가 생길만큼 함수가 커져서는 안 된다.

#### 한 가지만 해라

* 함수는 한 가지를 해야 한다. 그 한 가지를 잘 해야 한다. 그 한 가지만을 해야 한다.

#### 함수당 추상화 수준은 하나로

* 함수가 확실히 한 가지 작업만을 하려면 함수 내 모든 문장의 추상화 수준이 동일해야 한다.

#### 위에서 아래로 코드 읽기 : 내려가기 규칙

* 코드는 위에서 아래로 이야기처럼 읽혀야 좋다.
* 한 함수 다음에는 추상화 수준이 한 단계 낮은 함수가 온다. 즉, 위에서 아래로 프로그램을 읽으면 함수 추상화 수준이 한 번에 한 단계씩 낮아진다.

#### Switch문

* switch문은 작게 만들기 어렵다. case 분기가 단 두 개인 switch문도 너무 길며, 단일 블록이나 함수를 선호한다.
* 본질적으로 switch문은 N가지를 처리한다. 불행히도 switch문을 완전히 피할 방법은 없다.

```java
public Money calculatePay(Employee e) throws InvalidEmployeeType {
    switch (e.type) {
        case COMMISSIONED:
            return calculateCommissionedPay(e);
        case HOURLY:
            return calculateHourlyPay(e);
        case SALARIED:
            return calculateSalariedPay(e);
        default:
            throw new InvalidEmployeeType(e.type);
    }
}
```

* 위의 함수에는 몇 가지 문제가 있다.
* 첫째, 함수가 길다. 새로운 직원 유형을 추가하면 더 길어진다.
* 둘째, '한 가지' 작업만 수행하지 않는다.
* 셋째, SRP를 위반한다. 코드를 변경할 이유가 여럿이기 때문이다.
* 넷째, OCP를 위반한다. 새로운 직원 유형을 추가할 때마다 코드를 변경하기 때문이다.

```java
public abstract class Employee {

    // 인스턴스 필드와 같이 상태를 가져야하는 경우라면 추상 클래스를 선호한다.
    // 상태를 가지는 객체는 절대 캐싱하거나 공유해서 재사용하면 안 된다. 그렇게 되면 치명적인 데이터 오염 문제가 발생하기 때문이다.
    protected EmployeeType type;
    protected String name;

    public Employee(EmployeeRecord record) {
        this.name = record.name;
    }

    public abstract boolean isPayday();
    public abstract Money calculatePay();
    public abstract void deliverPay(Money pay);
}
```
```java
public interface EmployeeFactory {
    // 타입에 따라 어떤 객체를 만들지 정한다.
    Employee makeEmployee(EmployeeRecord r) throws InvalidEmployeeType;
}
```
```java
public class EmployeeFactoryImpl implements EmployeeFactory {

    @Override
    public Employee makeEmployee(EmployeeRecord r) throws InvalidEmployeeType {
        switch (r.type) {
            case HOURLY:
                return new HourlyEmployee(r);
            case SALARIED:
                return new SalariedEmployee(r);
            case COMMISSIONED:
                return new CommissionedEmployee(r);
            default:
                throw new InvalidEmployeeType(r.type);
        }
    }
}
```
* DTO든 Entity든 클라이언트 코드에서 new 키워드를 직접 쓰지 않고 정적 팩토리 메서드를 통해 생성하는 것이 모던 설계의 정석이다.
* 다만 Entity는 내부적으로 매번 진짜 새로운 객체를 new 키워드로 찍어내어 반환한다는 것이 본질적인 차이가 있다.

```java
public class User {
    private String name;

    private User(String name) { // 1. 외부 생성 원천 차단
        this.name = name;
    }

    public static User create(String name) {
        return new User(name); // 2. 호출될 때마다 독립된 'new' 실행!
    }
}
```

#### 서술적인 이름을 사용하라

* 함수가 하는 일을 잘 표현하면 훨씬 좋은 이름이 된다.
* 코드를 읽으면서 짐작했던 기능을 각 루틴이 그대로 수행한다면 깨끗한 코드라 불러도 된다.
* 한 가지만 하는 작은 함수에 좋은 이름을 붙인다면 이런 원칙을 달성함에 있어 이미 절반은 성공했다.
* 함수가 작고 단순할수록 서술적인 이름을 고르기도 쉬워진다.
* 길고 서술적인 이름이 길고 서술적인 주석보다 좋다.
* 서술적인 이름을 사용하면 개발자 머릿속에서도 설계가 뚜렷해지므로 코드를 개선하기 쉬워진다.
* 이름을 붙일 때는 일관성이 있어야 한다.

#### 함수 인수

* 함수에서 이상적인 인수 개수는 0개이다.
* 다음은 1개(단항)고, 다음은 2개(이항)이다. 3개(삼항)는 가능한 피하는 편이 좋다.
* 4개 이상(다항)은 특별한 이유가 필요하다. 특별한 이유가 있어도 사용하면 안 된다.

#### 인수 객체

* 인수가 2~3개 필요하다면 일부를 독자적인 클래스 변수로 선언할 가능성을 짚어본다.

```java
// ❌ Bad
Circle makeCircle(double x, double y, double radius);
```
```java
// ⭕ Good
Circle makeCircle(Point center, double radius);
```

* 객체를 생성해 인수를 줄이는 방법이 눈속임이라 여겨질지 모르지만 그렇지 않다.
* 위 예제에서 x, y를 묶었듯이 변수를 묶어 넘기면 이름을 붙이게 되므로 결국은 개념을 표현하게 된다.

#### 동사와 키워드

* 함수의 의도나 인수의 순서와 의도를 제대로 표현하려면 좋은 함수 이름이 필요하다.
* 단항 함수는 함수와 인수가 동사/명사 쌍을 이뤄야 한다.
* 함수 이름에 키워드를 추가하는 형식도 좋다. 즉, 함수 이름에 인수 이름을 넣는다.(`assertEquals`보다 `assertExpectedEqualsActual(expected, actual)`이 더 좋다.)

#### 부수 효과를 일으키지 마라

```java
public class UserValidator {

    private Cryptographer cryptographer;

    public boolean checkPassword(String userName, String password) {
        User user = UserGateway.findByName(userName);
        if (user != User.NULL) {
            String codedPhrase = user.getPhraseEncodedByPassword();
            String phrase = cryptographer.decrypt(codedPhrase, password);

            if ("Valid Password".equals(phrase)) {
                // ❌ 함수 이름만 보고는 절대 예상할 수 없는 숨겨진 부수 효과!
                Session.initialize();
                return true;
            }
        }
        return false;
    }
}
```

* 여기서 함수가 일으키는 부수효과는 바로 `Session.initialize()` 호출이다.
* 이름 그대로를 본다면 암호를 확인하는건데 세션을 초기화한다는 사실이 어디에서도 드러나지 않는다.
* 그래서 함수 이름만 보고 함수를 호출하는 사용자를 인증하면서 기존 세션 정보를 지워버릴 위험에 처한다.
* 이런 부수 효과가 시각적인 결합을 초래한다.

#### 명령과 조회를 분리하라

* 함수는 뭔가를 수행하거나 뭔가에 답하거나 둘 중 하나만 해야 한다. 둘 다 하면 안 된다.
* 객체 상태를 변경하거나 아니면 객체 정보를 반환하거나 둘 중 하나다. 둘 다 하면 혼란을 초래한다.

#### 오류 코드보다 예외를 사용하라

* 명령 함수에서 오류 코드를 반환하는 방식은 명령/조회 분리 규칙을 위반한다.
* 자칫하면 if문에서 명령을 표현식으로 사용하기 쉬운 탓이다.

```java
if (deletePage(page) == E_OK)
```
* 동사/형용사 혼란을 일으키진 않지만 여러 단계로 중첩되는 코드를 야기한다.
* 오류 코드를 반환하면 호출자는 오류 코드를 곧바로 처리해야 한다는 문제에 부딪힌다.
* 반면 오류 코드 대신 예외를 사용하면 오류 처리 코드를 원래 코드에서 분리할 수 있으므로 코드가 깔끔해진다.

```java
// ❌ Bad
try {
    deletePage(page);
    registry.deleteReference(page.name);
    configKeys.deleteKey(page.name.makeKey());
} catch(Exception e) {
    logger.log(e.getMessage());
}
```
* try/catch 블록은 추하다.
* 코드 구조에 혼란을 일으키며, 정상 동작과 오류 처리 동작을 뒤섞는다.
* 그러므로 try/catch 블록을 별도 함수로 뽑아내는 편이 좋다.

```java
// ⭕ Good
private void deletePageAndAllReferences(Page page) throws Exception {
    deletePage(page);
    registry.deleteReference(page.name);
    configKeys.deleteKey(page.name.makeKey());
}

private void logError(Exception e) {
    logger.log(e.getMessage());
}
```

#### 오류 처리도 한 가지 작업이다.

* 함수는 '한 가지' 작업만 해야 한다. 오류 처리도 '한 가지' 작업에 속한다.
* 그러므로 오류를 처리하는 함수는 오류만 처리해야 마땅하다.

#### 반복하지 마라

* 중복은 소프트웨어에서 모든 악의 근원이다. 그러므로 중복은 지양해야 한다.

#### 함수를 어떻게 짜야하는지?

* 서투른 코드를 빠짐없이 테스트하는 단위 테스트 케이스를 만들고 코드를 다듬고 함수를 만들고 이름을 바꾸고 중복을 제거한다.
* 메서드를 줄이고 순서를 바꾼다. 때로는 전체 클래스르 쪼개기도 한다.
* 이 와중에도 코드는 항상 단위 테스트를 통과해야 한다.

```java
// ⭕ Good
public class AfterHtmlUtils {

    private PageData pageData;
    private boolean isSuite;
    private WikiPage testPage;
    private StringBuffer newPageContent;
    private PageCrawler pageCrawler;

    public static String renderPageWithSetupsAndTeardowns(PageData pageData, boolean isSuite) throws Exception {
        boolean isTestPage = pageData.hasAttribute("Test");
        if (isTestPage) {
            AfterHtmlUtils includer = new AfterHtmlUtils(pageData, isSuite);
            includer.render();
        }
        return pageData.getHtml();
    }

    private AfterHtmlUtils(PageData pageData, boolean isSuite) {
        this.pageData = pageData;
        this.isSuite = isSuite;
        this.testPage = pageData.getWikiPage();
        this.newPageContent = new StringBuffer();
        this.pageCrawler = testPage.getPageCrawler();
    }

    private void render() throws Exception {
        includeSetupPages();
        includePageContent();
        includeTeardownPages();
        updatePageContent();
    }

    private void includeSetupPages() throws Exception {
        if (isSuite) {
            includeSuiteSetupPage();
        }
        includeSetupPage();
    }

    private void includeSuiteSetupPage() throws Exception {
        includePage(SuiteResponder.SUITE_SETUP_NAME, "-setup");
    }

    private void includeSetupPage() throws Exception {
        includePage("SetUp", "-setup");
    }

    private void includePageContent() {
        newPageContent.append(pageData.getContent());
    }

    private void includeTeardownPages() throws Exception {
        includeTeardownPage();
        if (isSuite) {
            includeSuiteTeardownPage();
        }
    }

    private void includeTeardownPage() throws Exception {
        includePage("TearDown", "-teardown");
    }

    private void includeSuiteTeardownPage() throws Exception {
        includePage(SuiteResponder.SUITE_TEARDOWN_NAME, "-teardown");
    }

    private void includePage(String pageName, String mode) throws Exception {
        WikiPage page = PageCrawlerImpl.getInheritedPage(pageName, testPage);
        if (page != null) {
            WikiPagePath pagePath = pageCrawler.getFullPath(page);
            String pagePathName = PathParser.render(pagePath);
            newPageContent.append("!include ").append(mode).append(" .").append(pagePathName).append("\n");
        }
    }

    private void updatePageContent() {
        pageData.setContent(newPageContent.toString());
    }

}
```
