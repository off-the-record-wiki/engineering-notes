# 가상 면접 사례로 배우는 대규모 시스템 설계 기초 1 - 알림 시스템 설계

#### 알림 유형별 지원 방안 - iOS 푸시 알림

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/bfb6eac0-c4f1-4fca-bbb1-4d946d4ccc5a.png)

* iOS에서 푸시 알림을 보내기 위해서는 3가지 컴포넌트가 필요하다.
* 알림 제공자(provider) : 알림 요청을 만들어 애플 푸시 알림 서비스(APNS)로 보내는 주체다. 알림 요청을 만들려면 다음과 같은 데이터가 필요하다.
  * 단말 토큰(device token) : 알림 요청을 보내는 데 필요한 고유 식별자이다.
  * 페이로드(payload) : 알림 내용을 담은 JSON 딕셔너리다.
  * APNS : 애플이 제공하는 원격 서비스다. 푸시 알림을 iOS 장치로 보내는 역할을 담당한다.
  * iOS 단말(iOS device) : 푸시 알림을 수신하는 사용자 단말이다. 

#### 알림 유형별 지원 방안 - 안드로이드 푸시 알림

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5348b347-5e6d-4d24-bbb9-b41bd6f9707e.png)

* 안드로이드 푸시 알림도 iOS와 비슷한 절차로 전송된다. APNS 대신 FCM(Firebase Cloud Messaging)을 사용한다는 점이 다르다.

#### 알림 유형별 지원 방안 - SMS 메시지

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3e27b18c-f764-431d-8a7d-46366522e719.png)

* SMS 메시지를 보낼 때는 제3사업자의 서비스를 많이 이용한다.
* 이런 서비스는 대부분 상용 서비스라서 이용요금을 내야 한다.

#### 알림 유형별 지원 방안 - 이메일

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/67a9d849-93ff-4b78-b295-df66e6d0120a.png)

* 많은 회사에서 상용 이메일 서비스를 이용한다.
* 전송 성공률도 높고, 데이터 분석 서비스도 제공한다.

#### 연락처 정보 수집 절차

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e4b939ab-b366-46b4-91b3-b69f162b0627.png)

* 알림을 보내려면 모바일 단말 토큰, 전화번호, 이메일 주소 등의 정보가 필요하다.
* 사용자가 자사 앱을 설치하거나 처음으로 계정을 등록하면 API 서버는 해당 사용자 정보를 수집해 데이터베이스에 저장한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fb0c7f87-2060-48af-926f-f1376119f599.png)

* 이 데이터베이스에 연락처 정보를 저장할 테이블 구조는 위와 같다.
* 필수 정보만 담은 개략적인 설계안으로서 이메일 주소와 전화번호는 `user` 테이블에 저장하고 단말 토큰은 `device` 테이블에 저장한다.
* 한 사용자가 여러 단말을 가질 수 있고 알림은 모든 단말에 전송되어야 한다는 점을 고려한 설계다.

#### 알림 전송 및 수신 절차(AS-IS)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/54f89b64-f461-4a9d-a2d2-100cdab7c4af.png)

* SPOF(Single-Point-Of-Failure) : 알림 서비스에 서버가 하나밖에 없다는 것은, 그 서버에 장애가 생기면 전체 서비스 장애로 이어진다는 뜻이다.
* 규모 확장성 : 한 대 서비스로 푸시 알림에 관계된 모든 것을 처리하므로 데이터베이스나 캐시 등 중요 컴포넌트의 규모를 개별적으로 늘릴 방법이 없다.
* 성능 병목 지점 : 알림을 처리하고 보내는 것은 자원을 많이 필요로 하는 작업일 수 있다. 모든 것을 한 서버로 처리하면 사용자 트래픽이 몰리는 시간에 시스템이 과부하 상태에 빠질 수 있다.

#### 알림 전송 및 수신 절차(TO-BE)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c684d1de-92bb-4f00-8894-82b7a8270305.png)

* 데이터베이스와 캐시를 알림 시스템 주 서버에서 분리한다.
* 알림 서버를 증설하고 자동으로 수평적 규모 확장이 이루어질 수 있도록 한다.
* 메시지 큐를 이용해 시스템 컴포넌트 사이 강결합을 끊는다.

#### 안정성 - 데이터 손실 방지

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/58ddf840-2dd4-4e91-92cc-ff868855c1af.png)

* 알림 전송 시스템의 중요한 요구사항 가운데 하나는 어떤 상황에서도 알림이 소실되면 안 된다고 명시되어있으나 알림은 부가적인 비즈니스 기능이기에 이 말이 꼭 옳다고는 생각하지 않는다. 알림이 지연되거나 순서가 틀려도 괜찮다.

#### 안정성 - 알림 중복 전송 방지

