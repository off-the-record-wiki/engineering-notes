# MySQL - Checking DB Metrics with SQL Queries

#### MySQL - Character Set

```sql
mysql> show variables like 'character_set%';
+--------------------------+--------------------------------+
| Variable_name            | Value                          |
+--------------------------+--------------------------------+
| character_set_client     | latin1                         |
| character_set_connection | latin1                         |
| character_set_database   | utf8mb4                        |
| character_set_filesystem | binary                         |
| character_set_results    | latin1                         |
| character_set_server     | utf8mb4                        |
| character_set_system     | utf8mb3                        |
| character_sets_dir       | /usr/share/mysql-8.0/charsets/ |
+--------------------------+--------------------------------+
8 rows in set (0.02 sec)
```

* `character_set_client` : 클라이언트(사용자)가 보내는 쿼리의 인코딩
* `character_set_connection` : 서버가 수신한 쿼리를 처리하기 위해 변환하는 인코딩
* `character_set_database` : 현재 사용 중인 데이터베이스의 기본 인코딩
* `character_set_results` : 서버가 결과를 클라이언트에 보낼 때 사용하는 인코딩
* `character_set_server` : MySQL 서버의 기본 인코딩

> **주의 1 : utf8 ≠ utf8mb4**
> * MySQL의 utf8은 사실 utf8mb3(글자당 최대 3바이트)의 별칭이다.
> * 3바이트로는 이모지나 일부 보조 문자(supplementary characters)를 저장할 수 없다.
>   * 저장 시도 시 Incorrect string value 에러 또는 데이터 잘림이 발생한다.
> * 완전한 UTF-8을 쓰려면 반드시 4바이트를 지원하는 utf8mb4 + utf8mb4_0900_ai_ci(MySQL 8.0 기본 collation)를 쓴다.

> **주의 2 : client/connection/results가 latin1이면 글자가 깨진다.**
> * `character_set_client/connection/results`가 `latin1`이면, 애플리케이션은 UTF-8로 보냈는데 서버는 `latin1`로 해석해 한글·이모지가 깨진다.
> * 세션 단위로 즉시 맞추려면 다음과 같이 할 수 있다.
> * 근본 해결은 연결(Connection) 단계에서 맞추는 것이다.
> * 데이터가 저장되는 인코딩(server/database/table/column)과 데이터가 오가는 인코딩(client/connection/results)이 모두 `utf8mb4`로 일치해야 안전하다.
```sql
-- client / connection / results 세 가지를 한 번에 utf8mb4로 설정
-- 세션 단위
SET NAMES utf8mb4;
```
```yml
# 근본 해결책
jdbc:mysql://host:3306/db?characterEncoding=UTF-8&connectionCollation=utf8mb4_0900_ai_ci
```

#### MySQL - Collation

```sql
mysql> show variables like 'collation%';
+----------------------+--------------------+
| Variable_name        | Value              |
+----------------------+--------------------+
| collation_connection | latin1_swedish_ci  |
| collation_database   | utf8mb4_0900_ai_ci |
| collation_server     | utf8mb4_0900_ai_ci |
+----------------------+--------------------+
3 rows in set (0.00 sec)
```

* `collation_connection` : 연결된 세션에서 문자열을 비교/정렬할 때의 규칙
* `collation_database` : 데이터베이스 내에서 문자열을 비교/정렬하는 규칙
* `collation_server` : 서버 전역 기본 규칙. 새 DB 생성 시 collation을 명시하지 않으면 이 값을 상속한다.

#### MySQL - InnoDB Buffer Pool Size

```sql
mysql> show variables like 'innodb_buffer_pool_size';
+-------------------------+-----------+
| Variable_name           | Value     |
+-------------------------+-----------+
| innodb_buffer_pool_size | 134217728 |
+-------------------------+-----------+
1 row in set (0.00 sec)
```

* `innodb_buffer_pool_size` : InnoDB 엔진이 데이터와 인덱스를 캐싱하기 위해 사용하는 메모리 크기

