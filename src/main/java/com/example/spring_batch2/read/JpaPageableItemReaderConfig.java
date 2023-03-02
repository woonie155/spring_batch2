package com.example.spring_batch2.read;

import com.example.spring_batch2.entity.User;
import com.example.spring_batch2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaPageableItemReaderConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserRepository userRepository;

    @Bean
    public Job pageableJob() {
        return jobBuilderFactory.get("jpaPageableItemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(pageableStep())
                .build();
    }

    @Bean
    public Step pageableStep() {
        Random random = new Random();
        for(int i=0; i<100; i++){
            User user = User.builder()
                    .age(random.nextInt(100))
                    .name(UUID.randomUUID().toString().substring(0, 6))
                    .build();
            userRepository.save(user);
        }

        return stepBuilderFactory.get("jpaPageableItemReaderStep")
                .<User, User>chunk(20)
                .reader(pageableItemReader(null))
                .writer(items -> {
                    log.info("======================\n write count: {}", items.size());
                    for (User u : items) {
                        System.out.println("age: " + u.getAge());
                    }
                    log.info("======================");
                })
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<User> pageableItemReader(
            @Value("#{jobParameters['age']}") Integer age) {

        return new RepositoryItemReaderBuilder<User>()
                .name("pageableItemReader")
                .arguments(Collections.singletonList(age))
                .methodName("findByAgeLessThanEqual")
                .repository(userRepository)
                .sorts(Collections.singletonMap("age", Sort.Direction.ASC))
                .build();
    }
}
