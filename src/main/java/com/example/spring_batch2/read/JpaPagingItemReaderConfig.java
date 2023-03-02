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
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaPagingItemReaderConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    private final UserRepository userRepository;

    @Bean
    public Job job() {
        Random random = new Random();
        for(int i=0; i<100; i++){
            User user = User.builder()
                    .age(random.nextInt(100))
                    .name(UUID.randomUUID().toString().substring(0, 6))
                    .build();
            userRepository.save(user);
        }

        return jobBuilderFactory.get("batchJob")
                .incrementer(new RunIdIncrementer())
                .start(step())
                .build();
    }

    @Bean
    public Step step() {
        return stepBuilderFactory.get("step_jpa")
                .<User, User>chunk(20)
                .reader(itemReader(null))
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
    public JpaPagingItemReader<User> itemReader(
            @Value("#{jobParameters['age']}") Integer age) {

        CustomQueryProvider queryProvider = new CustomQueryProvider();
        queryProvider.setAge(age);

        return new JpaPagingItemReaderBuilder<User>()
                .name("JpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(20)
//                .queryString("select u from User u where u.age <= :age")
                .queryProvider(queryProvider)
                .parameterValues(Collections.singletonMap("age", age))
                .build();
    }

    static class CustomQueryProvider extends AbstractJpaQueryProvider {
        private Integer age;
        public Query createQuery(){
            EntityManager em = getEntityManager();
            Query query = em.createQuery("select u from User u where u.age <= :age");
            query.setParameter("age", age);
            return query;
        }
        public void setAge(Integer age){
            this.age=age;
        }
        @Override
        public void afterPropertiesSet() throws Exception {
        }
    }
}