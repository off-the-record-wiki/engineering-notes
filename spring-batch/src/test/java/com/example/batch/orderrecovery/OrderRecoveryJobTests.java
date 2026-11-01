package com.example.batch.orderrecovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
class OrderRecoveryJobTests {

    private static final String READY = "READY_FOR_SHIPMENT";
    private static final String SHIPPED = "SHIPPED";
    private static final String CANCELLED = "CANCELLED";

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job orderRecoveryJob;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(orderRecoveryJob);
        jdbcTemplate.update("DELETE FROM orders");
    }

    @Test
    void 배송번호가_없는_SHIPPED_주문은_READY_FOR_SHIPMENT로_바뀐다() throws Exception {
        // given
        insertOrder(1L, SHIPPED, null);

        // when
        jobOperatorTestUtils.startJob(uniqueParameters());

        // then
        assertThat(statusOf(1L)).isEqualTo(READY);
    }

    @Test
    void 배송번호가_있는_CANCELLED_주문은_SHIPPED로_바뀐다() throws Exception {
        // given
        insertOrder(1L, CANCELLED, "SHP-1");

        // when
        jobOperatorTestUtils.startJob(uniqueParameters());

        // then
        assertThat(statusOf(1L)).isEqualTo(SHIPPED);
    }

    @Test
    void 배송번호가_없는_CANCELLED_주문은_READY_FOR_SHIPMENT로_바뀐다() throws Exception {
        // given
        insertOrder(1L, CANCELLED, null);

        // when
        jobOperatorTestUtils.startJob(uniqueParameters());

        // then
        assertThat(statusOf(1L)).isEqualTo(READY);
    }

    @Test
    void 배송번호가_있는_정상_SHIPPED_주문은_상태를_그대로_두고_쓰기_대상에서_빠진다() throws Exception {
        // given
        insertOrder(1L, SHIPPED, "SHP-1");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(uniqueParameters());

        // then
        assertThat(statusOf(1L)).isEqualTo(SHIPPED);
        assertThat(execution.getStepExecutions()).singleElement().extracting(StepExecution::getFilterCount).isEqualTo(1L);
        assertThat(execution.getStepExecutions()).singleElement().extracting(StepExecution::getWriteCount).isEqualTo(0L);
    }

    @Test
    void READY_FOR_SHIPMENT_주문은_읽지_않고_상태도_그대로_둔다() throws Exception {
        // given
        insertOrder(1L, READY, null);
        insertOrder(2L, READY, "SHP-2");
        insertOrder(3L, CANCELLED, "SHP-3");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(uniqueParameters());

        // then
        assertThat(execution.getStepExecutions()).singleElement().extracting(StepExecution::getReadCount).isEqualTo(1L);
        assertThat(statusOf(1L)).isEqualTo(READY);
        assertThat(statusOf(2L)).isEqualTo(READY);
    }

    @Test
    void 복구_대상이_25건이어도_조회에서_빠지는_행을_건너뛰지_않고_모두_규칙대로_바꾼다() throws Exception {
        // given
        for (long id = 1; id <= 25; id++) {
            insertPattern(id);
        }
        List<Map<String, Object>> expected = jdbcTemplate
                .queryForList("SELECT id, shipping_id FROM orders ORDER BY id")
                .stream()
                .map(row -> Map.<String, Object>of("id", row.get("id"), "status", expectedStatus(row.get("shipping_id"))))
                .toList();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(uniqueParameters());

        // then
        assertThat(execution.getStepExecutions()).singleElement().extracting(StepExecution::getReadCount).isEqualTo(25L);
        assertThat(jdbcTemplate.queryForList("SELECT id, status FROM orders ORDER BY id")).isEqualTo(expected);
    }

    @Test
    void 복구할_때_status만_바뀌고_customer_id_order_datetime_shipping_id는_그대로_둔다() throws Exception {
        // given
        insertOrder(1L, SHIPPED, null);
        insertOrder(2L, CANCELLED, "SHP-2");
        List<Map<String, Object>> before = orderColumns();

        // when
        jobOperatorTestUtils.startJob(uniqueParameters());

        // then
        assertThat(orderColumns()).isEqualTo(before);
        assertThat(statusOf(1L)).isEqualTo(READY);
    }

    @Test
    void 복구_대상이_없으면_COMPLETED로_끝나고_읽은_건수는_0이다() throws Exception {
        // given
        insertOrder(1L, READY, null);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(uniqueParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement().extracting(StepExecution::getReadCount).isEqualTo(0L);
    }

    private JobParameters uniqueParameters() {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder().toJobParameters();
    }

    private void insertOrder(long id, String status, String shippingId) {
        jdbcTemplate.update(
                "INSERT INTO orders (id, customer_id, order_datetime, status, shipping_id) VALUES (?, ?, ?, ?, ?)",
                id,
                1000L + id,
                Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 9, 0).plusHours(id)),
                status,
                shippingId);
    }

    private void insertPattern(long id) {
        switch ((int) (id % 4)) {
            case 0 -> insertOrder(id, SHIPPED, null);
            case 1 -> insertOrder(id, CANCELLED, "SHP-" + id);
            case 2 -> insertOrder(id, CANCELLED, null);
            default -> insertOrder(id, SHIPPED, "SHP-" + id);
        }
    }

    private static String expectedStatus(Object shippingId) {
        return shippingId == null ? READY : SHIPPED;
    }

    private String statusOf(long id) {
        return jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, id);
    }

    private List<Map<String, Object>> orderColumns() {
        return jdbcTemplate.queryForList(
                "SELECT id, customer_id, order_datetime, shipping_id FROM orders ORDER BY id");
    }
}