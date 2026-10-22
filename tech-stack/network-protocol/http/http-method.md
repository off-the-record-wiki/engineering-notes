# HTTP - HTTP Method

## 📚 HTTP API 개발

> 회원 관련 API를 설계

* 회원 목록 조회 : `/members`
* 회원 조회 : `/members/{id}`
* 회원 등록 : `/members/{id}`
* 회원 수정 : `/members/{id}`
* 회원 삭제 : `/members/{id}`

> 참고 : 계층 구조상 상위를 컬렉션으로 보고 복수 단어 사용을 권장한다.(member → members)

* API를 설계할 때, URI는 리소스만 식별한다.
* 리소스와 해당 리소스를 대상으로 하는 행위를 분리해야 한다.
  * 리소스 : 회원
  * 행위 : 조회, 등록, 삭제, 변경
* 리소스는 명사, 행위는 동사
* 행위(메서드)는 HTTP 메서드로 구분한다.

## 📚 HTTP 메서드 - GET, POST

> GET : 리소스 조회

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c85f56a7-0613-42bd-ae34-4b6867d069b8.png)

* 리소스 조회
* 서버에 전달하고 싶은 데이터는 쿼리 파라미터 혹은 쿼리 스트링을 통해서 전달한다.
* 메시지 바디를 사용해서 데이터를 전달할 수 있지만 지원하지 않는 곳이 많아서 권장하지 않는다.

> POST : 요청 데이터 처리, 주로 등록에 사용

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/500636f4-6009-46a8-bfb9-b338bc582b8d.png)

* 요청 데이터 처리
* 메시지 바디를 통해 서버로 요청 데이터 전달
* 서버는 요청 데이터를 처리한다.
  * 메시지 바디를 통해 들어온 데이터를 처리하는 모든 기능을 수행한다.
* 주로 전달된 데이터로 신규 리소스 등록, 프로세스 처리에 사용한다. 

## 📚 HTTP 메서드 - PUT, PATCH, DELETE

> PUT : 리소스 대체(완전 대체)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ee4e88ed-0b54-437d-94c5-28018799458c.png)

* 리소스가 있으면 대체한다.
* 리소스가 없으면 생성한다.
* 클라이언트가 리소스 위치를 알고 URI를 지정한다.

> PATCH : 리소스 대체(일부 대체)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3b1f72ea-be5b-429c-8187-66b7771491e4.png)

* 리소스를 부분 변경한다.

> DELETE : 리소스 삭제

* 리소스를 삭제한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/baa4d36a-60ba-4a0b-b443-f2fbaa212efc.png)

## 📚 HTTP 메서드 - 기타 메서드

* HEAD : GET과 동일하지만 메시지 부분을 제외하고 상태 줄과 헤더만 반환한다.
* OPTIONS : 대상 리소스에 대한 통신 가능 옵션(메서드)을 설명(주로 CORS에서 사용)한다.
* CONNECT : 대상 자원으로 식별되는 서버에 대한 터널을 설정한다.
* TRACE : 대상 리소스에 대한 경로를 따라 메시지 루프백 테스트를 수행한다.

## 📚 HTTP 메서드 속성

> 안전(Safe Methods) 

* 호출해도 리소스를 변경하지 않는다.
* 리소스를 호출해서 로그가 쌓이는 등 그런 부분까지 고려하지 않는다.

> 멱등(Idempotent Methods)

* f(f(x)) = f(x)
* 한 번 호출하든 두 번 호출하든 100번 호출하든 **결과가 똑같다.**
* 외부 요인에 의해 중간에 리소스가 변경되는 것까지 고려하지 않는다.

```
GET ; 한 번 조회하든 두 번 조회하든 결과는 똑같다.(멱등O)
PUT : 첫 번째 호출에 결과를 대체하고 그 이후 호출에도 결과는 똑같다.(멱등O)
DELETE : 첫 번째 호출에 리소스를 삭제하고 그 이후 호출에도 삭제된 결과는 똑같다.(멱등O)
POST : 한 번 호출하면 리소스가 생기고 그 이후 호출을 계속하면 리소스가 계속 생긴다.(멱등X)
```

> 캐시 가능(Cacheable)

* 응답 결과 리소스를 캐시해서 사용해도 되는가?
* 실제로 GET, HEAD 정도만 캐시로 사용한다.
