# Spring ‐ MVC Framework

#### 프론트 컨트롤러 패턴

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/45d6857d-4e67-4077-bb59-01a4966f972d.png)

* 프론트 컨트롤러 패턴(Front Controller Pattern 특징)
  * 단일 진입점 : 모든 HTTP 요청이 하나의 서블릿으로 집중된다.
  * 공통 로직 처리 : 보안, 인증, 권한 체크, 로깅, 다국어 처리, 인코딩 변환 등 모든 요청에서 반복되는 작업을 프론트 컨트롤러가 전담한다.
  * 뷰 렌더링 통합 : 하위 컨트롤러가 뷰를 직접 호출하는 것이 아니라 프론트 컨트롤러가 어떤 뷰를 최종적으로 렌더링할지 결정하고 실행하는 일관된 프로세스를 가진다.
