# (Clean Code 2) Clean Code - 의미 있는 이름

#### 의도를 분명히 밝혀라

* 변수나 함수, 그리고 클래스 이름은 다음과 같은 질문에 모두 답해야한다.
  * **변수(혹은 함수나 클래스)의 존재 이유는?**
  * **수행 기능은?**
  * **사용 방법은?**
* 따로 주석이 필요하다면 의도를 분명히 드러내지 못했다는 말이다.

```java
// ❌ Bad
int d;
```

```java
// ⭕ Good
int elapsedTimeInDays;
int daysSinceCreation;
int daysSinceModification;
int fileAgeInDays;
```

* 의도가 드러나는 이름을 사용하면 코드 이해와 변경이 쉬워진다. 다음 코드와 같이 코드 맥락이 코드 자체에 명시적으로 드러나지 않으면 이해를 하기 어려워진다.

```java
package ch2;

import java.util.ArrayList;
import java.util.List;

public class Make_your_intentions_clear {

    public static void main(String[] args) {

        // 함수가 반환하는 list1을 어떻게 사용하는가?
        List<int[]> list1 = getThem();

        for (int[] arr : list1) {
            System.out.println(arr[0] + ", " + arr[1]);
        }
    }

    static List<int[]> getThem() {
        // theList에 무엇이 들어있는가?
        List<int[]> theList = new ArrayList<>();

        theList.add(new int[]{4, 10});
        theList.add(new int[]{2, 20});
        theList.add(new int[]{4, 30});
        theList.add(new int[]{1, 40});

        List<int[]> result = new ArrayList<>();

        for (int[] x : theList) {
            // theList에서 0번째 값이 왜 중요한가?
            // 값 4가 무슨 의미인가?
            if (x[0] == 4) {
                result.add(x);
            }
        }

        return result;
    }
}
```

```java
package ch2;

import java.util.ArrayList;
import java.util.List;

public class MeaningfulNamesMinesweeper {

    private final List<Cell> gameBoard = new ArrayList<>();

    public MeaningfulNamesMinesweeper() {
        gameBoard.add(new Cell(CellStatus.FLAGGED, 0, 0));
        gameBoard.add(new Cell(CellStatus.HIDDEN, 0, 1));
        gameBoard.add(new Cell(CellStatus.FLAGGED, 1, 0));
        gameBoard.add(new Cell(CellStatus.REVEALED, 1, 1));
    }

    public List<Cell> getFlaggedCells() {
        List<Cell> flaggedCells = new ArrayList<>();

        // int 배열을 사용하는 대신, 칸을 간단한 클래스로 만들었다.
        for (Cell cell : gameBoard) {

            // 명시적인 함수를 사용해 FLAGGED 상수를 감춘다.
            // 구현이 아니라 의도를 드러내야 한다.
            if (cell.isFlagged()) {
                flaggedCells.add(cell);
            }
        }

        return flaggedCells;
    }

    public static void main(String[] args) {
        MeaningfulNamesMinesweeper minesweeper = new MeaningfulNamesMinesweeper();

        List<Cell> flaggedCells = minesweeper.getFlaggedCells();

        for (Cell cell : flaggedCells) {
            System.out.printf("깃발 위치: (%d, %d)%n", cell.getRow(), cell.getColumn());
        }
    }
}

enum CellStatus {
    HIDDEN,
    REVEALED,
    FLAGGED
}

class Cell {

    private final CellStatus status;
    private final int row;
    private final int column;

    public Cell(CellStatus status, int row, int column) {
        this.status = status;
        this.row = row;
        this.column = column;
    }

    public boolean isFlagged() {
        return status == CellStatus.FLAGGED;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public CellStatus getStatus() {
        return status;
    }
}
```

* `status == CellStatus.FLAGGED`는 어떻게 판단하는지를 보여주지만 `cell.isFlagged()`는 무엇을 판단하는지를 보여준다.
* How보다 What을 설명하는 습관을 들이도록 해야한다.

#### 그릇된 정보를 피하라

* 프로그래머는 코드에 그릇된 단서를 남겨선 안 된다.
* 그릇된 단서는 코드 의미를 흐리기 때문이다. 나름대로 널리 쓰이는 의미가 있는 단어를 다른 의미로 사용해도 안 된다.

