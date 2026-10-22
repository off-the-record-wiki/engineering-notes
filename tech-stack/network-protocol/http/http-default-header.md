# HTTP ‐ HTTP Default Header

## 📚 표현

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4d44e233-136b-4bf7-91e9-ccea6ae33754.png)

* 표현 헤더는 전송, 응답 둘 다 사용한다.
* Content-Type : 표현 데이터의 형식
* Content-Encoding : 표현 데이터 압축 방식
* Content-Language : 표현 데이터 자연 언어
* Content-Length : 표현 데이터 길이

## 📚 컨텐츠 협상

* 협상 헤더는 요청시에만 사용한다.
* Accept : 클라이언트가 선호하는 미디어 타입 전달
* Accept-Charset : 클라이언트가 선호하는 문자 인코딩
* Accept-Encoding : 클라이언트가 선호하는 압축 인코딩
* Accept-Language : 클라이언트가 선호하는 자연 언어

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ec169aa4-851c-499d-a150-dbab2f401c31.png)

* 협상과 우선순위1(Quality Values(q))
  * Quality Values(q) 값 사용
  * 0 ~ 1, 클수록 높은 우선순위라는 의미
  * 생략하면 기본적으로 1

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/77beda07-2e67-4655-b98f-d170d5f33be3.png)

* 협상과 우선순위2(Quality Values(q))
  * 구체적인 것이 우선한다.

## 📚 전송 방식

* 단순 전송

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a0dc4c9d-916f-45b9-9ed9-2ef1851b330c.png)

* 압축 전송

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b0200b2a-3e45-4ea5-9704-2ff7915e5712.png)

* 분할 전송

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/966c82b6-7f55-432f-bf57-06fb3840e3b2.png)

* 범위 전송

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/192d9a63-8b17-4aaa-8703-cede466a537a.png)

## 📚 일반 정보

* From : 유저 에이전트 이메일 정보
* Referer : 이전 웹 페이지 주소
* User-Agent : 유저 에이전트 애플리케이션 정보
* Server : 요청을 처리하는 Origin 서버의 소프트웨어 정보
* Date : 메시지가 발생한 날짜와 시간

## 📚 특별한 정보

* Host : 요청한 호스트 정보(도메인)
* Location : 페이지 리다이렉션
* Allow : 허용 가능한 HTTP 메서드
* Retry-After : 유저 에이전트가 다음 요청을 하기까지 기다려야 하는 시간

## 📚 인증

* Authorizaiton : 클라이언트 인증 정보를 서버에 전달
* WWW-Authenticate : 리소스 접근시 필요한 인증 방법 정의

## 📚 쿠키

* Set-Cookie : 서버에서 클라이언트로 쿠키를 전달(응답)
* Cookie : 클라이언트가 서버에서 받은 쿠키를 저장하고 HTTP 요청시 서버로 전달

---

* 로그인에 성공한 후 서버에서 쿠키를 만들어 응답으로 보낸다.
* 웹 브라우저가 쿠키 저장소에 해당 쿠키를 저장한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/45c3aa7d-d319-47fd-a9ba-6951f11099e0.png)

* 로그인 이후 인증을 필요로 하는 페이지에 접근할 때 쿠키를 포함해서 요청을 보낸다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/551a1282-037e-42cb-a071-469e522b58a3.png)

> ❗ 모든 요청에 사용자 정보를 포함해서 전송하는 방식의 문제점

* 모든 요청에 쿠키를 포함해서 전송할 경우 보안에 민감한 데이터가 유출될 문제가 있다.
* 허나 문제점만 있는 것은 아닌 것이 무상태로 설계하는 대표적인 케이스가 바로 JWT 토큰 기반 인증 방식이다.
* 이 JWT로 설계를 할 때 쿠키 정보를 클라이언트가 요청으로 보내고 서버에서 응답으로 쿠키를 만들어서 보내준다.
* 물론 네트워크 트래픽이 추가로 유발되긴하나 모든 기술에는 기회비용이라는 것이 존재하듯이 보안의 관점에서 보았을 때(단, 보안설정을 한 경우에 한해서) 효율적이다.

## 📚 쿠키 - 생명주기

* Set-Cookie: expires=~ → 만료일이 되면 쿠키 삭제
* Set-Cookie: max-age=3600(3600초), 0이나 음수 지정 시 쿠키 삭제
* 세션 쿠키 : 만료 날짜를 생략하면 브라우저 종료시 까지만 유지된다.
* 영속 쿠키 : 만료 날짜를 입력하면 해당 날짜까지 유지된다.

## 📚 쿠키 - 도메인

> Ex. domain = example.org

* 명시 : 명시한 문서 기준 도메인 + 서브 도메인 포함
* domain = example.org를 지정해서 쿠키 생성
  * example.org는 물론이고 dev.example.org도 쿠키 접근 가능
* 생략 : 현재 문서 기준 도메인만 적용
  * example.org에서 쿠키를 생성하고 domain 지정을 생략
    * example.org에서는 쿠키 접근 가능
    * dev.example.org에서는 쿠키 접근 불가능

## 📚 쿠키 - 경로

* 해당 경로를 포함한 하위 경로 페이지에만 쿠키 접근 가능

> Ex. path = /home

* /home에서는 쿠키 접근 가능
* /home/level1에서는 쿠키 접근 가능
* /hello에서는 쿠키 접근 불가능

## 📚 쿠키 - 보안

* Secure : 쿠키는 http, https를 구분하지 않고 전송, Secure를 활성화하면 https인 경우에만 전송한다.
* HttpOnly : XSS 공격 방지, 자바스크립트에서 접근이 불가능, HTTP 전송에만 사용한다.
* SameSite : XSRF 공격 방지, 요청 도메인과 쿠키에 설정된 도메인이 같은 경우에만 쿠키를 전송할 수 있다.
