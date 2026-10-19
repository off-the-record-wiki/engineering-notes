# Monitoring - JVM Metric(feat. DataDog)

#### 힙 메모리(Heap Memory)

> **❗JVM이 객체를 저장하는 주요 메모리 영역이다.**

* **jvm.heap_memory** : 현재 사용 중인 힙 메모리(bytes). 핵심 모니터링 지표. 지속적 증가 시 메모리 누수 의심.
* **jvm.heap_memory_max** : 힙 메모리 최대 한도(-Xmx). `used`가 이 값에 근접하면 OOM 위험.
* **jvm.heap_memory_committed** : OS로부터 할당받은 힙 메모리. JVM이 실제 확보한 메모리. `used ≤ committed ≤ max`
* **jvm.heap_memory_init** : JVM 시작 시 초기 힙 크기(-Xms). 설정 확인용.
* 해석법 : `heap_memory / heap_memory_max` 비율이 80% 이상이면 주의. 90% 이상이면 GC 빈도 급증 및 OOM 위험이 있다.

#### 비힙 메모리(Non-Heap Memory)

> **❗클래스 메타데이터, JIT 컴파일 코드 등을 저장한다.**

* **jvm.non_heap_memory** : 사용 중인 비힙 메모리. Metaspace + CompressedClassSpace + CodeCache의 합.
* **jvm.non_heap_memory_max** : 비힙 메모리 최대 한도. 제한이 없으면 `-1`로 표시.
* **jvm.gc.metaspace_size** : 클래스 로딩이 많으면 증가. 지속 증가 시 클래스 로더 누수 의심.

#### GC(Garbage Collection)

> **❗메모리 회수 성능의 핵심 지표이다.**

* **jvm.gc.minor_collection_count** : Minor GC 발생 빈도. Young Gen 회수. 빈도가 높아도 보통 정상.
* **jvm.gc.major_collection_count** : Major GC(Full GC) 발생 빈도. 빈번하면 STW(Stop-The-World) 시간 증가로 지연 발생.
* **jvm.gc.minor_collection_time** : Minor GC에 소요된 시간 비율(‰). 밀리퍼밀(permille) 단위. 낮을수록 좋음.
* **jvm.gc.major_collection_time** : Major GC에 소요된 시간 비율(‰). 이 값이 높으면 애플리케이션 응답 지연의 직접적 원인.
* **jvm.gc.eden_size** : Eden 영역 사용량. 새 객체가 할당되는 공간. 빠르게 차고 비워지는 것이 정상.
* **jvm.gc.old_gen_size** : Old Gen 영역 사용량. 지속 증가 시 메모리 누수 가능성 높음.
* **jvm.gc.survivor_size** : Survivor 영역 사용량. Minor GC 후 살아남은 객체가 대기하는 공간.
* 해석법 : `old_gen_size`가 GC 후에도 줄어들지 않고 계속 상승 추세를 보이면 장기 참조되는 객체가 쌓이고 있다는 의미로 메모리 누수를 의심해야 한다.

#### 스레드 & CPU

* **jvm.thread_count** : 활성 쓰레드 수. 급증 시 쓰레드 풀 고갈 또는 데드락 의심.
* **jvm.cpu_load.process** : 프로세스 CPU 사용률(0 ~ 1). 1에 가까우면 CPU 바운드 상태.
* **jvm.cpu_load.system** : 시스템 전체 CPU 사용률(0 ~ 1). 호스트 레벨 부하 확인용.

#### 기타

* **jvm.loaded_classes** : 현재 로드된 클래스 수. 지속 증가 시 클래스 로더 누수.
* **jvm.buffer_pool.direct.used** : Direct Buffer 사용량. NIO 사용 시 중요. 네이티브 메모리이므로 힙 외 OOM 원인 가능.
* **jvm.os.open_file_descriptors** : 열린 파일 디스크립터 수. OS 한도 초과 시 연결/파일 열기 실패.
