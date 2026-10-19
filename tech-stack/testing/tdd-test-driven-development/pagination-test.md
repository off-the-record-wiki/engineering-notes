# TDD - Pagination Test

#### [Continuation Token](https://developers.feedly.com/docs/understanding-continuation)

* 데이터를 순차적으로 탐색하는 페이지네이션 기법이다.
* 페이지네이션 데이터가 문자열 형식의 토큰으로 추상화된다.
* 요청에 토큰이 지정되지 않으면 첫 페이지를 반환한다.
* 반환된 페이지는 토큰을 포함한다.
* 요청에 토큰이 지정되면 토큰에 해당하는 위치의 페이지를 반환한다.
* 마지막 페이지는 토큰을 포함하지 않는다.
* 클라이언트의 책임이 가벼워진다.
* 무한 스크롤 UI에서 많이 사용된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4091c839-a688-4414-b849-941474077844.png)
