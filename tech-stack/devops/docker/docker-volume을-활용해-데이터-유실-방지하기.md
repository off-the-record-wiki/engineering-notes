# Docker ‐ Docker Volume

## 📚 컨테이너가 가지는 문제점

* Docker를 활용하면 특정 프로그램을 호스트 컴퓨터에 설치할 필요없이 컨테이너로 띄울 수 있다.
* 프로그램에 기능이 추가되면 새로운 이미지를 만들어서 컨테이너를 실행시켜야 한다.
* 이 때, Docker는 기존 컨테이너에서 변경된 부분을 수정하지 않고 아예 새로운 컨테이너를 만들어서 통째로 갈아끼우는 방식으로 교체를 한다.
* 이런 유실을 방지하고자 볼륨(Volume)이라는 개념을 활용할 수 있다.

## 📚 Docker Volume

* 도커의 볼륨(Volume)이란 도커 컨테이너에서 데이터를 영속적으로 저장하기 위한 방법이다.
* 볼륨(Volume)은 컨테이너 자체의 저장 공간을 사용하지 않고 호스트 자체의 저장 공간을 공유해서 사용하는 방법을 말한다. 이렇게 되면 컨테이너에서 변경된 부분이 있어 지워지고 새로 뜨더라도 데이터가 유실되지 않는다.
* 도커 볼륨을 그림으로 나타내면 다음과 같다.

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b89f0d9e-c225-4bbc-84fe-2149a1d8fdfb.png)

```bash
$ docker run -v [호스트 컴퓨터 디렉터리 절대 경로]:[컨테이너 디렉터리 절대 경로] [IMAGE NAME]
```

> ❗ Volume 사용시 주의할 점

* 위의 명령어에서 만약 `호스트 컴퓨터 디렉터리 절대 경로`가 이미 존재할 경우, 호스트 컴퓨터의 디렉터리가 컨테이너 디렉터리를 덮어 씌우게 된다.

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f7316198-2c8c-4994-8814-cbbd732e54d4.png)

* 만약 `호스트 컴퓨터 디렉터리 절대 경로`가 존재하지 않을 경우, 호스트 컴퓨터 디렉터리 절대 경로에 디렉터리를 새로 만들고 컨테이너 디렉터리에 있는 파일들을 호스트 컴퓨터 디렉터리로 복사해서 가져온다.

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a7594dfa-2a18-4f31-8e7e-b823e25bc01e.png)

* 따라서 Docker Volume을 사용할 것이라면 `호스트 컴퓨터 디렉터리 절대 경로`를 절대 미리 만들면 안 된다.

## 📚 MySQL을 Docker Container에서 실행시키기

```bash
$ docker run -e MYSQL_ROOT_PASSWORD=password123 -p 3306:3306 -d mysql
```

* 이미지별 컨테이너 실행 메뉴얼은 Docker hub에 자세하게 명시되어있다.
* [Dockerhub - MySQL](https://hub.docker.com/_/mysql)
* 호스트 컴퓨터의 포트 번호는 3306번, 컨테이너의 포트 번호는 3306번으로 매핑을 시킨 결과는 아래와 같다.
* 여기서 만약 MySQL 컨테이너를 내리고 다시 올리게 되면 기존에 올렸던 컨테이너에 저장된 데이터는 유실이 된다. 이제 이 부분을 Docker Volume을 활용해 데이터 유실을 개선해보겠다.

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3a953080-d060-4b87-84bf-051a13c1b2d8.png)

* Docker Volume으로 사용할 호스트 컴퓨터 디렉터리의 절대 주소를 확인한다. 필자는 `/Users/jwj/Desktop/Tool/docker-mysql` 하위에 생성할 예정이다.

```bash
$ docker run -e MYSQL_ROOT_PASSWORD=password123 -p 3306:3306 -v /Users/jwj/Desktop/Tool/docker-mysql/mysql_data:/var/lib/mysql -d mysql
```

* MySQL Container에 테스트용 testdb를 만든다.
* 그러고 나서 컨테이너를 삭제한 후 다시 위와 같은 명령어를 그대로 입력해서 컨테이너를 생성한다.

![스크린샷 2025-01-07 오후 11 55 53](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1d7ac08f-1f19-4a06-a99c-63853ec06987.png)

* 데이터베이스가 그대로 살아있음을 확인할 수 있다.

> ❗ 여기서 주의할 점

* 만약 위의 데이터베이스를 생성한 후 컨테이너를 새로 생성할 때 패스워드를 다르게 해서 컨테이너를 만들었다면 DB는 그대로 남아있겠지만 접속할 수 없게 된다.
* 왜냐하면 기존에 입력된 데이터베이스의 패스워드 자체도 Volume으로 설정해둔 폴더에 이미 비밀번호 정보가 저장되어버리기 때문이다.
* 최종 구조는 아래와 같다.

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d16ba765-eda6-470e-ad91-4f2deb30f4e8.png)
