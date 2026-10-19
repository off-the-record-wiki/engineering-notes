# Docker - Docker Essentials(1)

## 📚 Docker와 컨테이너 가상화 기술

* 컨테이너 기반의 오픈소스 가상화 플랫폼
* 기존 가상화 방식 -> OS를 가상화
* CPU의 가상화 기술 이용 방식 -> Kernel-based Virtual Machine
  * 전체 OS를 가상화하지 않음, 호스트 형식에 비해 속도 향상
  * OpenStack, AWS 등의 클라우드 서비스
  * 추가적인 OS는 여전히 필요, 성능 문제 발생
* 프로세스 격리 -> 리눅스 컨테이너
  * CPU나 메모리는 프로세스에 필요한 만큼 추가로 사용
  * 성능 손실 거의 없다.
  * 컨테이너들 사이는 서로 영향을 주지 않는다.
  * 컨테이너 생성 속도가 빠르다. 

## 📚 Docker Image 구조

* Docker Version 확인

```shell
$ docker version

jwj@MacBook-Pro-von-jang-ujin ~ % docker version
Client:
 Version:           28.2.2
 API version:       1.50
 Go version:        go1.24.3
 Git commit:        e6534b4
 Built:             Fri May 30 12:07:35 2025
 OS/Arch:           darwin/arm64
 Context:           desktop-linux

Server: Docker Desktop 4.42.0 (195023)
 Engine:
  Version:          28.2.2
  API version:      1.50 (minimum version 1.24)
  Go version:       go1.24.3
  Git commit:       45873be
  Built:            Fri May 30 12:07:27 2025
  OS/Arch:          linux/arm64
  Experimental:     false
 containerd:
  Version:          1.7.27
  GitCommit:        05044ec0a9a75232cad458027ca83437aae3f4da
 runc:
  Version:          1.2.5
  GitCommit:        v1.2.5-0-g59923ef
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0
```

* Dockerfile : Docker Image를 생성하기 위한 스크립트 파일
  * 자체 DSL 언어 사용 -> 이미지 생성 과정 기술
  * 서버에 프로그램을 설치하는 과정을 Dockerfile로 기록, 관리
  * 소스와 함께 버전 관리가 되며 누구나 수정이 가능하다.

## 📚 [Docker Container Commands](https://docs.docker.com/reference/cli/docker/container/)

* 도커 이미지를 실행한 상태 -> 컨테이너(=인스턴스)
  * 격리된 시스템 자원 및 네트워크를 사용할 수 있는 독립적인 실행 단위
  * 읽기 전용 상태인 이미지에 변경된 사항을 저장할 수 있는 컨테이너 계층에 저장

![스크린샷 2025-06-23 오후 11 53 35](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/964f4856-8b48-4902-805b-b22b8e67cfdd.png)

### [ Docker Container LifeCycle ] : Docker Container 실행과 중지에 따른 상태 변화

![스크린샷 2025-06-23 오후 11 57 23](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/35146e49-00a3-419d-ab16-4fea42ee7f9a.png)

> [참고 - Docker Hub](https://hub.docker.com/)

## 📚 Docker Port Mapping

* 도커 컨테이너에서 사용하고자 하는 Port를 자유롭게 설정 가능한 기능
* 호스트 시스템(사용자 PC)에서 도커 컨테이너 포트를 사용하려면 반드시 Port Mapping이 필요하다.

```shell
$ docker run -p host_port:container_port <IMAGE_NAME>
```

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/328fe2f4-e5b4-4c02-bc81-d4eb8f8e3ea0.png)
> [참고](https://linuxhandbook.com/docker-port-mapping/)
