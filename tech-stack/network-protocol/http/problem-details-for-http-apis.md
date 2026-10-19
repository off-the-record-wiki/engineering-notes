# HTTP - Problem Details for HTTP APIs

#### ProblemDetails

* HTTP API의 에러 응답 표준 포맷이다.
* RFC 7807(개정판 RFC 9457) "Problem Details for HTTP APIs"라는 IETF 표준 스펙이고 Spring 6부터 `org.springframework.http.ProblemDetail` 클래스로 내장 지원된다.
* 표준이 없다면 API마다 에러 응답이 제각각이 된다.

```
{"error": "duplicate email"}          // A팀 스타일
{"code": -1, "msg": "중복"}            // B팀 스타일
{"success": false, "reason": "..."}   // C팀 스타일
```

* 클라이언트가 API마다 에러 파싱 코드를 따로 짜야 한다. RFC 7807은 표준 필드로 통일한다.

|필드|의미|
|:---:|:---:|
|`type`|에러 유형을 식별하는 URI(기본값은 `about:black`)|
|`title`|에러 유형의 짧은 요약|
|`status`|HTTP 상태 코드|
|`detail`|이 발생 건에 대한 구체적인 설명|
|`instance`|에러가 발생한 요청 검토|
|확장 필드|자유롭게 추가 가능|

#### 레퍼런스

* [Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
* [RFC 7807: Problem Details for HTTP APIs](https://www.rfc-editor.org/info/rfc7807/)
* [ProblemDetail](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ProblemDetail.html)
