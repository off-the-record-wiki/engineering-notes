# PostgreSQL - Database Fundamentals & Introduction to PostgreSQL

#### PostgreSQL 소개

* PostgreSQL은 MySQL처럼 오픈소스로 무료이면서 Oracle에 버금가는 고급 기능을 갖춰서 두 DB의 약점을 동시에 메우는 선택지로 제시된다.
* `pgvector` 확장을 설치하면 PostgreSQL이 임베딩 벡터를 저장하고 키워드가 아니라 의미가 가까운 데이터를 찾는 유사도 검색까지 할 수 있게 된다.
* 그 결과 관계형 데이터와 벡터 데이터를 한 DB에 두고 조인·필터·유사도 검색을 SQL 한 번으로 처리할 수 있다.
* 별도의 벡터 DB가 필요 없어서 두 저장소 간 동기화 문제가 사라지고 백업·모니터링·권한 관리 같은 운영 부담도 DB 하나로 줄어든다.
* 다만 벡터가 수천만 건을 넘거나 검색 부하가 커지면 같은 DB의 일반 트랜잭션까지 느려질 수 있으므로 그때는 읽기 복제본 분리나 전용 벡터 DB를 검토해야 한다.

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: postgres-db
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: password1234
      POSTGRES_DB: fintech_db
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```