```java
package ch2;

public class Avoid_false_information {

    public static void main(String[] args) {

        int l = 1;      // 소문자 L은 숫자 1처럼 보인다.
        int O = 0;      // 대문자 O은 숫자 0처럼 보인다.
        int O1 = 10;

        int a = l;

        if (O == 1) {
            a = O;
        } else {
            l = O1;
        }

        System.out.println(a);
        System.out.println(l);
    }
}
```

#### 의미 있게 구분하라

* 컴파일러나 인터프리터만 통과하려는 생각으로 코드를 구현하는 프로그래머는 스스로 문제를 일으킨다.
* 동일한 범위 안에서 다른 두 개념에 같은 이름을 사용하지 못한다. 그래서 프로그래머가 한쪽 이름을 마음대로 바꾸고픈 유혹에 빠진다.
* 컴파일러를 통과할지라도 연속된 숫자를 덧붙이거나 불용어를 추가하는 방식은 적절하지 못하다.
* 이름이 달라야 한다면 의미도 달라져야 한다.
* 연속적인 숫자를 덧붙인 이름(a1, a2, ... aN)은 의도적인 이름과 정반대이다.
* 이런 이름은 그릇된 정보를 제공하는 이름도 아니며 아무런 정보를 제공하지 못하는 이름일 뿐이다.

```java
public class Make_a_meaningful_distinction {

    public static void main(String[] args) {
        char[] source = {'C', 'L', 'E', 'A', 'N'};
        char[] destination = new char[source.length];

        copyChars(source, destination);
        System.out.println(Arrays.toString(destination));## 
    }

    // ❌ a1이 무엇이고 a2가 무엇인가?
    public static void copyChars(char[] a1, char[] a2) {
        for (int i = 0; i < a1.length; i++) {
            a2[i] = a1[i];
        }
    }
}
```
* `Info`나 `Data`는 `a`, `an`, `the`와 마찬가지로 의미가 불분명한 불용어이다.
* 명확한 관례가 없다면 쉽게 구분이 안 된다. 읽는 사람이 차이를 알도록 이름을 지어야 한다.

#### 발음하기 쉬운 이름을 사용하라

```java
// ❌ Bad
class DtaRcrd102 {
    private Date genymdhms;
    private Date modymdhms;
    private String pszqint = "102";
}
```
```java
// ⭕ Good
class Customer {
    private Date generationTimestamp;
    private Date modificationTimestamp;
    private String recordId = "102";
}
```

#### 검색하기 쉬운 이름을 사용하라

* 문자 하나를 사용하는 이름과 상수는 텍스트 코드에서 쉽게 눈에 띄지 않는다는 문제점이 있다.
* `MAX_CLASSES_PER_STUDENT`는 `grep`으로 찾기 쉽지만, 숫자 7은 까다롭다. 7이 들어가는 파일 이름이나 수식이 모두 검색되기 때문이다.
* 상수가 여러 자리 숫자이고 누군가 상수 내 숫자 위치를 바꿨다면 문제는 더욱 심각해진다. 상수에 버그가 있으나 검색으로 찾아내지 못한다.
* 이런 관점으로 볼 때 긴 이름이 짧은 이름보다 좋다. 검색하기 쉬운 이름이 상수보다 좋다.

```java
public class UseSearchableNames {

    public static void main(String[] args) {

        // 34라는 숫자가 무엇을 의미하는지?
        int s = 34;

        for (int j = 0; j < 10; j++) {
            System.out.println(j * s);
        }
    }
}
```

#### 자신의 기억력을 자랑하지 마라

* 읽는 사람이 코드를 읽으면서 변수 이름을 자신이 아는 이름으로 변환해야 한다면 그 변수 이름은 바람직하지 못하다.
* 이는 일반적으로 문제 영역이나 해법 영역에서 사용하지 않는 이름을 선택했기 때문에 생기는 문제이다.
* 똑똑한 프로그래머와 전문가 프로그래머 사이에서 나타나는 차이점을 하나만 들어보자면 전문가 프로그래머근 명료함이 최고라는 사실을 인지한다. 전문가 프로그래머는 자신의 능력을 좋은 방향으로 사용해 남들이 이해하는 코드를 내놓는다.

#### 클래스 이름

