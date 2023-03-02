package com.example.spring_batch2.read;

import com.example.spring_batch2.entity.User;
import com.example.spring_batch2.read.service.ExternalService;
import com.example.spring_batch2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.adapter.ItemReaderAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class AdapterItemReaderConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserRepository userRepository;
    private final ExternalService externalService;

    @Bean
    public Job externalItemReaderJob() {
        return jobBuilderFactory.get("externalItemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(externalItemReaderStep())
                .build();
    }

    @Bean
    public Step externalItemReaderStep() {
        Random random = new Random();
        for(int i=0; i<100; i++){
            User user = User.builder()
                    .age(random.nextInt(100))
                    .name(UUID.randomUUID().toString().substring(0, 6))
                    .build();
            userRepository.save(user);
        }

        return stepBuilderFactory.get("externalItemReaderStep")
                .<User, User>chunk(20)
                .reader(itemReaderAdapter())
                .writer(items -> {
                    for (User u : items){
                        log.info("write count: {}", u.getId());
                    }
                })
                .build();
    }

    @Bean
    public ItemReaderAdapter<User> itemReaderAdapter(){
        ItemReaderAdapter<User> adapter = new ItemReaderAdapter<>();
        adapter.setTargetObject(externalService);
        adapter.setTargetMethod("cntRead");
        return adapter;
    }
}
