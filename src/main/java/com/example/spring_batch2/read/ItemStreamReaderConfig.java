package com.example.spring_batch2.read;

import com.example.spring_batch2.entity.User;
import com.example.spring_batch2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
//@Configuration
public class ItemStreamReaderConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserRepository userRepository;

    @Bean
    public Job streamReaderJob() {
        return jobBuilderFactory.get("streamReaderJob")
//                .incrementer(new RunIdIncrementer()) 두번 실행 필요.
                .start(streamReaderStep())
                .build();
    }

    @Bean
    public Step streamReaderStep() {
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            User user = User.builder()
                    .age(random.nextInt(100))
                    .name(UUID.randomUUID().toString().substring(0, 6))
                    .build();
            userRepository.save(user);
        }

        List<User> users = userRepository.findAll();

        return stepBuilderFactory.get("streamReaderStep")
                .<User, User>chunk(2)
                .reader(new CustomItemStreamReader(users))
                .writer(items -> {
                    for (User u : items){
                        log.info("write count: {}", u.getId());
                    }
                })
                .build();
    }
}
