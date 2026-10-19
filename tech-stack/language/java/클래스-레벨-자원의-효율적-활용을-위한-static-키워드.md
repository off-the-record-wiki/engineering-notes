# Java ‐ Static Keyword: Efficient Resource Management at the Class Level

#### static 키워드

* static 변수
  * 클래스에 속하는 변수로 객체가 아닌 클래스 레벨에서 관리된다.
  * 객체마다 따로 존재하지 않고, 클래스 단위로 하나만 존재한다.
  * 모든 객체가 값을 공유하며, 공통 데이터 저장 용도로 사용된다.
  * 메모리의 Method Area에 저장된다.
* static 변수의 주요 특징
  * 객체를 생성하지 않아도 사용이 가능
  * `클래스명.변수명` 형식으로 접근
  * 객체가 없어져도 메모리 상에는 계속 존재하며, 프로그램 종료 시까지 유지된다.
