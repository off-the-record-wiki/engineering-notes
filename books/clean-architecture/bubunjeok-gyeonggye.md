# (Clean Architecture 24) - 부분적 경계

#### 일차원 경계

![](https://github.com/user-attachments/assets/d5c86582-202d-461e-beab-f7dc8386e36d)

* 완벽한 형태의 아키텍처 경계는 양방향으로 격리된 상태를 유지해야 하므로 쌍방향 Boundary 인터페이스를 사용한다.
* 양방향으로 격리된 상태를 유지하려면 초기 설정할 때나 지속적으로 유지할 때도 비용이 많이 든다.

#### 퍼싸드(Facade) 패턴

![](https://github.com/user-attachments/assets/53d5017e-da89-4405-9722-dd2b882c11b7)

* 경계는 Facade 클래스로만 간단히 정의된다. Facade 클래스에는 모든 서비스 클래스를 메서드 형태로 정의하고, 서비스 호출이 발생하면 해당 서비스 클래스로 호출을 전달한다. 클라이언트는 이들 서비스 클래스에 직접 접근할 수 없다.
