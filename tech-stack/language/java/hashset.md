# Java ‐ HashSet

#### 문자열 해시 코드

* 컴퓨터는 문자를 이해하지 못한다. 그 대신에 각 문자에 고유한 숫자를 할당해서 인식한다.
* 해시 함수(Hash Function)
  * 해시 함수는 임의의 길이의 데이터를 입력으로 받아 고정된 길이의 해시값으로 출력하는 함수를 말한다.
  * 여기서 의미하는 고정된 길이는 저장 공간의 크기를 말한다.
  * 같은 데이터를 입력하면 항상 같은 해시 코드가 출력된다.
  * 다른 데이터를 입력해도 같은 해시 코드가 출력될 수 있는데, 이를 해시 충돌이라고 한다.
* 해시 코드(Hash Code)
  * 해시 코드는 데이터를 대표하는 값을 말한다. 해시 함수에 의해 만들어진다.
  * 예를 들어, 데이터 A의 해시 코드는 65라고 할 수 있다.
* 해시 인덱스(Hash Index)
  * 해시 인덱스는 데이터 저장 위치를 결정하는데, 주로 해시 코드를 사용해서 만든다.
  * 보통 해시 코드 결과에 배열의 크기를 나누어 구한다.

#### 자바의 hashCode

* 자바에서는 모든 객체가 자신만의 해시 코드를 표현할 수 있는 기능을 제공한다. Object 클래스에 있는 `hashCode()`이다.
* 최상위 클래스인 Object 클래스에서 제공하는 메서드를 그대로 사용한다기보다는 보통 재정의해서 사용한다.
* 이 메서드의 기본 구현은 객체의 참조값을 기준으로 해시 코드를 생성한다.
* 만약 논리적 값이 같다고 하더라도 인스턴스 참조값이 다르다면 해시 코드 역시 다르다.

```java
// equals()와 hashCode() 메서드를 재정의하지 않았다면?
public class Member {

	private String id;

	public Member(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
```

```java
// equals()와 hashCode() 메서드를 재정의했다면?
public class Member {

	private String id;

	public Member(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		Member member = (Member) object;
		return Objects.equals(id, member.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
````

* 동일성(객체의 참조값 기준 비교)을 기준으로 비교를 하게 될 경우 힙 영역에 서로 다른 인스턴스로 존재하기 때문에 false가 나오게 된다.
* 동등성(객체의 논리값 기준 비교)을 기준으로 비교를 하게 될 경우 메서드를 재정의했다면 같다고 보기 때문에 true가 나오게 된다.

#### equals, hashCode의 중요성

* 해시 자료 구조를 사용하려면 `hashCode()`도 중요하지만 해시 인덱스가 충돌할 경우를 대비해서 `equals()`도 반드시 재정의를 해야 한다.
* 해시 인덱스가 충돌할 경우 해시 인덱스에 있는 데이터들을 비교하는데 이 때, `equals()`를 사용해서 비교한다.
* Object의 기본 기능
  * `hashCode()` : 기본적으로 객체 참조값을 기반으로 해시 코드를 반환한다.
  * `equals()` : 기본적으로 객체 참조값을 기준으로 비교한다. 따라서, 객체 참조값이 같아야 true가 반환된다.
* 결국 클래스에서 해시 자료 구조를 사용하려면 참조값, 논리값 이 둘을 동시에 비교할 수 있어야 하므로 `hashCode()`, `equals()` 메서드를 재정의해야한다.

#### ❓`hashCode()`, `equals()`를 모두 구현하지 않은 경우

* 구현하지 않은 경우 기본적으로 Object 클래스에서 지원해주는 메서드를 그대로 사용하게 된다.
* 따라서, 객체의 참조값을 기준으로 비교를 하기 때문에 인스턴스 논리값이 같다고 하더라도 동일성과 동등성 측면 비교 시 모두 false가 나오게 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8d2a63be-167e-4cc7-b7a0-8487bd239098.png)

* 인스턴스의 논리값이 같다고 하더라도 객체의 참조값이 다르기 때문에 결과적으로는 다른 값을 반환하게 되기 때문에 같다고 인식하지 않는다.

#### ❓`hashCode()`는 구현했으나 `equals()`는 구현하지 않은 경우

* `hashCode()` 메서드를 재정의했기 때문에 같은 논리값을 가지는 2개의 인스턴스에 대해 비교를 할 경우 같은 해시 코드를 가지게 된다.
* 하지만 `equals()` 메서드는 Object 클래스에서 지원해주는 기능을 사용하기 때문에 기본적으로 객체의 참조값을 기준으로 비교한다. 따라서 false가 나오게 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/562c81e3-a51c-4bdc-aab3-464077cfc894.png)

#### ❓`hashCode()`와 `equals()`를 모두 구현한 경우

* 메서드를 재정의하면 같은 해시 코드를 가짐과 동시에 객체의 논리값을 기준으로 비교한다. 따라서 true가 나오게 된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c338857d-a145-415b-8d0f-6149c9b1efcc.png)
