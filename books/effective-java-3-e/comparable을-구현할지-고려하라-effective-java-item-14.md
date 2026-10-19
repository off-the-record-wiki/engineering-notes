# (Effective Java Item 14) Java ‐ Comparable을 구현할지 고려하라

* `Comparable` 인터페이스를 구현했다는 것은 그 클래스의 인스턴스들에게는 자연적인 순서가 있음을 뜻한다.
* `compareTo()` 메서드의 일반 규약은 `equals` 규약과 비슷하다.
  * 객체가 주어진 객체(파라미터)보다 작으면 음의 정수, 같으면 0, 크면 양의 정수를 반환
  * 비교할 수 없는 객체가 주어지면 `ClassCastException`을 던진다.

```java
class Student implements Comparable<Student> {
    String studentName; // 이름
    int studentId;      // 학번
    double score;       // 학점

    public Student(String studentName, int studentId, double score) {
        this.studentName = studentName;
        this.studentId = studentId;
        this.score = score;
    }

    @Override
    public String toString() {
        return "이름: " + studentName + ", 학번: " + studentId + ", 학점: " + score;
    }

    // 학번(studentId) 기준으로 오름차순 정렬
    @Override
    public int compareTo(Student o) {
        return Integer.compare(studentId, o.studentId);
    }
}
```

```java
public interface Comparable<T> {
    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure {@link Integer#signum
     * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
     * all {@code x} and {@code y}.  (This implies that {@code
     * x.compareTo(y)} must throw an exception if and only if {@code
     * y.compareTo(x)} throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
     * {@code x.compareTo(z) > 0}.
     *
     * <p>Finally, the implementor must ensure that {@code
     * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
     * == signum(y.compareTo(z))}, for all {@code z}.
     *
     * @apiNote
     * It is strongly recommended, but <i>not</i> strictly required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
     * class that implements the {@code Comparable} interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * @param   o the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     *
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this object.
     */
    public int compareTo(T o);
}
```
* 자기 자신(this)이 첫 번째 파라미터, 매개변수 객체가 두 번쨰 파라미터면 오름차순 정렬
* 위의 반대 케이스라면 내림차순 정렬

> [📚Comparable](https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html)
>
> **1. 애플리케이션 레벨의 정렬(In-Memory Sort)**
> * 데이터가 이미 DB에서 조회되어 JVM 힙 메모리에 적재된 상태에서 정렬을 수행할 때 사용된다.
> * DB 정렬만으로는 부족한 복잡한 비즈니스 로직이 필요할 때 주로 사용한다.
>
> **2. 자연스러운 순서(Natural Ordering)**
> * `Comparable`을 구현한다는 것은 해당 객체에 가장 기본이 되는 정렬 기준을 부여한다는 의미가 된다.
> * 클래스 설계 시 이 객체들의 가장 대표적인 정렬 기준이 무엇인가를 고민해야한다.
>
> **3. 데이터 구조와의 협력**
> * `TreeSet`이나 `TreeMap`처럼 원소를 넣자마자 자동으로 정렬을 유지해야 하는 자료구조들은 내부적으로 `Comparable` 혹은 `Comparator`을 필요로 한다.
> * 이를 구현하지 않은 객체를 넣으면 `ClassCastException`이 발생한다.
>
> **4. 주의사항 : DB 정렬 vs JVM 정렬**
> * 데이터베이스 정렬 시 인덱스를 활용하면 매우 빠르다. 수백만 건도 효율적인 처리가 가능하다.
> * JVM 정렬 시 데이터 양이 많을수록 CPU/메모리 부하가 크다. 대량 데이터에 적용 시 `OutOfMemory` 발생 위험이 있다.