* 클래스 이름과 객체 이름은 명사나 명사구가 적합하다. `Customer`, `WikiPage`, `Account`, `AddressParser` 등이 좋은 예시이다.
* `Manager`, `Processor`, `Data`, `Info` 등과 같은 단어는 피하고 동사는 사용하지 않는다.

#### 메서드 이름

* 메서드 이름은 동사나 동사구가 적합하다. `postPayment`, `deletePage`, `save`등이 좋은 예시이다.
* 접근자, 변경자, 조건자는 자바 빈 표준에 따라 값 앞에 `get~`, `set~`, `is~`를 붙인다.

```java
string name = employee.getName();
customer.setName("mike");
if (paycheck.isPosted()) ...
```

* 생성자를 중복 정의할 때, 정적 팩토리 메서드를 사용한다.
* 메서드는 인수를 설명하는 이름을 사용한다.

```java
// ⭕ Good
Complex fulcrumPoint = Complex.FromRealNumber(23.0);
```
```java
// ❌ Bad
Complex fulcrumPoint = new Complex(23.0);
```

#### 기발한 이름은 피하라

* 특정 문화에서만 사용하는 농담은 피하는 편이 좋다.
* 의도를 분명하고 솔직하게 표현하라.

#### 한 개념에 한 단어를 사용하라

* 추상적인 개념 하나에 단어 하나를 선택해 이를 고수한다. 예를 들어, 똑같은 메서드를 클래스마다 `fetch`, `retrieve`, `get`으로 제각각 부르면 혼란스럽기 마련이다.
* 일관성 있는 어휘는 코드를 사용할 프로그래머가 반갑게 여길 선물이다.

#### 말장난을 하지 마라

* 한 단어를 두 가지 목적으로 사용하지 마라. 다른 개념에 같은 단어를 사용한다면 그것은 말장난에 불과하다.
* 프로그래머는 코드를 최대한 이해하기 쉽게 짜야한다. 의미를 해독할 책임이 독자에게 있는 것이 아니라 의도를 밝힐 책임이 저자에게 있는 것이 바람직하다.

#### 문제 영역과 해법 영역의 차이

* **문제 영역(Problem Space)** : 해결해야 할 현실 세계의 비즈니스를 다룬다.
  * 관점 : 사용자, 도메인 전문가, 비즈니스 이해관계자의 관점
  * 사용 언어 : 도메인 용어, 비즈니스 규칙, 프로세스
  * 목적 : 올바른 문제 정의와 비즈니스 요구사항을 도출
* **해법 영역(Solution Space)** : 그것을 소프트웨어로 구현하는 기술적인 방법을 다룬다.
  * 관점 : 소프트웨어 엔지니어, 아키텍트의 관점
  * 사용 언어 : 프로그래밍 언어, 디자인 패턴, 인프라 용어
  * 목적 : 확장 가능하고 유지보수하기 쉬운 안정적인 시스템 설계

#### 의미 있는 맥락을 추가하라

* 스스로 의미가 분명한 이름이 없지 않다. 하지만 대다수의 이름은 그렇지 못하다.
* 그래서 클래스, 함수, 이름 공간에 넣어 맥락을 부여한다. 모든 방법이 실패하면 마지막 수단으로 접두어를 붙인다.
* 에를 들어, `firstName`, `lastName`, `street`, `houseNumber`, `city`, `state`, `zipcode`라는 변수가 있다.

```java
record Address(
    String state, // Address 컨텍스트 내부에 존재하므로 당연히 '배송지 주(State)'를 의미하게 됨
    ZipCode zipCode
) {
}
```
```java
record Money(double amount) {
    public Money plus(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money times(double percent) {
        return new Money(this.amount * percent);
    }
}
```
```java
record ZipCode(String value) {
    public ZipCode {
        Objects.requireNonNull(value, "우편번호는 null일 수 없습니다.");
        if (value.length() != 5) {
            throw new IllegalArgumentException("우편번호는 반드시 5자리여야 합니다.");
        }
    }

    public boolean isNewYorkArea() {
        return value.startsWith("10");
    }
}
```
```java
record Order(
    long id,
    Address deliveryAddress,
    Money totalProductAmount
) {
}
```

#### 불필요한 맥락을 없애라

* 일반적으로 짧은 이름이 긴 이름보다 좋다. 단, 의미가 분명한 경우에 한해서다.
* 이름에 불필요한 맥락을 추가하지 않도록 주의한다.
