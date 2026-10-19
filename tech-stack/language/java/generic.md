# Java ‐ Generic

#### 제네릭 적용

* 타입 추론 → 자바 컴파일러에 의한 타입 추론

```java
public class Generic<T> {

	private T value;

	public Generic(T value) {
		this.value = value;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Generic{" +
				"value=" + value +
				'}';
	}
}
```

#### 제네릭 용어와 관례

* 타입을 미리 결정하지 않는다. 실제 사용하는 생성 시점에 결정한다.(타입 추론)
* 타입 인자에는 기본형(Primitive Type)은 사용할 수 없다. 대신에 래퍼 클래스를 사용한다.

```java
class GenericBox<T> // 제네릭 타입, T : 타입 매개변수

// ..

GenericBox<Integer> // 타입 인자 : Integer
``` 

* E : Element
* K : Key
* N : Number
* T : Type
* V : Value

#### 타입 매개변수 제한

* 타입을 제한하지 않으면 해당 타입의 최상위 클래스인 Object를 포함한 모든 상위 타입을 전부 받을 수 있게 된다.
* 타입 추론을 한다고 할 때, T에 무엇이 들어올지 알 수 없기에 만약 제한을 두지 않게 되면 `Object`가 들어와서 `Animal`에서 지원하는 메서드를 사용할 수 없기 때문에 컴파일 오류가 발생한다.
* 이 때, `extends`를 사용해서 타입에 제한을 둘 수 있다.

```java
public class Hospital<T extends Animal> {

	private T animal;

	public T getAnimal() {
		return animal;
	}

	public void setAnimal(T animal) {
		this.animal = animal;
	}

	public void checkUp() {
		System.out.println(animal.getName());
		System.out.println(animal.getSize());
		animal.sound();
	}

	public T bigger(T target) {
		return animal.getSize() > target.getSize() ? animal : target;
	}

	@Override
	public String toString() {
		return "Hospital{" +
				"animal=" + animal +
				'}';
	}
}
```

#### 제네릭 메서드

* 메서드를 호출하는 시점에 타입 인자를 전달해서 타입을 지정하는 것이다.

```java
public class GenericMethod {

        // 제네릭 메서드 예시1
	public static <T> T GenericMethod1(T obj) {
		System.out.println("obj = " + obj);
		return obj;
	}

        // 제네릭 메서드 예시2
        // 타입 상한선을 Animal로 제한
	public static <T extends Animal> T GenericMethod2(T obj) {
		obj.sound();
		return obj;
	}

	public static void main(String[] args) {
		GenericMethod1(new Object());          // 시점에 인자 전달
		GenericMethod2(new Cat("cat", 30));    // 시점에 인자 전달
	}
}
```

#### 와일드 카드

```java
public class AnimalMethod {

	public static <T> void printGeneric(Box<T> box) {
		System.out.println(box.get());
	}

	public static void printWildCardV1(Box<?> box) { // 비제한 와일드 카드
		System.out.println(box.get());
	}

	public static void printWildCardV2(Box<? extends Animal> box) { // 와일드 카드 상한선
		Animal animal = box.get();
		System.out.println(animal.getName());
		System.out.println(animal.getSize());
	}
}
```

* 와일드 카드는 `?`를 사용해서 정의한다. 
* `?`만 사용해서 타입 제한을 걸지 않는 경우 `<? extends Object>`와 같다. 이것을 비제한 와일드 카드라고 한다.
* `<? extends Animal>` 의미는 상한선을 Animal로 두며 `<? super Animal>` 의미는 하한선을 Animal로 두는 것을 말한다.
* 다시 말해 extends의 경우 최대 Animal까지 들어올 수 있다는 것이고 super의 경우 최소 Animal 타입부터 들어올 수 있다는 것이다.

#### 타입 이레이저

* 제네릭은 컴파일 단계에서만 사용되고 컴파일 이후에는 제네릭 정보가 삭제된다.
* 컴파일 전인 `.java`에서는 제네릭의 타입 매개변수가 존재하지만, 컴파일 이후인 `.class` 자바 바이트 코드에는 타입 매개변수가 존재하지 않는다.
* 자바의 제네릭 타입은 컴파일 시점에만 존재하고 런타임 시에는 제네릭 정보가 지워지는데 이것을 타입 이레이저라고 한다.
* 컴파일 이후에는 타입 정보가 존재하지 않기 때문에 `new` 키워드와 `instanceof` 키워드를 사용할 수 없다.
