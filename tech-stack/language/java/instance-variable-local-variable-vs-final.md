# Java ‐ Instance Variable & Local Variable vs final

#### 인스턴스 변수의 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f2dc579a-86bc-4b3d-b382-bb08279436eb.png)

* 인스턴스 변수의 경우 **힙 영역**에서 보관된다.
* 쓰레드마다 해당 인스턴스 공유 자원을 같이 사용하게 되기 때문에 동시성 문제가 발생할 수 있다.

#### 지역 변수의 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/395c8f0f-26eb-454a-b640-540c1a8883d5.png)

* 지역 변수의 경우 각 쓰레드 별로 생성되는 **스택 영역**에서 보관된다.
* MyCounter 인스턴스에서 공유되는 자원이 없기 때문에 동시성 문제에서 벗어날 수 있다.

#### final이 붙은 경우

* 여러 쓰레드가 공유 자원에 접근하는 것 자체는 문제가 되지 않는다.
* 하지만 이 공유 자원을 사용하는 중간에 다른 쓰레드로부터의 요청이 들어와 공유 자원의 값을 변경하는 것이 문제가 되는 것이ㅏ다.
* 여러 쓰레드가 접근 가능한 공유 자원이라도 그 값을 아무도 변경할 수 없다면 문제가 되지 않는다. final이 붙은 경우 변화를 줄 수 없기 때문에 모든 쓰레드가 항상 같은 값을 읽게 된다.
* 필드에 final이 붙으면 어떤 쓰레드도 값을 변경할 수 없다. 따라서 멀티 쓰레드 상황에서 문제 없는 안전한 공유 자원이 된다.
