package com.getquer.tasktracker.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopic {
    public static final String TASK_EVENT_TOPIC = "task-events";
    @Bean
    public NewTopic taskEventsTopic(){
        return TopicBuilder
                .name(TASK_EVENT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
