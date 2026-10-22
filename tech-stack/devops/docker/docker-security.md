# Docker - Docker Security

## 📚 Docker와 Cloud Native Application 보안

* 컨테이너, 서버리스, 마이크로서비스 등 다양한 클라우드 기반 기술을 활용하는 애플리케이션의 전체 생명 주기에 걸쳐 보안을 적용
* 멀티 클라우드 환경에서의 데이터 보호, 동적 보안 정책 적용, 연속적인 보안 모니터링 등을 포함

|Docker 보안|Cloud Native Application 보안|
|:----:|:----:|
|컨테이너 수준에서의 보안 조치|보다 광범위한 클라우드 환경과 여러 서비스를 통합해 보안 강화|
|이미지와 컨테이너 취약점 관리에 초점 / 컨테이너 런타임 시 보안 정보 사용|동적 환경에서의 정책 기반 보안과 통합된 로깅/모니터링 전략 사용|
|이미지 보안 / 컨테이너 격리|Cloud Native Application을 구성하는 서비스들의 보안 기술 사용|

### [Docker Scout](https://docs.docker.com/scout/)
* 이미지를 분석해 구성 요소 인벤토리를 컴파일
* Docker Desktop에서는 기본 제공
* Docker Desktop, Docker Hub, Docker CLI, Docker Scout 대시보드와 상호작용가능한 독립적인 서비스 플랫폼

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4c851e10-a37e-43ce-b993-cda24c108015.png)

## 📚 Docker Daemon Security

* 컨테이너 실행을 위한 Docker Engine에 대한 보안

## 📚 Container Image Security

* 컨테이너 이미지가 안전하게 생성되었는지 검증 -> 이미지에 대한 보안 취약성 문제점 확인
* 컨테이너 이미지는 취약점에 민감한 레이어와 SW로 구성 -> 검증된 기본 이미지를 사용
* 보안 취약점 점검 필요 -> Clair, Trivy, Docker Security Scanning
* 이미지 크기 최소화 -> alpine

### [Trivy](https://trivy.dev/latest/)
* 컨테이너 취약점 및 구성 문제를 쉽게 검색할 수 있도록 설계된 보안 스캐너
* Trivy는 취약성 탐지 정확성이 높으며 데이터베이스 설치를 위한 사전 요구사항이 없다.

## 📚 Container Isolation

* 컨테이너 실행이 다른 컨테이너나 호스트에 영향을 주지 않는 독립적인 실행 환경인지?
* 컨테이너와 호스트는 기본적으로 격리 -> 무단 액세스 방지 및 위험을 최소화하기 위해 격리 강화
* Namespace Isolation : 프로세스, 네트워킹, 파일 시스템 격리
* Control Groups : 리소스를 보호하기 위해 CPU, Memory, I/O 리소스 제한
* 컨테이너 내의 사용자 ID를 호스트 다른 사용자 ID에 매핑하여 권한 상승 공격 위험 감소

## 📚 Runtime Security - Capabilities 제어

* 컨테이너 실행동안 지속적인 모니터링과 관리
