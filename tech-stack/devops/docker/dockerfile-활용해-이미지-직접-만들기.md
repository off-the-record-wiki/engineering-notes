# Docker - Dockerfile Image

## 📚 Dockerfile이란?

* Docker 이미지는 Dockerhub를 통해 다운 받아서 사용할 수 있다.
* Dockerfile을 활용하면 Docker 이미지를 직접 만들수도 있다.
* 정리하자면 Dockerfile이란, Docker 이미지를 만들게 해주는 파일이다.

## 📚 FROM: 베이스 이미지 생성

* FROM: 베이스 이미지를 생성하는 역할을 한다.
* Docker 컨테이너를 특정 초기 이미지를 기반으로 추가적인 세팅을 할 수 있다.

```shell
FROM [IMAGE NAME] # 태그명을 적지 않으면 기본적으로 최신(latest) 버전을 사용한다.
FROM [IMAGE NAME]:[TAG]
```

> Ex. JDK 17

```shell
# JDK 17
FROM openjdk:17-jdk
```

```bash
$ docker build -t my-jdk17-server . # 해당 Dockerfile로 이미지를 생성한다.(이 때, 이미지 이름은 my-jdk17-server)
```

```bash
$ docker run -d my-jdk17-server  # 이미지를 기반으로 컨테이너를 띄운다.
```

## 📚 종료된 컨테이너에 들어가 디버깅하고 싶을 때

* Docker 컨테이너는 내부적으로 필요한 명령을 다 수행하면 자동으로 종료된다.
* 이 종료된 컨테이너로는 `docker exec -it` 명령어로 접근할 수가 없다.
* 따라서, 종료된 컨테이너에 들어가 디버깅을 하고 싶다면 아래와 같이 작성을 해주면 된다.

```shell
FROM openjdk:17-jdk

ENTRYPOINT ["/bin/bash", "-c", "sleep 500"] # 의미 : 500초 동안 시스템을 일시 정지 시킨다.
```

## 📚 COPY: 파일 복사(이동)

* COPY: 호스트 컴퓨터에 있는 파일을 복사해서 컨테이너로 전달한다.

```shell
COPY [호스트 컴퓨터에 있는 복사할 파일 경로] [컨테이너에서 파일이 위치할 경로]
```

## 📚 ENTRYPOINT: 컨테이너가 시작될 때 실행되는 명령어

* ENTRYPOINT: 컨테이너가 최초로 실행할 때 수행되는 명령어를 말한다.

```shell
ENTRYPOINT [명령문]
```

## 📚 RUN: 이미지를 생성하는 과정에서 사용할 명령문 실행

* RUN: 이미지 생성 과정에서 명령어를 실행시켜야 할 때 사용한다.

```shell
RUN [명령문]
```

> ❗ RUN vs ENTRYPOINT

* RUN은 이미지 생성 과정에서 필요한 명령어를 실행시킬 때 사용한다.
* ENTRYPOINT는 생성된 이미지를 기반으로 컨테이너를 생성한 직후에 명령어를 실행시킬 때 사용한다.

```shell
FROM ubuntu

RUN apt update && apt install -y git # Ubuntu 환경 구성 및 Git 설치 명령어

ENTRYPOINT ["/bin/bash", "-c", "sleep 500"]
```

## 📚 WORKDIR: 작업 디렉터리를 지정

* WORKDIR: 작업 디렉터리를 전환하는 명령어로, RUN, CMD, ENTRYPOINT, COPY, ADD 등의 명령문은 해당 디렉터리를 기준으로 실행된다.
* 작업 디렉터리를 지정해주는 이유는 디렉터리 관리를 말끔히 하기 위함이다.

```shell
WORKDIR [작업 디렉터리로 사용할 절대 경로]
```

## 📚 EXPOSE: 컨테이너 내부에서 사용 중인 포트를 문서화하기

* EXPOSE는 컨테이너 내부에서 어떤 포트에 프로그램이 실행되는지를 문서화하는 역할을 한다.
* EXPOSE를 쓰나 안 쓰나 작동하는 방식에는 영향을 미치지 않는다.

```shell
EXPOSE [PORT]
```
