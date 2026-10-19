# Docker - Container-to-Container Communication

## 📚 Docker Network - 컨테이너 간 통신

* Docker Networks는 네트워크 동작에 영향을 미치는 다양한 종류의 드라이버를 지원한다.
* 디폴트 드라이버는 `bridge` 드라이버이다. 컨테이너가 동일한 네트워크에 있는 경우 이름만으로 서로를 식별할 수 있다.
* 그 외 `host`, `overlay`, `macvlan`, `none` 등이 있다.
  * `host` : standalone 컨테이너의 경우 컨테이너와 호스트 시스템 간 격리가 제거되고 localhost를 네트워크로 공유한다.
  * `overlay` : 여러 Docker 데몬(즉, 서로 다른 머신에서 실행되는 Docker)이 서로 연결될 수 있다. 여러 컨테이너를 연결하는 거의 사용되지 않는 Swarm 모드에서만 동작한다.
  * `macvlan` : 컨테이너에 커스텀 MAC 주소를 설정할 수 있다. 그러면 이 주소를 해당 컨테이너와 통신하는데 사용할 수 있다.
  * `none` : 모든 네트워킹이 비활성화된다.
