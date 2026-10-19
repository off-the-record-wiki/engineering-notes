# Docker - Docker Network and Storage

## 📚 Docker Network

* Container Isolation : Docker Container는 격리된 환경에서 작동한다. -> 즉, 다른 Container와 통신이 불가능하다.

![image](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/543bc2ab-f764-4eec-8d2b-19f929897559.png)

### [ Docker Network Type ]

* Bridge Network : 하나의 Host 컴퓨터 내에서 여러 컨테이너들이 서로 통신 가능한 네트워크
* Host Network : Host 컴퓨터와 동일한 네트워크에서 컨테이너를 가동, Host랑 같은 네트워크를 공유하기 때문에 포트포워딩 불필요
* None Network : 네트워크를 사용하지 않음, 외부와 단절된 환경
* Overlay Network : 여러 호스트에 분산되어 컨테이너 실행 가능

### [ Docker Network Management ]

|Command|설명|
|:----:|:----:|
|docker network ls|Docker Network 목록 보기|
|docker network inspect \<NETWORK_NAME>|Docker Network 상세 보기|
|docker network create \<NETWORK_NAME>|Docker Network 생성|
|docker network rm \<NETWORK_NAME>|Docker Network 삭제|
|docker network connect \<NETWORK_NAME> \<CONTAINER>|실행 중인 Container를 Docker Network에 추가|
|docker netowrk disconnect \<NETWORK_NAME> \<CONTAINER>|Docker Network에 추가된 Container를 삭제|

```shell
$ jwj@MacBook-Pro-von-jang-ujin ~ % docker network inspect eb0
[
    {
        "Name": "bridge",
        "Id": "eb071d8e21967d7cbbd1a96126562d9de6d6162d129234fbdf7fe789fb0fb41d",
        "Created": "2025-06-15T02:51:39.592637917Z",
        "Scope": "local",
        "Driver": "bridge",
        "EnableIPv4": true,
        "EnableIPv6": false,
        "IPAM": {
            "Driver": "default",
            "Options": null,
            "Config": [
                {
                    "Subnet": "172.17.0.0/16",
                    "Gateway": "172.17.0.1"
                }
            ]
        },
        "Internal": false,
        "Attachable": false,
        "Ingress": false,
        "ConfigFrom": {
            "Network": ""
        },
        "ConfigOnly": false,
        "Containers": {
            "a216eb1007206095b9634d4a7e9801e1ecfbcb49f51af8dd5c391d16da758e4e": {
                "Name": "mysql",
                "EndpointID": "776fb19c5482e4db128e8a98490e289755c1f5d5c046b2b91204617caaee2532",
                "MacAddress": "36:bb:23:fe:ec:17",
                "IPv4Address": "172.17.0.2/16",
                "IPv6Address": ""
            }
        },
        "Options": {
            "com.docker.network.bridge.default_bridge": "true",
            "com.docker.network.bridge.enable_icc": "true",
            "com.docker.network.bridge.enable_ip_masquerade": "true",
            "com.docker.network.bridge.host_binding_ipv4": "0.0.0.0",
            "com.docker.network.bridge.name": "docker0",
            "com.docker.network.driver.mtu": "65535"
        },
        "Labels": {}
    }
]
```

## 📚 Docker Volume Storage

* 컨테이너는 개별적인 가상화 환경 -> 모든 데이터가 컨테이너 내부에 존재
* 허나, 컨테이너가 삭제되면 모든 데이터가 삭제된다.
* 이런 문제를 해결하기 위해 컨테이너 내부의 데이터를 외부로 연결시켜주는 기능이 바로 Docker Volume이다.
* 컨테이너 생성 시 Volume 연결

```shell
$ docker run -v <볼륨 이름>:<컨테이너 내부 디렉터리 경로> --name <컨테이너 이름> <이미지 이름>
```

## 📚 [Docker Volume Mount](https://www.daleseo.com/docker-volumes-bind-mounts/)
