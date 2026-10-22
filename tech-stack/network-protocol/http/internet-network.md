# HTTP - Internet Network

## 📚 IP(인터넷 프로토콜, Internet Protocol)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b8c6f2b7-42cb-4b95-abe7-36b8c65ab27a.png)

* 지정한 IP 주소로 데이터를 전달한다.
* 이 때, 패킷 단위로 데이터를 전달한다.

## 📚 IP 프로토콜의 한계

* 클라이언트 - 서버 간 IP 프로토콜을 사용해 패킷을 주고 받는다.
* IP 프로토콜의 한계
  * 비연결성 : 패킷을 받을 대상이 없거나 서비스 불능 상태여도 패킷을 그대로 전송한다.
  * 비신뢰성 : 중간에 패킷이 소실될 수 있고 패킷이 순서대로 가지 않을수도 있다.
  * 프로그램 구분 : 자신이 인터넷 게임과 음악 듣기를 동시에 하고 있다면 애플리케이션을 어떻게 구분할 수 있는지?
* 위와 같은 IP 프로토콜의 한계로 등장하게 된 것이 바로 TCP/UDP이다.

## 📚 TCP/IP 4계층과 OSI 7계층

> ❓ TCP/IP 4계층과 OSI 7계층(출처 : [Inpa Dev](https://inpa.tistory.com/entry/WEB-%F0%9F%8C%90-TCP-IP-%EC%A0%95%EB%A6%AC-%F0%9F%91%AB%F0%9F%8F%BD-TCP-IP-4%EA%B3%84%EC%B8%B5))

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/7b94ebea-fda9-4d36-8b88-0b13fd0859e3.png)

```
▣ TCP/IP 4계층

* 네트워크 인터페이스 계층 : 실질적으로 데이터를 전달하며 장치 간에 신호를 주고 받는 규칙을 정하는 계층이다.
* 인터넷 계층 : 장치로부터 받은 네트워크 패킷을 IP 주소로 지정된 목적지로 전송하기 위해 사용되는 계층이다. 대표적인 프로토콜로 IP, ARP, ICMP 등이 있다.
* 전송 계층 : 송신자와 수신자를 연결하는 통신 서비스를 제공하며, 연결 지향 데이터 스트림 지원, 신뢰성, 흐름 제어를 제공할 수 있으며 애플리케이션과 인터넷 계층 사이의 데이터가 전달될 때 중개 역할을 한다. 대표적인 프로토콜로 TCP, UDP가 있다.
* 응용 계층 : 응용 프로그램이 사용되는 계층이다. 대표적인 예시로 FTP, SMTP, HTTP, SSH, DNS 등이 있다.

* TCP 특징(Transmission Control Protocol, 전송 제어 프로토콜)
  * 연결 지향형 - TCP 3 way handshake(논리 연결 O, 물리 연결 X)
  * 데이터 전달 보증
  * 순서 보장
  * 신뢰할 수 있는 프로토콜
  * 현재 대부분 TCP 사용

* UDP 특징(User Datagram Protocol, 유저 데이터그램 프로토콜)
  * 연결 지향형 - TCP 3-way handshake 과정 X
  * 데이터 전달 보증 X
  * 순서 보장 X
  * 데이터 전달 및 순서가 보장되진 않지만 단순하고 빠르다.
```

## 📚 TCP 3-way handshake

* TCP는 신뢰성을 확보할 때, 3-way handshake라는 작업을 거치게 된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c7cce7f7-b38b-4fd8-8a27-65e5c2ae41e2.png)

①. SYN 단계 : 클라이언트가 서버에 초기 네트워크 연결을 위해 ISN(Initial Sequence Numbers)을 보낸다.<br>
②. SYN + ACK 단계 : 서버가 클라이언트의 ISN을 수신하고 서버의 ISN을 보낸다.(이 때, 서버 측에서 보내는 ISN은 클라이언트 측에서 보내는 ISN + 1을 보낸다.)<br>
③. ACK 단계 : 클라이언트는 서버 측에서 보낸 ISN + 1값을 승인한 승인 번호를 담아 ACK를 보낸다.

## 📚 PORT

* 같은 IP 내에서 프로세스를 구분하는 용도로 사용한다.
* 0 ~ 65535 :  할당 가능하다.
* 0 ~ 1023 : Well Known Port(잘 알려진 포트), 사용하지 않는 것이 좋다.
  * FTP : 20, 21
  * TELNET : 23
  * HTTP : 80
  * HTTPS : 443

## 📚 DNS(Domain Name System)

* 도메인 명을 IP주소로 변환한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6662b8c2-3987-4e24-a28b-efc196432d02.png)
