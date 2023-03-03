package com.example.spring_batch2.jms;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.jms.JmsItemReader;
import org.springframework.batch.item.jms.JmsItemWriter;
import org.springframework.batch.item.jms.builder.JmsItemReaderBuilder;
import org.springframework.batch.item.jms.builder.JmsItemWriterBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.oxm.xstream.XStreamMarshaller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class JmsJobConfig {

    private final JmsTemplate jmsTemplate;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job jmsJob(){
        return jobBuilderFactory.get("jmsJob")
                .incrementer(new RunIdIncrementer())
                .start(jmsPubStep())
                .next(jmsSubStep())
                .build();
    }

    @Bean
    public Step jmsPubStep(){
        return stepBuilderFactory.get("jmsPubStep")
                .<Account,Account>chunk(5)
                .reader(flatFileItemReader(null))
                .writer(jmsItemWriter())
                .build();
    }
    @Bean
    public Step jmsSubStep(){
        return stepBuilderFactory.get("jmsSubStep")
                .<Account,Account>chunk(5)
                .reader(jmsItemReader())
                .writer(xmlItemWriter(null))
                .build();
    }

    @Bean
    public JmsItemReader<Account> jmsItemReader(){
        return new JmsItemReaderBuilder<Account>()
                .jmsTemplate(jmsTemplate)
                .itemType(Account.class)
                .build();
    }

    @Bean
    public JmsItemWriter<Account> jmsItemWriter(){
        return new JmsItemWriterBuilder<Account>()
                .jmsTemplate(jmsTemplate)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Account> flatFileItemReader(
            @Value("#{JobParameters['inputFile']}") String file) //account.csv
    {
        return new FlatFileItemReaderBuilder<Account>()
                .name("flatFileReader")
                .resource(new ClassPathResource(file))
                .delimited() //구분자 ','
                .names(new String[] {"name", "age"}) //커스터마이징 필요한 변환이라면 FieldSetMapper 설정
                .targetType(Account.class)
                .build();
    }

    @Bean
    @StepScope
    public StaxEventItemWriter<Account> xmlItemWriter( //inputFile=account.csv outputFile=C:\\Users\\nolan\\OneDrive\\
            @Value("#{JobParameters['outputFile']}") String file)
    {
        file+=LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd일_HH시_mm분_ss초_")) +"account.xml";

        Map<String, Class> aliases = new HashMap<>();
        aliases.put("account", Account.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);

        return new StaxEventItemWriterBuilder<Account>()
                .name("xmlFileWriter")
                .resource(new FileSystemResource(file))
                .marshaller(marshaller)
                .rootTagName("account")
                .build();
    }
}