> **주의 1 : 무엇을, 어떻게 캐싱하나**
> * 데이터 페이지 + 인덱스 페이지를 16KB 페이지 단위로 메모리에 올린다.
> * 읽기뿐 아니라 쓰기도 여기서 먼저 일어난다. → "버퍼"인 이유. 바뀐 페이지(dirty page)를 모아 나중에 디스크에 flush한다.
> * 전역 공유 캐시다. `sort_buffer_size/join_buffer_size`가 연결(세션)마다 잡히는 것과 달리, 버퍼 풀은 인스턴스 전체가 하나를 공유한다.

> **주의 2 : 기본값 128MB는 작다(사이징)**
> * 134217728 = 128MB (MySQL 기본값). 운영에선 거의 항상 부족하다.
> * 이상적 목표 : 버퍼 풀 ≥ 자주 쓰는 데이터+인덱스(working set). 핫데이터가 통째로 메모리에 들어가면 읽기 대부분이 디스크를 안 간다.
> * 너무 크게 잡으면 OS·다른 프로세스 메모리가 부족해 스와핑/OOM → 오히려 느려진다.

> **주의 3 : 캐시 히트율**
> * `Innodb_buffer_pool_read_requests` : 논리적 읽기 요청(버퍼 풀에서 찾은 것 포함)
> * `Innodb_buffer_pool_reads` : 버퍼 풀에 없어서 디스크에서 읽은 횟수(미스)
> * 히트율 ≈ `1 - reads / read_requests`. 99% 이상이 건강한 값. 낮으면 버퍼 풀이 작다는 신호이다.

> **주의 4 : LRU 방출 + 스캔 오염 방지**
> * 풀이 꽉 차면 LRU(가장 오래 안 쓴 페이지) 부터 밀어낸다.
> * InnoDB는 young/old로 나눈 변형 LRU를 씀 → 큰 풀스캔 한 번이 뜨거운 페이지를 전부 밀어내는 "캐시 오염"을 막기 위함이다.

> **주의 5 : 큰 풀을 위한 튜닝 2가지**
> * `innodb_buffer_pool_instances` : 풀을 여러 조각으로 나눠 뮤텍스 경합 감소(큰 풀에서 유효)
> * 온라인 리사이즈 가능

> **주의 6 : dirty page와 쓰기 성능**
> * 변경은 메모리(dirty page)에 먼저 → 백그라운드로 flush
> * `innodb_max_dirty_pages_pct` 등이 flush 타이밍을 좌우한다. dirty가 너무 쌓이면 체크포인트 폭주로 쓰기 지연이 된다.

#### MySQL - Max Connections

```sql
mysql> show variables like 'max_connections';
+-----------------+-------+
| Variable_name   | Value |
+-----------------+-------+
| max_connections | 151   |
+-----------------+-------+
1 row in set (0.00 sec)
```

* `max_connections` : 서버가 허용하는 최대 동시 접속자 수

> **주의 1 : 커넥션 1개 = 메모리 비용(무작정 늘린다고 좋은 것이 아니다)**
> * 커넥션마다 스레드 + 세션 전용 버퍼(sort_buffer, join_buffer, read_buffer, net_buffer, 스레드 스택)를 쓴다.
> * 대략 : 총 메모리 ≈ 버퍼 풀(전역) + max_connections × 세션 버퍼
> * 그래서 `max_connections`를 크게 올리면 메모리가 터질(OOM) 수 있다. 버퍼 풀 사이징과 함께 봐야 한다.

> **주의 2 : 커넥션 풀과의 관계**
> * 앱은 요청마다 연결을 새로 열면 안 되고 커넥션 풀(Spring이면 HikariCP)로 재사용한다.
> * (앱 인스턴스 수 × 풀 최대 크기) + 여유 ≤ max_connections
> * 스케일아웃(인스턴스 증설) 시 이 계산을 다시 해야 한다.

> **주의 3 : 풀은 클수록 좋은 게 아니다**
> * 커넥션이 많다고 처리량이 오르지 않는다. 오히려 컨텍스트 스위칭·락 경합으로 느려진다.
> * HikariCP 권장 : 대개 작은 풀(수십 개 이하)이 최적. 경험식 pool size ≈ (CPU 코어 × 2) + 디스크 수 에서 출발해 부하 테스트로 조정하는 것을 권장한다.

