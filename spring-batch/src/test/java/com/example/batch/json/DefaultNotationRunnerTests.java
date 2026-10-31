package com.example.batch.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

@SpringBootTest(args = {"--spring.batch.job.name=parameterJob", "name=batch", "count=3,java.lang.Integer"})
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class DefaultNotationRunnerTests {

    @Test
    void JSON_변환기_빈이_있어도_명령행의_기본_표기법으로_Job이_실행된다(CapturedOutput output) {
        // given

        // when

        // then
        assertThat(output.getOut()).contains("Hello, batch (3/3)");
    }
}