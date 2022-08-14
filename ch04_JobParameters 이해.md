# JobParameters 이해

- `배치 실행에 필요한 값`을 `parameters를 통해 외부에서 주입`
- 외부에서 주입된 parameter를 관리하는 객체
- parameter를 **JobParameters** 와 **SpEL(Spring Expression Language) ** 로 접근

	- 방법 1 ) String parameter = jobParameters.getString(key, defaultValue); //key값으로 된 value가 없으면 defaultValue 반환
	- 방법 2 ) `@Value("#{jobParameters[key]}")`

## 1. 방법 1. `StepExecution.getJobParameters==JobParameters` 이용
```java

package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
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
                .start(startStep())
                .build();
    }

    @Bean
    public Step startStep() {

        return stepBuilderFactory.get("startStep")
                .tasklet(paramTasklet())
                .build();
    }

    private Tasklet paramTasklet() {

        List<String> items = getItems();

        return (contribution, chunkContext) -> {
            StepExecution stepExecution = contribution.getStepExecution();
            JobParameters jobParameters = stepExecution.getJobParameters();

            //jobParameters에서 ""key 값이 없으면 10
            String value = jobParameters.getString("chunkSize", "10");
            int chunkSize = !StringUtils.isEmpty(value) ? Integer.parseInt(value) : 10;
            int startIdx = stepExecution.getReadCount();
            int endIdx = startIdx + chunkSize;

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


```

`Edit Configuration` - `Program Arguments` 

`-chunkSize=20 --job.name=paramJob` 로 수정

```
2022-08-14 23:57:49.972  INFO 7332 --- [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: [-chunkSize=20]
2022-08-14 23:57:50.290  INFO 7332 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=paramJob]] launched with the following parameters: [{chunkSize=20, run.id=3}]
2022-08-14 23:57:50.337  INFO 7332 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [startStep]
2022-08-14 23:57:50.356  INFO 7332 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-14 23:57:50.364  INFO 7332 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-14 23:57:50.371  INFO 7332 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-14 23:57:50.379  INFO 7332 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-14 23:57:50.385  INFO 7332 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-14 23:57:50.405  INFO 7332 --- [           main] o.s.batch.core.step.AbstractStep         : Step: [startStep] executed in 68ms
2022-08-14 23:57:50.421  INFO 7332 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=paramJob]] completed with the following parameters: [{chunkSize=20, run.id=3}] and the following status: [COMPLETED] in 106ms
```

➡ 20 개씩 끊어서 반복 수행되는 모습을 확인

## 2. 방법 2. SpEL 사용

1. Step에 `@JobScope` 달아주기
2. Step의 인자 앞에 롬복이 아닌 `org.springframework.beans.factory.annotation.Value` 어노테이션을 달아주기
3. `Edit Configuration 에서 -parameterName=~ 처럼 파라미터 값 넣어주기`
4. Job에서 해당 step의 인자값 넣어주기

```java
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
 @JobScope  public Step startStep(@Value("#{jobParameters[chunkSize]}") String chunkSize) {  
  
        return stepBuilderFactory.get("startStep")  
                .tasklet(paramTasklet(chunkSize))  
                .build();  
    }  
  
    private Tasklet paramTasklet(String chunkSize) {  
  
        List<String> items = getItems();  
  
        return (contribution, chunkContext) -> {  
            StepExecution stepExecution = contribution.getStepExecution();  
  
            //jobParameters에서 ""key 값이 없으면 10  int unit = !StringUtils.isEmpty(chunkSize) ? Integer.parseInt(chunkSize) : 10;  
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
```

```
2022-08-15 00:07:40.577  INFO 28652 --- [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: [-chunkSize=20]
2022-08-15 00:07:40.777  INFO 28652 --- [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=paramJob]] launched with the following parameters: [{chunkSize=20, run.id=4}]
2022-08-15 00:07:40.834  INFO 28652 --- [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [startStep]
2022-08-15 00:07:40.852  INFO 28652 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-15 00:07:40.859  INFO 28652 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-15 00:07:40.866  INFO 28652 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-15 00:07:40.874  INFO 28652 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
2022-08-15 00:07:40.879  INFO 28652 --- [           main] f.s.batch.part3.JobParametersConfig      : subList size : 20
```

▶ 동일하게 20개씩 끊어서 반복 실행하는 것을 확인 가능