> **주의 4 : 유휴 커넥션과 타임아웃**
> * `wait_timeout` / `interactive_timeout` : 유휴 커넥션을 언제 끊을지 설정하는 값이다.
> * DB의 `wait_timeout`이 풀의 유휴 검증보다 짧으면, 풀이 죽은 커넥션을 들고 있다가 쿼리 시 에러가 발생한다. HikariCP `maxLifetime`을 DB `wait_timeout`보다 짧게 잡아야 안전하다.

#### MySQL - Threads Connected

```sql
mysql> show status like 'threads_connected';
+-------------------+-------+
| Variable_name     | Value |
+-------------------+-------+
| Threads_connected | 1     |
+-------------------+-------+
1 row in set (0.00 sec)
```

* `Threads_connected` : 현재 서버에 연결되어 있는 클라이언트 수

#### MySQL - Uptime

```sql
mysql> show status like 'uptime';
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| Uptime        | 940   |
+---------------+-------+
1 row in set (0.00 sec)
```

* `Uptime` : MySQL 서버가 시작된 이후 경과된 시간(초)

#### MySQL - CharacterSet

```sql
-- 데이터베이스별 문자셋 확인
mysql> select schema_name, default_character_set_name, default_collation_name from information_schema.schemata;
+--------------------+----------------------------+------------------------+
| SCHEMA_NAME        | DEFAULT_CHARACTER_SET_NAME | DEFAULT_COLLATION_NAME |
+--------------------+----------------------------+------------------------+
| mysql              | utf8mb4                    | utf8mb4_0900_ai_ci     |
| information_schema | utf8mb3                    | utf8mb3_general_ci     |
| performance_schema | utf8mb4                    | utf8mb4_0900_ai_ci     |
| sys                | utf8mb4                    | utf8mb4_0900_ai_ci     |
| mysql_fundamentals | utf8mb4                    | utf8mb4_0900_ai_ci     |
| crud_patterns      | utf8mb4                    | utf8mb4_0900_ai_ci     |
+--------------------+----------------------------+------------------------+
6 rows in set (0.00 sec)
```

