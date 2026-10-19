# Test - JMeter

#### [JMeter](https://github.com/apache/jmeter)

* 성능 측정 및 부하 테스트 기능을 제공하는 오픈소스 자바 애플리케이션이다
* 다양한 형태의 애플리케이션 테스트를 지원한다.
  * 웹 - HTTP, HTTPS
  * SOAP / REST API 서비스
  * FTP
  * 데이터베이스
  * Mail(SMTP, POP3, IMAP)

#### 주요 개념

* `Thread Group` : 동시에 실행할 쓰레드 수(=가상 유저 수) 설정
* `Sampler` : 어떤 유저가 해야 하는 액션
* `Listener` : 응답을 받았을 때 할 일(리포팅, 검증, 그래프 그리기 등)
* `Configuration` : Sampler 또는 Listener가 사용할 설정 값
* `Assertion` : 응답이 성공적인지 확인하는 방법

#### JMeter 주의사항

* JMeter를 사용할거라면 테스트 대상이 되는 서버와 JMeter를 실행시키는 서버를 분리해야 한다. 그 이유는 다음과 같다.
  * JMeter 자체가 많은 리소스를 사용 - CPU, 메모리, 네트워크 대역폭을 소비, 테스트 대상 서버와 리소스 경합이 발생한다.
  * 측정 오류 방지 - JMeter 리소스 사용량과 실제 애플리케이션 리소스 사용량을 구분하기 어렵다. 성능 병목이 애플리케이션인지 아니면 JMeter인지 파악하기 어렵다.
  * 현실적인 테스트 환경 - 실제 환경도 부하 생성기와 서버가 분리되어 있다는 점을 감안해야 한다. 그래야 실제 성능을 정확히 측정할 수 있다.
* `Thread Group`
  * `Number of Threads` : 쓰레드 개수
  * `Ramp-up period` : 쓰레드 개수를 만드는데 소요되는 시간
  * `Loop Count` : `infinite` 옵션을 체크해서 활성화시키면 위에서 정한 쓰레드 개수로 계속 요청을 보내준다. 값을 입력하면 `해당 쓰레드 개수 x 루프 횟수`만큼 요청을 보낸다.
* `Sampler`
  * 여러 종류의 샘플러가 있지만 그 중에 우리가 사용할 샘플러는 HTTP Request 샘플러
  * HTTP Sampler : 요청을 보낼 호스트, 포트, URI, 요청 본문(Body) 지정
  * 여러 샘플러를 순차 등록하는 것도 가능하다.
* `Listener`
  * View Results Tree, View Results in Table
  * Summary Report, Aggregate Report
  * Response Time Graph, Graph Results
* `Assertion`
  * 응답 코드 확인
  * 응답 본문 확인
