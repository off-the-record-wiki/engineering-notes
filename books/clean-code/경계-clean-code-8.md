# (Clean Code 8) Clean Code - 경계

#### 깨끗한 경계

* 경계에서는 변경과 같은 일이 많이 벌어진다.
* 통제가 불가능한 외부 패키지에 의존하는 대신 통제가 가능한 우리 코드에 의존하는 편이 훨씬 좋다.
* 외부 패키지를 호출하는 코드를 가능한 줄여 경계를 관리하는 것이 중요하다.
* 새로운 클래스로 경계를 감싸거나 아니면 Adapter 패턴을 사용해 우리 서비스에서 원하는 인터페이스를 패키지가 제공하는 인터페이스로 변환하자.

```java
// ⭕ Good(Adapter)
class DartFinancialAdapter(
    private val dartClient: DartClient          // 외부 라이브러리
) : FinancialStatementPort {                    // 내 인터페이스

    override fun fetch(corpCode: String): FinancialStatement {
        val response = dartClient.getFinancials(corpCode)   // 외부 형식
        return toFinancialStatement(response)               // 내 도메인으로 변환
    }

    // 변환 로직을 경계 안에 격리
    private fun toFinancialStatement(
        response: DartResponse
    ): FinancialStatement {
        return FinancialStatement(
            revenue     = response.thstrm_amount.toBigDecimal(),
            operProfit  = response.thstrm_add_amount.toBigDecimal(),
            reportedAt  = LocalDate.parse(response.rcept_dt)
        )
    }
}
```
