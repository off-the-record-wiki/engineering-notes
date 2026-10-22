# Docker - Advanced Docker Usage

### [Cloud Native Component 구성](https://www.cncf.io/blog/2018/03/08/introducing-the-cloud-native-landscape-2-0-interactive-edition/)

## 📚 Docker & Microservice Architecture

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/96f6483b-1a22-4b68-820b-c20aee60c076.png)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9f8d8119-bc22-4e76-83ea-ec36ca77cc15.png)

## 📚 멀티 플랫폼 이미지를 위한 Docker Buildx⚠️

```shell
$ jwj@MacBook-Pro-von-jang-ujin ~ % docker buildx ls
NAME/NODE           DRIVER/ENDPOINT     STATUS    BUILDKIT   PLATFORMS
default             docker                                   
 \_ default          \_ default         running   v0.22.0    linux/amd64 (+2), linux/arm64, linux/arm (+2), linux/ppc64le, (3 more)
desktop-linux*      docker                                   
 \_ desktop-linux    \_ desktop-linux   running   v0.22.0    linux/amd64 (+2), linux/arm64, linux/arm (+2), linux/ppc64le, (3 more)
```

* Multi Platform 빌드 기능 지원 -> Docker Build
