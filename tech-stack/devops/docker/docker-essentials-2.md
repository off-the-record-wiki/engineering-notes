# Docker - Docker Essentials(2)

## 📚 Docker Image

* 컨테이너를 만드는 데 필요한 읽기 전용 템플릿
* 컨테이너 실행에 필요한 파일과 설정 값 등을 포함한다.

![스크린샷 2025-06-27 오전 12 04 36](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6be458a4-ffa4-4862-bf40-9944589ba7a2.png)

## 📚 Dockerfile

* Dockerfile : 이미지 빌드용 DSL(Domain Specific Language)

|명령어|설명|
|:---:|:---:|
|FROM|Base Image 지정 명령어|
|RUN|특정 Layer 생성|
|COPY|이미지 파일 생성 시 호스트 파일 복사|
|ADD|이미지 파일 생성 시 호스트 파일 복사(tar, url)|
|WORKDIR|이미지 파일 생성 시 명령어가 실행될 작업 디렉터리 위치 지정|
|ENTRYPOINT|컨테이너가 실행될 때 가장 먼저 실행될 프로그램을 지정(Overwrite 불가)|
|CMD|컨테이너가 실행될 때 가장 먼저 실행될 프로그램을 지정(Overwrite 가능)|
|ENV|컨테이너 내의 환경 변수 설정|
|EXPOSE|컨테이너 특정 포트를 외부에 오픈|

## 📚 Docker Image 저장소 Registry

* Docker를 통해 생성하는 Image들을 저장해주는 저장소
* Docker Image들의 위치 제어 및 CI/CD를 위한 자동화 Pipeline 구축 가능

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/feb96bdd-7862-4b0b-a4e2-75afb3dbc325.png)
