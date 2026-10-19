# Java ‐ Set

#### HashSet, LinkedHashSet, TreeSet

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f557337a-0add-49cd-a5c8-391b96fb6a34.png)

* HashSet
  * 구현 : 해시 자료 구조를 사용해서 요소를 저장한다.
  * 순서 : 요소들은 특정한 순서 없이 저장된다.
  * 시간 복잡도 : 주요 연산들은 평균적으로 `O(1)` 시간 복잡도를 가진다.
  * 용도 : 데이터의 유일성만 중요하고 순서가 중요하지 않은 경우에 적합하다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ef554c36-deba-4af2-aa2e-ec9ca3e88aba.png)

* LinkedHashSet
  * 구현 : LinkedHashSet은 HashSet에 연결 리스트를 추가해서 요소들의 순서를 유지한다.
  * 순서 : 요소들은 추가된 순서대로 유지된다.
  * 시간 복잡도 : HashSet과 마찬가지로 `O(1)` 시간 복잡도를 가진다.
  * 용도 : 데이터 유일성과 함께 삽입 순서를 유지해야 할 때 적합하다.
  * 연결 링크를 유지해야 하기 때문에 HashSet보다 상대적으로 무겁다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1626cf3c-0f61-4f90-abc8-ef97226ea64e.png)

* TreeSet
  * 구현 : TreeSet은 이진 탐색 트리를 개선한 레드-블랙 트리를 내부에서 사용한다.
  * 순서 : 요소들은 정렬된 순서로 저장된다.
  * 시간 복잡도 : `O(log N)` 시간 복잡도를 가진다. 
  * 용도 : 데이터들은 정렬된 순서로 유지하면서 집합의 특성을 유지해야 할 때 사용한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/89672e17-46c4-44b2-a458-9ec1cbdb9115.png)