```sql
-- 슬로우 쿼리 설정
mysql> set global slow_query_log = 'on';
Query OK, 0 rows affected (0.01 sec)

-- 2초 이상
mysql> set global long_query_time = 2;
Query OK, 0 rows affected (0.00 sec)

-- 현재 로그 설정 확인
mysql> show variables like '%log%';
+------------------------------------------------+---------------------------------------------+
| Variable_name                                  | Value                                       |
+------------------------------------------------+---------------------------------------------+
| activate_all_roles_on_login                    | OFF                                         |
| back_log                                       | 151                                         |
| binlog_cache_size                              | 32768                                       |
| binlog_checksum                                | CRC32                                       |
| binlog_direct_non_transactional_updates        | OFF                                         |
| binlog_encryption                              | OFF                                         |
| binlog_error_action                            | ABORT_SERVER                                |
| binlog_expire_logs_auto_purge                  | ON                                          |
| binlog_expire_logs_seconds                     | 2592000                                     |
| binlog_format                                  | ROW                                         |
| binlog_group_commit_sync_delay                 | 0                                           |
| binlog_group_commit_sync_no_delay_count        | 0                                           |
| binlog_gtid_simple_recovery                    | ON                                          |
| binlog_max_flush_queue_time                    | 0                                           |
| binlog_order_commits                           | ON                                          |
| binlog_rotate_encryption_master_key_at_startup | OFF                                         |
| binlog_row_event_max_size                      | 8192                                        |
| binlog_row_image                               | FULL                                        |
| binlog_row_metadata                            | MINIMAL                                     |
| binlog_row_value_options                       |                                             |
| binlog_rows_query_log_events                   | OFF                                         |
| binlog_stmt_cache_size                         | 32768                                       |
| binlog_transaction_compression                 | OFF                                         |
| binlog_transaction_compression_level_zstd      | 3                                           |
| binlog_transaction_dependency_history_size     | 25000                                       |
| binlog_transaction_dependency_tracking         | COMMIT_ORDER                                |
| expire_logs_days                               | 0                                           |
| general_log                                    | OFF                                         |
| general_log_file                               | /var/lib/mysql/10a9b30b1275.log             |
| innodb_api_enable_binlog                       | OFF                                         |
| innodb_flush_log_at_timeout                    | 1                                           |
| innodb_flush_log_at_trx_commit                 | 1                                           |
| innodb_log_buffer_size                         | 16777216                                    |
| innodb_log_checksums                           | ON                                          |
| innodb_log_compressed_pages                    | ON                                          |
| innodb_log_file_size                           | 50331648                                    |
| innodb_log_files_in_group                      | 2                                           |
| innodb_log_group_home_dir                      | ./                                          |
| innodb_log_spin_cpu_abs_lwm                    | 80                                          |
| innodb_log_spin_cpu_pct_hwm                    | 50                                          |
| innodb_log_wait_for_flush_spin_hwm             | 400                                         |
| innodb_log_write_ahead_size                    | 8192                                        |
| innodb_log_writer_threads                      | ON                                          |
| innodb_max_undo_log_size                       | 1073741824                                  |
| innodb_online_alter_log_max_size               | 134217728                                   |
| innodb_print_ddl_logs                          | OFF                                         |
| innodb_redo_log_archive_dirs                   |                                             |
| innodb_redo_log_capacity                       | 104857600                                   |
| innodb_redo_log_encrypt                        | OFF                                         |
| innodb_undo_log_encrypt                        | OFF                                         |
| innodb_undo_log_truncate                       | ON                                          |
| log_bin                                        | ON                                          |
| log_bin_basename                               | /var/lib/mysql/binlog                       |
| log_bin_index                                  | /var/lib/mysql/binlog.index                 |
| log_bin_trust_function_creators                | OFF                                         |
| log_bin_use_v1_row_events                      | OFF                                         |
| log_error                                      | stderr                                      |
| log_error_services                             | log_filter_internal; log_sink_internal      |
| log_error_suppression_list                     |                                             |
| log_error_verbosity                            | 2                                           |
| log_output                                     | FILE                                        |
| log_queries_not_using_indexes                  | OFF                                         |
| log_raw                                        | OFF                                         |
| log_replica_updates                            | ON                                          |
| log_slave_updates                              | ON                                          |
| log_slow_admin_statements                      | OFF                                         |
| log_slow_extra                                 | OFF                                         |
| log_slow_replica_statements                    | OFF                                         |
| log_slow_slave_statements                      | OFF                                         |
| log_statements_unsafe_for_binlog               | ON                                          |
| log_throttle_queries_not_using_indexes         | 0                                           |
| log_timestamps                                 | UTC                                         |
| max_binlog_cache_size                          | 18446744073709547520                        |
| max_binlog_size                                | 1073741824                                  |
| max_binlog_stmt_cache_size                     | 18446744073709547520                        |
| max_relay_log_size                             | 0                                           |
| relay_log                                      | 10a9b30b1275-relay-bin                      |
| relay_log_basename                             | /var/lib/mysql/10a9b30b1275-relay-bin       |
| relay_log_index                                | /var/lib/mysql/10a9b30b1275-relay-bin.index |
| relay_log_info_file                            | relay-log.info                              |
| relay_log_info_repository                      | TABLE                                       |
| relay_log_purge                                | ON                                          |
| relay_log_recovery                             | OFF                                         |
| relay_log_space_limit                          | 0                                           |
| slow_query_log                                 | ON                                          |
| slow_query_log_file                            | /var/lib/mysql/10a9b30b1275-slow.log        |
| sql_log_bin                                    | ON                                          |
| sql_log_off                                    | OFF                                         |
| sync_binlog                                    | 1                                           |
| sync_relay_log                                 | 10000                                       |
| sync_relay_log_info                            | 10000                                       |
| terminology_use_previous                       | NONE                                        |
+------------------------------------------------+---------------------------------------------+
92 rows in set (0.01 sec)
```

