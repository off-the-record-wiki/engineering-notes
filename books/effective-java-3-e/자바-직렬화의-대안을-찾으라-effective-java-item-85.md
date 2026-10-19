# (Effective Java Item 85) Java - 자바 직렬화의 대안을 찾으라

#### 직렬화란?

```java
public class Member implements Serializable {
    private String name;
    private String email;
    private int age;

    public Member(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }
    @Override
    public String toString() {
        return String.format("Member{name='%s', email='%s', age='%s'}", name, email, age);
    }
}
```
```java
public static void main(String[] args){
    Member member = new Member("홍길동", "hong@gmail.com", 25);
    byte[] serializedMember;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(member);
            serializedMember = baos.toByteArray();
        }
    }
    System.out.println(Base64.getEncoder().encodeToString(serializedMember));
}
```
 
* `java.io.Serializable` 인터페이스를 상속받은 객체를 외부의 자바 시스템에서도 사용할 수 있도록 `byte` 형태의 데이터로 변환하는 기술을 말한다.
* 역직렬화는 반대로 `byte`로 변환된 데이터를 `Object`로 변환하는 기술이다.

#### 역직렬화의 위험성

* 직렬화는 프로그래머가 어렵지 않게 분산 객체를 만들 수 있다는 구호는 매력적이었지만, 보이지 않는 생성자, API와 구현 사이의 모호해진 경계, 잠재적인 정확성 문제, 성능, 보안, 유지보수성 등 그 대가가 크다.
* 직렬화의 근본적인 문제는 공격 범위가 너무 넓고 지속적으로 더 넓어져 방어하기 어렵다는 점이다.
* `ObjectInputStream`의 `readObject()` 메서드를 호출하면서 객체 그래프가 역직렬화되기 때문이다. `readObject()` 메서드는 클래스패스 안의 거의 모든 타입의 객체를 만들어 낼 수 있는, 사실상 마법 같은 생성자다.
* 바이트 스트림을 역직렬화하는 과정에서 이 메서드는 그 타입들 안의 모든 코드를 수행할 수 있다. 이 말인즉슨, 그 타입들의 코드 전체가 공격 범위에 들어간다는 뜻이다.
* 자바의 표준 라이브러리나 아파치 커먼즈 컬렉션 같은 서드파티 라이브러리는 물론 애플리케이션 자신의 클래스들도 공격 범위에 포함된다. 관련된 모든 모범 사례를 따르고 모든 직렬화 가능 클래스들을 공격에 대비하도록 작성한다 해도 애플리케이션은 여전히 취약할 수 있다.

#### 객체와 바이트 시퀀스를 변환해주는 다른 메커니즘, 크로스 플랫폼

* 객체와 바이트 시퀀스를 변환해주는 다른 메커니즘이 많다.
* 이런 방식들은 자바 직렬화의 여러 위험을 회피하면서 다양한 플랫폼 지원, 우수한 성능, 풍부한 지원 도구, 활발한 커뮤니티와 전문가 집단 등 수많은 이점까지 제공한다.

#### 크로스 플랫폼의 대표주자 JSON과 프로토콜 버퍼(ProtoBuf)

* Json은 텍스트 기반이라 사람이 읽을 수 있고, 프로토콜 버퍼는 이진 표현이라 효율이 훨씬 높다.
* Json은 오직 데이터를 표현하는 데에만 쓰이지만, 프로토콜 버퍼는 문서를 위한 스키마를 제공하고 올바르게 쓰도록 강요한다.
* 효율은 프로토콜 버퍼가 훨씬 좋지만 텍스트 기반 표현에는 Json이 아주 효과적이다.
* 프로토콜 버퍼는 이진 표현 뿐만 아니라 사람이 읽을 수 있는 텍스트 표현도 지원한다.
