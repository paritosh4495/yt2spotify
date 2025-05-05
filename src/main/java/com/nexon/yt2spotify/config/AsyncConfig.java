package com.nexon.yt2spotify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // Define the primary TaskExecutor bean Spring should use for @Async
    // Note: Bean name "taskExecutor" is often looked for by default.
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Configure core pool size, max pool size, queue capacity, etc. as needed
        executor.setCorePoolSize(5); // Example: Start with 5 threads
        executor.setMaxPoolSize(10); // Example: Allow up to 10 threads
        executor.setQueueCapacity(25); // Example: Queue up to 25 tasks
        executor.setThreadNamePrefix("AsyncTransfer-");
        executor.initialize(); // Initialize the pool

        // **** Wrap the executor to propagate SecurityContext ****
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
