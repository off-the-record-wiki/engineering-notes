# Redis - Reduce DB write load using Redis

## 📚 List 자료구조(RPUSH, LRANGE, LPOP)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/400adcef-2091-45c1-873b-1df854b3235c.png)

* Redis에서 List는 순서대로 데이터를 저장할 수 있는 자료구조이다. 이런 특성 때문에 실제 서비스에서 스택과 큐로 주로 사용된다.

```shell
# RPUSH [key] [value] : 리스트 오른쪽에 데이터를 추가한다. 
$ RPUSH my_list 1
$ RPUSH my_list 2

# 리스트가 잘 생성됐는 지 조회해보기
$ keys *

# LRANGE [key] [start index] [end index] : 인덱스 범위를 활용해 리스트 데이터 조회
$ LRANGE my_list 0 -1 # 리스트의 모든 데이터 조회

# LLEN [key] : 리스트에서 데이터의 총 개수를 조회
$ LLEN my_list

# LPOP [key] : 리스트 왼쪽(첫 부분)에서 데이터를 꺼내온다.
$ LPOP my_list # 1 
$ LPOP my_list # 2
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9ab88928-fc37-4b7a-bca3-f4cc6a101f76.png)

* **Write Back** 전략을 활용하면 된다. 
* **Write Back** 전략이란 클라이언트의 요청이 들어오면 먼저 Redis에 데이터를 쌓아두고 클라이언트에게 먼저 응답을 반환하고나서 Redis에 쌓인 데이터를 스케줄러가 주기적으로 DB에 한꺼번에 저장하는 방식을 뜻한다. 
* 즉, Redis가 데이터를 쌓아두는 Queue의 역할을 한다.

> Write-Through vs Write-Back의 차이점
> 
> * Write-Through 전략에서는 클라이언트의 요청이 들어오면, 먼저 캐시(여기서는 Redis)를 업데이트하고, 그 다음으로 RDB를 업데이트한다. 두 작업은 순차적으로 이루어지며, RDB의 업데이트가 완료된 후에야 클라이언트에게 응답이 반환되어 캐시와 백엔드 저장소 사이의 데이터 일관성을 유지할 수 있다.<br>
> * Write-Back 전략에서는 클라이언트의 요청이 들어오면, 먼저 캐시를 업데이트하고 즉시 클라이언트에게 응답을 반환한다. 이후 RDB의 업데이트는 다음 두 가지 방식 중 하나로 이루어진다.<br>
> a. 일정 시간 간격으로 업데이트: 캐시에서 변경된 데이터를 일정 시간 간격으로 모아서 한 번에 백엔드 저장소에 반영<br>
> b. 특정 이벤트 발생 시 업데이트: 캐시의 데이터가 변경된 후 특정 이벤트가 발생했을 때 백엔드 저장소를 업데이트<br>

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b527bfb7-b10b-4cfa-90b4-4142bac1c7b2.png)
