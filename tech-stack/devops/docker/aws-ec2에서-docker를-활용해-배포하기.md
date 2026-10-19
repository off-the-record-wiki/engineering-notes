# Docker ‐ Deploy(feat. AWS ECR)

## 📚 Ubuntu에서 Docker, Docker Compose 설치하기

* Ubuntu에서 Docker, Docker Compose 설치하기

```shell
$ $ sudo apt-get update && \
	sudo apt-get install -y apt-transport-https ca-certificates curl software-properties-common && \
	curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add - && \
	sudo apt-key fingerprint 0EBFCD88 && \
	sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" && \
	sudo apt-get update && \
	sudo apt-get install -y docker-ce && \
	sudo usermod -aG docker ubuntu && \
	newgrp docker && \
	sudo curl -L "https://github.com/docker/compose/releases/download/2.27.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose && \
	sudo chmod +x /usr/local/bin/docker-compose && \
	sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
```

* Docker, Docker Compose 설치 확인

```shell
$ docker -v # docker 버전을 확인한다.

$ docker compose version # docker compose 버전을 확인한다.
```

## 📚 AWS ECR(Elastic Container Registry)

* 클라우드 환경에서 인프라를 구축하는 일이 많아 AWS ECR을 사용하면 다른 AWS Resource와의 연동이 편하고 AWS 내에서 한 번에 관리할 수 있기에 편하다는 장점이 있다.
