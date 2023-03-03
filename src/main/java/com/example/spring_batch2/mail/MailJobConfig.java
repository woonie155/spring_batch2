package com.example.spring_batch2.mail;

import com.example.spring_batch2.entity.User;
import com.example.spring_batch2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.batch.item.mail.SimpleMailMessageItemWriter;
import org.springframework.batch.item.mail.builder.SimpleMailMessageItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.*;

@Configuration
@RequiredArgsConstructor
public class MailJobConfig {
    @Value("${spring.mail.username}")
    private String send_email;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job mailJob() throws MessagingException {
        Random random = new Random();
        for(int i=0; i<4; i++){
            User user = User.builder()
                    .age(random.nextInt(25))
                    .name(UUID.randomUUID().toString().substring(0, 6))
                    .build();
            userRepository.save(user);
        }

        return jobBuilderFactory.get("mailJob")
                .incrementer(new RunIdIncrementer())
                .start(mailStep())
                .build();
    }

    @Bean
    public Step mailStep() throws MessagingException {
        return stepBuilderFactory.get("mailStep")
                .<User, MimeMessage>chunk(2)
                .reader(mailItemReader(null))
                .processor(itemProcessor())
                .writer(items -> items.forEach(i->javaMailSender.send(i)))
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<User> mailItemReader(
            @Value("#{jobParameters['age']}") Integer age) {

        CustomQueryProvider queryProvider = new CustomQueryProvider();
        queryProvider.setAge(age);

        return new JpaPagingItemReaderBuilder<User>()
                .name("JpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(20)
                .queryProvider(queryProvider)
                .parameterValues(Collections.singletonMap("age", age))
                .build();
    }

    @Bean
    public ItemProcessor<User, MimeMessage> itemProcessor() throws MessagingException {
        return new ItemProcessor<User, MimeMessage>() {
            @Override
            public MimeMessage process(User item) throws Exception {
                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message, true, "utf-8");
                mimeMessageHelper.setFrom(send_email);
                mimeMessageHelper.setTo("woonie155@gmail.com");
                mimeMessageHelper.setSubject("제목작성");
                StringBuilder sb = new StringBuilder();
                sb.append("<html><body>\n html <b>전송</b> \n</body></html>");
                mimeMessageHelper.setText(sb.toString(), true);
                return message;
            }
        };
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
