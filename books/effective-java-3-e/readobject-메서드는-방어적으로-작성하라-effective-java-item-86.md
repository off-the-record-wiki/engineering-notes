# (Effective Java Item 88) Java - readObject 메서드는 방어적으로 작성하라

#### `readObject()`

* `InputObjectStream`, `OutputObjectStream`을 통해 객체를 읽고 쓴다.
* 이 클래스에 포함된 메서드가 `readObject()`, `writeObject()`이다.
* 클래스에 `readObject()`, `writeObject()`가 정의되어 있다면, 기본 직렬화 과정에서 이 메서드를 통해 직렬화/역직렬화를 수행한다.
  * 커스텀한 직렬화를 하고싶다면?
  * `private` 메서드로 작성 필요
  * 메서드들의 처음에 `defaultWriteObject()`, `defaultReadObject()`를 호출하여 기본 직렬화를 실행하게 해야한다.
  * 리플렉션을 통해 작업을 수행한다.

#### `readObject()`의 문제점

* 새로운 객체를 만들어내는 public 생성자와 같다고 볼 수 있다.
* 생성자처럼 유효성 검사, 방어적 복사를 수행해야 한다. 그렇지 않으면 불변식을 보장받지 못한다.

```java
public final class Period implements Serializable {

   // private 선언
   private Date start;
   private Date end;

   // 생성자
   public Period(Date start, Date end) {
       this.start = new Date(start.getTime());   // 방어적 복사
       this.end = new Date(end.getTime());
       if (this.start.compareTo(this.end) > 0) { // 유효성 검사
           throw new IllegalArgumentException(start + " after " + end);
       }
   }

   public Date start() {
       return new Date(start.getTime());
   }

   public Date end() {
       return new Date(end.getTime());
   }
}
```
```java
public class BogusPeriod {
    // 불변식을 깨뜨리도록 조작된 바이트 스트림
    private static final byte[] serializedForm = {
        (byte)0xac, (byte)0xed, 0x00, 0x05, 0x73, 0x72, 0x00, 0x06,
        0x50, 0x65, 0x72, 0x69, 0x6f, 0x64, 0x40, 0x7e, (byte)0xf8,
        0x2b, 0x4f, 0x46, (byte)0xc0, (byte)0xf4, 0x02, 0x00, 0x02,
        0x4c, 0x00, 0x03, 0x65, 0x6e, 0x64, 0x74, 0x00, 0x10, 0x4c,
        0x6a, 0x61, 0x76, 0x61, 0x2f, 0x75, 0x74, 0x69, 0x6c, 0x2f,
        0x44, 0x61, 0x74, 0x65, 0x3b, 0x4c, 0x00, 0x05, 0x73, 0x74,
        0x61, 0x72, 0x74, 0x71, 0x00, 0x7e, 0x00, 0x01, 0x78, 0x70,
        0x73, 0x72, 0x00, 0x0e, 0x6a, 0x61, 0x76, 0x61, 0x2e, 0x75,
        0x74, 0x69, 0x6c, 0x2e, 0x44, 0x61, 0x74, 0x65, 0x68, 0x6a,
        (byte)0x81, 0x01, 0x4b, 0x59, 0x74, 0x19, 0x03, 0x00, 0x00,
        0x78, 0x70, 0x77, 0x08, 0x00, 0x00, 0x00, 0x66, (byte)0xdf,
        0x6e, 0x1e, 0x00, 0x78, 0x73, 0x71, 0x00, 0x7e, 0x00, 0x03,
        0x77, 0x08, 0x00, 0x00, 0x00, (byte)0xd5, 0x17, 0x69, 0x22,
        0x00, 0x78
    };

    public static void main(String[] args) {
        Period p = (Period) deserialize(serializedForm);
        System.out.println(p.start);
        System.out.println(p.end);
    }
    
    static Object deserialize(byte[] sf) {
        try {
            return new ObjectInputStream(new ByteArrayInputStream(sf)).readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
```
* 위의 바이트 스트림 정보는 `start`의 시각을 `end` 시각보다 느리게 조작한 것이다.
* 결과를 보면 객체가 생성되면서 방어적 복사와 유효성 검증 등을 모두 회피한 것을 볼 수 있다.
* `readObject()`를 정의하고 유효성 검사를 실시한다.
```java
private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();          // 기본 직렬화 수행
    if (start.compareTo(end) > 0) { // 유효성 검사
        throw new InvalidObjectException(start + " 가 " + end + " 보다 늦을 수 없습니다.");
    }
}
```
* 객체를 역직렬화할 때는 클라이언트가 소유해선 안되는 객체 참조를 갖는 필드를 모두 방어적 복사한다.
* 불변 클래스 안의 모든 `private` 가변 요소를 방어적 복사한다.
```java
private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
   s.defaultReadObject();

   // 방어적 복사를 통해 인스턴스의 필드값 초기화
   start = new Date(start.getTime());
   end = new Date(end.getTime());

   // 유효성 검사
   if (start.compareTo(end) > 0)
       throw new InvalidObjectException(start +" after "+ end);
}
```
