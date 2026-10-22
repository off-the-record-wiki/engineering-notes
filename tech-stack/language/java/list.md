# Java ‐ List

#### 리스트 추상화

* List 자료구조 : 순서가 있고 중복을 허용하는 자료 구조

> ❓의존 관계 주입
> 
> * 적절한 추상화를 사용하여 추상화된 인터페이스에 의존하는 것을 말한다.<br>
> * 생성자를 통한 의존관계 주입, 수정자를 통한 의존관계 주입 등이 있는데 수정자의 경우 변경의 여지가 있어 안전하지 않다.<br>
> * `final` 필드를 사용하고 생성자를 통해 최초에 생성 시 초기화를 해주게 되면 불변으로 의존관계를 주입할 수 있게 된다.<br>

#### 컴파일 타임, 런타임 의존관계

* 컴파일 타임 : 코드 컴파일 시점
* 런타임 : 프로그램 실행 시점

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/485fc15d-0fb0-425b-b7ee-f6aaeb127487.png)

* 컴파일 타임 의존관계는 자바 컴파일러가 보는 의존관계이다. 클래스에 모든 의존관계가 다 나타난다. 

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/1a490caf-1a33-46c3-b612-ea44088f1998.png)

* 런타임 의존관계는 실제 프로그램이 작동할 때 보이는 의존관계이다. 주로 생성된 인스턴스와 그것을 참조하는 의존관계이다.
* 런타임 의존관계는 프로그램이 실행될 때 계속 변할 수 있다.

> ❓전략 패턴(Strategy Pattern)
>
> * 전략 패턴은 알고리즘을 클라리언트 코드의 변경 없이 쉽게 교체할 수 있다.<br>
> * `List` 인터페이스가 바로 전략을 정의하는 인터페이스가 되고 각각의 구현체인 `ArrayList`, `LinkedList`가 전략의 구체적인 구현이 된다.<br>
> * 그리고 전략을 클라이언트 코드의 변경없이 손쉽게 교체할 수 있다.<br>

#### 자바 리스트

* List 자료구조 : 순서가 있고 중복을 허용하는 자료 구조를 말한다.
* 자바의 컬렉션 프레임워크가 제공하는 대표적인 자료 구조로 아래와 같은 구조를 가진다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e6510f7a-4307-491f-b56b-d8b405d2e288.png)

* 자바 ArrayList
  * 배열을 사용해서 데이터를 관리한다.
  * 기본 CAPACITY는 10이다.
  * 메모리 고속 복사 연산을 사용한다.
    * ArrayList의 경우 앞이나 뒤에 데이터를 추가하면 해당 데이터 이후의 데이터들을 이동시켜야 한다.
    * 자바가 제공하는 ArrayList의 경우 이 부분을 최적화해주는데, 배열의 요소 이동은 시스템 레벨에서 최적화된 메모리 고속 복사 연산을 사용해서 비교적 빠르게 수행된다.
* 자바 LinkedList
  * 자바의 LinkedList는 이중 연결 구조를 사용한다.
  * 이 구조는 다음 노드 뿐만 아니라 이전 노드로도 이동이 가능하다.
