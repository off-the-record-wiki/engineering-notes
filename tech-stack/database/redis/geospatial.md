# Redis - Geospatial

## 📚 Redis - Geospatial

* Redis에서 Geospatial은 위치 데이터(경도, 위도)를 저장하고 검색하는 데 최적화된 자료 구조이다. 
* 이 자료 구조는 특정 위치 기준으로 주어진 거리 내의 데이터를 찾아야 할 때 자주 사용하는 자료 구조이다.

```shell
# 경도, 위도 데이터를 저장
# GEOADD [key] [위도] [경도] [member]
$ GEOADD cafe 127.0276 37.4979 a_cafe
$ GEOADD cafe 127.0294 37.4981 b_cafe
$ GEOADD cafe 127.0330 37.4950 c_cafe

# 데이터가 잘 저장됐는지 key로 확인하기
$ keys *

# 모든 member 조회하기
$ ZRANGE cafe 0 -1 # 0번 인덱스(처음)부터 -1번 인덱스(끝)까지의 데이터를 조회

# 저장된 특정 member의 경도, 위도 좌표를 조회하는 명령어
# GEOPOS [key] [member...]
$ GEOPOS cafe a_cafe b_cafe c_cafe

# 두 데이터 사이의 직선 거리를 조회
# GEOPOS [key] [member1] [member2] [단위]
$ GEODIST cafe a_cafe b_cafe m

# 특정 위치를 기준으로 원하는 반경 내에 있는 데이터 조회 
# (+ 거리 가까운 순으로 조회 = ASC)
# (+ 거리 포함해서 출력 = WITHDIST)
# GEOSEARCH [key] FROMLONLAT [위도] [경도] BYRADIUS [반경] [단위] [ASC/DESC] WITHDIST
$ GEOSEARCH cafe FROMLONLAT 127.0280 37.4975 BYRADIUS 1000 m ASC WITHDIST
```
> Geospatial 자료 구조에 저장된 데이터는 내부적으로 Sorted Set 방식으로 저장된다. 그래서 Sorted Set 명령어를 입력해도 작동한다.
