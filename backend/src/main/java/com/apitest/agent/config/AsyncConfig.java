package com.apitest.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private final AppProperties appProperties;

    public AsyncConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean(name = "testExecutionExecutor")
    public Executor testExecutionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int concurrency = appProperties.getTesting().getMaxConcurrency();
        executor.setCorePoolSize(concurrency > 0 ? concurrency : 5);
        executor.setMaxPoolSize((concurrency > 0 ? concurrency : 5) * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("test-exec-");
        executor.initialize();
        return executor;
    }
}
