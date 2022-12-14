package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ChunkProcessingConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job chunkProcessingJob(){
        return jobBuilderFactory.get("chunkProcessingJob")
                .incrementer(new RunIdIncrementer())
                .start(taskBaseStep())
                .next(chunkBaseStep())
                .build();
    }

    //chunk 기반 step
    @Bean
    public Step chunkBaseStep(){
        return stepBuilderFactory.get("chunkBaseStep")
                .<String,String>chunk(10)//100 개 데이터를 10개씩 나누게 될 것
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    private ItemWriter<String> itemWriter() {
        return items -> log.info("chunk item size: {}",items.size());
       // return items-> items.forEach(item->log.info(item));
    }

    private ItemProcessor<String, String> itemProcessor() {
        //reader에서 읽은 item에 ", Spring Batch" 붙여주기
        return item-> item+", Spring Batch";
    }

    private ItemReader<String> itemReader() {
        return new ListItemReader<>(getItems());
    }


    //tasklet 기반 step
    @Bean
    public Step taskBaseStep(){
        return stepBuilderFactory.get("taskBaseStep")
                .tasklet(tasklet())
                .build();
    }

    @Bean
    public Tasklet tasklet(){
        return ((contribution, chunkContext) -> {
           List<String> items = getItems();

           log.info("task item size : {}", items.size());

           return RepeatStatus.FINISHED;
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
