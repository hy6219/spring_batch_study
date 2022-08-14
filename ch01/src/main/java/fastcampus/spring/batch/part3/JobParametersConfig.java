package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JobParametersConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job paramJob() {
        return jobBuilderFactory.get("paramJob")
                .incrementer(new RunIdIncrementer())
                .start(startStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step startStep(@Value("#{jobParameters[chunkSize]}") String chunkSize) {

        return stepBuilderFactory.get("startStep")
                .tasklet(paramTasklet(chunkSize))
                .build();
    }

    private Tasklet paramTasklet(String chunkSize) {

        List<String> items = getItems();

        return (contribution, chunkContext) -> {
            StepExecution stepExecution = contribution.getStepExecution();

            //jobParameters에서 ""key 값이 없으면 10
            int unit = !StringUtils.isEmpty(chunkSize) ? Integer.parseInt(chunkSize) : 10;
            int startIdx = stepExecution.getReadCount();
            int endIdx = startIdx + unit;

            if (startIdx >= items.size()) {
                return RepeatStatus.FINISHED;
            }

            List<String> subList = items.subList(startIdx, endIdx);

            log.info("subList size : {}", subList.size());

            stepExecution.setReadCount(endIdx);

            return RepeatStatus.CONTINUABLE;
        };
    }

    private List<String> getItems() {

        List<String> items = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            items.add(i + " Hello");
        }

        return items;
    }
}
