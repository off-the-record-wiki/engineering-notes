# Spring - Database Access

#### @Transactional이 적용된 테스트

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c58f210b-0ffa-4db3-892c-3e19dba4537a.png)

1. 자동 커밋을 수동 커밋으로 변경한다. 
2. 리포지토리에서 트랜잭션 동기화 매니저에 있는 커넥션을 가져와 테스트를 실행한다.
3. DML(INSERT문)을 사용해 데이터를 저장한다.
4. 검증을 위해서 DML(SELECT문)을 날려 데이터를 조회한다.
5. `@Transactional` 어노테이션이 테스트에 있으면 테스트가 끝날 때 트랜잭션을 강제로 롤백한다.
6. 롤백에 의해 DML(INSERT문)로 넣었던 데이터들이 제거된다.

>❗가끔 데이터가 잘 들어갔는지 테스트를 해보는 경우가 있는데 그런 경우에는 `@Commit` 어노테이션을 사용한다.
