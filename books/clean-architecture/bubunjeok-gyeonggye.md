# (Clean Architecture 24) - 부분적 경계

#### 일차원 경계

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7d81cbdc-8554-4134-9351-dfbe1dfa094b.png)

* 인터페이스 하나로 심어두는 경계. 의존성 역전(DIP)이 이미 적용된 상태이다.

#### 퍼싸드(Facade) 패턴

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c7925f90-bf6b-4d38-9406-18b87efb4571.png)

* Facade 클래스에 서비스들의 메서드를 나열해두고 호출이 오면 해당 서비스로 전달만 한다.
* Client는 서비스 클래스에 직접 접근하지 못한다.
* Facade는 인터페이스가 아니라 모든 서비스를 아는 클래스이다. `Client → Facade → ServiceA/B/C`로 추이 종속성이 그대로 이어지게 된다.
* Facade는 복잡한 서브시스템을 쓰기 쉽게 만드는 편의 목적의 패턴이다.
