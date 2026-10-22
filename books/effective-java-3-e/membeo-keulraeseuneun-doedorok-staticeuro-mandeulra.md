# (Effective Java Item 24) Java ‐ 멤버 클래스는 되도록 static으로 만들라

#### 멤버 클래스는 되도록 static으로 만들라

* 중첩 클래스(nested class)란, 다른 클래스 안에 함께 정의된 클래스를 말한다.
* 중첩 클래스는 자신을 감싼 클래스에서만 쓰여야 하며, 그 외의 쓰임새가 있다면 톱레벨 클래스로 만들어야 한다.

#### 정적 멤버 클래스

* class 내부에서 static으로 선언된 클래스
* 다른 클래스 안에 선언된다.
* 바깥 클래스의 private 멤버에도 접근할 수 있다.
```java
public class Book {
    private final String title;
    private final String author;
    private final int pages;

    // 생성자를 private으로 만들어서 Builder를 통해서만 생성하도록 강제
    private Book(Builder builder) {
        this.title = builder.title;
        this.author = builder.author;
        this.pages = builder.pages;
    }

    @Override
    public String toString() {
        return "Book{title='" + title + "', author='" + author + "', pages=" + pages + '}';
    }

    /**
     * 정적 멤버 클래스: Builder
     * 외부 클래스의 인스턴스 없이 독립적으로 사용 가능
     */
    public static class Builder {
        private String title;
        private String author;
        private int pages;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder pages(int pages) {
            this.pages = pages;
            return this;
        }

        public Book build() {
            return new Book(this);
        }
    }
}
```

#### 비정적 멤버 클래스

* 바깥 클래스의 인스턴스와 암묵적으로 연결
* 비정적 멤버 클래스의 인스턴스 메서드에서 정규화된 this(클래스명.this)를 사용해 바깥 인스턴스의 메서드를 호출하거나 참조를 가져올 수 있다.
* 개념상 중첩 클래스의 인스턴스가 바깥 인스턴스와 독립적으로 존재할 수 있다면 정적 멤버 클래스로 만들어야 한다.
* 비정적 멤버 클래스와 바깥 인스턴스 사이의 관계는 멤버 클래스가 인스턴스화될 때 확립되며, 더 이상 변경할 수 없다.
```java
public class MyList<T> implements Iterable<T> {
    private Object[] elements;
    private int size;
    private static final int DEFAULT_CAPACITY = 10;

    public MyList() {
        this.elements = new Object[DEFAULT_CAPACITY];
        this.size = 0;
    }

    public void add(T element) {
        if (size == elements.length) {
            // 용량 확장 로직 (생략)
        }
        elements[size++] = element;
    }

    @Override
    public Iterator<T> iterator() {
        // iterator() 메소드는 MyList의 특정 인스턴스에 속하므로
        // 이 인스턴스의 데이터에 접근할 수 있는 내부 클래스를 반환
        return new IteratorImpl();
    }

    /**
     * 비정적 멤버 클래스: IteratorImpl (Inner Class)
     * 
     * 이 클래스는 자신이 생성된 MyList 인스턴스의 'elements' 배열과 'size' 변수에 
     * 직접 접근할 수 있습니다.
     */
    private class IteratorImpl implements Iterator<T> {
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            // 외부 클래스 MyList의 size 필드에 직접 접근
            return currentIndex < size; 
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            // 외부 클래스 MyList의 elements 배열에 직접 접근
            return (T) elements[currentIndex++]; 
        }
    }
}
```
#### 비정적 멤버 클래스를 권장하지 않는 이유
> * **숨겨진 참조 및 메모리 누수 위험성** : 비정적 내부 클래스는 외부 클래스 인스턴스에 대한 숨거진 참조를 갖게 된다. 예시로 위의 코드의 IteratorImpl가 단적인 예시이다. 이로 인해 불필요하게 객체가 메모리에 오래 남이있게 되어 메모리 누수가 발생할 가능성이 크다.<br>
> * **높은 결합도 및 낮은 유연성** : 외부 클래스 없이는 내부 클래스 인스턴스 생성이 불가능하므로 코드 유연성이 저하되고 테스트가 어렵다.<br>
> * **코드 명확성 저하** : 외부 클래스의 특정 인스턴스 상태에 의존하는지 명확히 파악하기 어렵다.<br> 

#### 익명 클래스

* 익명 클래스는 이름이 없는 클래스로 ㅂ쓰이는 시점에 선언과 동시에 인스턴스가 만들어진다.
* 선언 지점에서만 인스턴스를 만들 수 있고, instanceof 검사나 클래스 이름이 필요한 작업은 수행할 수 없다.
* 여러 인터페이스를 구현할 수 없고, 인터페이스를 구현하는 동시에 다른 클래스를 상속할 수도 없다.
* 익명 클래스는 표현식이 중간에 등장하므로 짧지 않으면 가독성이 떨어진다.

```java
// 람다가 등장한 이후로 사용하지 않는 방식
List<Integer> list = Arrays.asList(10, 5, 6, 7, 1, 3, 4);
Collections.sort(list, new Comparator<Integer>() {
    @Override
    public int compare(Integer o1, Integer o2) {
        return Integer.compare(o1, o2);
    }
});
System.out.println(list);
```

```java
Collections.sort(list, (o1, o2) -> Integer.compare(o1, o2));
```

#### 지역 클래스

* 지역 변수를 선언할 수 있는 곳이면 실질적으로 어디서든 선언할 수 있고, 유효 범위도 지역 변수와 같다.
```java
// 대표적인 예시로 Thread가 있다.
public class TestThread {
    public static void main(String[] args) {
        Thread goodNightThread = new Thread(() -> {
            try {
                for (int i = 0; i <10; i++){
                    Thread.sleep(1000);
                    System.out.println("양 " + i + "마리...");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        goodNightThread.start();
    }
}
```
