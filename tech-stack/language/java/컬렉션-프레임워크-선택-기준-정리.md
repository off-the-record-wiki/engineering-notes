# Java ‐ Collection Framework Selection Standard

#### 다양한 Set

* `HashSet`
  * 순서 없음, 중복 허용 안됨
  * 검색, 추가, 삭제 속도가 빠름 `O(1)`
  * 해시 기반으로 동작 : 데이터를 저장할 때 특정 규칙(해시함수)을 통해 메모리 위치를 빠르게 계산 
* `LinkedHashSet`
  * 입력된 순서를 유지하는 HashSet
  * HashSet보다 약간 느리지만, 순서 유지가 필요할 때 사용
  * 중복은 여전히 허용하지 않음
* `TreeSet`
  * 자동 정렬(기본: 오름차순, Comparable or Comparator 사용 가능)
  * 이진 탐색 트리 기반으로 `O(log N)` 성능
  * 중복 불가능, 정렬이 필요한 경우 사용

#### 다양한 Map

* `HashMap`
  * Key-Value 저장, 순서 보장 안됨
  * 빠른 검색 성능 `O(1)`, 중복 Key 불가능
  * 동기화 미지원(멀티스레드 환경에서 안전하지 않음)
* `LinkedHashMap`
  * 입력된 순서를 유지하는 HashMap
  * HashMap과 성능 비슷하지만, 순서 유지가 필요할 때 사용
* `TreeMap`
  * Key 기준 정렬이 자동으로 수행됨
  * Comparable 또는 Comparator를 사용해 정렬 방식 변경 가능
  * 이진 탐색 트리 기반으로 `O(log N)` 성능 
* `ConcurrentHashMap`
  * 멀티스레드 환경에서 안전하게 사용할 수 있는 HashMap
  * 전체 맵이 아닌 버킷 단위로 동기화하여 성능 저하 최소화
  * 멀티스레드 환경에서도 동시 읽기, 쓰기 가능하지만 순서는 보장되지 않음
