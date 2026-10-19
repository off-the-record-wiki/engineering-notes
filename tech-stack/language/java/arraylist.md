# Java ‐ ArrayList

#### 배열의 특징 파악

* 배열 정리
  * 배열 인덱스 사용 : `O(1)`
  * 배열 순차 검색 : `O(N)`

#### 빅오 표기법

* 빅오(Big O) 표기법은 알고리즘 성능을 분석할 때 사용하는 수학적 표현 방식이다. 특히 알고리즘이 처리해야 할 데이터의 양이 증가할 때, 그 알고리즘이 얼마나 빠르게 실행되는지를 나타낸다.
* 여기서 중요한 것은 알고리즘의 정확한 실행 시간을 계산하는 것이 아니라, 데이터 양의 증가에 따른 성능 변화 추세를 이해하는 것이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/70d7f94c-4470-4401-aa11-c675afecda51.png)

* `O(1)` - 상수 시간, 입력 데이터 크기에 관계없이 알고리즘 실행 시간이 일정하다.
* `O(N)` - 선형 시간, 알고리즘 실행 시간이 입력 데이터 크기에 비례하여 증가한다.
* `O(N^2)` - 제곱 시간, 알고리즘 실행 시간이 입력 데이터 크기의 제곱에 비례하여 증가한다.
* `O(log N)` - 로그 시간, 알고리즘 실행 시간이 데이터 크기의 로그에 비례하여 증가한다.
* `O(N log N)` - 선형 로그 시간

```java
public class CustomArray {

	private static final int DEFAULT_CAPACITY_VALUE = 5;
	private int[] arr; // 배열(정적 크기 고정)

	public CustomArray() {
		this.arr = new int[DEFAULT_CAPACITY_VALUE];
	}

	public void addFirst(int[] arr, int newValue) {
		for (int i = arr.length - 1; i > 0; i--) {
			arr[i] = arr[i-1];
		}
		arr[0] = newValue;
	}

	public void addAtIndex(int[] arr, int index, int newValue) {
		for (int i = arr.length - 1; i > index; i--) {
			arr[i] = arr[i-1];
		}
		arr[index] = newValue;
	}

	public void addLast(int[] arr, int newValue) {
		arr[arr.length - 1] = newValue;
	}
}
```

* 배열은 가장 기본적인 자료 구조이고, 인덱스를 사용한 조회 시 최고의 효율을 자랑한다.
* 하지만 이런 배열에는 가장 큰 단점이 있는데 바로 배열의 크기를 배열을 생성하는 시점에 미리 정해야 한다는 점이다.
* 배열은 처음부터 정적으로 길이가 정해져 있는 것이라 처음부터 너무 많은 공간을 확보하면 그만큼 메모리가 많이 낭비되기에 신중한 선택을 해야한다.

#### 직접 구현하는 배열 리스트

```java
public class ArrayMainV2 {

	public static void main(String[] args) {

		CustomArrayList customArrayList = new CustomArrayList(3);

		customArrayList.add("a");
		customArrayList.add("b");
		customArrayList.add("c");
		System.out.println(customArrayList);

		Object o = customArrayList.get(2);
		System.out.println(o);

		int size = customArrayList.size();
		System.out.println(size);

		int index = customArrayList.indexOf("a");
		System.out.println(index);

		customArrayList.add("f");
		System.out.println(customArrayList); // Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3

	}
}
```

→ 문제점 : 기존에 지정한 크기를 넘어서게 되면 위와 같이 ArrayIndexOutOfBoundsException 오류가 발생한다.

```java
public void add(Object e) {
	if (size == elementData.length) {
		grow(); // 동적으로 확장
	}
	elementData[size] = e;
	size++;
}

private void grow() {
	Object[] newArray = Arrays.copyOf(elementData, elementData.length * 2);
        elementData = newArray;
}
```

* 배열을 새로 복사해서 만드는 연산은 배열을 새로 만들고 기존 데이터를 새로운 배열에 복사하는데 시간이 걸리므로 가능한 시간을 줄이는 것이 좋다. 
* 이렇게 2배씩 증가하면 배열을 새로 만들고 복사하는 연산을 줄일 수 있다. 
* 반면에 배열의 크기를 너무 크게 증가시키게 되면 사용되지 않고 낭비되는 메모리가 많아지는 단점이 발생한다.
* 배열 리스트의 빅오
  * 데이터 추가 Case
    * 마지막에 추가 : `O(1)` - 데이터를 이동시킬 필요가 없기 때문에
    * 앞이나 중간에 추가 : `O(N)` - 데이터 추가 후 데이터를 이동시켜야 하기 때문에
  * 데이터 삭제 Case
    * 마지막 삭제 : `O(1)` - 데이터를 이동시킬 필요가 없기 때문에
    * 앞이나 중간 삭제 : `O(N)` - 데이터 삭제 후 데이터를 이동시켜야 하기 때문에
  * 데이터 검색 Case : `O(N)` - 순차 검색이므로
  * 데이터 조회 Case : `O(1)` - Index 조회
