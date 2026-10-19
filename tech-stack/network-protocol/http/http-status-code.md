# HTTP - HTTP Status Code

## 📚 2xx - 성공

```
* 200 OK : 요청 성공
* 201 Created : 요청 성공해서 새로운 리소스가 생성된 경우
* 202 Accepted : 요청이 접수되었으나 처리가 완료되지 않았음
* 204 No Content : 서버가 요청을 성공적으로 수행했지만 응답 페이로드 본문에 보낼 데이터가 없는 경우
```

## 📚 3xx - 리다이렉션1 / 리다이렉션2

* 영구 리다이렉션 - 특정 리소스의 URI가 영구적으로 이동
* 일시 리다이렉션 - 일시적인 변경, PRG(Post/Redirect/Get)
* 특수 리다이렉션 - 결과 대신 캐시를 사용

> 영구 리다이렉션 - 301, 308

* 리소스의 URI가 영구적으로 이동
* 원래의 URL을 사용X, 검색 엔진 등에서도 변경 인지
* 301 Moved Permanently : 리다이렉트시 요청 메서드가 GET으로 변하고 본문이 제거될 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4ce8c5b7-bff3-46ed-a868-eb7abd5e3b78.png)

* 308 Permanent Redirect : 301과 동일한 기능, 다만 리다이렉트 요청 메서드와 본문이 유지된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3ea91655-aaa3-4d07-8c69-e0468d3f7079.png)

> 일시 리다이렉션 - 302, 303, 307

* 리소스의 URI가 일시적으로 변경
* 검색 엔진 등에서 URL을 변경하면 안 된다.
* 302 Found : 리다이렉트시 요청 메서드가 GET으로 변하고 본문이 제거될 수 있다.
* 303 See Other : 리다이렉트시 요청 메서드가 GET으로 변한다.
* 307 Temporary Redirect : 리다이렉트시 요청 메서드와 본문을 유지한다.

### ❓ PRG: Post/Redirect/Get

* POST로 처리한 후 새로고침을 하게 되면 다시 POST로 요청을 보내게 된다.
* 따라서, POST로 요청 후 결과 화면을 GET으로 리다이렉트시킨다.
* 여기서 새로고침해도 결과는 GET으로 조회를 하기 때문에 안전하다.

> 기타 리다이렉션 - 300, 304

* 300 Multiple Choices : 안 쓴다.
* 304 Not Modified
  * 캐시를 목적으로 사용한다. 
  * 304 응답에 메시지 바디를 포함하면 안 된다. 
  * 클라이언트에게 리소스가 수정되지 않았음을 알려준다.

## 📚 4xx - 클라이언트 오류

```
* 400 Bad Request : 클라이언트가 잘못된 요청을 해서 서버가 요청을 처리할 수 없다. 요청 내용을 검토해서 다시 보내야 한다.
* 401 Unauthorized
  * 클라이언트가 해당 리소스에 대한 인증이 필요한 경우이다. 
  * 인증되지 않음을 말하는 경우
  * 401 오류 발생 시 응답에 WWW-Authenticate 헤더와 함께 인증 방법을 설명한다.
  * 인증과 인가는 다른 문제
* 403 Forbidden : 서버가 요청을 이해했지만 승인을 거부한 경우
* 404 Not Found : 요청 리소스를 찾을 수 없는 경우
```

## 📚 5xx - 서버 오류

```
* 500 Internal Server Error : 서버 문제로 오류 발생
* 503 Service Unavailable : 서버가 일시적인 과부하 또는 예정된 작업으로 잠시 요청을 처리할 수 없는 경우, Retry-After 헤더 필드로 얼마 뒤에 복구되는지 보낼 수도 있다.
```

#### 외부 서비스와의 연동 과정에서 발생하는 오류에 대한 상태 코드

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/70db4fae-b4fa-4ada-82ef-4d9e00a2b2f0.png)

* 503 Service Unavailable는 일시적으로 서버가 요청을 처리할 수 없는 상황을 가정한 상태이다.
