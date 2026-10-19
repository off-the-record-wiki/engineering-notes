# HTTP - HTTP Method Application

## 📚 클라이언트에서 서버로 데이터 전송

> HTML Form 데이터 전송

* HTML Form submit시 POST로 전송해야한다.
* Content-Type: application/x-www-form-urlencoded 사용
  * form 내용을 메시지 바디를 통해서 전송(key=value, 쿼리 파라미터 형식)
  * 전송 데이터를 url encoding 처리(한글 데이터)
* HTML Form은 GET 전송도 가능하다.
* Content-Type: multipart/form-data 
  * 파일 업로드와 같은 바이너리 데이터 전송시에 사용한다.
  * 다른 종류의 여러 파일과 폼의 내용을 함께 전송할 수 있다.
* HTML Form의 경우 GET, POST 메서드만 사용이 가능하다.

> HTTP API 데이터 전송

* POST, PUT, PATCH : 메시지 바디를 통해 데이터 전송
* GET : 조회, 쿼리 파라미터(쿼리 스트링)로 데이터 전달
* Content-Type: application/json(사실상 표준)

## 📚 HTTP API 설계 예시

> 회원 관리 시스템 예시 : POST 기반 등록

* 클라이언트는 등록될 리소스의 URI를 모른다.
* 서버가 새로 등록된 리소스 URI를 생성해준다.
* 컬렉션(Collections) 
  * 서버가 관리하는 리소스 디렉터리
  * 서버가 리소스 URI를 생성하고 관리한다.
  * 여기서 컬렉션은 `/members`가 된다.(복수 권장)
* [찾아본 참고자료 - Rest Resource Naming](https://restfulapi.net/resource-naming/)


```
* 회원 목록 : /members → GET
* 회원 등록 : /members → POST
* 회원 조회 : /members/{id} → GET
* 회원 수정 : /members/{id} → PATCH, PUT, POST
* 회원 삭제 : /members/{id} → DELETE
```

> 파일 관리 시스템 : PUT 기반 등록

* 클라이언트가 리소스의 URI를 알고 있어야 한다.
* 클라이언트가 직접 리소스의 URI를 지정한다.
* 스토어(Store)
  * 클라이언트가 관리하는 리소스 저장소
  * 클라이언트가 리소스의 URI를 알고 관리한다.
  * 여기서 스토어는 `/files`

```
* 파일 목록 : /files → GET 
* 파일 조회 : /files/{fileName} → GET
* 파일 등록 : /files/{fileName} → PUT
* 파일 삭제 : /files/{fileName} → DELETE
* 파일 대량 등록 : /files → POST
```
