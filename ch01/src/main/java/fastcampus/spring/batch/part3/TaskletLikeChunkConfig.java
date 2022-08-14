package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class TaskletLikeChunkConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job taskletToChunkJob(){
        return jobBuilderFactory.get("taskletToChunkJob")
                .incrementer(new RunIdIncrementer())
                .start(taskletBaseStep())
                .build();
    }

    @Bean
    public Step taskletBaseStep() {
        return stepBuilderFactory.get("taskletBaseStep")
                .tasklet(taskletBean())
                .build();
    }

    private Tasklet taskletBean() {

        List<String> items = getItems();

        return ((contribution, chunkContext) -> {

            StepExecution stepExecution = contribution.getStepExecution();

            int chunkSize = 10;
            //지금까지 읽은 chunk의 크기
            int fromIdx = stepExecution.getReadCount();
            int toIdx = fromIdx + chunkSize;

            if(fromIdx >= items.size()){
                return RepeatStatus.FINISHED;
            }

            List<String> subList = items.subList(fromIdx, toIdx);

            log.info("subList size : {}",subList.size());

            //마지막 읽은 값을 저장해주기
            stepExecution.setReadCount(toIdx);

            return RepeatStatus.CONTINUABLE;
        });
    }

    private List<String> getItems(){
        List<String> items = new ArrayList<>();
        //100.for
        for (int i = 0; i < 100; i++) {
            items.add(i+" Hello");
        }

        return items;
    }

}
