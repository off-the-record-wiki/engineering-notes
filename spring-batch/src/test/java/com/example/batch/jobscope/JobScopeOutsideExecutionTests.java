package com.example.batch.jobscope;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.batch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
class JobScopeOutsideExecutionTests {

    @Autowired
    VisitedSteps visitedSteps;

    @Test
    void Job_실행_밖에서_JobScope_빈을_쓰면_스코프가_없어_예외가_난다() {
        // given

        // when, then
        assertThatThrownBy(() -> visitedSteps.getSteps()).isInstanceOf(ScopeNotActiveException.class);
    }
}