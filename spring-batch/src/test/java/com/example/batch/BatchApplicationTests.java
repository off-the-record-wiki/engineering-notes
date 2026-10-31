package com.example.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
class BatchApplicationTests {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void MySQL에서_컨텍스트가_뜨면_배치_메타_테이블이_생성된다() {
        // given

        // when
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT UPPER(TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                String.class);

        // then
        assertThat(tables).contains(
                "BATCH_JOB_INSTANCE",
                "BATCH_JOB_EXECUTION",
                "BATCH_JOB_EXECUTION_PARAMS",
                "BATCH_JOB_EXECUTION_CONTEXT",
                "BATCH_STEP_EXECUTION",
                "BATCH_STEP_EXECUTION_CONTEXT");
    }
}