# Java ‐ Hash

#### List vs Set

* 자료 구조에서 List와 Set은 각각 특정한 방식으로 데이터를 저장하고 관리하는 데 사용된다.
* 리스트(List)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/0ba17e01-7bff-4270-80cc-dc282273a3f0.png)

* 정의 : 리스트는 요소들의 순차적인 컬렉션이다. 요소들은 특정 순서를 가지며, 같은 요소가 여러 번 나타날 수 있다. 즉, 중복을 허용하며 순서를 가지고 있다.
* 순서 유지 : 리스트에 추가된 요소는 특정한 순서를 유지한다. 이 순서는 요소가 추가된 순서를 반영할 수 있다.
* 중복 허용 : 리스트는 동일한 값이나 객체 중복을 허용한다.
* 인덱스 접근 : 리스트 각 요소는 인덱스를 통해 접근할 수 있다. 이 인덱스는 보통 0부터 시작한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/666ea5fc-2cb2-4999-9d95-cb82e55e2f94.png)

* 정의 : 세트는 유일한 요소들의 컬렉션이다. 
* 유일성 : 세트에는 중복된 요소가 존재하지 않는다. 세트에 요소를 추가할 때, 이미 존재하는 요소이면 무시된다.
* 순서 미보장 : 대부분의 세트 구현에는 요소들의 순서를 보장하지 않는다. 즉 요소를 출력할 때, 입력 순서와 다를 수 있다.
* 빠른 검색 : 세트는 요소의 유무를 빠르게 확인할 수 있도록 최적화되어 있다. 이는 데이터 중복을 방지하고 빠른 조회를 가능하게 한다.

```java
public class CustomSet {

	private int[] elementData = new int[10];
	private int size = 0;

	// O(N)
	public boolean add(int value) {
		if (contains(value)) {
			return false;
		}
		elementData[size++] = value;
		return true;
	}

	// O(N)
	public boolean contains(int value) {
		for (int data : elementData) {
			if (data == value) {
                return true;
            }
		}
		return false;
	}

	public int size() {
		return size;
	}

	@Override
	public String toString() {
		return "CustomSet{" +
				"elementData=" + Arrays.toString(elementData) +
				", size=" + size +
				'}';
	}
}
```

#### 해시 알고리즘

* 해시 인덱스를 사용(각 인덱스별 LinkedList 버킷)
  * 데이터 저장 : `O(N)`
  * 데이터 조회 : `O(N)`
* 해시 인덱스를 사용하면 최악의 경우(O(N))는 거의 발생하지 않는다. 배열의 크기만 적절하게 잡아준다면 대부분 O(1)에 가까운 빠른 성능을 보여줄 수 있다.
