# Java ‐ Map & Stack & Queue

#### Map

* Map은 키-값의 쌍을 저장하는 자료구조이다.
  * 키는 맵 내에서 유일해야 한다. 그리고 키를 통해 빠르게 값을 검색할 수 있다.
  * 키는 중복될 수 없으나 값은 중복될 수 있다.
  * Map은 순서를 유지하지 않는다. 

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e63239ff-4ec0-4155-bf0d-6741ddebdae5.png)

* Map은 키와 값을 보관하는 자료구조이다.
* 키와 값을 하나로 묶어서 보관하는 것을 Entry라고 한다.
* HashMap
  * 구조 : 해시를 사용해서 요소를 저장한다. 키를 해시 함수를 통해 해시 코드로 반환되고 이 해시 코드로 데이터를 저장하고 검색하는 데 사용된다.
  * 특징 : 삽입, 삭제, 검색 작업은 해시 자료 구조를 사용하므로 일반적으로 `O(1)` 시간 복잡도를 가진다.
  * 순서 : 순서를 보장하지 않는다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8b6a185a-1c5d-4830-af93-8f14518e278b.png)

* LinkedHashMap
  * 구조 : LinkedHashMap은 HashMap과 유사하지만 연결 리스트를 사용하여 삽입 순서 또는 접근 순서에 따라 요소를 유지한다.
  * 특징 : 입력 순서에 따라 순회가 가능하다. 마찬가지로 입력 순서를 링크로 유지해야 하기 때문에 조금 더 무겁다.
  * 성능 : HashMap과 유사하게 대부분의 작업은 `O(1)` 시간 복잡도를 가진다.
  * 순서 : 입력 순서를 보장한다.
* TreeMap
  * 구조 : 레드-블랙 트리를 기반으로 한 구현이다.
  * 특징 : 모든 키는 자연 순서 또는 생성자에 제공된 Comparator에 의해 정렬된다.
  * 성능 : `O(log N)` 시간 복잡도를 가진다.
  * 순서 : 키는 정렬된 순서를 유지한다.

#### Stack

* 후입선출(LIFO)
  * 가장 마지막에 넣은 것이 가장 먼저 나오는 구조이다.
  * Stack에 값을 넣는 것을 push, 값을 빼는 것을 pop이라고 한다.

>❗Stack 클래스는 사용하지 말자
>
> * 자바의 Stack 클래스는 내부에서 Vector 자료 구조를 사용한다.<br>
> * 이 자료 구조는 자바 1.0에 개발되었는데, 지금은 하위 호환을 위해 존재한다.<br>
> * 따라서 Vector를 사용하는 Stack 대신, Deque를 사용하는 것을 권장한다.<br>

#### Queue

* 선입선출(FIFO)
  * 가장 먼저 넣은 것이 가장 먼저 나오는 구조이다.
  * 큐에 값을 넣는 것을 offer, 값을 빼는 것을 poll이라고 한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3f323349-163e-44c8-861c-c7ba77bfd9fe.png)

#### Deque(Double Ended Queue)

* Deque는 일반적인 큐와 스택의 기능을 모두 포함하고 있어 매우 유연한 자료구조이다.
* 대표적인 구현체로 ArrayDeque가 있다.
