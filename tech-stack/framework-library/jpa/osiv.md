# JPA - OSIV

📚 API 개발 - OSIV(Open Session Iin View)

![스크린샷 2025-01-02 오전 11 32 58](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2d955597-ba61-4ab2-8e6f-babbc610261a.png)

* OSIV 전략은 최초 데이터베이스 커넥션 시작 시점부터 API 응답이 끝날 때까지 영속성 컨텍스트와 데이터베이스 커넥션을 유지한다.
* 지연 로딩은 영속성 컨텍스트가 살아있어야 가능하고 영속성 컨텍스트는 기본적으로 데이터베이스 커넥션을 유지한다.
* 하지만 이 전략은 너무 오랜 시간동안 리소스를 사용하기 때문에 실시간 트래픽이 많이 발생하는 서비스의 경우 장애로 이어진다.
* OSIV 설정을 off하면 영속성 컨텍스트의 범위는 아래와 같아진다.

![스크린샷 2025-01-02 오후 4 30 39](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/58a1c944-06ca-43c2-a385-1ad6c4365e50.png)

* 컨트롤러에서 설정해준 지연 로딩이 작동하지 않는다.
* [참고사항] : [CQS](https://en.wikipedia.org/wiki/Command%E2%80%93query_separation)
