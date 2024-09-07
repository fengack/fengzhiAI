package com.fengzhi.ai.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;


@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private Integer port;
    private String password;

    @Bean
    public RedissonClient redisonClient() {
        Config config = new Config();
        String redisUrl = String.format("redis://%s:%s", host, String.valueOf(port));
        config.useSingleServer()
                .setAddress(redisUrl)
                .setPassword(password)
                .setDatabase(1)
                .setConnectionMinimumIdleSize(10);
        return Redisson.create(config);


    }
}
