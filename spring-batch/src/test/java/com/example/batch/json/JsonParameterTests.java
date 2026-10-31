package com.example.batch.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class JsonParameterTests {

    @Autowired
    JobOperator jobOperator;

    @Autowired
    JsonJobParametersReader jsonJobParametersReader;

    @Autowired
    Job parameterJob;

    @Test
    void JSON_표기법으로_넘기면_쉼표가_들어간_값으로_Job이_실행된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jsonJobParametersReader.read(jsonProperties());

        // when
        jobOperator.start(parameterJob, parameters);

        // then
        assertThat(output.getOut())
                .contains("Hello, batch, json (1/2)")
                .contains("Hello, batch, json (2/2)");
    }

    @Test
    void JSON_표기법은_쉼표가_들어간_값과_지정한_타입을_그대로_유지한다() {
        // given
        Properties properties = jsonProperties();

        // when
        JobParameters parameters = jsonJobParametersReader.read(properties);

        // then
        assertThat(parameters.getParameter("name").value()).isEqualTo("batch, json");
        assertThat(parameters.getParameter("count").type()).isEqualTo(Integer.class);
    }

    private Properties jsonProperties() {
        Properties properties = new Properties();
        properties.setProperty("name", "{\"value\":\"batch, json\",\"type\":\"java.lang.String\"}");
        properties.setProperty("count", "{\"value\":\"2\",\"type\":\"java.lang.Integer\"}");
        return properties;
    }
}