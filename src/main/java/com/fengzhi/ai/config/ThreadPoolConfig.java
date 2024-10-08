package com.fengzhi.ai.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "myai.threadpool")
@Data
public class ThreadPoolConfig {
    private int corePoolSize;
    private int maximumPoolSize;
    private int keepAliveTime;
    private int queueSize;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {

        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count);
                count++;
                return thread;
            }
        };

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize),threadFactory);

        return threadPoolExecutor;
    }
}
