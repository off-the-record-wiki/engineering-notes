# (Clean Code 7) Clean Code - 오류 처리

#### 오류 코드보다 예외를 사용하라

```java
// ❌ Bad(비즈니스 로직과 오류 처리가 뒤섞여 있다, 정상 플로우와 예외 플로우가 혼재되어있음)
public class DeviceController {

    public void sendShutDown() {
        DeviceHandle handle = getHandle(DEV1);

        if (handle != DeviceHandle.INVALID) {
            retrieveDeviceRecord(handle);

            if (record.getStatus() != DEVICE_SUSPENDED) {
                pauseDevice(handle);
                clearDeviceWorkQueue(handle);
                closeDevice(handle);
            } else {
                logger.log("Device suspended. Unable to shut down");
            }
        } else {
            logger.log("Invalid handle for: " + DEV1.toString());
        }
    }
}
```

* 오류가 발생하면 예외를 던지는 편이 낫다. 그러면 호출자의 코드가 더 깔끔해진다.

```java
// ⭕ Good(논리가 오류 처리 코드와 뒤섞이지 않는다)
public class DeviceController {

    public void sendShutDown() {
        try {
            tryToShutDown();
        } catch (DeviceShutDownError e) {
            logger.log(e);
        }
    }

    // 비즈니스 로직만 집중
    private void tryToShutDown() throws DeviceShutDownError {
        DeviceHandle handle = getHandle(DEV1);
        retrieveDeviceRecord(handle);

        pauseDevice(handle);
        clearDeviceWorkQueue(handle);
        closeDevice(handle);
    }

    private DeviceHandle getHandle(DeviceID id) throws DeviceShutDownError {
        DeviceHandle handle = lookupDevice(id);

        if (handle == DeviceHandle.INVALID) {
            throw new DeviceShutDownError("Invalid handle for: " + id);
        }

        return handle;
    }

    private void retrieveDeviceRecord(DeviceHandle handle) throws DeviceShutDownError {
        if (record.getStatus() == DEVICE_SUSPENDED) {
            throw new DeviceShutDownError("Device suspended. Unable to shut down");
        }
    }
}
```

#### try-catch-finally 문부터 작성하라

* try 블록은 트랜잭션과 비슷하다. try 블록에서 무슨 일이 생기든지 catch 블록은 프로그램 상태를 일관성있게 유지해야 한다.
* 그러므로 예외가 발생할 코드를 짤 때는 `try-catch-finally`문으로 시작하는 편이 낫다.

```java
public List<String> readLines(String path) {
    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
        return reader.lines().collect(Collectors.toList());
    } catch (IOException e) {
        throw new StorageException("파일 읽기 실패: " + path, e);
    }
}
```

#### 미확인(UnChecked) 예외를 사용하라

* 최상위 함수가 아래 함수를 호출하고 아래 함수가 그 아래 함수를 호출한다.
* 단계를 내려갈수록 호출하는 함수의 수는 늘어난다. 이제 최하위 함수를 변경해 새로운 오류를 던진다고 가정할 때, 확인된 오류를 던진다면 함수 선언부에 `throws`절을 추가해야 한다.
* 그러면 변경한 함수를 호출하는 함수 모두가 `catch` 블록에서 새로운 예외를 처리하거나 선언부에 `throw`절을 추가해야 한다. 결과적으로 최하위 단계에서 최상위 단계까지 연쇄적인 수정이 일어난다.
* 때로는 확인된 예외도 유용하다. 아주 중요한 라이브러리를 작성한다면 모든 예외를 잡아야 한다. 하지만 일반적인 애플리케이션은 의존성이라는 비용이 이익보다 크다.

#### 예외에 의미를 제공하라

* 예외를 던질 때는 전후 상황을 충분히 덧붙인다. 그러면 오류가 발생한 원인과 위치를 찾기가 쉬워진다.
* 실패한 코드의 의미를 파악하려면 호출 스택만으로 부족하다.
* 오류 메시지에 정보를 담아 예외와 함께 던진다. 실패한 연산 이름과 실패 유형도 언급한다.
* 애플리케이션이 로깅 기능을 사용한다면 `catch` 블록에서 오류를 기록하도록 충분한 정보를 남겨준다.

#### 호출자를 고려해 예외 클래스를 정의하라

* 애플리케이션에서 오류를 정의할 때 프로그래머에게 가장 중요한 관심사는 오류를 잡아내는 방법이 되어야 한다.

