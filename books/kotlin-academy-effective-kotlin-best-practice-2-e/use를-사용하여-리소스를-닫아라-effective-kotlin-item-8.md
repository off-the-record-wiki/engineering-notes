# (Effective Kotlin Item 8) Kotlin - use를 사용하여 리소스를 닫아라

#### use를 사용하여 리소스를 닫아라

* 자동으로 닫히지 않는 리소스가 더 이상 필요하지 않다면 명시적으로 `close()` 메서드를 호출해야 한다.
* 코틀린/JVM에서 사용하는 자바 표준 라이브러리에는 다음과 같은 리소스가 포함되어 있다.
  * `InputStream`과 `OutputStream`
  * `java.sql.Connection`
  * `java.io.Reader(FileReader, BufferedReader, CSSParser)`
  * `java.new.Socket`과 `java.util.Scanner`
* 이런 모든 리소스는 `AutoCloseable`을 확장하는 `Closeable` 인터페이스를 구현한다.
* 문제는 이런 리소스들은 비용이 많이 들고 자동으로 닫히기 어려우므로 사용하고 난 후에는 반드시 `close()` 메서드를 호출해야 한다.
* 리소스에 대한 참조가 최종적으로 없어지게 되면 GC가 처리하게 되지만 처리하는데 시간이 걸린다.
* 리소스를 닫기 위해 전통적으로 `try-finally` 블록으로 래핑하고 `close()` 메서드를 호출한다.
* 코틀린에서는 리소스를 올바르게 닫고 예외를 처리하려면 `use()` 메서드를 사용해야 한다. 이 함수는 모든 `Closeable` 객체에 사용할 수 있다.
```kotlin
// ⭕ Good
fun countCharactersInFile(path: String): Int {
    val reader = BufferedReader(FileReader(path))
    reader.use {
        return reader.lineSequence().sumBy { it.length }
    }
}
```
* `use()` 함수는 파일을 다루는 데 자주 사용되지만 파일을 한 줄씩 읽는 것이 일반적이므로 코틀린 표준 라이브러리는 파일을 줄 단위 시퀀스로 읽고 처리가 끝난 뒤 닫는 `useLines()` 함수도 제공한다.
* 시퀀스를 사용하여 필요에 따라 한 줄씩 읽기 때문에 한 줄 분량의 메모리만 차지하므로 대규모의 파일을 처리할 때 적합하다.
* 시퀀스의 단점은 파일을 한 번씩밖에 못 읽는다는 점이다. 만약 파일을 두 번 이상 반복해서 읽어야 한다면 해당 파일을 두 번 이상 열어야 한다.
* `useLines()` 함수는 표현식으로도 사용할 수 있다.
```kotlin
// ⭕ Good
fun countCharactersInFile(path: String): Int = 
    File(path).useLines { lines ->
        lines.sumBy { it.length }
    }
```
* 시퀀스 덕분에 파일 전체를 불러오는 대신 한 줄씩 읽을 수 있게 되었다.
