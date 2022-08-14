# Task기반 배치, Chunk 기반 배치

## 1. Tasklet을 이용한 `Task 기반 처리`

- `배치 처리 과정이 비교적 쉬운 경우` 쉽게 사용
- 대량 처리를 하는 경우 더 복잡
- 하나의 큰 덩어리를 여러 덩어리로 나누어 처리하기 부적합

```java
package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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
public class ChunkProcessingConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job chunkProcessingJob(){
        return jobBuilderFactory.get("chunkProcessingJob")
                .incrementer(new RunIdIncrementer())
                .start(taskBaseStep())
                .build();
    }

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

```

```
2022-08-14 22:50:33.060  INFO 1180 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : task item size : 100
```

▶ 100 개 데이터를 한번에 읽어오게 됨

## 2. Chunk를 사용한 `Chunk 기반 처리`

- ItemReader, ItemProcessor, ItemWriter 의 관계 이해 필요
- `대량 처리를 하는 경우` Tasklet보다 비교적 쉽게 구현
- ex) 10,000 개의 데이터 중 1,000개씩 10개의 덩어리로 수행
(Tasklet으로 처리하면 10,000개를 한번에 처리하거나, 수동으로 1,000개씩 분할)

![](https://github.com/hy6219/spring_batch_study/blob/main/%EC%8A%A4%ED%94%84%EB%A7%81%20%EB%B0%B0%EC%B9%98%20%EC%A2%85%EB%A5%98/chunk%20%EA%B8%B0%EB%B0%98%20%EC%B2%98%EB%A6%AC.PNG?raw=true)

- 🧡 `ItemReader에서 null을 return 할때까지 Step 반복`
- `.<String,String>chunk(10) == .<INPUT, OUTPUT>chunk(int)`
	
		- ItemReader : INPUT을 리턴
		- ItemProcessor : INPUT 을 받아서 processing 후 OUTPUT을 리턴 ( INPUT과 OUTPUT의 타입이 같을 수 있음 )
		- ItemWriter : List<OUTPUT>을 받아 write
		- chunk 사이즈 이하로 ItemReader, ItemProcessor에서 처리 후 마지막에 ItemWriter를 일괄 처리


```java
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
  
    //chunk 기반 step  @Bean  
  public Step chunkBaseStep(){  
        return stepBuilderFactory.get("chunkBaseStep")  
                .<String,String>chunk(10)//100 개 데이터를 10개씩 나누게 될 것  
  .reader(itemReader())  
                .processor(itemProcessor())  
                .writer(itemWriter())  
                .build();  
    }  
  
    private ItemWriter<String> itemWriter() {
        //return items -> log.info("chunk item size: {}",items.size());
        return items-> items.forEach(item->log.info(item));
    }
  
    private ItemProcessor<String, String> itemProcessor() {  
        //reader에서 읽은 item에 ", Spring Batch" 붙여주기  
  return item-> item+", Spring Batch";  
    }  
  
    private ItemReader<String> itemReader() {  
        return new ListItemReader<>(getItems());  
    }  
  
  
    //tasklet 기반 step  @Bean  
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
```

```
2022-08-14 23:10:16.758  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : task item size : 100
2022-08-14 23:10:16.771  INFO 29464 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [taskBaseStep] executed in 30ms
2022-08-14 23:10:16.793  INFO 29464 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [chunkBaseStep]
2022-08-14 23:10:16.806  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.813  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.819  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.826  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.834  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.841  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.847  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.856  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.863  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
2022-08-14 23:10:16.870  INFO 29464 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : chunk item size: 10
```

```
2022-08-14 23:14:13.520  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : task item size : 100
2022-08-14 23:14:13.532  INFO 15692 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [taskBaseStep] executed in 28ms
2022-08-14 23:14:13.555  INFO 15692 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [chunkBaseStep]
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 0 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 1 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 2 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 3 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 4 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 5 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 6 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 7 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 8 Hello, Spring Batch
2022-08-14 23:14:13.568  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 9 Hello, Spring Batch
2022-08-14 23:14:13.576  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 10 Hello, Spring Batch
2022-08-14 23:14:13.576  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 11 Hello, Spring Batch
2022-08-14 23:14:13.576  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 12 Hello, Spring Batch
2022-08-14 23:14:13.577  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 13 Hello, Spring Batch
2022-08-14 23:14:13.577  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 14 Hello, Spring Batch
2022-08-14 23:14:13.577  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 15 Hello, Spring Batch
2022-08-14 23:14:13.577  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 16 Hello, Spring Batch
2022-08-14 23:14:13.577  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 17 Hello, Spring Batch
2022-08-14 23:14:13.577  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 18 Hello, Spring Batch
2022-08-14 23:14:13.577  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 19 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 20 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 21 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 22 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 23 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 24 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 25 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 26 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 27 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 28 Hello, Spring Batch
2022-08-14 23:14:13.584  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 29 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 30 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 31 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 32 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 33 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 34 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 35 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 36 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 37 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 38 Hello, Spring Batch
2022-08-14 23:14:13.592  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 39 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 40 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 41 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 42 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 43 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 44 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 45 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 46 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 47 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 48 Hello, Spring Batch
2022-08-14 23:14:13.600  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 49 Hello, Spring Batch
2022-08-14 23:14:13.607  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 50 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 51 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 52 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 53 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 54 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 55 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 56 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 57 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 58 Hello, Spring Batch
2022-08-14 23:14:13.608  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 59 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 60 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 61 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 62 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 63 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 64 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 65 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 66 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 67 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 68 Hello, Spring Batch
2022-08-14 23:14:13.615  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 69 Hello, Spring Batch
2022-08-14 23:14:13.623  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 70 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 71 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 72 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 73 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 74 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 75 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 76 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 77 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 78 Hello, Spring Batch
2022-08-14 23:14:13.624  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 79 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 80 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 81 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 82 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 83 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 84 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 85 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 86 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 87 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 88 Hello, Spring Batch
2022-08-14 23:14:13.631  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 89 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 90 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 91 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 92 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 93 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 94 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 95 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 96 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 97 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 98 Hello, Spring Batch
2022-08-14 23:14:13.639  INFO 15692 --- [           main] f.s.batch.part3.ChunkProcessingConfig    : 99 Hello, Spring Batch
```

▶ 100 개를 10개씩 나누어 작업하는 것을 확인!!
▶ 작업량을 정크사이즈만큼씩 나누어서 자동으로 반복 작업 진행

- chunk 사이즈를 정해두면, 페이징 처리를 하는 것처럼 크기를 나누고 진행
➡ Tasklet은 사이즈를 나눠서 진행할 수 없을까?
➡ 가능하지만, 코드양이 많아지고 chunk 처럼 나이스하게 처리할 수 없음

1. [지금까지 읽은 chunk 크기 f, 지금까지 읽은 chunk 크기 f+ chunk 단위) 만큼 읽고
2. f >= 총 아이템 크기 라면, 종료
3. 마지막 읽은 값을 StepExecution에 넣어주기

```java

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


```

```
2022-08-14 23:32:14.691  INFO 9868 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [taskletBaseStep]
2022-08-14 23:32:14.709  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.718  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.724  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.732  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.738  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.745  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.753  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.760  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.767  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
2022-08-14 23:32:14.775  INFO 9868 --- [           main] f.s.batch.part3.TaskletLikeChunkConfig   : subList size : 10
```
▶ chunk 처럼 10개씩 나누어 작업하는 것을 확인 가능