```java
// ❌ Bad(예외 종류만 다를 뿐 처리 방식이 전부 동일)
ACMEPort port = new ACMEPort(12);

try {
    port.open();
} catch (DeviceResponseException e) {
    reportPortError(e);
    logger.log("Device response exception", e);
} catch (ATM1212UnlockedException e) {
    reportPortError(e);
    logger.log("Unlock exception", e);
} catch (GMXError e) {
    reportPortError(e);
    logger.log("Device response exception", e);
} finally {
    // ...
}
```
```java
// ⭕ Good
public class LocalPort {

    private ACMEPort innerPort;

    public LocalPort(int portNumber) {
        this.innerPort = new ACMEPort(portNumber);
    }

    public void open() {
        try {
            innerPort.open();
        } catch (DeviceResponseException e) {
            throw new PortDeviceFailure(e);  // 단일 예외로 변환
        } catch (ATM1212UnlockedException e) {
            throw new PortDeviceFailure(e);
        } catch (GMXError e) {
            throw new PortDeviceFailure(e);
        }
    }
}
```

#### 정상 흐름을 정의하라

```java
// ❌ Bad(예외로 흐름 제어)
try {
    MealExpenses expenses = expenseReportDAO.getMeals(employee.getId());
    totalExpenses += expenses.getTotal();
} catch (MealExpensesNotFound e) {
    totalExpenses += getMealPerDiem();  // 예외를 흐름 제어에 사용
}
```
```java
// ⭕ Good
public MealExpenses getMeals(int employeeId) {
    MealExpenses expenses = findMeals(employeeId);

    if (expenses == null) {
        return new PerDiemMealExpenses();  // 기본값 객체 반환
    }

    return expenses;
}
```

#### null을 반환하지 마라

* `null`을 반환하는 코드는 일거리를 늘릴 뿐만 아니라 호출자에게 문제를 떠넘긴다.
* `null` 확인을 빼먹는다면 애플리케이션이 통제 불능에 빠질지도 모른다.

```java
// ❌ Bad
public Employee getEmployee(String name) {
    if (/* 직원이 없으면 */) {
        return null;  // 호출 측에 null 처리 책임 전가
    }
    return employee;
}
```
```java
// ⭕ Good(빈 컬렉션 반환)
// 리스트라면 null 대신 빈 컬렉션
public List<Employee> getEmployees() {
    if (/* 직원이 없으면 */) {
        return Collections.emptyList();  // null 대신
    }
    return employees;
}

// ...

// 호출 측 — null 확인 불필요
for (Employee e : getEmployees()) {
    totalPay += e.getPay();
}
```
```java
// ⭕ Good(Optional 반환)
// 단건 조회라면 Optional
public Optional<Employee> getEmployee(String name) {
    return Optional.ofNullable(findEmployee(name));
}

// ...

// 호출 측
getEmployee(name)
    .map(Employee::getMealExpenses)
    .ifPresent(e -> totalExpenses += e.getTotal());
```
```java
// ⭕ Good(정상 흐름 유지를 위한 예외 객체 반환)
public Employee getEmployee(String name) {
    Employee employee = findEmployee(name);

    if (employee == null) {
        return new NullEmployee();  // null 대신 기본 객체
    }

    return employee;
}

// ...

public class NullEmployee implements Employee {

    @Override
    public MealExpenses getMealExpenses() {
        return new PerDiemMealExpenses();  // 기본값
    }

    @Override
    public int getPay() {
        return 0;
    }
}
```

#### null을 전달하지 마라

* 메서드에서 `null`을 반환하는 방식도 나쁘지만 메서드로 `null`을 전달하는 방식은 더 나쁘다.
* 정상적인 인수로 `null`을 기대하는 API가 아니라면 메서드로 `null`을 전달하는 코드는 최대한 피한다.

```java
// ❌ Bad(null이 들어올 가능성을 메서드가 직접 방어해야 함)
// 메서드 — null 방어 코드가 강제됨
public double xProjection(Point p1, Point p2) {
    if (p1 == null || p2 == null) {
        throw new InvalidArgumentException("Invalid argument for xProjection");
    }
    return (p2.x - p1.x) * 1.5;
}

// ...

// 호출 측에서 null 전달
calculator.xProjection(null, new Point(12, 13));
```
```java
// ⭕ Good(assert로 사전 조건 명시)
public double xProjection(Point p1, Point p2) {
    assert p1 != null : "p1 should not be null";
    assert p2 != null : "p2 should not be null";

    return (p2.x - p1.x) * 1.5;
}
```
```java
// ⭕ Good(null이 들어올 수 없는 구조로 설계)
public double xProjection(Point p1, Point p2) {
    return (p2.x - p1.x) * 1.5;
}

// ...

// 호출 측 책임 — 유효한 값만 전달
Point p1 = pointRepository.findById(id)
    .orElseThrow(() -> new PointNotFoundException(id));

calculator.xProjection(p1, new Point(12, 13));
```
```java
// ⭕ Good(컴파일 타임에 null 전달 불가)
fun xProjection(p1: Point, p2: Point): Double {
    return (p2.x - p1.x) * 1.5
}
```
