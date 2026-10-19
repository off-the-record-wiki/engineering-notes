# Docker ‐ Docker CLI

## 📚 이미지(Image) 다운로드

```bash
$ docker pull [IMAGE NAME] # 이미지를 다운로드한다.
```

* 이미지를 다운로드 할 때, Dockerhub라는 곳에서 이미지를 다운받는다.
* Dockerhub는 사람들이 올려놓은 이미지들이 저장되어 있어서 pull을 통해 다운받아서 사용할 수 있다.
* Dockerhub는 Github처럼 이미지를 저장 및 다운받을 수 있는 저장소 역할을 한다.

## 📚 이미지(Image) 조회 / 삭제

```bash
$ docker image ls # 다운받은 모든 이미지를 조회할 수 있다.
```

```bash
$ docker image rm [IMAGE ID] # 다운받은 이미지를 삭제할 수 있다.
```

```bash
$ docker image rm -f [IMAGE ID] # 다운받은 이미지를 강제로 삭제할 수 있다.
```

```bash
$ docker image rm $(docker images -q) # 컨테이너에서 사용하고 있지 않은 이미지만 전체 삭제한다.

$ docker image rm -f $(docker images -q) # 컨테이너에서 사용하고 있는 이미지를 포함해서 전체 삭제한다.
```

## 📚 컨테이너(Container) 생성 / 실행

* `create`의 경우 컨테이너를 생성만 할 뿐, 실행은 하지 않는다.
* 실행은 `start`로 해야 한다.

```bash
$ docker create [IMAGE NAME] # 해당 이미지를 베이스로 하는 컨테이너를 생성한다.
```

```bash
$ docker start [CONTAINER ID] # 해당 컨테이너를 실행한다.
```

* `docker run` = `docker create` + `docker start` 이다.
* 즉, 컨테이너를 생성하고 실행하는 과정을 모두 처리해주는 명령어이다.
* 실행 결과는 포그라운드에서 작동을 한다.
* 백그라운드에서 실행시키길 원한다면 아래와 같이 입력을 해야 한다.

```bash
$ docker run [IMAGE NAME] # 해당 이미지 베이스의 컨테이너를 생성 및 실행한다.(포그라운드로)

$ docker run -d [IMAGE NAME] # 해당 이미지 베이스의 컨테이너를 생성 및 실행한다.(백그라운드로)
```

> ❗ 백그라운드(BackGround) : 내가 실행시킨 프로그램이 컴퓨터 내부적으로 실행되는 상태를 말한다. 그래서 프로그램이 어떻게 실행되고 있는지에 대한 정보를 화면에서 확인할 수 없다. 이런 특성 때문에 다른 명령어를 추가로 입력할 수도 있고, 새로운 프로그램을 조작할 수도 있다.

> ❗ 포그라운드(ForeGround) : 내가 실행시킨 프로그램의 내용이 화면에서 실행되고 출력되는 상태를 뜻한다. 그러다보니 포그라운드 상태에서 다른 프로그램을 조작할 수가 없다.

* 지금까지 배운 내용으로 컨테이너를 생성하면 `elegant_burnell`과 같이 임의로 이름이 부여된다.
* 아래와 같이 작성하면 자신이 만들 컨테이너에 이름을 부여할 수 있다.

```bash
$ docker run -d --name [CONTAINER NAME] [IMAGE NAME] # 컨테이너에 이름을 붙여 생성 및 실행한다.
```

## 📚 Nginx를 80번 포트로 실행시키고 호스트 컴퓨터에서 4000번 포트로 접속하기

```bash
$ docker run -d -p 4000:80 nginx
```

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8d567e1d-f17a-4a11-bb38-2c4d49a85540.png)

* 도커를 실행하는 전체 컴퓨터(호스트 컴퓨터)의 4000번 포트를 내부의 도커 컨테이너 80번 포트로 연결하도록 설정한다.

## 📚 컨테이너(Container) 조회 / 중지 / 삭제

```bash
$ docker ps # 실행 중인 컨테이너들만 조회한다.

$ docker ps -a # 작동 중인 컨테이너와 작동을 멈춘 컨테이너 모두를 조회한다.
```

```bash
$ docker stop [CONTAINER NAME or CONTAINER ID] # 컨테이너를 정상적으로 종료한다.

$ docker kill [CONTAINER NAME or CONTAINER ID] # 컨테이너를 강제로 종료한다.
```

```bash
$ docker rm [CONTAINER NAME or CONTAINER ID] # 중지된 특정 컨테이너를 삭제한다.

$ docker rm -f [CONTAINER NAME or CONTAINER ID] # 실행되고 있는 특정 컨테이너를 삭제한다.

$ docker rm $(docker ps -qa) # 중지된 모든 컨테이너를 삭제한다.

$ docker rm -f $(docker ps -qa) # 실행되고 있는 모든 컨테이너를 삭제한다.
```

## 📚 컨테이너(Container) 로그 조회

* 컨테이너를 백그라운드로 실행시키면 실행 내역을 화면에서 확인할 수 없다.
* 백그라운드에서 실행되는 컨테이너의 실행 내역을 로그로 확인하려면 `logs` 명령어를 사용한다.

```bash
$ docker logs [CONTAINER ID or CONTAINER NAME]
```

* 전체 실행 내역이 많아서 가장 최신 로그만을 간추려서 보고 싶다면 `--tail` 옵션을 사용한다.

```bash
$ docker logs --tail [LINE] [CONTAINER ID or CONTAINER NAME]
```

* 또한, 기존 로그 조회와 이 시점 이후 생성되는 로그를 실시간으로 보고 싶은 경우 `-f 옵션을 사용한다.

```bash
$ docker logs -f [CONTAINER ID or CONTAINER NAME]
```

## 📚 실행 중인 컨테이너 내부에 접속하기

* 호스트 컴퓨터 내부에 Nginx 컨테이너를 동작시킨 결과를 그림으로 확인하면 아래와 같다.
* 호스트 컴퓨터에서 터미널을 실행시켜 컨테이너 내부로 들어가려면 `exec -it` 명령어를 사용한다.

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ce306c63-29d6-4dfb-aa1a-9558c604451b.png)

```bash
$ docker exec -it [CONTAINER ID] bash
```