* 같은 알림이 여러 번 반복되는 것을 완전히 막는 것은 가능하지 않다.
* 대부분의 경우 알림은 딱 한 번만 전송되는데 분산 시스템의 특성상 가끔 같은 알림이 중복되어 전송되기도 할 것이다.
* 그 빈도를 줄이려면 중복을 탐지하는 메커니즘을 도입하고 오류를 신중히 처리해야 한다.
* 보내야 할 알림이 도착하면 그 이벤트 ID를 검사해 이전에 본 적이 있는 이벤트인지 살핀다. 중복된 이벤트라면 버리고 그렇지 않으면 알림을 발송한다.

#### 추가로 필요한 컴포넌트 및 고려사항 1 - 알림 템플릿

* 대형 알림 시스템은 하루에 수백만 건 이상의 알림을 처리한다. 그런데 그 알림 메시지 대부분 형식이 비슷하다.
* 알림 템플릿은 이런 유사성을 고려해 알림 메시지 모든 부분을 처음부터 다시 만들 필요가 없도록 해준다.
* 알림 템플릿은 인자나 스타일, 추적 링크를 조정하기만 하면 사전에 지정한 형식에 맞춰 알림을 만들어 내는 틀이다.
  * 본문
  * 타이틀
* 템플릿을 사용하면 전송될 알림들의 형식을 일관성있게 유지할 수 있고, 오류 가능성뿐 아니라 알림 작성에 드는 시간도 줄일 수 있다.

#### 추가로 필요한 컴포넌트 및 고려사항 2 - 알림 설정

* 사용자는 이미 너무 많은 알림을 받고 있어 쉽게 피곤함을 느낀다.
* 따라서 많은 웹사이트와 앱에서는 사용자가 알림 설정을 상세히 조정할 수 있도록 하고 있다.
* 이 정보는 알림 설정 테이블에 보관되며, 이 테이블에는 다음과 같은 필드들이 필요할 것이다.
  * `user_id` : `bigint`
  * `channel` : `varchar` - 알림이 전송될 채널. 푸시 알림, 이메일, SNS 등
  * `opt_in` : `boolean` - 해당 채널로 알림을 받을 것인지 여부
* 이와 같은 설정을 도입한 뒤에는 특정 종류의 알림을 보내기 전에 반드시 해당 사용자가 알림을 활성화했는가를 확인해야 한다.

#### 추가로 필요한 컴포넌트 및 고려사항 3 - 전송률 제한

* 사용자에게 너무 많은 알림을 보내지 않도록 하는 한 가지 방법은 한 사용자가 받을 수 있는 알림의 빈도를 제한하는 것이다.
* 알림을 너무 많이 보내기 시작하면 사용자가 알림 기능을 꺼버릴 수도 있기 때문이다.

#### 추가로 필요한 컴포넌트 및 고려사항 4 - 재시도 방법

* 제3자 서비스가 알림 전송에 실패하면 해당 알림을 재시도 전용 큐에 넣는다.
* 같은 문제가 계속해서 발생하면 개발자에게 통지한다.

#### 추가로 필요한 컴포넌트 및 고려사항 5 - 푸시 알림과 보안

* iOS와 안드로이드 앱의 경우 알림 전송 API는 `appKey`, `appSecret`을 사용하여 보안을 유지한다.
* 따라서 인증된(authenticated), 혹은 승인된(verified) 클라이언트만 해당 API를 사용해 알림을 보낼 수 있다.

#### 추가로 필요한 컴포넌트 및 고려사항 6 - 큐 모니터링

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f5c22a7d-a57c-4b53-953e-5329181a071b.png)

* 알림 시스템을 모니터링 할 때 중요한 메트릭 하나는 큐에 쌓인 알림의 개수이다.
* 이 수가 너무 크면 작업 서버들이 이벤트를 빠르게 처리하고 있지 못하다는 뜻이다.
* 이런 경우 작업 서버를 증설하는 것이 바람직하다.

#### 추가로 필요한 컴포넌트 및 고려사항 7 - 이벤트 추적

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/de41cd01-1b59-44eb-98ce-d0a7981cbf22.png)

* 알림 확인율, 클릭율, 실제 앱 사용으로 이어지는 비율 같은 메트릭은 사용자를 이해하는 데 중요하다.
* 데이터 분석 서비스는 보통 이벤트 추적 기능도 제공한다.
* 보통 알림 시스템을 만들면 데이터 분석 서비스와도 통합해야만 한다.

#### 최종 시스템 디자인 설계

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/80dcb445-e235-4083-ac1f-42e8062d785c.png)

* 알림 서버는 인증과 전송률 제한을 거친 요청을 받고 캐시와 데이터베이스에서 사용자 정보와 설정을 읽으며, 템플릿으로 알림 이벤트를 만든다.
* 이벤트는 채널별 큐에 들어가고 작업 서버가 이를 꺼내 외부 서비스로 보낸다. 실패하면 재시도하고 전송 과정은 로그와 모니터링 시스템에 남으며, 사용자 클릭은 데이터 분석 서비스로 돌아온다.