#### MySQL Performance Metrics and EXPLAIN Analysis

```sql
-- 연결 상태 상세 정보
mysql> select id, user, host, db, command, time, state, left(info, 100) as query_snippet from information_schema.processlist where command != 'sleep' order by time desc;
+----+-----------------+-----------+---------------+---------+------+------------------------+------------------------------------------------------------------------------------------------------+
| id | user            | host      | db            | command | time | state                  | query_snippet                                                                                        |
+----+-----------------+-----------+---------------+---------+------+------------------------+------------------------------------------------------------------------------------------------------+
|  5 | event_scheduler | localhost | NULL          | Daemon  | 2723 | Waiting on empty queue | NULL                                                                                                 |
|  8 | root            | localhost | crud_patterns | Query   |    0 | executing              | select id, user, host, db, command, time, state, left(info, 100) as query_snippet from information_s |
+----+-----------------+-----------+---------------+---------+------+------------------------+------------------------------------------------------------------------------------------------------+
2 rows in set, 1 warning (0.00 sec)
```
* `event_scheduler` : MySQL의 이벤트 스케줄러 관리 프로세스, 시스템 기본 프로세스로 서버가 정상적으로 예약 작업 대기 상태에 있음을 의미한다.
* `root` : 본인의 세션, executing은 현재 쿼리를 실행 중이라는 뜻을 가리킨다.
* `id` : 프로세스의 고유 번호
* `user/host` : 누가, 어떤 IP(어떤 서버)에서 접속했는지
* `db` : 현재 해당 세션이 사용 중인 데이터베이스 이름
* `command` : 현재 수행 중인 작업의 종류(query는 조회/수정, daemon은 백그라운드 작업 중을 의미한다)
* `time` : 해당 상태가 지속된 시간(초)(여기서 쿼리가 0초라면 매우 빠르게 실행된 것이지만 수백 초라면 성능 저하를 의심해야 한다)
* `state` : 현재 작업의 구체적인 단계(executing은 실행 중, waiting for table metadata lock은 락 대기 중을 의미한다)
* `info` : 실행 중인 전체 쿼리 문장

```sql
-- 버퍼 풀 히트율 확인
mysql> SELECT ROUND((1 - ((SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME = 'innodb_buffer_pool_reads') / (SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME = 'innodb_buffer_pool_read_requests'))) * 100, 2) AS buffer_pool_hit_ratio_percent;
+-------------------------------+
| buffer_pool_hit_ratio_percent |
+-------------------------------+
|                         99.99 |
+-------------------------------+
1 row in set (0.00 sec)
```

```sql
-- 실행 계획 확인
mysql> explain select * from accounts where year(created_at) = 2023;
+----+-------------+----------+------------+------+---------------+------+---------+------+------+----------+-------------+
| id | select_type | table    | partitions | type | possible_keys | key  | key_len | ref  | rows | filtered | Extra       |
+----+-------------+----------+------------+------+---------------+------+---------+------+------+----------+-------------+
|  1 | SIMPLE      | accounts | NULL       | ALL  | NULL          | NULL | NULL    | NULL |    1 |   100.00 | Using where |
+----+-------------+----------+------------+------+---------------+------+---------+------+------+----------+-------------+
1 row in set, 1 warning (0.00 sec)
```
```sql
mysql> explain select * from accounts where user_id = 123;
+----+-------------+----------+------------+------+---------------+-------------+---------+-------+------+----------+-------+
| id | select_type | table    | partitions | type | possible_keys | key         | key_len | ref   | rows | filtered | Extra |
+----+-------------+----------+------------+------+---------------+-------------+---------+-------+------+----------+-------+
|  1 | SIMPLE      | accounts | NULL       | ref  | idx_user_id   | idx_user_id | 8       | const |    1 |   100.00 | NULL  |
+----+-------------+----------+------------+------+---------------+-------------+---------+-------+------+----------+-------+
1 row in set, 1 warning (0.00 sec)
```
