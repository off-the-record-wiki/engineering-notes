# Redis - Solving Concurrency Issues (1)

## 📚 String 자료구조(SET, GET, DEL, SETNX)

```shell
# key, value 구조로 데이터 저장하기
$ SET hobby soccer

# key로 value 조회하기
$ GET hobby

# 기존에 hobby라는 key가 존재하더라도, 해당 key에 value를 덮어씌워서 데이터를 저장한다
$ SET hobby baseball

# value가 바뀌었는 지 확인해보기
$ GET hobby

# 지정한 키가 없을 때에만 데이터를 저장
# SET [key] [value] NX
$ SET hobby golf NX

# value가 바뀌었는 지 확인해보기
$ GET hobby

# key로 데이터 삭제하기
$ DEL hobby

# 지정한 키가 없을 때에만 데이터를 저장
$ SET hobby golf NX

# key, value가 잘 저장됐는 지 확인해보기
$ GET hobby
```

> [Distributed Locks with Redis](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/)
