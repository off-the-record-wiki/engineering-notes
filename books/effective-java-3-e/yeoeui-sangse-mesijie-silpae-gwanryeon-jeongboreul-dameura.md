# (Effective Java Item 75) Java ‐ 예외의 상세 메시지에 실패 관련 정보를 담으라

* 실패 순간을 포착하려면 발생한 예외에 관여된 모든 매개변수와 필드의 값을 실패 메시지에 담아야 한다.

#### 1. 예외 메시지와 사용자 오류 메시지 구분
- **예외 상세 메시지**와 **최종 사용자에게 보여줄 오류 메시지**를 혼동해선 안 된다.
- 예외 메시지는 개발자와 디버깅을 위한 것이다.
- 사용자 오류 메시지는 사용자 친화적이어야 한다.

#### 2. 가독성보다 내용이 중요
- 예외 메시지는 가독성보다 **담긴 내용이 훨씬 중요**하다.
- 모든 관련 정보를 포함해야 한다.

#### 생성자에서 상세 메시지 생성

* 실패를 적절히 포착하려면 필요한 정보를 예외 생성자에서 모두 받아 상세 메시지까지 미리 생성해놓는 방법도 괜찮다.

```java
public class IndexOutOfBoundsException extends RuntimeException {
    private final int lowerBound;
    private final int upperBound;
    private final int index;

    public IndexOutOfBoundsException(int lowerBound, int upperBound, int index) {
        super(String.format("최솟값: %d, 최댓값: %d, 인덱스: %d", lowerBound, upperBound, index));
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.index = index;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public int getIndex() {
        return index;
    }
}
